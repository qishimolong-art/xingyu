-- Add gift flag support for sale quote and sale cart item details.

ALTER TABLE `erp_sale_quote_items`
    ADD COLUMN `gift_flag` TINYINT(1) DEFAULT 0 COMMENT '是否赠品' AFTER `count`;

ALTER TABLE `erp_sale_cart_items`
    ADD COLUMN `gift_flag` TINYINT(1) DEFAULT 0 COMMENT '是否赠品' AFTER `count`;

INSERT INTO `system_field_definition`
(`module`, `field_key`, `field_label`, `field_group`, `sort`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
('erp_sale_quote', 'item_giftFlag', '是否为赠品', 'detail_item', 425, '1', NOW(), '1', NOW(), b'0', 1),
('erp_sale_cart', 'item_giftFlag', '是否为赠品', 'detail_item', 425, '1', NOW(), '1', NOW(), b'0', 1)
ON DUPLICATE KEY UPDATE
  `field_label` = VALUES(`field_label`),
  `field_group` = VALUES(`field_group`),
  `sort` = VALUES(`sort`),
  `updater` = VALUES(`updater`),
  `update_time` = VALUES(`update_time`),
  `deleted` = b'0';
