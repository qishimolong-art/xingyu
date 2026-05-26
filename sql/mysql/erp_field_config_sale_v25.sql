-- =============================================
-- 销售模块字段配置初始化（报价订单 / 销售手推车 / 销售退货）
-- 幂等：先按 module_key 删除再插入
-- 执行方式：mysql -u root -p --default-character-set=utf8mb4 ruoyi-vue-pro < erp_field_config_sale_v25.sql
-- =============================================

-- 清理旧数据（逻辑删除的也一并清理）
DELETE FROM erp_field_config WHERE module_key IN ('sale_quote', 'sale_cart', 'sale_return');

-- =============================================
-- 报价订单（sale_quote）：31 个字段，5 个必填
-- =============================================
INSERT INTO erp_field_config (module_key, field_name, field_label, required, sort, tenant_id, creator, updater, deleted) VALUES
('sale_quote', 'no', '报价单号', 0, 1, 1, '1', '1', 0),
('sale_quote', 'quoteTime', '报价时间', 1, 2, 1, '1', '1', 0),
('sale_quote', 'customerId', '客户', 1, 3, 1, '1', '1', 0),
('sale_quote', 'orderType', '订单类型', 0, 4, 1, '1', '1', 0),
('sale_quote', 'saleUserId', '业务员', 1, 5, 1, '1', '1', 0),
('sale_quote', 'settleMethod', '结算方式', 0, 6, 1, '1', '1', 0),
('sale_quote', 'prepayment', '先款后货', 0, 7, 1, '1', '1', 0),
('sale_quote', 'accountId', '结算账户', 0, 8, 1, '1', '1', 0),
('sale_quote', 'priority', '优先级', 1, 9, 1, '1', '1', 0),
('sale_quote', 'deliveryMethod', '送货方式', 1, 10, 1, '1', '1', 0),
('sale_quote', 'proxyDelivery', '代客户发货', 0, 11, 1, '1', '1', 0),
('sale_quote', 'deliveryAddress', '收货地址', 0, 12, 1, '1', '1', 0),
('sale_quote', 'allowancePrice', '折让金额', 0, 13, 1, '1', '1', 0),
('sale_quote', 'ticketNo', '票据号', 0, 14, 1, '1', '1', 0),
('sale_quote', 'invoiceType', '开票类型', 0, 15, 1, '1', '1', 0),
('sale_quote', 'freightType', '运费类型', 0, 16, 1, '1', '1', 0),
('sale_quote', 'freightAmount', '费用金额', 0, 17, 1, '1', '1', 0),
('sale_quote', 'logisticsCompany', '物流公司', 0, 18, 1, '1', '1', 0),
('sale_quote', 'deptId', '部门', 0, 19, 1, '1', '1', 0),
('sale_quote', 'receiverName', '收货人', 0, 20, 1, '1', '1', 0),
('sale_quote', 'receiverPhone', '收货电话', 0, 21, 1, '1', '1', 0),
('sale_quote', 'priceType', '价格类型', 0, 22, 1, '1', '1', 0),
('sale_quote', 'branchDelivery', '分店发货', 0, 23, 1, '1', '1', 0),
('sale_quote', 'expectedDeliveryTime', '预计发货时间', 0, 24, 1, '1', '1', 0),
('sale_quote', 'billingMethod', '开单方式', 0, 25, 1, '1', '1', 0),
('sale_quote', 'vehiclePlateNo', '车牌号', 0, 26, 1, '1', '1', 0),
('sale_quote', 'businessType', '业务类型', 0, 27, 1, '1', '1', 0),
('sale_quote', 'developerUserId', '开发员', 0, 28, 1, '1', '1', 0),
('sale_quote', 'vin', 'VIN车架号', 0, 29, 1, '1', '1', 0),
('sale_quote', 'remark', '备注', 0, 30, 1, '1', '1', 0),
('sale_quote', 'internalRemark', '内部说明', 0, 31, 1, '1', '1', 0);

