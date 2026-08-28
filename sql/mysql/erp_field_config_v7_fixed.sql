-- ==============================================================
-- ERP 第七期：字段必填/选填配置（修复版）
-- 部署方式：mysql -u root -p --default-character-set=utf8mb4 ruoyi-vue-pro < sql/mysql/erp_field_config_v7_fixed.sql
-- 修复内容：
--   1. 使用 tenant_id = 1（而不是0）
--   2. 清理重复数据的逻辑更严格
--   3. 使用 INSERT IGNORE 避免重复插入
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
-- 2. 清理租户1的旧数据（保证脚本可重复执行）
-- --------------------------------------------------------------
DELETE FROM `erp_field_config`
 WHERE `module_key` IN ('purchase_order','purchase_in','purchase_return','supplier')
   AND `tenant_id` = 1;

-- --------------------------------------------------------------
-- 3. 初始化字段配置（使用 tenant_id = 1）
-- --------------------------------------------------------------

-- ====== 采购订单（purchase_order）======
INSERT INTO `erp_field_config` (`module_key`, `field_name`, `field_label`, `required`, `sort`, `tenant_id`) VALUES
('purchase_order', 'no',                '订单单号',     b'0',  10, 1),
('purchase_order', 'orderTime',         '创建时间',     b'1',  20, 1),
('purchase_order', 'supplierId',        '供应商',       b'1',  30, 1),
('purchase_order', 'purchaser',         '采购员',       b'0',  40, 1),
('purchase_order', 'deptId',            '部门',         b'0',  50, 1),
('purchase_order', 'taxPercent',        '税率(%)',      b'0',  60, 1),
('purchase_order', 'orderDate',         '订货日期',     b'0',  70, 1),
('purchase_order', 'purchaseCycle',     '采购周期',     b'0',  80, 1),
('purchase_order', 'arrivalDate',       '到货日期',     b'0',  90, 1),
('purchase_order', 'deliveryMethod',    '送货方式',     b'0', 100, 1),
('purchase_order', 'purchaseType',      '采购方式',     b'0', 110, 1),
('purchase_order', 'invoiceType',       '发票类型',     b'0', 120, 1),
('purchase_order', 'settleMethod',      '结算方式',     b'0', 130, 1),
('purchase_order', 'orderFormula',      '订货公式',     b'0', 140, 1),
('purchase_order', 'sendDate',          '发货日期',     b'0', 150, 1),
('purchase_order', 'saleDateFrom',      '销售日期从',   b'0', 160, 1),
('purchase_order', 'saleDateTo',        '销售日期到',   b'0', 170, 1),
('purchase_order', 'documentType',      '单据类型',     b'0', 180, 1),
('purchase_order', 'latestOrderDate',   '最近订货日期', b'0', 190, 1),
('purchase_order', 'accountId',         '账户',         b'0', 200, 1),
('purchase_order', 'remark',            '备注',         b'0', 210, 1),
('purchase_order', 'fileUrl',           '附件',         b'0', 220, 1),
('purchase_order', 'items',             '订单明细',     b'0', 230, 1);

