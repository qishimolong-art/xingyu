-- 销售手推车快递单迁移至销售单（v131）。
-- MySQL 5.7 / 8.0 兼容，可重复执行。
-- 安全约束：保留销售手推车与销售单原 file_url，只回填目标空值，不删除或覆盖既有权限。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS add_erp_sale_out_express_file_v131;

DELIMITER //
CREATE PROCEDURE add_erp_sale_out_express_file_v131()
BEGIN
    IF EXISTS (
        SELECT 1
          FROM information_schema.TABLES
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = 'erp_sale_out'
    ) AND NOT EXISTS (
        SELECT 1
          FROM information_schema.COLUMNS
         WHERE TABLE_SCHEMA = DATABASE()
           AND TABLE_NAME = 'erp_sale_out'
           AND COLUMN_NAME = 'express_file_url'
    ) THEN
        ALTER TABLE `erp_sale_out`
            ADD COLUMN `express_file_url` VARCHAR(512) DEFAULT NULL COMMENT '快递单图片地址' AFTER `file_url`;
    END IF;
END //
DELIMITER ;

CALL add_erp_sale_out_express_file_v131();
DROP PROCEDURE IF EXISTS add_erp_sale_out_express_file_v131;

-- 仅迁移已由销售手推车生成、目标仍为空且租户一致的历史销售单。
UPDATE `erp_sale_out` sale_out
INNER JOIN `erp_sale_cart` sale_cart
        ON sale_cart.`id` = sale_out.`source_id`
       AND sale_cart.`tenant_id` = sale_out.`tenant_id`
       AND sale_cart.`deleted` = b'0'
SET sale_out.`express_file_url` = sale_cart.`file_url`
WHERE sale_out.`source_type` = 30
  AND sale_out.`deleted` = b'0'
  AND (sale_out.`express_file_url` IS NULL OR TRIM(sale_out.`express_file_url`) = '')
  AND sale_cart.`file_url` IS NOT NULL
  AND TRIM(sale_cart.`file_url`) <> '';

-- 为每个现有租户补充销售单快递单字段定义；不修改既有角色字段隐藏配置。
INSERT INTO `system_field_definition`
(`module`, `field_key`, `field_label`, `field_group`, `sort`,
 `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT 'erp_sale_out', 'expressFileUrl', '快递单', 'main_form', 120,
       '1', NOW(), '1', NOW(), b'0', tenant.`id`
FROM `system_tenant` tenant
WHERE tenant.`deleted` = b'0'
  AND NOT EXISTS (
      SELECT 1
      FROM `system_field_definition` field_definition
      WHERE field_definition.`module` = 'erp_sale_out'
        AND field_definition.`field_key` = 'expressFileUrl'
        AND field_definition.`tenant_id` = tenant.`id`
        AND field_definition.`deleted` = b'0'
  );

-- 新增独立按钮权限，不替换销售单原查询、更新、审批或导出权限，也不自动改写角色授权。
SET @sale_out_menu_id := (
    SELECT menu.`id`
    FROM `system_menu` menu
    WHERE menu.`component` = 'erp/sale/out/index'
      AND menu.`deleted` = b'0'
    ORDER BY menu.`id`
    LIMIT 1
);

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '销售单上传快递单', 'erp:sale-out:upload-express', 3, 7, @sale_out_menu_id,
       '', '', '', NULL, 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @sale_out_menu_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1
      FROM `system_menu` menu
      WHERE menu.`permission` = 'erp:sale-out:upload-express'
        AND menu.`deleted` = b'0'
  );
