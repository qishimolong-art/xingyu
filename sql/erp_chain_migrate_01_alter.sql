-- =============================================
-- 汽配连锁 ERP 系统 - SQL 迁移脚本
-- 基于 yudao-module-erp 扩展
-- =============================================

-- =============================================
-- 阶段一：基础增强
-- =============================================

-- 1. 供应商表扩展
ALTER TABLE erp_supplier ADD COLUMN code VARCHAR(64) COMMENT '编码(自动生成)';
ALTER TABLE erp_supplier ADD COLUMN old_code VARCHAR(64) COMMENT '旧编码';
ALTER TABLE erp_supplier ADD COLUMN short_name VARCHAR(64) COMMENT '简称';
ALTER TABLE erp_supplier ADD COLUMN foreign_name VARCHAR(128) COMMENT '外文名';
ALTER TABLE erp_supplier ADD COLUMN region VARCHAR(64) COMMENT '区域';
ALTER TABLE erp_supplier ADD COLUMN category VARCHAR(64) COMMENT '往来类别';
ALTER TABLE erp_supplier ADD COLUMN account VARCHAR(128) COMMENT '账户';
ALTER TABLE erp_supplier ADD COLUMN settle_method VARCHAR(32) COMMENT '结算方式';
ALTER TABLE erp_supplier ADD COLUMN settle_locked BOOLEAN DEFAULT FALSE COMMENT '结算锁定';
ALTER TABLE erp_supplier ADD COLUMN supplier_type VARCHAR(32) COMMENT '供应商类型';
ALTER TABLE erp_supplier ADD COLUMN arrival_cycle INT COMMENT '到货周期(天)';
ALTER TABLE erp_supplier ADD COLUMN purchase_lead_days INT COMMENT '采购提前期(天)';
ALTER TABLE erp_supplier ADD COLUMN transport_method VARCHAR(32) COMMENT '运输方式';
ALTER TABLE erp_supplier ADD COLUMN freight_type VARCHAR(32) COMMENT '运费类型';
ALTER TABLE erp_supplier ADD COLUMN wubi_code VARCHAR(32) COMMENT '五笔码';
ALTER TABLE erp_supplier ADD COLUMN pinyin_code VARCHAR(64) COMMENT '拼音码';
ALTER TABLE erp_supplier ADD COLUMN purchaser VARCHAR(64) COMMENT '采购员';
ALTER TABLE erp_supplier ADD COLUMN company_nature VARCHAR(64) COMMENT '公司性质';
ALTER TABLE erp_supplier ADD COLUMN obsolete BOOLEAN DEFAULT FALSE COMMENT '淘汰';
ALTER TABLE erp_supplier ADD COLUMN invoice_type VARCHAR(32) COMMENT '开票类型';
ALTER TABLE erp_supplier ADD COLUMN group_supplier BOOLEAN DEFAULT FALSE COMMENT '集团供应商';
ALTER TABLE erp_supplier ADD COLUMN allow_branch_order BOOLEAN DEFAULT FALSE COMMENT '是否允许分店开单';
ALTER TABLE erp_supplier ADD COLUMN logistics_company VARCHAR(64) COMMENT '物流公司';
ALTER TABLE erp_supplier ADD COLUMN arrival_point VARCHAR(128) COMMENT '到货点';
ALTER TABLE erp_supplier ADD COLUMN postal_code VARCHAR(16) COMMENT '邮政编码';
ALTER TABLE erp_supplier ADD COLUMN member_code VARCHAR(64) COMMENT '会员编码(自动生成)';
ALTER TABLE erp_supplier ADD COLUMN address VARCHAR(256) COMMENT '地址';
ALTER TABLE erp_supplier ADD COLUMN province VARCHAR(32) COMMENT '省';
ALTER TABLE erp_supplier ADD COLUMN city VARCHAR(32) COMMENT '市';
ALTER TABLE erp_supplier ADD COLUMN district VARCHAR(32) COMMENT '区/县';
ALTER TABLE erp_supplier ADD COLUMN website VARCHAR(256) COMMENT '网址';
ALTER TABLE erp_supplier ADD COLUMN legal_person VARCHAR(64) COMMENT '法定代表';
ALTER TABLE erp_supplier ADD COLUMN credit_code VARCHAR(64) COMMENT '统一信用代码';
ALTER TABLE erp_supplier ADD COLUMN purchase_control VARCHAR(32) COMMENT '采购管控';
ALTER TABLE erp_supplier ADD COLUMN float_update_last_price VARCHAR(32) COMMENT '浮动是否更新供应商最后进价';
ALTER TABLE erp_supplier ADD COLUMN taxpayer_id VARCHAR(64) COMMENT '纳税人识别号';
ALTER TABLE erp_supplier ADD COLUMN invoice_bank VARCHAR(128) COMMENT '开票银行';
ALTER TABLE erp_supplier ADD COLUMN invoice_bank_account VARCHAR(64) COMMENT '开票银行账号';
ALTER TABLE erp_supplier ADD COLUMN invoice_address VARCHAR(256) COMMENT '开票地址';
ALTER TABLE erp_supplier ADD COLUMN invoice_phone VARCHAR(32) COMMENT '开票电话';
ALTER TABLE erp_supplier ADD COLUMN invoice_company VARCHAR(128) COMMENT '开票单位';
ALTER TABLE erp_supplier ADD COLUMN finance_phone VARCHAR(32) COMMENT '财务联系电话';
ALTER TABLE erp_supplier ADD COLUMN performance_profit_ref VARCHAR(64) COMMENT '绩效考核利润参考依据';

