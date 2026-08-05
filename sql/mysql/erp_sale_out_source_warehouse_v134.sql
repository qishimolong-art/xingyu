-- 销售单明细记录跨部门调拨前的来源仓库与来源部门（v134）。
-- MySQL 5.7 / 8.0 兼容，可重复执行。
-- 不回填历史数据：同产品、同直发仓可能对应多个来源仓库，自动推断存在误写风险。

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

DROP PROCEDURE IF EXISTS add_erp_sale_out_source_warehouse_v134;

DELIMITER //
CREATE PROCEDURE add_erp_sale_out_source_warehouse_v134()
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out_items'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_sale_out_items'
          AND COLUMN_NAME = 'source_warehouse_id'
    ) THEN
        ALTER TABLE `erp_sale_out_items`
            ADD COLUMN `source_warehouse_id` BIGINT DEFAULT NULL
                COMMENT '跨部门销售调拨前的来源仓库编号' AFTER `warehouse_id`;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.TABLES
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_out_items'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'erp_sale_out_items'
          AND COLUMN_NAME = 'source_dept_id'
    ) THEN
        ALTER TABLE `erp_sale_out_items`
            ADD COLUMN `source_dept_id` BIGINT DEFAULT NULL
                COMMENT '跨部门销售调拨前的来源部门快照' AFTER `source_warehouse_id`;
    END IF;
END //
DELIMITER ;

CALL add_erp_sale_out_source_warehouse_v134();
DROP PROCEDURE IF EXISTS add_erp_sale_out_source_warehouse_v134;

-- 更新已有“仓库”字段的展示名称，不改变字段键及既有角色隐藏配置。
UPDATE `system_field_definition`
SET `field_label` = '出库仓库',
    `updater` = '1',
    `update_time` = NOW()
WHERE `module` = 'erp_sale_out'
  AND `field_key` = 'item_warehouseName'
  AND `deleted` = b'0'
  AND `field_label` <> '出库仓库';

-- 为每个现有租户新增来源仓库字段定义；不创建或覆盖角色字段权限。
INSERT INTO `system_field_definition`
(`module`, `field_key`, `field_label`, `field_group`, `sort`,
 `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT 'erp_sale_out', 'item_sourceWarehouseName', '来源仓库', 'detail_item', 525,
       '1', NOW(), '1', NOW(), b'0', tenant.`id`
FROM `system_tenant` tenant
WHERE tenant.`deleted` = b'0'
  AND NOT EXISTS (
      SELECT 1
      FROM `system_field_definition` field_definition
      WHERE field_definition.`module` = 'erp_sale_out'
        AND field_definition.`field_key` = 'item_sourceWarehouseName'
        AND field_definition.`tenant_id` = tenant.`id`
        AND field_definition.`deleted` = b'0'
  );
