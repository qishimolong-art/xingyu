-- ==============================================================
-- ERP 采购退货 八期 收尾补丁（终极版）：一次性放宽主表所有业务字段的 NOT NULL 约束
-- 部署：mysql -u root -p ruoyi-vue-pro < sql/mysql/erp_purchase_return_v8_nullable_all.sql
--
-- 背景：按库存退货模式下，前端只传主表的 supplier_id / return_time / return_mode / items +
--   若干用户填写的可选字段。其他大量业务字段（order_no / purchaser / invoice_type 等）
--   MyBatis-Plus 动态 SQL 会省略不传的列，而 DB 原 DDL 这些列是 NOT NULL 无默认值，
--   MySQL 拒绝 INSERT 报错："Field 'xxx' doesn't have a default value"。
--
-- 此前已单独放宽过的列（保持幂等，重复执行不出错）：
--   - account_id / discount_percent / discount_price / other_price / total_price（七期）
--   - supplier_id（七期）
--   - order_id（八期第一次放宽）
--
-- 本脚本一次性把剩余所有业务字段全部放宽为 NULL DEFAULT NULL，保留：
--   - id / no / status / tenant_id 保持 NOT NULL
--   - BaseDO 系统字段（create_time / update_time / creator / updater / deleted）保持原状
--   - return_mode / return_time 保留 NOT NULL（业务必填，前端一定会传）
--
-- 幂等性：MySQL MODIFY COLUMN 可重复执行，不会报错。
-- ==============================================================

ALTER TABLE `erp_purchase_return`
    MODIFY COLUMN `order_no`           VARCHAR(64)    NULL     DEFAULT NULL   COMMENT '采购订单号（按库存退货可空）',
    MODIFY COLUMN `refund_price`       DECIMAL(20,4)  NULL     DEFAULT 0.0000 COMMENT '已退款金额',
    MODIFY COLUMN `total_count`        DECIMAL(20,4)  NULL     DEFAULT 0.0000 COMMENT '合计数量',
    MODIFY COLUMN `total_product_price` DECIMAL(20,4) NULL     DEFAULT 0.0000 COMMENT '合计产品价格',
    MODIFY COLUMN `total_tax_price`    DECIMAL(20,4)  NULL     DEFAULT 0.0000 COMMENT '合计税额',
    MODIFY COLUMN `file_url`           VARCHAR(512)   NULL     DEFAULT NULL   COMMENT '附件地址',
    MODIFY COLUMN `remark`             VARCHAR(500)   NULL     DEFAULT NULL   COMMENT '备注';

-- 汽配扩展字段（七期加的，部分未放宽；统一放宽）
ALTER TABLE `erp_purchase_return`
    MODIFY COLUMN `return_type`        VARCHAR(64)    NULL     DEFAULT NULL   COMMENT '退货类型（已弃用，由 return_mode 取代）',
    MODIFY COLUMN `purchaser`          VARCHAR(64)    NULL     DEFAULT NULL   COMMENT '采购员（八期已改为用户下拉，旧字符串字段保留兼容）',
    MODIFY COLUMN `invoice_type`       VARCHAR(64)    NULL     DEFAULT NULL   COMMENT '开票类型',
    MODIFY COLUMN `transport_method`   VARCHAR(64)    NULL     DEFAULT NULL   COMMENT '运输方式',
    MODIFY COLUMN `settle_method`      VARCHAR(64)    NULL     DEFAULT NULL   COMMENT '结算方式',
    MODIFY COLUMN `package_count`      INT            NULL     DEFAULT 0      COMMENT '件数',
    MODIFY COLUMN `freight_amount`     DECIMAL(20,4)  NULL     DEFAULT 0.0000 COMMENT '运费金额',
    MODIFY COLUMN `logistics_company`  VARCHAR(255)   NULL     DEFAULT NULL   COMMENT '物流公司',
    MODIFY COLUMN `doc_source`         VARCHAR(64)    NULL     DEFAULT NULL   COMMENT '单据来源',
    MODIFY COLUMN `factory_order_no`   VARCHAR(64)    NULL     DEFAULT NULL   COMMENT '厂家单号',
    MODIFY COLUMN `maker`              VARCHAR(64)    NULL     DEFAULT NULL   COMMENT '制单人（旧字符串字段，八期改用 handler BIGINT）',
    MODIFY COLUMN `dept`               VARCHAR(64)    NULL     DEFAULT NULL   COMMENT '部门（旧字符串字段，八期改用 dept_id BIGINT）',
    MODIFY COLUMN `shipping_area`      VARCHAR(64)    NULL     DEFAULT NULL   COMMENT '发货区',
    MODIFY COLUMN `warehouse_type`     VARCHAR(64)    NULL     DEFAULT NULL   COMMENT '仓库类型',
    MODIFY COLUMN `freight_type`       VARCHAR(64)    NULL     DEFAULT NULL   COMMENT '运费类型',
    MODIFY COLUMN `logistics_no`       VARCHAR(128)   NULL     DEFAULT NULL   COMMENT '物流单号',
    MODIFY COLUMN `priority`           VARCHAR(32)    NULL     DEFAULT NULL   COMMENT '优先级',
    MODIFY COLUMN `order_method`       VARCHAR(64)    NULL     DEFAULT NULL   COMMENT '开单方式';

-- ==============================================================
-- 脚本结束
-- ==============================================================
