-- =============================================
-- 汽配连锁 ERP 系统 - SQL 迁移脚本（新建表）
-- =============================================

-- =============================================
-- 阶段一：库存占用 + 采购调价单
-- =============================================

-- 10. 库存占用表
CREATE TABLE erp_stock_lock (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL COMMENT '产品编号',
    warehouse_id BIGINT NOT NULL COMMENT '仓库编号',
    lock_count DECIMAL(24,6) NOT NULL COMMENT '锁定数量',
    biz_type TINYINT NOT NULL COMMENT '1销售订单 2连锁开单',
    biz_id BIGINT NOT NULL COMMENT '业务单据ID',
    biz_item_id BIGINT NOT NULL COMMENT '业务单据项ID',
    biz_no VARCHAR(64) COMMENT '业务单号',
    status TINYINT DEFAULT 1 COMMENT '1锁定中 2已释放 3已扣减',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    INDEX idx_product_warehouse (product_id, warehouse_id),
    INDEX idx_biz (biz_type, biz_id)
) COMMENT 'ERP 库存占用表';

-- 11. 采购调价单
CREATE TABLE erp_purchase_price_adjust (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    no VARCHAR(64) NOT NULL COMMENT '调价单号(CGTJ前缀)',
    status TINYINT NOT NULL COMMENT '10未审核 20已审核',
    adjust_date DATETIME COMMENT '日期',
    supplier_id BIGINT COMMENT '供应商',
    dept VARCHAR(64) COMMENT '部门',
    adjust_user VARCHAR(64) COMMENT '调价人',
    adjust_type VARCHAR(32) COMMENT '调价类型(按入库单调价等)',
    remark VARCHAR(512) COMMENT '备注',
    total_adjust_price DECIMAL(24,2) COMMENT '调价总金额(正加负减)',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号'
) COMMENT 'ERP 采购调价单';

-- 12. 采购调价单明细
CREATE TABLE erp_purchase_price_adjust_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    adjust_id BIGINT NOT NULL COMMENT '调价单ID',
    purchase_in_no VARCHAR(64) COMMENT '采购单号(关联)',
    part_code VARCHAR(64) COMMENT '配件编码',
    part_name VARCHAR(128) COMMENT '配件名称',
    vehicle_model VARCHAR(256) COMMENT '车型',
    standard VARCHAR(128) COMMENT '规格',
    feature_code VARCHAR(64) COMMENT '特征码',
    origin_place VARCHAR(64) COMMENT '产地',
    brand VARCHAR(64) COMMENT '品牌',
    unit VARCHAR(32) COMMENT '单位',
    drawing_no VARCHAR(64) COMMENT '图号',
    in_count DECIMAL(24,6) COMMENT '入库数',
    old_price DECIMAL(24,2) COMMENT '进价(原价)',
    new_price DECIMAL(24,2) COMMENT '调后价',
    adjust_price DECIMAL(24,2) COMMENT '调价金额=(new-old)*in_count',
    shelf VARCHAR(64) COMMENT '货架',
    product_id BIGINT COMMENT '产品ID',
    purchase_in_item_id BIGINT COMMENT '关联采购入库项ID',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    INDEX idx_adjust_id (adjust_id)
) COMMENT 'ERP 采购调价单明细';

-- =============================================
-- 阶段二：连锁开单（跨租户）
-- =============================================

-- 13. 连锁开单主表（不受租户隔离）
CREATE TABLE erp_chain_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    no VARCHAR(64) NOT NULL COMMENT '连锁开单号(LSKD前缀)',
    status TINYINT NOT NULL COMMENT '10待审核 20已审核 30已完成 90已取消',
    hq_tenant_id BIGINT NOT NULL COMMENT '总公司租户ID',
    branch_tenant_id BIGINT NOT NULL COMMENT '分公司租户ID',
    customer_id BIGINT COMMENT '终端客户ID(无仓分公司)',
    hq_warehouse_id BIGINT COMMENT '总公司出库仓库ID',
    branch_warehouse_id BIGINT COMMENT '分公司入库仓库ID(有仓)',
    branch_type TINYINT NOT NULL COMMENT '1有仓分公司 2无仓分公司',
    order_time DATETIME COMMENT '下单时间',
    approve_time DATETIME COMMENT '审核时间',
    total_count DECIMAL(24,6) COMMENT '合计数量',
    total_price DECIMAL(24,2) COMMENT '合计金额',
    hq_sale_out_id BIGINT COMMENT '总公司销售出库单ID',
    branch_purchase_in_id BIGINT COMMENT '分公司采购入库单ID(有仓)',
    remark VARCHAR(512) COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    INDEX idx_hq_tenant (hq_tenant_id),
    INDEX idx_branch_tenant (branch_tenant_id)
) COMMENT 'ERP 连锁开单(跨租户)';

