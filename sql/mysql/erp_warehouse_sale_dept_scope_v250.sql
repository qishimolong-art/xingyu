-- v250: 仓库销售部门授权范围。
-- 普通仓库默认全部部门可用；系统直发仓默认仅所属部门及指定授权部门可用。
-- MySQL 5.7 / 8.0 兼容。重复执行不会覆盖上线后用户手动修改的授权范围。
-- DDL 会隐式提交；执行前请停写并确认 @warehouse_scope_schema 与当前数据库完全一致。

SET NAMES utf8mb4;

DROP PROCEDURE IF EXISTS migrate_warehouse_sale_dept_scope_v250;
DELIMITER $$
CREATE PROCEDURE migrate_warehouse_sale_dept_scope_v250()
BEGIN
    DECLARE column_exists INT DEFAULT 0;

    IF @warehouse_scope_schema IS NULL OR BINARY DATABASE() <> BINARY @warehouse_scope_schema THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Set warehouse_scope_schema to the reviewed target database';
    END IF;

    IF NOT EXISTS (
        SELECT 1
          FROM information_schema.tables
         WHERE table_schema = DATABASE()
           AND table_name = 'erp_warehouse'
           AND table_type = 'BASE TABLE'
    ) THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'erp_warehouse does not exist';
    END IF;

    SELECT COUNT(*) INTO column_exists
      FROM information_schema.columns
     WHERE table_schema = DATABASE()
       AND table_name = 'erp_warehouse'
       AND column_name = 'sale_dept_scope_type';

    IF column_exists = 0 THEN
        ALTER TABLE erp_warehouse
            ADD COLUMN sale_dept_scope_type varchar(16) NOT NULL DEFAULT 'SPECIFIED'
            COMMENT '销售部门授权范围：ALL-全部部门，SPECIFIED-指定部门';

        UPDATE erp_warehouse
           SET sale_dept_scope_type = 'ALL'
         WHERE deleted = b'0'
           AND direct_warehouse = b'0';

        UPDATE erp_warehouse
           SET sale_dept_scope_type = 'SPECIFIED'
         WHERE deleted = b'0'
           AND direct_warehouse = b'1';
    ELSEIF NOT EXISTS (
        SELECT 1
          FROM information_schema.columns
         WHERE table_schema = DATABASE()
           AND table_name = 'erp_warehouse'
           AND column_name = 'sale_dept_scope_type'
           AND data_type = 'varchar'
           AND character_maximum_length = 16
           AND is_nullable = 'NO'
           AND column_default = 'SPECIFIED'
    ) THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Unexpected sale_dept_scope_type schema; inspect before continuing';
    END IF;
END$$
DELIMITER ;

CALL migrate_warehouse_sale_dept_scope_v250();
DROP PROCEDURE migrate_warehouse_sale_dept_scope_v250;

SELECT sale_dept_scope_type, direct_warehouse, COUNT(*) AS warehouse_count
  FROM erp_warehouse
 WHERE deleted = b'0'
 GROUP BY sale_dept_scope_type, direct_warehouse
 ORDER BY direct_warehouse, sale_dept_scope_type;

SELECT COUNT(*) AS invalid_scope_count
  FROM erp_warehouse
 WHERE deleted = b'0'
   AND sale_dept_scope_type NOT IN ('ALL', 'SPECIFIED');
