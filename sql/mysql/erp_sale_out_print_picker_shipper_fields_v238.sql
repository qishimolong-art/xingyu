-- Sale out print picker value and shipper label fix (v238).
-- Safe to execute repeatedly. Only updates the sale out print field label for checkerName.

UPDATE `system_field_definition`
SET `field_label` = '发货人',
    `updater` = '1',
    `update_time` = NOW()
WHERE `module` = 'erp_sale_out'
  AND `field_key` = 'checkerName'
  AND `deleted` = b'0'
  AND `field_label` <> '发货人';

