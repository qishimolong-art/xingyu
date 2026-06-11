-- ERP stock record business type dictionary patch (v58)
-- Scope:
--   1. Add missing purchase price adjustment dictionary values used by backend stock records.
--   2. Add sale price adjustment dictionary value for stock record list display.
--
-- Safety:
--   - Idempotent: inserts only when the active dict_type + value does not exist.
--   - Does not delete or overwrite existing dictionary/menu/permission data.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO `system_dict_data`
(`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 90, CONVERT(0xE98787E8B4ADE8B083E4BBB7 USING utf8mb4), '90', 'erp_stock_record_biz_type', 0, '', '', '', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (
    SELECT 1
    FROM `system_dict_data`
    WHERE `dict_type` = 'erp_stock_record_biz_type'
      AND `value` = '90'
      AND `deleted` = b'0'
);

INSERT INTO `system_dict_data`
(`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 91, CONVERT(0xE98787E8B4ADE8B083E4BBB7EFBC88E4BD9CE5BA9FEFBC89 USING utf8mb4), '91', 'erp_stock_record_biz_type', 0, 'danger', '', '', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (
    SELECT 1
    FROM `system_dict_data`
    WHERE `dict_type` = 'erp_stock_record_biz_type'
      AND `value` = '91'
      AND `deleted` = b'0'
);

INSERT INTO `system_dict_data`
(`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 92, CONVERT(0xE99480E594AEE8B083E4BBB7 USING utf8mb4), '92', 'erp_stock_record_biz_type', 0, '', '', '', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (
    SELECT 1
    FROM `system_dict_data`
    WHERE `dict_type` = 'erp_stock_record_biz_type'
      AND `value` = '92'
      AND `deleted` = b'0'
);
