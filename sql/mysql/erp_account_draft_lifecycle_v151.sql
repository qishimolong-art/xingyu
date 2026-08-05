-- ERP 账户管理草稿生命周期。
-- 1. document_status：0-草稿，10-正式；既有账户自动保持为正式。
-- 2. 草稿允许暂不填写账户名称；正式创建、更新并提交、直接提交仍由后端严格校验。
ALTER TABLE `erp_account`
    ADD COLUMN `document_status` tinyint NOT NULL DEFAULT 10
        COMMENT '提交状态：0-草稿 10-正式' AFTER `status`,
    MODIFY COLUMN `name` varchar(255) NULL COMMENT '账户名称';

CREATE INDEX `idx_document_status` ON `erp_account` (`document_status`);
