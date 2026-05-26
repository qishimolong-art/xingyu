-- =============================================
-- 客户表单问题修复 SQL 脚本 v22
-- 执行方式: mysql -u root -p --default-character-set=utf8mb4 ruoyi-vue-pro < sql/mysql/erp_customer_fix_v22.sql
-- =============================================

-- 1. 隐藏"销售订单"菜单（不删除，仅隐藏）
UPDATE system_menu SET visible = b'0' WHERE name = '销售订单' AND deleted = b'0' AND visible = b'1';

-- 2. 隐藏"销售配置"菜单（不删除，仅隐藏）
UPDATE system_menu SET visible = b'0' WHERE name = '销售配置' AND deleted = b'0' AND visible = b'1';

-- 3. erp_customer 表 sort 字段设默认值 0（兼容 MySQL 5.7+）
DROP PROCEDURE IF EXISTS erp_customer_fix_v22_apply;
DELIMITER $$
CREATE PROCEDURE erp_customer_fix_v22_apply()
BEGIN
    -- 修改 sort 字段默认值为 0
    ALTER TABLE erp_customer MODIFY COLUMN sort INT DEFAULT 0 COMMENT '排序';

    -- 确保 code 允许 NULL（如果当前是 NOT NULL 则改为可空）
    IF EXISTS (SELECT 1 FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_customer'
               AND COLUMN_NAME = 'code' AND IS_NULLABLE = 'NO') THEN
        ALTER TABLE erp_customer MODIFY COLUMN code VARCHAR(64) DEFAULT NULL COMMENT '客户编码';
    END IF;

    -- 确保 member_code 允许 NULL
    IF EXISTS (SELECT 1 FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_customer'
               AND COLUMN_NAME = 'member_code' AND IS_NULLABLE = 'NO') THEN
        ALTER TABLE erp_customer MODIFY COLUMN member_code VARCHAR(64) DEFAULT NULL COMMENT '会员编码';
    END IF;

    -- 确保 platform_code 允许 NULL
    IF EXISTS (SELECT 1 FROM information_schema.COLUMNS
               WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_customer'
               AND COLUMN_NAME = 'platform_code' AND IS_NULLABLE = 'NO') THEN
        ALTER TABLE erp_customer MODIFY COLUMN platform_code VARCHAR(64) DEFAULT NULL COMMENT '平台唯一码';
    END IF;
END$$
DELIMITER ;
CALL erp_customer_fix_v22_apply();
DROP PROCEDURE IF EXISTS erp_customer_fix_v22_apply;
