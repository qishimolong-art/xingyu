-- 回滚前停写，设置 @dw_schema、@dw_tenant_id、@dw_expected_count（首次备份条数）。
-- 仅恢复备份中的名称与标识；若仓库后来改名、换部门、删除，或出现新增直发仓，停止并人工核实。
-- 回退旧代码时保留新增列、64 字符容量和备份表，旧代码可兼容；不自动 DROP 或缩短字段。
-- 若必须恢复物理结构，需另行核验所有租户无标识依赖、名称均 <= 原长度后安排 DDL。
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS rollback_direct_warehouse_20260927;
DELIMITER $$
CREATE PROCEDURE rollback_direct_warehouse_20260927()
BEGIN
    DECLARE changed_rows INT;
    DECLARE EXIT HANDLER FOR SQLEXCEPTION BEGIN ROLLBACK; RESIGNAL; END;
    IF @dw_schema IS NULL OR BINARY DATABASE() <> BINARY @dw_schema OR @dw_tenant_id IS NULL
       OR @dw_expected_count IS NULL OR @dw_expected_count<=0 THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Set reviewed database, tenant and backup count';
    END IF;
    START TRANSACTION;
    IF (SELECT COUNT(*) FROM erp_direct_warehouse_backup_20260927 WHERE tenant_id=@dw_tenant_id) <> @dw_expected_count THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Backup count mismatch';
    END IF;
    IF EXISTS (SELECT 1 FROM erp_warehouse w WHERE w.tenant_id=@dw_tenant_id AND w.deleted=b'0'
                 AND w.direct_warehouse=b'1' AND NOT EXISTS (
                     SELECT 1 FROM erp_direct_warehouse_backup_20260927 b WHERE b.tenant_id=w.tenant_id AND b.warehouse_id=w.id)) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'New direct warehouses exist; reconcile before reverting old code';
    END IF;
    IF EXISTS (SELECT 1 FROM erp_direct_warehouse_backup_20260927 b
               LEFT JOIN erp_warehouse w ON w.id=b.warehouse_id AND w.tenant_id=b.tenant_id
               WHERE b.tenant_id=@dw_tenant_id AND (w.id IS NULL OR w.deleted<>b'0' OR NOT (w.dept_id <=> b.dept_id)
                 OR NOT ((BINARY w.name=BINARY b.migrated_name AND w.direct_warehouse=b'1')
                      OR (BINARY w.name=BINARY b.old_name AND w.direct_warehouse=b.old_direct_warehouse)))) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Warehouse changed since migration; rollback requires review';
    END IF;
    UPDATE erp_warehouse w JOIN erp_direct_warehouse_backup_20260927 b ON b.tenant_id=w.tenant_id AND b.warehouse_id=w.id
       SET w.name=b.old_name,w.direct_warehouse=b.old_direct_warehouse,w.update_time=w.update_time
     WHERE w.tenant_id=@dw_tenant_id AND w.deleted=b'0' AND w.dept_id=b.dept_id
       AND (BINARY w.name<>BINARY b.old_name OR w.direct_warehouse<>b.old_direct_warehouse);
    SET changed_rows=ROW_COUNT();
    IF (SELECT COUNT(*) FROM erp_warehouse w JOIN erp_direct_warehouse_backup_20260927 b
          ON b.tenant_id=w.tenant_id AND b.warehouse_id=w.id
         WHERE w.tenant_id=@dw_tenant_id AND w.deleted=b'0' AND BINARY w.name=BINARY b.old_name
           AND w.direct_warehouse=b.old_direct_warehouse AND w.dept_id=b.dept_id) <> @dw_expected_count THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Rollback postcheck failed';
    END IF;
    COMMIT;
    SELECT changed_rows AS restored_rows;
END$$
DELIMITER ;
CALL rollback_direct_warehouse_20260927();
DROP PROCEDURE rollback_direct_warehouse_20260927;