-- 14. 连锁开单明细（跨租户）
CREATE TABLE erp_chain_order_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    chain_order_id BIGINT NOT NULL COMMENT '连锁开单ID',
    product_id BIGINT NOT NULL COMMENT '产品编号',
    product_unit_id BIGINT COMMENT '产品单位编号',
    product_price DECIMAL(24,2) COMMENT '产品单价',
    count DECIMAL(24,6) NOT NULL COMMENT '数量',
    total_price DECIMAL(24,2) COMMENT '总价',
    remark VARCHAR(512) COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    INDEX idx_chain_order_id (chain_order_id)
) COMMENT 'ERP 连锁开单明细(跨租户)';

-- =============================================
-- 阶段三：价格历史 + 销售调价
-- =============================================

-- 15. 价格历史记录
CREATE TABLE erp_price_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT NOT NULL COMMENT '产品编号',
    partner_type TINYINT NOT NULL COMMENT '1供应商 2客户',
    partner_id BIGINT NOT NULL COMMENT '往来对象ID',
    price DECIMAL(24,2) NOT NULL COMMENT '价格',
    count DECIMAL(24,6) COMMENT '数量',
    biz_type TINYINT COMMENT '1采购入库 2销售出库 3采购调价 4销售调价',
    biz_id BIGINT COMMENT '业务单据ID',
    biz_no VARCHAR(64) COMMENT '业务单号',
    price_time DATETIME COMMENT '价格时间',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    INDEX idx_product_partner (product_id, partner_type, partner_id, price_time)
) COMMENT 'ERP 价格历史';

-- 16. 销售调价单
CREATE TABLE erp_sale_price_adjust (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    no VARCHAR(64) NOT NULL COMMENT '调价单号(XSTJ前缀)',
    status TINYINT NOT NULL COMMENT '10未审核 20已审核',
    adjust_date DATETIME COMMENT '日期',
    customer_id BIGINT COMMENT '客户',
    dept VARCHAR(64) COMMENT '部门',
    adjust_user VARCHAR(64) COMMENT '调价人',
    adjust_type VARCHAR(32) COMMENT '调价类型',
    remark VARCHAR(512) COMMENT '备注',
    total_adjust_price DECIMAL(24,2) COMMENT '调价总金额',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号'
) COMMENT 'ERP 销售调价单';

-- 17. 销售调价单明细
CREATE TABLE erp_sale_price_adjust_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    adjust_id BIGINT NOT NULL COMMENT '调价单ID',
    sale_out_no VARCHAR(64) COMMENT '销售单号(关联)',
    part_code VARCHAR(64) COMMENT '配件编码',
    part_name VARCHAR(128) COMMENT '配件名称',
    vehicle_model VARCHAR(256) COMMENT '车型',
    origin_place VARCHAR(64) COMMENT '产地',
    brand VARCHAR(64) COMMENT '品牌',
    unit VARCHAR(32) COMMENT '单位',
    out_count DECIMAL(24,6) COMMENT '出库数',
    old_price DECIMAL(24,2) COMMENT '原售价',
    new_price DECIMAL(24,2) COMMENT '调后价',
    adjust_price DECIMAL(24,2) COMMENT '调价金额',
    product_id BIGINT COMMENT '产品ID',
    sale_out_item_id BIGINT COMMENT '关联销售出库项ID',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    INDEX idx_adjust_id (adjust_id)
) COMMENT 'ERP 销售调价单明细';

-- =============================================
-- 阶段四：车型适配
-- =============================================

