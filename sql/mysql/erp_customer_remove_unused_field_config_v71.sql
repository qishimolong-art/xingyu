-- ERP customer unused field config cleanup v71.
-- Scope is limited to customer main form field config and erp_customer field permission definitions.
-- This script does not drop erp_customer table columns.

DELETE rfp
FROM `system_role_field_permission` rfp
JOIN `system_field_definition` fd ON fd.`id` = rfp.`field_id`
WHERE fd.`module` = 'erp_customer'
  AND fd.`field_key` IN (
      'oldCode',
      'foreignName',
      'companyNature',
      'fax',
      'deliveryArea',
      'arrivalPoint',
      'profitReferencePriceLevel',
      'ecommercePayment',
      'customerTag',
      'wechatService'
  );

UPDATE `system_field_definition`
SET `deleted` = b'1',
    `update_time` = NOW()
WHERE `module` = 'erp_customer'
  AND `field_key` IN (
      'oldCode',
      'foreignName',
      'companyNature',
      'fax',
      'deliveryArea',
      'arrivalPoint',
      'profitReferencePriceLevel',
      'ecommercePayment',
      'customerTag',
      'wechatService'
  )
  AND `deleted` = b'0';

UPDATE `erp_field_config`
SET `deleted` = b'1',
    `update_time` = NOW()
WHERE `module_key` = 'customer'
  AND `field_name` IN (
      'oldCode',
      'foreignName',
      'companyNature',
      'fax',
      'deliveryArea',
      'arrivalPoint',
      'profitReferencePriceLevel',
      'ecommercePayment',
      'customerTag',
      'wechatService'
  )
  AND `deleted` = b'0';
