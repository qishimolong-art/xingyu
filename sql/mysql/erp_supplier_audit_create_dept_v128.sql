-- 供应商档案审计创建部门与业务分配部门拆分（v128）。
-- MySQL 5.7 / 8.0 兼容，脚本可重复执行。
-- 本脚本不删除、不覆盖任何菜单、角色或字段权限关系。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS add_erp_supplier_create_dept_v128;

DELIMITER //
CREATE PROCEDURE add_erp_supplier_create_dept_v128()
BEGIN
    IF EXISTS (
        SELECT 1
          FROM information_schema.TABLES
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = 'erp_supplier'
    ) AND NOT EXISTS (
        SELECT 1
          FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = 'erp_supplier'
           AND COLUMN_NAME = 'create_dept_id'
    ) THEN
        ALTER TABLE `erp_supplier`
            ADD COLUMN `create_dept_id` BIGINT DEFAULT NULL COMMENT '创建时所在部门编号' AFTER `dept_id`;
    END IF;

    IF EXISTS (
        SELECT 1
          FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = 'erp_supplier'
           AND COLUMN_NAME = 'create_dept_id'
    ) AND NOT EXISTS (
        SELECT 1
          FROM information_schema.STATISTICS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = 'erp_supplier'
           AND INDEX_NAME = 'idx_create_dept_id'
    ) THEN
        CREATE INDEX `idx_create_dept_id` ON `erp_supplier` (`create_dept_id`);
    END IF;
END //
DELIMITER ;

CALL add_erp_supplier_create_dept_v128();
DROP PROCEDURE IF EXISTS add_erp_supplier_create_dept_v128;

-- 历史供应商优先按创建人当前主部门回填；创建人不可解析时，
-- 再使用供应商现有业务部门作为兼容值。已有创建部门的记录永不覆盖。
UPDATE `erp_supplier` supplier
LEFT JOIN `system_users` creator_user
       ON supplier.`creator` REGEXP '^[0-9]+$'
      AND CAST(supplier.`creator` AS UNSIGNED) = creator_user.`id`
      AND supplier.`tenant_id` = creator_user.`tenant_id`
   SET supplier.`create_dept_id` = COALESCE(creator_user.`dept_id`, supplier.`dept_id`)
 WHERE supplier.`create_dept_id` IS NULL
   AND supplier.`deleted` = b'0'
   AND COALESCE(creator_user.`dept_id`, supplier.`dept_id`) IS NOT NULL;
