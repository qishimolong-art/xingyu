-- erp_sale_price_adjust_fix_v16: 销售调价主表字段重命名/类型修正 + 子表扩展字段
-- 兼容 MySQL 5.7 / 8.0.x 全版本（不使用 ADD COLUMN IF NOT EXISTS / CHANGE COLUMN IF EXISTS，改为基于 information_schema 判断的存储过程）
-- 幂等：重复执行安全

DROP PROCEDURE IF EXISTS erp_sale_price_adjust_fix_v16_apply;

DELIMITER $$
CREATE PROCEDURE erp_sale_price_adjust_fix_v16_apply()
BEGIN
    -- ========================================================
    -- 主表 erp_sale_price_adjust
    -- ========================================================

    -- 1. CHANGE COLUMN `dept` (varchar) -> `dept_id` (bigint, nullable)
    --    如果旧列 dept 存在且新列 dept_id 不存在，则先加新列再删旧列
    --    如果新列 dept_id 已存在，跳过
    IF EXISTS (SELECT 1 FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_price_adjust' AND COLUMN_NAME = 'dept')
       AND NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_price_adjust' AND COLUMN_NAME = 'dept_id') THEN
        ALTER TABLE erp_sale_price_adjust ADD COLUMN dept_id BIGINT DEFAULT NULL COMMENT '部门ID';
        ALTER TABLE erp_sale_price_adjust DROP COLUMN dept;
    END IF;
    -- 如果旧列已不存在但新列也不存在（极端情况），补建新列
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_price_adjust' AND COLUMN_NAME = 'dept_id') THEN
        ALTER TABLE erp_sale_price_adjust ADD COLUMN dept_id BIGINT DEFAULT NULL COMMENT '部门ID';
    END IF;

    -- 2. CHANGE COLUMN `adjust_user` (varchar) -> `adjust_user_id` (bigint, nullable)
    IF EXISTS (SELECT 1 FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_price_adjust' AND COLUMN_NAME = 'adjust_user')
       AND NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_price_adjust' AND COLUMN_NAME = 'adjust_user_id') THEN
        ALTER TABLE erp_sale_price_adjust ADD COLUMN adjust_user_id BIGINT DEFAULT NULL COMMENT '调价人ID';
        ALTER TABLE erp_sale_price_adjust DROP COLUMN adjust_user;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_price_adjust' AND COLUMN_NAME = 'adjust_user_id') THEN
        ALTER TABLE erp_sale_price_adjust ADD COLUMN adjust_user_id BIGINT DEFAULT NULL COMMENT '调价人ID';
    END IF;

    -- 3. CHANGE COLUMN `adjust_type` (varchar) -> `adjust_type` (int, default 10)
    --    列名不变，只改类型：varchar -> int
    --    MySQL 5.7 支持 MODIFY COLUMN，直接改类型（已有数据如果是纯数字字符串可自动转换）
    IF EXISTS (SELECT 1 FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_price_adjust'
                 AND COLUMN_NAME = 'adjust_type' AND DATA_TYPE = 'varchar') THEN
        ALTER TABLE erp_sale_price_adjust MODIFY COLUMN adjust_type INT DEFAULT 10 COMMENT '调价类型（10按销售单 20添加明细）';
    END IF;
    -- 如果 adjust_type 列完全不存在，补建
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_price_adjust' AND COLUMN_NAME = 'adjust_type') THEN
        ALTER TABLE erp_sale_price_adjust ADD COLUMN adjust_type INT DEFAULT 10 COMMENT '调价类型（10按销售单 20添加明细）';
    END IF;

    -- 4. ADD `settle_method` varchar(64)
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_price_adjust' AND COLUMN_NAME = 'settle_method') THEN
        ALTER TABLE erp_sale_price_adjust ADD COLUMN settle_method VARCHAR(64) DEFAULT NULL COMMENT '结算方式';
    END IF;

    -- 5. ADD `delivery_method` varchar(64)
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_price_adjust' AND COLUMN_NAME = 'delivery_method') THEN
        ALTER TABLE erp_sale_price_adjust ADD COLUMN delivery_method VARCHAR(64) DEFAULT NULL COMMENT '送货方式';
    END IF;

    -- 6. ADD `logistics_company` varchar(128)
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_price_adjust' AND COLUMN_NAME = 'logistics_company') THEN
        ALTER TABLE erp_sale_price_adjust ADD COLUMN logistics_company VARCHAR(128) DEFAULT NULL COMMENT '物流公司';
    END IF;

    -- ========================================================
    -- 子表 erp_sale_price_adjust_item
    -- ========================================================

    -- 7. ADD `adjust_reason` varchar(500)
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_price_adjust_item' AND COLUMN_NAME = 'adjust_reason') THEN
        ALTER TABLE erp_sale_price_adjust_item ADD COLUMN adjust_reason VARCHAR(500) DEFAULT NULL COMMENT '调价原因';
    END IF;

    -- 8. ADD `item_remark` varchar(500)
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_price_adjust_item' AND COLUMN_NAME = 'item_remark') THEN
        ALTER TABLE erp_sale_price_adjust_item ADD COLUMN item_remark VARCHAR(500) DEFAULT NULL COMMENT '备注';
    END IF;

    -- 9. ADD `sale_out_id` bigint
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_sale_price_adjust_item' AND COLUMN_NAME = 'sale_out_id') THEN
        ALTER TABLE erp_sale_price_adjust_item ADD COLUMN sale_out_id BIGINT DEFAULT NULL COMMENT '关联销售单ID';
    END IF;

END$$
DELIMITER ;

CALL erp_sale_price_adjust_fix_v16_apply();
DROP PROCEDURE IF EXISTS erp_sale_price_adjust_fix_v16_apply;
