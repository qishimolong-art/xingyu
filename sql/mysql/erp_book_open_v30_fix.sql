-- =====================================================================
-- S6 修复：给 erp_book_open 表加唯一索引兜底
--   防止并发请求绕过 Service 层 validateBookOpenDuplicate 内存校验，
--   导致同 (chain_name, fiscal_year, period) 出现重复记录。
--
-- 索引定义：uk_chain_year_period (chain_name, fiscal_year, period, deleted, tenant_id)
--   - deleted 参与索引：保证逻辑删除后可重新创建同期间记录
--   - tenant_id 参与索引：满足多租户隔离
--
-- 兼容性：使用 information_schema.STATISTICS 存储过程方案，
--         兼容 MySQL 5.7 / 8.0.x 全版本（项目要求禁用 8.0.23+ 专有语法）
-- =====================================================================

DROP PROCEDURE IF EXISTS erp_book_open_v30_fix_apply;
DELIMITER $$
CREATE PROCEDURE erp_book_open_v30_fix_apply()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.STATISTICS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'erp_book_open' AND INDEX_NAME = 'uk_chain_year_period') THEN
        ALTER TABLE erp_book_open ADD UNIQUE KEY uk_chain_year_period (chain_name, fiscal_year, period, deleted, tenant_id);
    END IF;
END$$
DELIMITER ;
CALL erp_book_open_v30_fix_apply();
DROP PROCEDURE IF EXISTS erp_book_open_v30_fix_apply;
