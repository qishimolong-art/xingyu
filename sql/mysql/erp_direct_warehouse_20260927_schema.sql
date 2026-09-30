-- 第一步：停写后执行；DDL 会隐式提交，不能依赖 ROLLBACK 撤销。
-- 先在当前连接设置 SET @dw_schema = '目标数据库名'; 并 USE 对应数据库。
-- mysql 客户端禁止使用 --force；任一错误立即停止后续脚本。
SET NAMES utf8mb4;
DROP PROCEDURE IF EXISTS migrate_direct_warehouse_schema_20260927;
DELIMITER $$
CREATE PROCEDURE migrate_direct_warehouse_schema_20260927()
BEGIN
    DECLARE name_length BIGINT;
    DECLARE name_charset VARCHAR(64);
    DECLARE name_collation VARCHAR(64);
    IF @dw_schema IS NULL OR BINARY DATABASE() <> BINARY @dw_schema THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Set dw_schema to the reviewed target database';
    END IF;
    SELECT character_maximum_length,character_set_name,collation_name INTO name_length,name_charset,name_collation
      FROM information_schema.columns
     WHERE table_schema = DATABASE() AND table_name = 'erp_warehouse'
       AND column_name = 'name' AND data_type = 'varchar';
    IF name_length IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Unexpected warehouse name schema';
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
                   AND table_name = 'erp_warehouse' AND column_name = 'direct_warehouse') THEN
        ALTER TABLE erp_warehouse ADD COLUMN direct_warehouse bit(1) NOT NULL DEFAULT b'0'
            COMMENT '是否直发仓，由系统维护';
    ELSEIF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = DATABASE()
                   AND table_name = 'erp_warehouse' AND column_name = 'direct_warehouse'
                   AND data_type = 'bit' AND is_nullable = 'NO' AND column_default = 'b''0''') THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Unexpected direct_warehouse schema; inspect before continuing';
    END IF;
    IF name_length < 64 THEN
        SET @dw_name_ddl = CONCAT('ALTER TABLE erp_warehouse MODIFY COLUMN name varchar(64) CHARACTER SET ',
            name_charset,' COLLATE ',name_collation,' NOT NULL COMMENT ''仓库名称''');
        PREPARE dw_name_stmt FROM @dw_name_ddl;
        EXECUTE dw_name_stmt;
        DEALLOCATE PREPARE dw_name_stmt;
    END IF;
    -- 永久保留首次迁移的原值；重新执行不得覆盖备份。
    CREATE TABLE IF NOT EXISTS erp_direct_warehouse_backup_20260927 (
        tenant_id bigint NOT NULL,
        warehouse_id bigint NOT NULL,
        dept_id bigint NOT NULL,
        old_name varchar(255) NOT NULL,
        old_direct_warehouse bit(1) NOT NULL,
        migrated_name varchar(255) NOT NULL,
        PRIMARY KEY (tenant_id, warehouse_id)
    ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='直发仓改名迁移原值备份';
END$$
DELIMITER ;
CALL migrate_direct_warehouse_schema_20260927();
DROP PROCEDURE migrate_direct_warehouse_schema_20260927;
