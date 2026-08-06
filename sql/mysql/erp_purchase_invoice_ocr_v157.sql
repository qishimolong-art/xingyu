-- ERP purchase invoice OCR foundation (v157)
-- Adds OCR batch/item tables and minimal query/upload permissions.
--
-- Safety:
--   - Append-only tables and menu permissions.
--   - No DELETE and no broad overwrite of role permissions.
--   - OCR credentials are not stored in this script.

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `erp_purchase_invoice_ocr_batch` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_no` varchar(64) DEFAULT NULL COMMENT '批次号',
  `status` varchar(32) NOT NULL COMMENT '批次状态',
  `file_count` int NOT NULL DEFAULT 0 COMMENT '文件数量',
  `generated_invoice_count` int NOT NULL DEFAULT 0 COMMENT '已生成采购票据数量',
  `remark` varchar(512) DEFAULT NULL COMMENT '备注',
  `error_msg` varchar(1024) DEFAULT NULL COMMENT '异常信息',
  `creator` varchar(64) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_batch_no` (`batch_no`),
  KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 采购发票 OCR 批次';

CREATE TABLE IF NOT EXISTS `erp_purchase_invoice_ocr_item` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `batch_id` bigint NOT NULL COMMENT 'OCR 批次ID',
  `status` varchar(32) NOT NULL COMMENT '明细状态',
  `file_name` varchar(255) NOT NULL COMMENT '原始文件名',
  `file_url` varchar(512) NOT NULL COMMENT '文件URL',
  `file_type` varchar(128) DEFAULT NULL COMMENT '文件类型',
  `file_size` bigint DEFAULT NULL COMMENT '文件大小',
  `invoice_no` varchar(128) DEFAULT NULL COMMENT '发票号码',
  `invoice_date` date DEFAULT NULL COMMENT '开票日期',
  `invoice_type` varchar(128) DEFAULT NULL COMMENT '发票类型',
  `total_amount` decimal(24,6) DEFAULT NULL COMMENT '发票金额',
  `tax_exclusive_amount` decimal(24,6) DEFAULT NULL COMMENT '不含税金额',
  `tax_amount` decimal(24,6) DEFAULT NULL COMMENT '税额',
  `invoice_remark` varchar(1024) DEFAULT NULL COMMENT 'OCR识别备注',
  `parsed_factory_order_no` varchar(128) DEFAULT NULL COMMENT 'OCR自动解析厂家单号',
  `factory_order_no` varchar(128) DEFAULT NULL COMMENT '最终匹配厂家单号',
  `raw_json` longtext COMMENT 'OCR原始响应',
  `matched_purchase_in_ids` varchar(512) DEFAULT NULL COMMENT '匹配采购入库ID快照',
  `matched_purchase_in_nos` varchar(1024) DEFAULT NULL COMMENT '匹配采购入库单号快照',
  `purchase_in_total_amount` decimal(24,6) DEFAULT NULL COMMENT '匹配时采购入库金额合计快照',
  `diff_amount` decimal(24,6) DEFAULT NULL COMMENT '差额',
  `generated_invoice_id` bigint DEFAULT NULL COMMENT '生成的采购票据ID',
  `error_msg` varchar(1024) DEFAULT NULL COMMENT '异常信息',
  `creator` varchar(64) DEFAULT NULL,
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updater` varchar(64) DEFAULT NULL,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `deleted` bit(1) NOT NULL DEFAULT b'0',
  `tenant_id` bigint NOT NULL DEFAULT 0,
  PRIMARY KEY (`id`),
  KEY `idx_batch_id` (`batch_id`),
  KEY `idx_status` (`status`),
  KEY `idx_factory_order_no` (`factory_order_no`),
  KEY `idx_invoice_no` (`invoice_no`),
  KEY `idx_generated_invoice_id` (`generated_invoice_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 采购发票 OCR 明细';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(900210, '采购发票识别核对', '', 2, 6, 2602, 'invoice-ocr', 'fa:file-text-o',
 'erp/purchase/invoice-ocr/index', 'ErpPurchaseInvoiceOcr',
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
 `name` = VALUES(`name`),
 `permission` = VALUES(`permission`),
 `type` = VALUES(`type`),
 `sort` = VALUES(`sort`),
 `parent_id` = VALUES(`parent_id`),
 `path` = VALUES(`path`),
 `icon` = VALUES(`icon`),
 `component` = VALUES(`component`),
 `component_name` = VALUES(`component_name`),
 `status` = VALUES(`status`),
 `visible` = VALUES(`visible`),
 `keep_alive` = VALUES(`keep_alive`),
 `always_show` = VALUES(`always_show`),
 `updater` = '1',
 `update_time` = NOW(),
 `deleted` = b'0';

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
(900211, '采购发票识别查询', 'erp:purchase-invoice-ocr:query', 3, 1, 900210, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(900212, '采购发票识别上传', 'erp:purchase-invoice-ocr:upload', 3, 2, 900210, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0')
ON DUPLICATE KEY UPDATE
 `name` = VALUES(`name`),
 `permission` = VALUES(`permission`),
 `type` = VALUES(`type`),
 `sort` = VALUES(`sort`),
 `parent_id` = VALUES(`parent_id`),
 `path` = VALUES(`path`),
 `icon` = VALUES(`icon`),
 `component` = VALUES(`component`),
 `component_name` = VALUES(`component_name`),
 `status` = VALUES(`status`),
 `visible` = VALUES(`visible`),
 `keep_alive` = VALUES(`keep_alive`),
 `always_show` = VALUES(`always_show`),
 `updater` = '1',
 `update_time` = NOW(),
 `deleted` = b'0';

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT owned_role.role_id,
       target_menu.id,
       '1',
       NOW(),
       '1',
       NOW(),
       b'0',
       owned_role.tenant_id
FROM system_role_menu owned_role
JOIN system_menu owned_menu
  ON owned_menu.id = owned_role.menu_id
 AND owned_menu.deleted = b'0'
 AND owned_menu.permission COLLATE utf8mb4_unicode_ci IN (
       'erp:purchase-invoice:query' COLLATE utf8mb4_unicode_ci,
       'erp:purchase-invoice:create' COLLATE utf8mb4_unicode_ci
 )
JOIN system_menu target_menu
  ON target_menu.deleted = b'0'
 AND target_menu.id IN (900210, 900211, 900212)
WHERE owned_role.deleted = b'0'
  AND NOT EXISTS (
      SELECT 1
      FROM system_role_menu exists_role_menu
      WHERE exists_role_menu.role_id = owned_role.role_id
        AND exists_role_menu.menu_id = target_menu.id
        AND exists_role_menu.tenant_id = owned_role.tenant_id
        AND exists_role_menu.deleted = b'0'
  );

INSERT INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
SELECT DISTINCT super_role.id,
       target_menu.id,
       '1',
       NOW(),
       '1',
       NOW(),
       b'0',
       super_role.tenant_id
FROM system_role super_role
JOIN system_menu target_menu
  ON target_menu.deleted = b'0'
 AND target_menu.id IN (900210, 900211, 900212)
WHERE super_role.code COLLATE utf8mb4_unicode_ci = 'super_admin' COLLATE utf8mb4_unicode_ci
  AND super_role.deleted = b'0'
  AND NOT EXISTS (
      SELECT 1
      FROM system_role_menu exists_role_menu
      WHERE exists_role_menu.role_id = super_role.id
        AND exists_role_menu.menu_id = target_menu.id
        AND exists_role_menu.tenant_id = super_role.tenant_id
        AND exists_role_menu.deleted = b'0'
  );

SELECT m.id,
       m.name,
       m.permission,
       m.parent_id,
       m.sort,
       COUNT(DISTINCT rm.role_id) AS granted_role_count
FROM system_menu m
LEFT JOIN system_role_menu rm
  ON rm.menu_id = m.id
 AND rm.deleted = b'0'
WHERE m.id IN (900210, 900211, 900212)
  AND m.deleted = b'0'
GROUP BY m.id, m.name, m.permission, m.parent_id, m.sort
ORDER BY m.id;
