-- 00G 库存实有父行流水游标。MySQL >= 8.0.29；须暂停库存写入，DDL 非整脚本原子。
-- 仅在进入双成本准备/启用流程时执行：期初核对需要用该游标锁定旧库存流水水位，默认关闭的只读报表不依赖本脚本。
-- 仅从实际流水派生定位，不改数量、成本、权限或业务证据；不自动修复脏游标。
-- 在支持 DELIMITER 的客户端执行；脚本错误后停止，不使用 --force。
DELIMITER $$
CREATE PROCEDURE erp_stock_record_cursor_20260909_check()
BEGIN
 DECLARE column_count INT DEFAULT 0;
 DECLARE definition_count INT DEFAULT 0;
 DECLARE invalid_count BIGINT DEFAULT 0;
 DECLARE expected_insert LONGTEXT;
 DECLARE expected_guard LONGTEXT;
 SET expected_insert='UPDATE erp_stock SET legacy_record_cursor_id=GREATEST(legacy_record_cursor_id,NEW.id) WHERE tenant_id=NEW.tenant_id AND product_id=NEW.product_id AND warehouse_id=NEW.warehouse_id AND deleted=b\'0\' AND NEW.deleted=b\'0\'';
 SET expected_guard='BEGIN IF NEW.legacy_record_cursor_id<OLD.legacy_record_cursor_id THEN SIGNAL SQLSTATE \'45000\' SET MESSAGE_TEXT=\'stock record cursor cannot decrease\'; END IF; END';
 SELECT COUNT(*) INTO invalid_count FROM (
   SELECT tenant_id,product_id,warehouse_id FROM erp_stock WHERE deleted=b'0'
   GROUP BY tenant_id,product_id,warehouse_id HAVING COUNT(*)>1
 ) duplicate_dimensions;
 IF invalid_count>0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='duplicate active stock dimensions; reconcile before cursor migration'; END IF;
 SELECT COUNT(*) INTO definition_count FROM information_schema.TRIGGERS
 WHERE TRIGGER_SCHEMA=DATABASE() AND TRIGGER_NAME='erp_stock_record_cursor_ai';
 IF definition_count>0 THEN
   SELECT COUNT(*) INTO definition_count FROM information_schema.TRIGGERS
   WHERE TRIGGER_SCHEMA=DATABASE() AND TRIGGER_NAME='erp_stock_record_cursor_ai'
     AND EVENT_OBJECT_TABLE='erp_stock_record' AND ACTION_TIMING='AFTER' AND EVENT_MANIPULATION='INSERT'
     AND ACTION_ORIENTATION='ROW'
     AND ACTION_ORDER=(SELECT MAX(other.ACTION_ORDER) FROM information_schema.TRIGGERS other
       WHERE other.TRIGGER_SCHEMA=DATABASE() AND other.EVENT_OBJECT_TABLE='erp_stock_record'
         AND other.ACTION_TIMING='AFTER' AND other.EVENT_MANIPULATION='INSERT')
     AND LOWER(REGEXP_REPLACE(REPLACE(ACTION_STATEMENT,'`',''),'[[:space:]]',''))
       =LOWER(REGEXP_REPLACE(expected_insert,'[[:space:]]',''));
   IF definition_count<>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='erp_stock_record_cursor_ai definition conflict; not overwritten'; END IF;
 END IF;
 SELECT COUNT(*) INTO definition_count FROM information_schema.TRIGGERS
 WHERE TRIGGER_SCHEMA=DATABASE() AND TRIGGER_NAME='erp_stock_record_cursor_bu';
 IF definition_count>0 THEN
   SELECT COUNT(*) INTO definition_count FROM information_schema.TRIGGERS
   WHERE TRIGGER_SCHEMA=DATABASE() AND TRIGGER_NAME='erp_stock_record_cursor_bu'
     AND EVENT_OBJECT_TABLE='erp_stock' AND ACTION_TIMING='BEFORE' AND EVENT_MANIPULATION='UPDATE'
     AND ACTION_ORIENTATION='ROW'
     AND ACTION_ORDER=(SELECT MAX(other.ACTION_ORDER) FROM information_schema.TRIGGERS other
       WHERE other.TRIGGER_SCHEMA=DATABASE() AND other.EVENT_OBJECT_TABLE='erp_stock'
         AND other.ACTION_TIMING='BEFORE' AND other.EVENT_MANIPULATION='UPDATE')
     AND LOWER(REGEXP_REPLACE(REPLACE(ACTION_STATEMENT,'`',''),'[[:space:]]',''))
       =LOWER(REGEXP_REPLACE(expected_guard,'[[:space:]]',''));
   IF definition_count<>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='erp_stock_record_cursor_bu definition conflict; not overwritten'; END IF;
 END IF;
 SELECT COUNT(*) INTO column_count FROM information_schema.COLUMNS
 WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='erp_stock' AND COLUMN_NAME='legacy_record_cursor_id';
 IF column_count=0 THEN
   ALTER TABLE erp_stock ADD COLUMN legacy_record_cursor_id BIGINT NOT NULL DEFAULT 0 COMMENT '实际有效库存流水最大ID，触发器同事务维护';
   UPDATE erp_stock s LEFT JOIN (
     SELECT tenant_id,product_id,warehouse_id,MAX(id) AS record_id FROM erp_stock_record WHERE deleted=b'0'
     GROUP BY tenant_id,product_id,warehouse_id
   ) r ON r.tenant_id=s.tenant_id AND r.product_id=s.product_id AND r.warehouse_id=s.warehouse_id
   SET s.legacy_record_cursor_id=COALESCE(r.record_id,0) WHERE s.deleted=b'0';
 ELSE
   SELECT COUNT(*) INTO definition_count FROM information_schema.COLUMNS
   WHERE TABLE_SCHEMA=DATABASE() AND TABLE_NAME='erp_stock' AND COLUMN_NAME='legacy_record_cursor_id'
     AND DATA_TYPE='bigint' AND COLUMN_TYPE NOT LIKE '%unsigned%' AND IS_NULLABLE='NO'
     AND COLUMN_DEFAULT='0' AND COALESCE(GENERATION_EXPRESSION,'')='';
   IF definition_count<>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='legacy_record_cursor_id column definition conflict; not overwritten'; END IF;
   SELECT COUNT(*) INTO invalid_count FROM erp_stock s LEFT JOIN (
     SELECT tenant_id,product_id,warehouse_id,MAX(id) AS record_id FROM erp_stock_record WHERE deleted=b'0'
     GROUP BY tenant_id,product_id,warehouse_id
   ) r ON r.tenant_id=s.tenant_id AND r.product_id=s.product_id AND r.warehouse_id=s.warehouse_id
   WHERE s.deleted=b'0' AND s.legacy_record_cursor_id<>COALESCE(r.record_id,0);
   IF invalid_count>0 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='existing stock cursor differs from actual history; reconcile without automatic reset'; END IF;
 END IF;
 -- 展示其他名称的相关触发器，供迁移审查执行顺序；不删除或宣称覆盖管理员逻辑。
 SELECT TRIGGER_NAME,EVENT_OBJECT_TABLE,ACTION_TIMING,EVENT_MANIPULATION,ACTION_ORDER,ACTION_STATEMENT
 FROM information_schema.TRIGGERS WHERE TRIGGER_SCHEMA=DATABASE()
   AND EVENT_OBJECT_TABLE IN ('erp_stock','erp_stock_record')
   AND TRIGGER_NAME NOT IN ('erp_stock_record_cursor_ai','erp_stock_record_cursor_bu');
END$$
CALL erp_stock_record_cursor_20260909_check()$$
DROP PROCEDURE erp_stock_record_cursor_20260909_check$$
CREATE TRIGGER IF NOT EXISTS erp_stock_record_cursor_ai AFTER INSERT ON erp_stock_record FOR EACH ROW
UPDATE erp_stock SET legacy_record_cursor_id=GREATEST(legacy_record_cursor_id,NEW.id)
WHERE tenant_id=NEW.tenant_id AND product_id=NEW.product_id AND warehouse_id=NEW.warehouse_id
 AND deleted=b'0' AND NEW.deleted=b'0'$$
CREATE TRIGGER IF NOT EXISTS erp_stock_record_cursor_bu BEFORE UPDATE ON erp_stock FOR EACH ROW
BEGIN
 IF NEW.legacy_record_cursor_id<OLD.legacy_record_cursor_id THEN
   SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='stock record cursor cannot decrease';
 END IF;
END$$
DELIMITER ;