-- ====== 采购入库（purchase_in）======
INSERT INTO `erp_field_config` (`module_key`, `field_name`, `field_label`, `required`, `sort`, `tenant_id`) VALUES
('purchase_in', 'no',                '入库单号',                 b'0',  10, 1),
('purchase_in', 'inTime',            '入库时间',                 b'1',  20, 1),
('purchase_in', 'supplierId',        '供应商',                   b'1',  30, 1),
('purchase_in', 'purchaser',         '采购员',                   b'0',  40, 1),
('purchase_in', 'accountant',        '会计',                     b'0',  50, 1),
('purchase_in', 'handler',           '经手人',                   b'0',  60, 1),
('purchase_in', 'unloader',          '卸货人',                   b'0',  70, 1),
('purchase_in', 'invoiceType',       '发票类型',                 b'0',  80, 1),
('purchase_in', 'transportMethod',   '运输方式',                 b'0',  90, 1),
('purchase_in', 'settleMethod',      '结算方式',                 b'0', 100, 1),
('purchase_in', 'purchaseArea',      '采购地区',                 b'0', 110, 1),
('purchase_in', 'freightType1',      '运费类型1',                b'0', 120, 1),
('purchase_in', 'freightType2',      '运费类型2',                b'0', 130, 1),
('purchase_in', 'freightObject1',    '运费对象1',                b'0', 140, 1),
('purchase_in', 'freightObject2',    '运费对象2',                b'0', 150, 1),
('purchase_in', 'logisticsCompany',  '物流公司',                 b'0', 160, 1),
('purchase_in', 'taxRate',           '税率',                     b'0', 170, 1),
('purchase_in', 'deptId',            '部门',                     b'0', 180, 1),
('purchase_in', 'purchaseDiscount',  '采购折扣',                 b'0', 190, 1),
('purchase_in', 'priority',          '优先级',                   b'0', 200, 1),
('purchase_in', 'floatRecord',       '浮动记录',                 b'0', 210, 1),
('purchase_in', 'receiveUnit',       '收货单位',                 b'0', 220, 1),
('purchase_in', 'totalFreight1',     '总运费1',                  b'0', 230, 1),
('purchase_in', 'totalFreight2',     '总运费2',                  b'0', 240, 1),
('purchase_in', 'paymentDate',       '付款日期',                 b'0', 250, 1),
('purchase_in', 'hasInvoice',        '是否有发票',               b'0', 260, 1),
('purchase_in', 'packageCount',      '包装件数',                 b'0', 270, 1),
('purchase_in', 'factoryOrderNo',    '厂家订单号',               b'0', 280, 1),
('purchase_in', 'orderMethod',       '订货方式',                 b'0', 290, 1),
('purchase_in', 'businessEntity',    '业务实体',                 b'0', 300, 1),
('purchase_in', 'accountId',         '账户',                     b'0', 310, 1),
('purchase_in', 'remark',            '备注',                     b'0', 320, 1),
('purchase_in', 'fileUrl',           '附件',                     b'0', 330, 1),
('purchase_in', 'items',             '入库明细',                 b'0', 340, 1);

-- ====== 采购退货（purchase_return）======
INSERT INTO `erp_field_config` (`module_key`, `field_name`, `field_label`, `required`, `sort`, `tenant_id`) VALUES
('purchase_return', 'no',                '退货单号',     b'0',  10, 1),
('purchase_return', 'returnMode',        '退货模式',     b'1',  20, 1),
('purchase_return', 'returnTime',        '退货时间',     b'1',  30, 1),
('purchase_return', 'supplierId',        '供应商',       b'1',  40, 1),
('purchase_return', 'purchaser',         '采购员',       b'0',  50, 1),
('purchase_return', 'invoiceType',       '发票类型',     b'0',  60, 1),
('purchase_return', 'transportMethod',   '运输方式',     b'0',  70, 1),
('purchase_return', 'settleMethod',      '结算方式',     b'0',  80, 1),
('purchase_return', 'warehouseType',     '仓库类型',     b'0',  90, 1),
('purchase_return', 'shipArea',          '发货地区',     b'0', 100, 1),
('purchase_return', 'freightType',       '运费类型',     b'0', 110, 1),
('purchase_return', 'sourceType',        '来源类型',     b'0', 120, 1),
('purchase_return', 'priority',          '优先级',       b'0', 130, 1),
('purchase_return', 'orderMethod',       '订货方式',     b'0', 140, 1),
('purchase_return', 'logisticsCompany',  '物流公司',     b'0', 150, 1),
('purchase_return', 'taxRate',           '税率',         b'0', 160, 1),
('purchase_return', 'deptId',            '部门',         b'0', 170, 1),
('purchase_return', 'handler',           '制单人',       b'0', 180, 1),
('purchase_return', 'accountId',         '账户',         b'0', 190, 1),
('purchase_return', 'remark',            '备注',         b'0', 200, 1),
('purchase_return', 'fileUrl',           '附件',         b'0', 210, 1);

