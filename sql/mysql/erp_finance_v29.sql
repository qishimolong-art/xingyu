-- ===========================================================================
-- 财务核算六期：会计科目加 voucher_type 字段 + 菜单隐藏 5 个新业务表单（v29）
--
-- 1. erp_accounting_subject 加 voucher_type（凭证类型：1=客户 2=连锁）
-- 2. 隐藏 5 个二级菜单（visible=false）：
--    其他应收单(3070) / 预收款单(3075) / 预付款单(3080) / 预收账款单(3085) / 其他应付单(3090)
--    数据表保留，仅菜单隐藏，原因：凭证生成时仍需后端接口可用
--
-- 执行方式：mysql -h127.0.0.1 -P3306 -uroot -p --default-character-set=utf8mb4 ruoyi-vue-pro < erp_finance_v29.sql
-- ===========================================================================

-- ===================== 1. 会计科目加 voucher_type =====================

DROP PROCEDURE IF EXISTS erp_accounting_subject_v29_apply;
DELIMITER $$
CREATE PROCEDURE erp_accounting_subject_v29_apply()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_accounting_subject' AND COLUMN_NAME = 'voucher_type') THEN
        ALTER TABLE erp_accounting_subject ADD COLUMN voucher_type TINYINT DEFAULT 1 COMMENT '凭证类型：1=客户 2=连锁';
    END IF;
END$$
DELIMITER ;
CALL erp_accounting_subject_v29_apply();
DROP PROCEDURE IF EXISTS erp_accounting_subject_v29_apply;

-- 既有数据全部默认为 1（客户）
UPDATE erp_accounting_subject SET voucher_type = 1 WHERE voucher_type IS NULL;

-- ===================== 2. 隐藏 5 个二级业务表单菜单 =====================
-- 仅改 visible，权限点和数据保留

UPDATE system_menu SET visible = b'0' WHERE id IN (3070, 3075, 3080, 3085, 3090);
