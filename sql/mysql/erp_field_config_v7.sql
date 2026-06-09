-- ==============================================================
-- ERP 第七期：字段必填/选填配置
-- 部署方式：mysql -u root -p ruoyi-vue-pro < sql/mysql/erp_field_config_v7.sql
-- ==============================================================

-- --------------------------------------------------------------
-- 1. 建表
-- --------------------------------------------------------------
CREATE TABLE IF NOT EXISTS `erp_field_config` (
  `id`           BIGINT          NOT NULL AUTO_INCREMENT COMMENT '主键',
  `module_key`   VARCHAR(50)     NOT NULL COMMENT '模块标识：purchase_order/purchase_in/purchase_return/supplier',
  `field_name`   VARCHAR(100)    NOT NULL COMMENT '字段名（对应前端 schema 的 fieldName）',
  `field_label`  VARCHAR(100)    NULL     COMMENT '字段中文名',
  `required`     BIT(1)          NOT NULL DEFAULT b'0' COMMENT '是否必填',
  `visible`      BIT(1)          NOT NULL DEFAULT b'1' COMMENT '是否显示',
  `sort`         INT             NOT NULL DEFAULT 0 COMMENT '排序',
  `creator`      VARCHAR(64)     NULL     DEFAULT '',
  `create_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater`      VARCHAR(64)     NULL     DEFAULT '',
  `update_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted`      BIT(1)          NOT NULL DEFAULT b'0',
  `tenant_id`    BIGINT          NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_tenant_module_field` (`tenant_id`, `module_key`, `field_name`, `deleted`),
  KEY `idx_module_key` (`tenant_id`, `module_key`, `deleted`)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT 'ERP 字段必填配置表';

-- --------------------------------------------------------------
-- 2. 清理默认租户旧数据（保证脚本可重复执行）
-- --------------------------------------------------------------
DELETE FROM `erp_field_config`
 WHERE `module_key` IN ('purchase_order','purchase_in','purchase_return','supplier')
   AND `tenant_id` = 0;

-- --------------------------------------------------------------
-- 3. 初始化字段配置
-- --------------------------------------------------------------

-- ====== 采购订单（purchase_order）======
INSERT INTO `erp_field_config` (`module_key`, `field_name`, `field_label`, `required`, `sort`, `tenant_id`) VALUES
('purchase_order', 'no',                '订单单号',     b'0',  10, 0),
('purchase_order', 'orderTime',         '创建时间',     b'1',  20, 0),
('purchase_order', 'supplierId',        '供应商',       b'1',  30, 0),
('purchase_order', 'purchaser',         '采购员',       b'0',  40, 0),
('purchase_order', 'deptId',            '部门',         b'0',  50, 0),
('purchase_order', 'taxPercent',        '税率(%)',      b'0',  60, 0),
('purchase_order', 'orderDate',         '订货日期',     b'0',  70, 0),
('purchase_order', 'purchaseCycle',     '采购周期',     b'0',  80, 0),
('purchase_order', 'arrivalDate',       '到货日期',     b'0',  90, 0),
('purchase_order', 'deliveryMethod',    '送货方式',     b'0', 100, 0),
('purchase_order', 'purchaseType',      '采购方式',     b'0', 110, 0),
('purchase_order', 'settleMethod',      '结算方式',     b'0', 120, 0),
('purchase_order', 'invoiceType',       '开票类型',     b'0', 130, 0),
('purchase_order', 'factoryOrderNo',    '厂家单号',     b'0', 140, 0),
('purchase_order', 'receiveAddress',    '收货地址',     b'0', 150, 0),
('purchase_order', 'orderFormula',      '订货公式',     b'0', 160, 0),
('purchase_order', 'orderCompany',      '订货公司',     b'0', 170, 0),
('purchase_order', 'sendDate',          '发出日期',     b'0', 180, 0),
('purchase_order', 'latestArrivalDate', '最近到货日期', b'0', 190, 0),
('purchase_order', 'saleDateFrom',      '销售日期从',   b'0', 200, 0),
('purchase_order', 'saleDateTo',        '到销售日期',   b'0', 210, 0),
('purchase_order', 'remark',            '备注',         b'0', 220, 0),
('purchase_order', 'fileUrl',           '附件',         b'0', 230, 0);

-- ====== 采购入库（purchase_in）======
INSERT INTO `erp_field_config` (`module_key`, `field_name`, `field_label`, `required`, `sort`, `tenant_id`) VALUES
('purchase_in', 'no',               '入库单号',   b'0',  10, 0),
('purchase_in', 'inTime',           '入库时间',   b'1',  20, 0),
('purchase_in', 'orderNo',          '关联订单',   b'0',  30, 0),
('purchase_in', 'supplierId',       '供应商',     b'1',  40, 0),
('purchase_in', 'remark',           '备注',       b'0',  50, 0),
('purchase_in', 'fileUrl',          '附件',       b'0',  60, 0),
('purchase_in', 'purchaser',        '采购员',     b'0', 130, 0),
('purchase_in', 'deptId',           '部门',       b'0', 140, 0),
('purchase_in', 'purchaseArea',     '进货区',     b'0', 150, 0),
('purchase_in', 'accountant',       '记账员',     b'0', 160, 0),
('purchase_in', 'invoiceType',      '开票类型',   b'0', 170, 0),
('purchase_in', 'transportMethod',  '运输方式',   b'0', 180, 0),
('purchase_in', 'settleMethod',     '结算方式',   b'0', 190, 0),
('purchase_in', 'priority',         '优先级',     b'0', 200, 0),
('purchase_in', 'orderMethod',      '开单方式',   b'0', 210, 0),
('purchase_in', 'factoryOrderNo',   '厂家单号',   b'0', 220, 0),
('purchase_in', 'floatRate',        '浮动率',     b'0', 230, 0),
('purchase_in', 'packageCount',     '件数',       b'0', 240, 0),
('purchase_in', 'freightType1',     '运费类型1',  b'0', 250, 0),
('purchase_in', 'freightType2',     '运费类型2',  b'0', 260, 0),
('purchase_in', 'freightObject1',   '运费对象1',  b'0', 270, 0),
('purchase_in', 'freightObject2',   '运费对象2',  b'0', 280, 0),
('purchase_in', 'logisticsCompany', '物流公司',   b'0', 290, 0),
('purchase_in', 'totalFreight1',    '总运费1',    b'0', 300, 0),
('purchase_in', 'totalFreight2',    '总运费2',    b'0', 310, 0),
('purchase_in', 'handler',          '经办人',     b'0', 320, 0),
('purchase_in', 'taxRate',          '税率(%)',    b'0', 330, 0),
('purchase_in', 'purchaseDiscount', '采购折让',   b'0', 340, 0),
('purchase_in', 'unloader',         '卸货员',     b'0', 350, 0),
('purchase_in', 'floatRecord',      '浮动记录',   b'0', 360, 0),
('purchase_in', 'receiveUnit',      '收货单位',   b'0', 370, 0),
('purchase_in', 'paymentDate',      '付款日期',   b'0', 380, 0),
('purchase_in', 'hasInvoice',       '是否发票',   b'0', 390, 0),
('purchase_in', 'businessEntity',   '所属经营',   b'0', 400, 0);

-- ====== 采购退货（purchase_return）======
INSERT INTO `erp_field_config` (`module_key`, `field_name`, `field_label`, `required`, `sort`, `tenant_id`) VALUES
('purchase_return', 'returnMode',       '退货模式',   b'1',  10, 0),
('purchase_return', 'no',               '退货单号',   b'0',  20, 0),
('purchase_return', 'returnTime',       '退货时间',   b'1',  30, 0),
('purchase_return', 'orderNo',          '关联订单',   b'0',  40, 0),
('purchase_return', 'sourceInNo',       '原入库单',   b'0',  50, 0),
('purchase_return', 'supplierId',       '供应商',     b'1',  60, 0),
('purchase_return', 'returnType',       '退货类型',   b'0',  70, 0),
('purchase_return', 'purchaser',        '采购员',     b'0',  80, 0),
('purchase_return', 'transportMethod',  '运输方式',   b'0',  90, 0),
('purchase_return', 'settleMethod',     '结算方式',   b'0', 100, 0),
('purchase_return', 'invoiceType',      '开票类型',   b'0', 110, 0),
('purchase_return', 'logisticsCompany', '物流公司',   b'0', 120, 0),
('purchase_return', 'logisticsNo',      '物流单号',   b'0', 130, 0),
('purchase_return', 'freightAmount',    '运费金额',   b'0', 140, 0),
('purchase_return', 'packageCount',     '件数',       b'0', 150, 0),
('purchase_return', 'freightType',      '运费类型',   b'0', 160, 0),
('purchase_return', 'factoryOrderNo',   '厂家单号',   b'0', 170, 0),
('purchase_return', 'priority',         '优先级',     b'0', 180, 0),
('purchase_return', 'orderMethod',      '开单方式',   b'0', 190, 0),
('purchase_return', 'remark',           '备注',       b'0', 200, 0),
('purchase_return', 'fileUrl',          '附件',       b'0', 210, 0);

-- ====== 供应商（supplier）======
-- 基础信息 + 分类与采购 + 结算与物流 + 地址信息 + 开票信息 + 财务信息
INSERT INTO `erp_field_config` (`module_key`, `field_name`, `field_label`, `required`, `sort`, `tenant_id`) VALUES
-- 基础信息
('supplier', 'name',                  '供应商名称',                 b'1',  10, 0),
('supplier', 'shortName',             '简称',                       b'0',  20, 0),
('supplier', 'foreignName',           '外文名',                     b'0',  30, 0),
('supplier', 'code',                  '编码',                       b'0',  40, 0),
('supplier', 'oldCode',               '旧编码',                     b'0',  50, 0),
('supplier', 'contact',               '联系人',                     b'0',  60, 0),
('supplier', 'mobile',                '手机号码',                   b'0',  70, 0),
('supplier', 'telephone',             '联系电话',                   b'0',  80, 0),
('supplier', 'email',                 '电子邮箱',                   b'0',  90, 0),
('supplier', 'fax',                   '传真',                       b'0', 100, 0),
('supplier', 'status',                '开启状态',                   b'0', 110, 0),
('supplier', 'sort',                  '排序',                       b'1', 120, 0),
('supplier', 'remark',                '备注',                       b'0', 130, 0),
-- 分类与采购
('supplier', 'region',                '区域',                       b'0', 140, 0),
('supplier', 'category',              '往来类别',                   b'0', 150, 0),
('supplier', 'supplierType',          '供应商类型',                 b'0', 160, 0),
('supplier', 'purchaser',             '采购员',                     b'0', 170, 0),
('supplier', 'companyNature',         '公司性质',                   b'0', 180, 0),
('supplier', 'purchaseControl',       '采购管控',                   b'0', 190, 0),
('supplier', 'arrivalCycle',          '到货周期(天)',               b'0', 200, 0),
('supplier', 'purchaseLeadDays',      '采购提前期(天)',             b'0', 210, 0),
('supplier', 'obsolete',              '淘汰',                       b'0', 220, 0),
('supplier', 'groupSupplier',         '集团供应商',                 b'0', 230, 0),
('supplier', 'allowBranchOrder',      '允许分店开单',               b'0', 240, 0),
-- 结算与物流
('supplier', 'settleMethod',          '结算方式',                   b'0', 250, 0),
('supplier', 'settleLocked',          '结算锁定',                   b'0', 260, 0),
('supplier', 'transportMethod',       '运输方式',                   b'0', 270, 0),
('supplier', 'freightType',           '运费类型',                   b'0', 280, 0),
('supplier', 'logisticsCompany',      '物流公司',                   b'0', 290, 0),
('supplier', 'arrivalPoint',          '到货点',                     b'0', 300, 0),
('supplier', 'floatUpdateLastPrice',  '浮动是否更新供应商最后进价', b'0', 310, 0),
('supplier', 'performanceProfitRef',  '绩效考核利润参考依据',       b'0', 320, 0),
-- 地址信息
('supplier', 'address',               '地址',                       b'0', 330, 0),
('supplier', 'areaIds',               '省/市/区县',                 b'0', 340, 0),
('supplier', 'postalCode',            '邮政编码',                   b'0', 350, 0),
('supplier', 'website',               '网址',                       b'0', 360, 0),
-- 开票信息
('supplier', 'invoiceType',           '开票类型',                   b'0', 370, 0),
('supplier', 'taxpayerId',            '纳税人识别号',               b'0', 380, 0),
('supplier', 'invoiceBank',           '开票银行',                   b'0', 390, 0),
('supplier', 'invoiceBankAccount',    '开票银行账号',               b'0', 400, 0),
('supplier', 'invoiceAddress',        '开票地址',                   b'0', 410, 0),
('supplier', 'invoicePhone',          '开票电话',                   b'0', 420, 0),
('supplier', 'invoiceCompany',        '开票单位',                   b'0', 430, 0),
-- 财务信息
('supplier', 'account',               '账户',                       b'0', 440, 0),
('supplier', 'bankName',              '开户行',                     b'0', 450, 0),
('supplier', 'bankAccount',           '开户账号',                   b'0', 460, 0),
('supplier', 'bankAddress',           '开户地址',                   b'0', 470, 0),
('supplier', 'taxNo',                 '纳税人识别号',               b'0', 480, 0),
('supplier', 'taxPercent',            '税率(%)',                    b'0', 490, 0),
('supplier', 'financePhone',          '财务联系电话',               b'0', 500, 0),
('supplier', 'memberCode',            '会员编码',                   b'0', 510, 0),
('supplier', 'legalPerson',           '法定代表',                   b'0', 520, 0),
('supplier', 'creditCode',            '统一信用代码',               b'0', 530, 0);

-- ==============================================================
-- 脚本结束
-- ==============================================================
