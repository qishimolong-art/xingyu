-- Make product barcode optional.
-- Reason:
--   Product save/import now treats blank barCode as NULL, so the table column
--   must allow NULL values in strict MySQL mode.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

ALTER TABLE `erp_product`
    MODIFY COLUMN `bar_code` VARCHAR(128) NULL DEFAULT NULL COMMENT '条形码';

UPDATE `erp_product`
   SET `bar_code` = NULL
 WHERE `deleted` = b'0'
   AND `bar_code` = '';

UPDATE `erp_field_config`
   SET `required` = b'0',
       `updater` = '1',
       `update_time` = NOW()
 WHERE `module_key` = 'erp_product'
   AND `field_name` = 'barCode'
   AND `deleted` = b'0';
