-- ERP sale config v13: generic sales configuration dictionary.
-- Execute after erp_sale_price_adjust_v12.sql.

CREATE TABLE IF NOT EXISTS `erp_sale_config` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'ID',
    `config_type` varchar(64) NOT NULL COMMENT 'Config type',
    `code` varchar(64) NOT NULL COMMENT 'Code',
    `name` varchar(128) NOT NULL COMMENT 'Name',
    `config_value` varchar(1000) DEFAULT NULL COMMENT 'Extra config value',
    `status` tinyint NOT NULL DEFAULT 0 COMMENT 'Status: 0 enabled, 1 disabled',
    `sort` int DEFAULT 0 COMMENT 'Sort',
    `remark` varchar(500) DEFAULT NULL COMMENT 'Remark',
    `creator` varchar(64) DEFAULT '' COMMENT 'Creator',
    `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT 'Create time',
    `updater` varchar(64) DEFAULT '' COMMENT 'Updater',
    `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT 'Update time',
    `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT 'Deleted',
    `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT 'Tenant id',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_sale_config_type_code` (`config_type`, `code`, `deleted`, `tenant_id`),
    KEY `idx_sale_config_type_status` (`config_type`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP sale config';

INSERT INTO `erp_sale_config` (`config_type`, `code`, `name`, `status`, `sort`, `remark`)
VALUES
    ('CONTRACT_TYPE', 'standard', '标准合同', 0, 10, '销售合同类型'),
    ('ROUTE', 'default', '默认线路', 0, 10, '物流线路'),
    ('FREIGHT_EXPLAIN', 'customer_pay', '客户承担运费', 0, 10, '运费说明'),
    ('INTERNAL_ACCOUNT', 'default', '默认内部账户', 0, 10, '内部账户'),
    ('CHAIN_GROUP', 'default', '默认连锁/集团', 0, 10, '连锁/集团信息'),
    ('GENERAL_DISTRIBUTOR', 'default', '默认总经销', 0, 10, '总经销信息'),
    ('TASK_LEVEL', 'monthly', '月度任务', 0, 10, '任务级别')
ON DUPLICATE KEY UPDATE `name` = VALUES(`name`);
