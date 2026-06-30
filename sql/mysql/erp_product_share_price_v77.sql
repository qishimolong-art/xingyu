-- ERP product share price v77
-- Adds product-level share price used by cross-department stock move.

DROP PROCEDURE IF EXISTS add_erp_product_share_price_v77;

CREATE PROCEDURE add_erp_product_share_price_v77()
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = 'erp_product'
          AND column_name = 'share_price'
    ) THEN
        ALTER TABLE `erp_product`
            ADD COLUMN `share_price` DECIMAL(10, 2) NULL COMMENT '股份价' AFTER `wholesale_price`;
    END IF;
END;

CALL add_erp_product_share_price_v77();
DROP PROCEDURE IF EXISTS add_erp_product_share_price_v77;
