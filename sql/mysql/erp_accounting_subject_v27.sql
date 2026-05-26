-- ===========================================================================
-- 财务核算四期：补齐其他出/入库自动凭证使用的对方科目（v27）
--
-- 新增 1901 待处理财产损益（资产类，借方余额）
-- 用途：
--   - 其他入库（盘盈/收料/捐赠等） 借 1405 库存商品 / 贷 1901 待处理财产损益
--   - 其他出库（盘亏/领用/损耗等） 借 1901 待处理财产损益 / 贷 1405 库存商品
--
-- 与二期 erp_accounting_subject_v26.sql 列结构完全一致；INSERT IGNORE 支持重复执行。
-- ===========================================================================

INSERT IGNORE INTO `erp_accounting_subject`
(`subject_code`, `subject_name`, `short_name`, `subject_category`, `parent_code`,
 `subject_level`, `is_leaf`, `balance_direction`, `opening_balance`, `enable`,
 `sort`, `creator`, `updater`, `tenant_id`)
VALUES
('1901',   '待处理财产损益', '待损益',   1, NULL, 1, b'1', 1, 0.00, b'1', 16, '1', '1', 1);
