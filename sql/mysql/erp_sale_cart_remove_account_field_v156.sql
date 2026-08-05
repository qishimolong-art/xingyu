-- Remove settlement account from sales cart create/edit/detail field metadata.
-- The business column remains for historical compatibility; only UI field
-- configuration and field-permission metadata are removed.

DELETE FROM `erp_field_config`
WHERE `module_key` = 'sale_cart'
  AND `field_name` = 'accountId';

DELETE FROM `system_field_permission`
WHERE `module` = 'erp_sale_cart'
  AND `field_key` = 'accountId';
