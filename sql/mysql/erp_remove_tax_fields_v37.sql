-- Remove ERP business document tax fields from field-permission configuration.
-- Scope: purchase/sale business documents only. Customer and supplier master data are intentionally excluded.

SET @tax_modules = 'erp_purchase_order,erp_purchase_in,erp_purchase_return,erp_purchase_invoice,erp_sale_quote,erp_sale_order,erp_sale_cart,erp_sale_out,erp_sale_return';

DELETE rfp
FROM system_role_field_permission rfp
JOIN system_field_definition fd ON fd.id = rfp.field_id
WHERE FIND_IN_SET(fd.module, @tax_modules)
  AND fd.field_key IN (
      'taxPercent', 'taxRate', 'taxPrice', 'totalTaxPrice',
      'taxAmount', 'taxExclusiveAmount', 'taxExclusivePrice',
      'taxInclusivePrice', 'taxInclusiveAmount',
      'col_taxPercent', 'col_taxRate', 'col_taxPrice', 'col_totalTaxPrice',
      'col_taxAmount', 'col_taxExclusiveAmount', 'col_taxExclusivePrice',
      'col_taxInclusivePrice', 'col_taxInclusiveAmount'
  );

UPDATE system_field_definition
SET deleted = b'1'
WHERE FIND_IN_SET(module, @tax_modules)
  AND field_key IN (
      'taxPercent', 'taxRate', 'taxPrice', 'totalTaxPrice',
      'taxAmount', 'taxExclusiveAmount', 'taxExclusivePrice',
      'taxInclusivePrice', 'taxInclusiveAmount',
      'col_taxPercent', 'col_taxRate', 'col_taxPrice', 'col_totalTaxPrice',
      'col_taxAmount', 'col_taxExclusiveAmount', 'col_taxExclusivePrice',
      'col_taxInclusivePrice', 'col_taxInclusiveAmount'
  );
