-- ERP customer archive: hide standalone phone fields by default (v167).
-- Safe to execute repeatedly. Scope is limited to active tenant-1 customer field config rows.

UPDATE `erp_field_config`
   SET `visible` = b'0',
       `update_time` = NOW()
 WHERE `module_key` = 'customer'
   AND `field_name` IN ('telephone', 'financeTelephone')
   AND `tenant_id` = 1
   AND `deleted` = b'0';
