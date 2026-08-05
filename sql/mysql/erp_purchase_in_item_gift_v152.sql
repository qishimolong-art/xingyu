-- 采购入库明细增加赠品标识，并从关联采购订单明细回填历史赠品数据。
-- 仅在字段首次创建时回填，避免重复执行脚本覆盖后续人工调整。
DROP PROCEDURE IF EXISTS `erp_purchase_in_item_gift_v152_apply`;

DELIMITER //

CREATE PROCEDURE `erp_purchase_in_item_gift_v152_apply`()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM `information_schema`.`COLUMNS`
        WHERE `TABLE_SCHEMA` = DATABASE()
          AND `TABLE_NAME` = 'erp_purchase_in_items'
          AND `COLUMN_NAME` = 'gift'
    ) THEN
        ALTER TABLE `erp_purchase_in_items`
            ADD COLUMN `gift` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否赠品（0=否，1=是）'
                AFTER `product_price`;

        UPDATE `erp_purchase_in_items` AS `purchase_in_item`
            INNER JOIN `erp_purchase_order_items` AS `purchase_order_item`
                ON `purchase_order_item`.`id` = `purchase_in_item`.`order_item_id`
        SET `purchase_in_item`.`gift` = COALESCE(`purchase_order_item`.`gift`, 0)
        WHERE `purchase_in_item`.`order_item_id` IS NOT NULL;
    END IF;
END //

DELIMITER ;

CALL `erp_purchase_in_item_gift_v152_apply`();
DROP PROCEDURE IF EXISTS `erp_purchase_in_item_gift_v152_apply`;
