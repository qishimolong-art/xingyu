-- 第二步：保持停写，在新版后端启动前执行。先执行 schema.sql。
-- 必须设置 @dw_schema、@dw_tenant_id、@dw_expected_count、@dw_ids（已审查仓库 ID 的 JSON 数组）。
-- 2026-09-27 只读预检：租户 1，7 条，JSON_ARRAY(7,21,22,23,25,27,28)。执行前重新确认。
-- 仅迁移明确列出的 ID；不按名称后缀推断身份。事务内备份、更新、核验后提交。
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS migrate_direct_warehouse_data_20260927;
DELIMITER $$
CREATE PROCEDURE migrate_direct_warehouse_data_20260927()
BEGIN
    DECLARE expected_rows INT;
    DECLARE changed_rows INT;
    DECLARE name_length BIGINT;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
    IF @dw_schema IS NULL OR BINARY DATABASE() <> BINARY @dw_schema
       OR @dw_tenant_id IS NULL OR @dw_expected_count IS NULL OR @dw_expected_count <= 0
       OR @dw_ids IS NULL OR JSON_TYPE(@dw_ids) <> 'ARRAY' THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Set reviewed database, tenant, expected count and ID array';
    END IF;
    SET expected_rows = @dw_expected_count;
    DROP TEMPORARY TABLE IF EXISTS tmp_direct_warehouse_ids_20260927;
    CREATE TEMPORARY TABLE tmp_direct_warehouse_ids_20260927 (id bigint NOT NULL PRIMARY KEY);
    START TRANSACTION;
    INSERT INTO tmp_direct_warehouse_ids_20260927
        SELECT id FROM JSON_TABLE(@dw_ids, '$[*]' COLUMNS(id bigint PATH '$' ERROR ON ERROR)) ids;
    IF (SELECT COUNT(*) FROM tmp_direct_warehouse_ids_20260927) <> expected_rows THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Reviewed ID count mismatch';
    END IF;
    IF EXISTS (SELECT 1 FROM erp_warehouse w WHERE w.tenant_id=@dw_tenant_id AND w.deleted=b'0'
                 AND (BINARY w.name=BINARY '直发仓' OR BINARY w.remark=BINARY '系统自动创建：销售手推车跨部门调拨专用直发仓')
                 AND w.direct_warehouse=b'0' AND w.id NOT IN (SELECT id FROM tmp_direct_warehouse_ids_20260927)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Unreviewed historical candidates exist; refresh ID list';
    END IF;
    -- 精确名称为旧系统识别规则；固定备注用于保留已自定义名称的自动直发仓。
    IF (SELECT COUNT(*) FROM erp_warehouse w JOIN tmp_direct_warehouse_ids_20260927 ids ON ids.id=w.id
         WHERE w.tenant_id=@dw_tenant_id AND w.deleted=b'0'
           AND (BINARY w.name=BINARY '直发仓'
                OR BINARY w.remark=BINARY '系统自动创建：销售手推车跨部门调拨专用直发仓'
                OR EXISTS (SELECT 1 FROM erp_direct_warehouse_backup_20260927 b
                           WHERE b.tenant_id=w.tenant_id AND b.warehouse_id=w.id))) <> expected_rows THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Candidate missing, deleted, wrong tenant or no longer eligible';
    END IF;
    SELECT character_maximum_length INTO name_length FROM information_schema.columns
      WHERE table_schema=DATABASE() AND table_name='erp_warehouse' AND column_name='name';
    IF EXISTS (SELECT 1 FROM erp_warehouse w JOIN tmp_direct_warehouse_ids_20260927 ids ON ids.id=w.id
               LEFT JOIN system_dept d ON d.id=w.dept_id AND d.tenant_id=w.tenant_id AND d.deleted=b'0'
               WHERE w.tenant_id=@dw_tenant_id AND w.deleted=b'0'
                 AND (d.id IS NULL OR CHAR_LENGTH(TRIM(d.name))=0
                      OR (BINARY w.name=BINARY '直发仓' AND CHAR_LENGTH(CONCAT(TRIM(d.name),'直发仓'))>name_length))) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Invalid department or generated warehouse name too long';
    END IF;
    IF EXISTS (SELECT 1 FROM erp_warehouse w WHERE w.tenant_id=@dw_tenant_id AND w.deleted=b'0' AND w.status=0
                 AND (w.direct_warehouse=b'1' OR w.id IN (SELECT id FROM tmp_direct_warehouse_ids_20260927))
               GROUP BY w.dept_id HAVING COUNT(*)>1) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Multiple enabled direct warehouses in one department';
    END IF;
    -- 重跑前检查后续人工变更，避免旧备份覆盖新业务数据。
    IF EXISTS (SELECT 1 FROM erp_warehouse w JOIN tmp_direct_warehouse_ids_20260927 ids ON ids.id=w.id
               JOIN erp_direct_warehouse_backup_20260927 b ON b.tenant_id=w.tenant_id AND b.warehouse_id=w.id
               WHERE w.tenant_id=@dw_tenant_id AND (NOT (w.dept_id <=> b.dept_id)
                 OR NOT ((BINARY w.name=BINARY b.old_name AND w.direct_warehouse=b.old_direct_warehouse)
                      OR (BINARY w.name=BINARY b.migrated_name AND w.direct_warehouse=b'1')))) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Warehouse changed since backup; review before retry';
    END IF;
    INSERT INTO erp_direct_warehouse_backup_20260927
        (tenant_id,warehouse_id,dept_id,old_name,old_direct_warehouse,migrated_name)
        SELECT w.tenant_id,w.id,w.dept_id,w.name,w.direct_warehouse,
               IF(BINARY w.name=BINARY '直发仓',CONCAT(TRIM(d.name),'直发仓'),w.name)
          FROM erp_warehouse w JOIN tmp_direct_warehouse_ids_20260927 ids ON ids.id=w.id
          JOIN system_dept d ON d.id=w.dept_id AND d.tenant_id=w.tenant_id AND d.deleted=b'0'
         WHERE w.tenant_id=@dw_tenant_id AND w.deleted=b'0'
           AND NOT EXISTS (SELECT 1 FROM erp_direct_warehouse_backup_20260927 b
                            WHERE b.tenant_id=w.tenant_id AND b.warehouse_id=w.id);
    UPDATE erp_warehouse w JOIN tmp_direct_warehouse_ids_20260927 ids ON ids.id=w.id
      JOIN erp_direct_warehouse_backup_20260927 b ON b.tenant_id=w.tenant_id AND b.warehouse_id=w.id
       SET w.name=b.migrated_name,w.direct_warehouse=b'1',w.update_time=w.update_time
     WHERE w.tenant_id=@dw_tenant_id AND w.deleted=b'0' AND w.dept_id=b.dept_id
       AND (BINARY w.name<>BINARY b.migrated_name OR w.direct_warehouse<>b'1');
    SET changed_rows=ROW_COUNT();
    IF (SELECT COUNT(*) FROM erp_warehouse w JOIN tmp_direct_warehouse_ids_20260927 ids ON ids.id=w.id
         JOIN erp_direct_warehouse_backup_20260927 b ON b.tenant_id=w.tenant_id AND b.warehouse_id=w.id
         WHERE w.tenant_id=@dw_tenant_id AND w.deleted=b'0' AND w.dept_id=b.dept_id
           AND BINARY w.name=BINARY b.migrated_name AND w.direct_warehouse=b'1') <> expected_rows THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Postcheck failed; data changes rolled back';
    END IF;
    COMMIT;
    SELECT changed_rows AS changed_rows,expected_rows AS verified_rows;
    DROP TEMPORARY TABLE tmp_direct_warehouse_ids_20260927;
END$$
DELIMITER ;
CALL migrate_direct_warehouse_data_20260927();
DROP PROCEDURE migrate_direct_warehouse_data_20260927;
