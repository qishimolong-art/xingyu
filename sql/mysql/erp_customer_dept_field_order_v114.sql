-- ERP customer department fields order (v114).
-- Safe to rerun. Only adjusts the display sort for the two customer department fields.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

UPDATE `erp_field_config`
   SET `sort` = CASE `field_name`
        WHEN 'allowMultiDept' THEN 181
        WHEN 'deptIds' THEN 182
        ELSE `sort`
       END,
       `updater` = '1',
       `update_time` = NOW()
 WHERE `module_key` = 'customer'
   AND `field_name` IN ('allowMultiDept', 'deptIds')
   AND `deleted` = b'0';

UPDATE `system_field_definition`
   SET `sort` = CASE `field_key`
        WHEN 'allowMultiDept' THEN 181
        WHEN 'deptIds' THEN 182
        ELSE `sort`
       END,
       `updater` = '1',
       `update_time` = NOW()
 WHERE `module` = 'erp_customer'
   AND `field_key` IN ('allowMultiDept', 'deptIds')
   AND `deleted` = b'0';