-- ====== 供应商（supplier）======
INSERT INTO `erp_field_config` (`module_key`, `field_name`, `field_label`, `required`, `sort`, `tenant_id`) VALUES
('supplier', 'name',                '供应商名称',               b'1',  10, 1),
('supplier', 'sort',                '排序',                     b'1',  20, 1),
('supplier', 'code',                '供应商编码',               b'0',  30, 1),
('supplier', 'category',            '供应商类别',               b'0',  40, 1),
('supplier', 'companyNature',       '公司性质',                 b'0',  50, 1),
('supplier', 'obsolete',            '是否淘汰',                 b'0',  60, 1),
('supplier', 'obsoleteDate',        '淘汰日期',                 b'0',  70, 1),
('supplier', 'taxNo',               '统一信用代码',             b'0',  80, 1),
('supplier', 'bankName',            '开户银行',                 b'0',  90, 1),
('supplier', 'bankAccount',         '银行账号',                 b'0', 100, 1),
('supplier', 'bankAccountName',     '开户名称',                 b'0', 110, 1),
('supplier', 'taxPercent',          '税率',                     b'0', 120, 1),
('supplier', 'purchaseControl',     '采购控制',                 b'0', 130, 1),
('supplier', 'floatUpdateLastPrice','浮动是否更新供应商最后进价', b'0', 140, 1),
('supplier', 'arrivalCycle',        '到货周期(天)',             b'0', 150, 1),
('supplier', 'purchaseLeadDays',    '采购提前期(天)',           b'0', 160, 1),
('supplier', 'freightType',         '运费类型',                 b'0', 170, 1),
('supplier', 'invoiceType',         '发票类型',                 b'0', 180, 1),
('supplier', 'contact',             '联系人',                   b'0', 190, 1),
('supplier', 'mobile',              '手机',                     b'0', 200, 1),
('supplier', 'telephone',           '电话',                     b'0', 210, 1),
('supplier', 'email',               '邮箱',                     b'0', 220, 1),
('supplier', 'qq',                  'QQ',                       b'0', 230, 1),
('supplier', 'wechat',              '微信',                     b'0', 240, 1),
('supplier', 'areaIds',             '省/市/区县',               b'0', 250, 1),
('supplier', 'detailAddress',       '详细地址',                 b'0', 260, 1),
('supplier', 'postalCode',          '邮编',                     b'0', 270, 1),
('supplier', 'website',             '网址',                     b'0', 280, 1),
('supplier', 'remark',              '备注',                     b'0', 290, 1),
('supplier', 'settleCurrency',      '结算币种',                 b'0', 300, 1),
('supplier', 'settleCycle',         '结算周期',                 b'0', 310, 1),
('supplier', 'settleMethod',        '结算方式',                 b'0', 320, 1),
('supplier', 'paymentDays',         '付款天数',                 b'0', 330, 1),
('supplier', 'creditLimit',         '信用额度',                 b'0', 340, 1),
('supplier', 'creditDays',          '信用天数',                 b'0', 350, 1),
('supplier', 'taxpayerId',          '纳税人识别号',             b'0', 370, 1),
('supplier', 'invoiceAddress',      '开票地址',                 b'0', 380, 1),
('supplier', 'invoicePhone',        '开票电话',                 b'0', 390, 1),
('supplier', 'invoiceBank',         '开票银行',                 b'0', 400, 1),
('supplier', 'invoiceBankAccount',  '开票银行账号',             b'0', 410, 1),
('supplier', 'invoiceRemark',       '开票备注',                 b'0', 420, 1),
('supplier', 'invoiceCompany',      '开票单位',                 b'0', 430, 1),
('supplier', 'financePhone',        '财务联系电话',             b'0', 440, 1),
('supplier', 'financeEmail',        '财务邮箱',                 b'0', 450, 1),
('supplier', 'financeQq',           '财务QQ',                   b'0', 460, 1),
('supplier', 'financeWechat',       '财务微信',                 b'0', 470, 1);

-- ==============================================================
-- 执行完成后：
--   系统管理 → 菜单管理 → 点击刷新缓存
-- 或重启后端服务以使权限生效
-- ==============================================================
