-- 配件档案审计创建部门与业务分配部门拆分（v127）。
-- MySQL 5.7 / 8.0 兼容，脚本可重复执行。
-- 本脚本不删除、不覆盖任何菜单、角色或字段权限关系。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS add_erp_product_create_dept_v127;

DELIMITER //
CREATE PROCEDURE add_erp_product_create_dept_v127()
BEGIN
    IF EXISTS (
        SELECT 1
          FROM information_schema.TABLES
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = 'erp_product'
    ) AND NOT EXISTS (
        SELECT 1
          FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = 'erp_product'
           AND COLUMN_NAME = 'create_dept_id'
    ) THEN
        ALTER TABLE `erp_product`
            ADD COLUMN `create_dept_id` BIGINT DEFAULT NULL COMMENT '创建时所在部门编号' AFTER `dept_id`;
    END IF;

    IF EXISTS (
        SELECT 1
          FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = 'erp_product'
           AND COLUMN_NAME = 'create_dept_id'
    ) AND NOT EXISTS (
        SELECT 1
          FROM information_schema.STATISTICS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = 'erp_product'
           AND INDEX_NAME = 'idx_create_dept_id'
    ) THEN
        CREATE INDEX `idx_create_dept_id` ON `erp_product` (`create_dept_id`);
    END IF;
END //
DELIMITER ;

CALL add_erp_product_create_dept_v127();
DROP PROCEDURE IF EXISTS add_erp_product_create_dept_v127;

-- 历史配件优先使用创建人当前的主部门回填；创建人不可解析时，
-- 再使用配件现有部门作为可用的兼容值。已有创建部门的记录永不覆盖。
UPDATE `erp_product` product
LEFT JOIN `system_users` creator_user
       ON product.`creator` REGEXP '^[0-9]+$'
      AND CAST(product.`creator` AS UNSIGNED) = creator_user.`id`
      AND product.`tenant_id` = creator_user.`tenant_id`
   SET product.`create_dept_id` = COALESCE(creator_user.`dept_id`, product.`dept_id`)
 WHERE product.`create_dept_id` IS NULL
   AND product.`deleted` = b'0'
   AND COALESCE(creator_user.`dept_id`, product.`dept_id`) IS NOT NULL;

-- 同步所有租户的字段配置与字段权限定义展示名，
-- 不改变字段键、可见性或授权关系。
UPDATE `erp_field_config`
   SET `field_label` = '开启批次号',
       `updater` = '1',
       `update_time` = NOW()
 WHERE `module_key` = 'erp_product'
   AND `field_name` = 'batchNoEnabled'
   AND `deleted` = b'0';

UPDATE `system_field_definition`
   SET `field_label` = '开启批次号',
       `updater` = '1',
       `update_time` = NOW()
 WHERE `module` = 'erp_product'
   AND `field_key` = 'batchNoEnabled'
   AND `deleted` = b'0';