-- 18. 车型品牌
CREATE TABLE erp_vehicle_brand (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(64) NOT NULL COMMENT '品牌名称(丰田/本田/大众)',
    logo VARCHAR(256) COMMENT 'Logo图片',
    sort INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 0 COMMENT '状态(0正常 1停用)',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号'
) COMMENT '车型品牌';

-- 19. 车系
CREATE TABLE erp_vehicle_series (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    brand_id BIGINT NOT NULL COMMENT '品牌ID',
    name VARCHAR(64) NOT NULL COMMENT '车系名称(凯美瑞/雅阁)',
    sort INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 0 COMMENT '状态(0正常 1停用)',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    INDEX idx_brand_id (brand_id)
) COMMENT '车系';

-- 20. 车型
CREATE TABLE erp_vehicle_model (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    series_id BIGINT NOT NULL COMMENT '车系ID',
    name VARCHAR(128) NOT NULL COMMENT '车型名称',
    year_start INT COMMENT '起始年份',
    year_end INT COMMENT '结束年份',
    engine_model VARCHAR(64) COMMENT '发动机型号',
    displacement VARCHAR(32) COMMENT '排量',
    sort INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 0 COMMENT '状态(0正常 1停用)',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    INDEX idx_series_id (series_id)
) COMMENT '车型';

-- 21. 车型配件适配
CREATE TABLE erp_vehicle_product_fit (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    vehicle_model_id BIGINT NOT NULL COMMENT '车型ID',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    fit_position VARCHAR(128) COMMENT '安装位置',
    remark VARCHAR(256) COMMENT '备注',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    UNIQUE INDEX uk_model_product (vehicle_model_id, product_id)
) COMMENT '车型配件适配';

-- =============================================
-- 阶段五：自动订货
-- =============================================

-- 22. 自动订货规则
CREATE TABLE erp_auto_order_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    product_id BIGINT COMMENT '产品ID(空=全局)',
    category_id BIGINT COMMENT '产品分类ID',
    warehouse_id BIGINT COMMENT '仓库ID',
    min_stock DECIMAL(24,6) COMMENT '最低库存',
    max_stock DECIMAL(24,6) COMMENT '补到此值',
    calc_days INT DEFAULT 30 COMMENT '销量计算天数',
    safety_days INT DEFAULT 7 COMMENT '安全天数',
    lead_days INT DEFAULT 3 COMMENT '采购提前期(天)',
    status TINYINT DEFAULT 0 COMMENT '状态(0正常 1停用)',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号'
) COMMENT '自动订货规则';

-- 23. 采购建议单
CREATE TABLE erp_purchase_suggestion (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    no VARCHAR(64) NOT NULL COMMENT '建议单号(CGJY前缀)',
    status TINYINT NOT NULL COMMENT '10待确认 20已确认 30已生成采购单',
    warehouse_id BIGINT COMMENT '仓库ID',
    suggest_time DATETIME COMMENT '建议时间',
    total_count DECIMAL(24,6) COMMENT '合计数量',
    total_price DECIMAL(24,2) COMMENT '合计金额',
    purchase_order_id BIGINT COMMENT '生成的采购订单ID',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号'
) COMMENT '采购建议单';

-- 24. 采购建议单明细
CREATE TABLE erp_purchase_suggestion_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    suggestion_id BIGINT NOT NULL COMMENT '建议单ID',
    product_id BIGINT NOT NULL COMMENT '产品ID',
    current_stock DECIMAL(24,6) COMMENT '当前库存',
    lock_stock DECIMAL(24,6) COMMENT '锁定库存',
    avg_daily_sale DECIMAL(24,6) COMMENT '日均销量',
    suggest_count DECIMAL(24,6) COMMENT '建议采购量',
    supplier_id BIGINT COMMENT '供应商ID',
    last_purchase_price DECIMAL(24,2) COMMENT '最近采购价',
    creator VARCHAR(64) DEFAULT '' COMMENT '创建者',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updater VARCHAR(64) DEFAULT '' COMMENT '更新者',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted BIT(1) DEFAULT 0 COMMENT '是否删除',
    tenant_id BIGINT NOT NULL DEFAULT 0 COMMENT '租户编号',
    INDEX idx_suggestion_id (suggestion_id)
) COMMENT '采购建议单明细';
