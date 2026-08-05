-- ERP 仓库移货单草稿允许尚未填写移货日期、移出仓库和移入仓库；
-- 正式创建、更新及草稿提交仍由 VO / Service 执行严格必填校验。
ALTER TABLE `erp_warehouse_move`
    MODIFY COLUMN `move_time` DATETIME NULL DEFAULT NULL COMMENT '移货日期（草稿可空）',
    MODIFY COLUMN `from_warehouse_id` BIGINT NULL DEFAULT NULL COMMENT '移出仓库（草稿可空）',
    MODIFY COLUMN `to_warehouse_id` BIGINT NULL DEFAULT NULL COMMENT '移入仓库（草稿可空）';