-- =============================================
-- 销售手推车（sale_cart）：26 个字段，2 个必填
-- =============================================
INSERT INTO erp_field_config (module_key, field_name, field_label, required, sort, tenant_id, creator, updater, deleted) VALUES
('sale_cart', 'no', '手推车单号', 0, 1, 1, '1', '1', 0),
('sale_cart', 'cartTime', '开单时间', 1, 2, 1, '1', '1', 0),
('sale_cart', 'customerId', '客户', 1, 3, 1, '1', '1', 0),
('sale_cart', 'saleUserId', '销售人员', 0, 4, 1, '1', '1', 0),
('sale_cart', 'accountId', '结算账户', 0, 5, 1, '1', '1', 0),
('sale_cart', 'businessType', '业务类型', 0, 6, 1, '1', '1', 0),
('sale_cart', 'orderType', '订单类型', 0, 7, 1, '1', '1', 0),
('sale_cart', 'billingMethod', '开单方式', 0, 8, 1, '1', '1', 0),
('sale_cart', 'settleMethod', '结算方式', 0, 9, 1, '1', '1', 0),
('sale_cart', 'invoiceType', '发票类型', 0, 10, 1, '1', '1', 0),
('sale_cart', 'deliveryMethod', '配送方式', 0, 11, 1, '1', '1', 0),
('sale_cart', 'freightType', '运费类型', 0, 12, 1, '1', '1', 0),
('sale_cart', 'priority', '优先级', 0, 13, 1, '1', '1', 0),
('sale_cart', 'priceType', '价格类型', 0, 14, 1, '1', '1', 0),
('sale_cart', 'logisticsCompany', '物流公司', 0, 15, 1, '1', '1', 0),
('sale_cart', 'developerUserId', '开发人员', 0, 16, 1, '1', '1', 0),
('sale_cart', 'contactPerson', '联系人', 0, 17, 1, '1', '1', 0),
('sale_cart', 'contactPhone', '联系电话', 0, 18, 1, '1', '1', 0),
('sale_cart', 'deliveryAddress', '送货地址', 0, 19, 1, '1', '1', 0),
('sale_cart', 'deliveryDate', '送货日期', 0, 20, 1, '1', '1', 0),
('sale_cart', 'taxRate', '税率', 0, 21, 1, '1', '1', 0),
('sale_cart', 'totalFreight', '运费合计', 0, 22, 1, '1', '1', 0),
('sale_cart', 'paymentDate', '付款日期', 0, 23, 1, '1', '1', 0),
('sale_cart', 'businessEntity', '经营主体', 0, 24, 1, '1', '1', 0),
('sale_cart', 'orderMethod', '订货方式', 0, 25, 1, '1', '1', 0),
('sale_cart', 'sourceType2', '来源类型', 0, 26, 1, '1', '1', 0),
('sale_cart', 'remark2', '备注2', 0, 27, 1, '1', '1', 0),
('sale_cart', 'remark', '备注', 0, 28, 1, '1', '1', 0);

-- =============================================
-- 销售退货（sale_return）：15 个字段，3 个必填
-- =============================================
INSERT INTO erp_field_config (module_key, field_name, field_label, required, sort, tenant_id, creator, updater, deleted) VALUES
('sale_return', 'no', '退货单号', 0, 1, 1, '1', '1', 0),
('sale_return', 'returnTime', '退货时间', 1, 2, 1, '1', '1', 0),
('sale_return', 'returnMode', '退货模式', 1, 3, 1, '1', '1', 0),
('sale_return', 'sourceOutNo', '关联销售单', 0, 4, 1, '1', '1', 0),
('sale_return', 'orderNo', '关联订单', 0, 5, 1, '1', '1', 0),
('sale_return', 'customerId', '客户', 1, 6, 1, '1', '1', 0),
('sale_return', 'saleUserId', '销售人员', 0, 7, 1, '1', '1', 0),
('sale_return', 'remark', '备注', 0, 8, 1, '1', '1', 0),
('sale_return', 'fileUrl', '附件', 0, 9, 1, '1', '1', 0),
('sale_return', 'discountPercent', '优惠率', 0, 10, 1, '1', '1', 0),
('sale_return', 'discountPrice', '退款优惠', 0, 11, 1, '1', '1', 0),
('sale_return', 'discountedPrice', '优惠后金额', 0, 12, 1, '1', '1', 0),
('sale_return', 'otherPrice', '其他费用', 0, 13, 1, '1', '1', 0),
('sale_return', 'accountId', '结算账户', 0, 14, 1, '1', '1', 0),
('sale_return', 'totalPrice', '应收金额', 0, 15, 1, '1', '1', 0);