-- 2. 采购订单表扩展
ALTER TABLE erp_purchase_order ADD COLUMN arrival_date DATE COMMENT '到货日期';
ALTER TABLE erp_purchase_order ADD COLUMN delivery_method VARCHAR(32) COMMENT '送货方式';
ALTER TABLE erp_purchase_order ADD COLUMN purchase_type VARCHAR(32) COMMENT '采购类型';
ALTER TABLE erp_purchase_order ADD COLUMN order_formula VARCHAR(128) COMMENT '订货公式';
ALTER TABLE erp_purchase_order ADD COLUMN send_date DATE COMMENT '发出日期';
ALTER TABLE erp_purchase_order ADD COLUMN latest_arrival_date DATE COMMENT '最近到货日期';
ALTER TABLE erp_purchase_order ADD COLUMN sale_date_from DATETIME COMMENT '销售日期从';
ALTER TABLE erp_purchase_order ADD COLUMN sale_date_to DATETIME COMMENT '到销售日期';
ALTER TABLE erp_purchase_order ADD COLUMN factory_order_no VARCHAR(64) COMMENT '厂家单号';
ALTER TABLE erp_purchase_order ADD COLUMN receive_address VARCHAR(256) COMMENT '收货地址';
ALTER TABLE erp_purchase_order ADD COLUMN invoice_type VARCHAR(32) COMMENT '开票类型';
ALTER TABLE erp_purchase_order ADD COLUMN settle_method VARCHAR(32) COMMENT '结算方式';

-- 3. 采购退货表扩展
ALTER TABLE erp_purchase_return ADD COLUMN return_type VARCHAR(32) COMMENT '退货类型(入库单退货等)';
ALTER TABLE erp_purchase_return ADD COLUMN purchaser VARCHAR(64) COMMENT '采购员';
ALTER TABLE erp_purchase_return ADD COLUMN invoice_type VARCHAR(32) COMMENT '开票类型';
ALTER TABLE erp_purchase_return ADD COLUMN transport_method VARCHAR(32) COMMENT '运输方式';
ALTER TABLE erp_purchase_return ADD COLUMN settle_method VARCHAR(32) COMMENT '结算方式';
ALTER TABLE erp_purchase_return ADD COLUMN package_count INT DEFAULT 0 COMMENT '件数';
ALTER TABLE erp_purchase_return ADD COLUMN freight_amount DECIMAL(24,2) DEFAULT 0 COMMENT '运费金额';
ALTER TABLE erp_purchase_return ADD COLUMN logistics_company VARCHAR(64) COMMENT '物流公司';
ALTER TABLE erp_purchase_return ADD COLUMN doc_source VARCHAR(32) COMMENT '单据来源(正常退货等)';
ALTER TABLE erp_purchase_return ADD COLUMN factory_order_no VARCHAR(64) COMMENT '厂家单号';
ALTER TABLE erp_purchase_return ADD COLUMN maker VARCHAR(64) COMMENT '制单人';
ALTER TABLE erp_purchase_return ADD COLUMN dept VARCHAR(64) COMMENT '部门';
ALTER TABLE erp_purchase_return ADD COLUMN shipping_area VARCHAR(64) COMMENT '发货区';
ALTER TABLE erp_purchase_return ADD COLUMN warehouse_type VARCHAR(32) COMMENT '仓库类型';
ALTER TABLE erp_purchase_return ADD COLUMN freight_type VARCHAR(32) COMMENT '运费类型';
ALTER TABLE erp_purchase_return ADD COLUMN logistics_no VARCHAR(64) COMMENT '物流单号';
ALTER TABLE erp_purchase_return ADD COLUMN priority VARCHAR(32) COMMENT '优先级';
ALTER TABLE erp_purchase_return ADD COLUMN order_method VARCHAR(32) COMMENT '开单方式(正常单等)';

-- 4. 采购退货明细表扩展
ALTER TABLE erp_purchase_return_items ADD COLUMN part_code VARCHAR(64) COMMENT '零件编码';
ALTER TABLE erp_purchase_return_items ADD COLUMN part_name VARCHAR(128) COMMENT '零件名称';
ALTER TABLE erp_purchase_return_items ADD COLUMN vehicle_model VARCHAR(256) COMMENT '适用车型';
ALTER TABLE erp_purchase_return_items ADD COLUMN origin_place VARCHAR(64) COMMENT '产地';

-- 5. 产品表扩展
ALTER TABLE erp_product ADD COLUMN brand VARCHAR(64) COMMENT '品牌';
ALTER TABLE erp_product ADD COLUMN oe_number VARCHAR(128) COMMENT 'OE编号';
ALTER TABLE erp_product ADD COLUMN origin_place VARCHAR(64) COMMENT '产地';
ALTER TABLE erp_product ADD COLUMN vehicle_model_text VARCHAR(512) COMMENT '适用车型文本(冗余)';
ALTER TABLE erp_product ADD COLUMN feature_code VARCHAR(64) COMMENT '特征码';
ALTER TABLE erp_product ADD COLUMN drawing_no VARCHAR(64) COMMENT '图号';
ALTER TABLE erp_product ADD COLUMN shelf VARCHAR(64) COMMENT '货架位置';

-- 6. 仓库表扩展
ALTER TABLE erp_warehouse ADD COLUMN warehouse_type TINYINT DEFAULT 1 COMMENT '1总仓 2分仓 3虚拟仓';

-- 7. 销售订单表扩展
ALTER TABLE erp_sale_order ADD COLUMN order_type TINYINT DEFAULT 1 COMMENT '1正式单 2报价单';

-- 8. 销售订单项扩展
ALTER TABLE erp_sale_order_items ADD COLUMN gift_flag BOOLEAN DEFAULT FALSE COMMENT '是否赠品';

-- 9. 库存表扩展
ALTER TABLE erp_stock ADD COLUMN lock_count DECIMAL(24,6) DEFAULT 0 COMMENT '锁定数量';
