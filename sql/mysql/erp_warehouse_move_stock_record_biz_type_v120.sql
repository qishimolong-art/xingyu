-- ERP warehouse move stock record business type dictionary patch (v120)
-- Scope:
--   Add missing stock record business type dictionary values 34/35 for warehouse move records.
--
-- Safety:
--   - Idempotent: inserts only when active dict_type + value does not exist.
--   - Does not delete or overwrite existing dictionary/menu/permission data.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

INSERT INTO `system_dict_data`
(`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 34, '仓库移货入库', '34', 'erp_stock_record_biz_type', 0, 'success', '', '', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (
    SELECT 1
    FROM `system_dict_data`
    WHERE `dict_type` = 'erp_stock_record_biz_type'
      AND `value` = '34'
      AND `deleted` = b'0'
);

INSERT INTO `system_dict_data`
(`sort`, `label`, `value`, `dict_type`, `status`, `color_type`, `css_class`, `remark`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT 35, '仓库移货出库', '35', 'erp_stock_record_biz_type', 0, 'danger', '', '', '1', NOW(), '1', NOW(), b'0'
WHERE NOT EXISTS (
    SELECT 1
    FROM `system_dict_data`
    WHERE `dict_type` = 'erp_stock_record_biz_type'
      AND `value` = '35'
      AND `deleted` = b'0'
);
