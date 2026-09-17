-- ERP 凭证生成快照字段热修：修复新版代码查询 erp_voucher 时旧库缺 generation_snapshot 导致的 Unknown column。
SET @erp_voucher_generation_snapshot_fix_ddl = IF(
    EXISTS(SELECT 1 FROM information_schema.tables
           WHERE table_schema = DATABASE() AND table_name = 'erp_voucher')
    AND NOT EXISTS(SELECT 1 FROM information_schema.columns
                   WHERE table_schema = DATABASE() AND table_name = 'erp_voucher' AND column_name = 'generation_snapshot'),
    'ALTER TABLE `erp_voucher` ADD COLUMN `generation_snapshot` LONGTEXT NULL COMMENT ''生成时来源、规则版本、金额依据快照'' AFTER `remark`',
    'SELECT 1'
);
PREPARE erp_voucher_generation_snapshot_fix_stmt FROM @erp_voucher_generation_snapshot_fix_ddl;
EXECUTE erp_voucher_generation_snapshot_fix_stmt;
DEALLOCATE PREPARE erp_voucher_generation_snapshot_fix_stmt;
