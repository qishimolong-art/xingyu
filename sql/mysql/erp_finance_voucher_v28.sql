-- ===========================================================================
-- 财务核算五期：5 张新业务单据建表 + 预置科目 + 菜单（v28）
--
-- 新增单据：
--   1. erp_other_receivable / erp_other_receivable_item  (其他应收单)
--   2. erp_pre_receipt / erp_pre_receipt_item            (预收款单)
--   3. erp_pre_payment / erp_pre_payment_item            (预付款单)
--   4. erp_pre_receivable / erp_pre_receivable_item      (预收账款单)
--   5. erp_other_payable / erp_other_payable_item        (其他应付单)
--
-- 新增科目：1221 其他应收款 / 2203 预收账款 / 2204 预收账款(另) / 2241 其他应付款
-- （1123 预付账款 二期已预置，无需重复）
--
-- 执行方式：mysql -h127.0.0.1 -P3306 -uroot -p --default-character-set=utf8mb4 ruoyi-vue-pro < erp_finance_voucher_v28.sql
-- ===========================================================================

-- ===================== 1. 预置科目 =====================

INSERT IGNORE INTO `erp_accounting_subject`
(`subject_code`, `subject_name`, `short_name`, `subject_category`, `parent_code`,
 `subject_level`, `is_leaf`, `balance_direction`, `opening_balance`, `enable`,
 `sort`, `creator`, `updater`, `tenant_id`)
VALUES
('1221', '其他应收款',   '其他应收', 1, NULL, 1, b'1', 1, 0.00, b'1', 17, '1', '1', 1),
('2203', '预收账款',     '预收',     2, NULL, 1, b'1', 2, 0.00, b'1', 18, '1', '1', 1),
('2204', '预收账款(另)', '预收另',   2, NULL, 1, b'1', 2, 0.00, b'1', 19, '1', '1', 1),
('2241', '其他应付款',   '其他应付', 2, NULL, 1, b'1', 2, 0.00, b'1', 20, '1', '1', 1);

-- ===================== 2. 其他应收单 =====================

DROP TABLE IF EXISTS `erp_other_receivable`;
CREATE TABLE `erp_other_receivable` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `no`              VARCHAR(64)  NOT NULL COMMENT '单据编号',
  `status`          TINYINT      NOT NULL DEFAULT 10 COMMENT '审核状态：10=未审核 20=已审核 30=已反审',
  `biz_time`        DATETIME     NOT NULL COMMENT '业务日期',
  `party_type`      TINYINT      NOT NULL COMMENT '对方类型：1=客户 2=供应商 3=员工',
  `party_id`        BIGINT       NULL     COMMENT '对方ID',
  `party_name`      VARCHAR(128) NULL     COMMENT '对方名称',
  `account_id`      BIGINT       NULL     COMMENT '结算账户ID',
  `total_amount`    DECIMAL(24,6) NOT NULL DEFAULT 0 COMMENT '合计金额',
  `discount_amount` DECIMAL(24,6) NOT NULL DEFAULT 0 COMMENT '折让金额',
  `actual_amount`   DECIMAL(24,6) NOT NULL DEFAULT 0 COMMENT '实际金额',
  `remark`          VARCHAR(512) NULL     COMMENT '备注',
  `file_url`        VARCHAR(512) NULL     COMMENT '附件URL',
  `creator`         VARCHAR(64)  NULL     COMMENT '创建者',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`         VARCHAR(64)  NULL     COMMENT '更新者',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`         BIT(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`       BIGINT       NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_no` (`no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 其他应收单';

DROP TABLE IF EXISTS `erp_other_receivable_item`;
CREATE TABLE `erp_other_receivable_item` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `receivable_id` BIGINT     NOT NULL COMMENT '其他应收单ID',
  `summary`     VARCHAR(256)  NULL     COMMENT '摘要',
  `amount`      DECIMAL(24,6) NOT NULL DEFAULT 0 COMMENT '金额',
  `remark`      VARCHAR(512)  NULL     COMMENT '备注',
  `creator`     VARCHAR(64)   NULL     COMMENT '创建者',
  `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`     VARCHAR(64)   NULL     COMMENT '更新者',
  `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     BIT(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`   BIGINT        NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_receivable_id` (`receivable_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 其他应收单明细';

-- ===================== 3. 预收款单 =====================

DROP TABLE IF EXISTS `erp_pre_receipt`;
CREATE TABLE `erp_pre_receipt` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `no`              VARCHAR(64)  NOT NULL COMMENT '单据编号',
  `status`          TINYINT      NOT NULL DEFAULT 10 COMMENT '审核状态：10=未审核 20=已审核 30=已反审',
  `biz_time`        DATETIME     NOT NULL COMMENT '业务日期',
  `party_type`      TINYINT      NOT NULL COMMENT '对方类型：1=客户 2=供应商 3=员工',
  `party_id`        BIGINT       NULL     COMMENT '对方ID',
  `party_name`      VARCHAR(128) NULL     COMMENT '对方名称',
  `account_id`      BIGINT       NULL     COMMENT '结算账户ID',
  `total_amount`    DECIMAL(24,6) NOT NULL DEFAULT 0 COMMENT '合计金额',
  `discount_amount` DECIMAL(24,6) NOT NULL DEFAULT 0 COMMENT '折让金额',
  `actual_amount`   DECIMAL(24,6) NOT NULL DEFAULT 0 COMMENT '实际金额',
  `remark`          VARCHAR(512) NULL     COMMENT '备注',
  `file_url`        VARCHAR(512) NULL     COMMENT '附件URL',
  `creator`         VARCHAR(64)  NULL     COMMENT '创建者',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`         VARCHAR(64)  NULL     COMMENT '更新者',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`         BIT(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`       BIGINT       NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_no` (`no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 预收款单';

DROP TABLE IF EXISTS `erp_pre_receipt_item`;
CREATE TABLE `erp_pre_receipt_item` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `pre_receipt_id` BIGINT    NOT NULL COMMENT '预收款单ID',
  `summary`     VARCHAR(256)  NULL     COMMENT '摘要',
  `amount`      DECIMAL(24,6) NOT NULL DEFAULT 0 COMMENT '金额',
  `remark`      VARCHAR(512)  NULL     COMMENT '备注',
  `creator`     VARCHAR(64)   NULL     COMMENT '创建者',
  `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`     VARCHAR(64)   NULL     COMMENT '更新者',
  `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     BIT(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`   BIGINT        NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_pre_receipt_id` (`pre_receipt_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 预收款单明细';

-- ===================== 4. 预付款单 =====================

DROP TABLE IF EXISTS `erp_pre_payment`;
CREATE TABLE `erp_pre_payment` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `no`              VARCHAR(64)  NOT NULL COMMENT '单据编号',
  `status`          TINYINT      NOT NULL DEFAULT 10 COMMENT '审核状态：10=未审核 20=已审核 30=已反审',
  `biz_time`        DATETIME     NOT NULL COMMENT '业务日期',
  `party_type`      TINYINT      NOT NULL COMMENT '对方类型：1=客户 2=供应商 3=员工',
  `party_id`        BIGINT       NULL     COMMENT '对方ID',
  `party_name`      VARCHAR(128) NULL     COMMENT '对方名称',
  `account_id`      BIGINT       NULL     COMMENT '结算账户ID',
  `total_amount`    DECIMAL(24,6) NOT NULL DEFAULT 0 COMMENT '合计金额',
  `discount_amount` DECIMAL(24,6) NOT NULL DEFAULT 0 COMMENT '折让金额',
  `actual_amount`   DECIMAL(24,6) NOT NULL DEFAULT 0 COMMENT '实际金额',
  `remark`          VARCHAR(512) NULL     COMMENT '备注',
  `file_url`        VARCHAR(512) NULL     COMMENT '附件URL',
  `creator`         VARCHAR(64)  NULL     COMMENT '创建者',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`         VARCHAR(64)  NULL     COMMENT '更新者',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`         BIT(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`       BIGINT       NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_no` (`no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 预付款单';

DROP TABLE IF EXISTS `erp_pre_payment_item`;
CREATE TABLE `erp_pre_payment_item` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `pre_payment_id` BIGINT    NOT NULL COMMENT '预付款单ID',
  `summary`     VARCHAR(256)  NULL     COMMENT '摘要',
  `amount`      DECIMAL(24,6) NOT NULL DEFAULT 0 COMMENT '金额',
  `remark`      VARCHAR(512)  NULL     COMMENT '备注',
  `creator`     VARCHAR(64)   NULL     COMMENT '创建者',
  `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`     VARCHAR(64)   NULL     COMMENT '更新者',
  `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     BIT(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`   BIGINT        NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_pre_payment_id` (`pre_payment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 预付款单明细';

-- ===================== 5. 预收账款单 =====================

DROP TABLE IF EXISTS `erp_pre_receivable`;
CREATE TABLE `erp_pre_receivable` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `no`              VARCHAR(64)  NOT NULL COMMENT '单据编号',
  `status`          TINYINT      NOT NULL DEFAULT 10 COMMENT '审核状态：10=未审核 20=已审核 30=已反审',
  `biz_time`        DATETIME     NOT NULL COMMENT '业务日期',
  `party_type`      TINYINT      NOT NULL COMMENT '对方类型：1=客户 2=供应商 3=员工',
  `party_id`        BIGINT       NULL     COMMENT '对方ID',
  `party_name`      VARCHAR(128) NULL     COMMENT '对方名称',
  `account_id`      BIGINT       NULL     COMMENT '结算账户ID',
  `total_amount`    DECIMAL(24,6) NOT NULL DEFAULT 0 COMMENT '合计金额',
  `discount_amount` DECIMAL(24,6) NOT NULL DEFAULT 0 COMMENT '折让金额',
  `actual_amount`   DECIMAL(24,6) NOT NULL DEFAULT 0 COMMENT '实际金额',
  `remark`          VARCHAR(512) NULL     COMMENT '备注',
  `file_url`        VARCHAR(512) NULL     COMMENT '附件URL',
  `creator`         VARCHAR(64)  NULL     COMMENT '创建者',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`         VARCHAR(64)  NULL     COMMENT '更新者',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`         BIT(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`       BIGINT       NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_no` (`no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 预收账款单';

DROP TABLE IF EXISTS `erp_pre_receivable_item`;
CREATE TABLE `erp_pre_receivable_item` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `pre_receivable_id` BIGINT NOT NULL COMMENT '预收账款单ID',
  `summary`     VARCHAR(256)  NULL     COMMENT '摘要',
  `amount`      DECIMAL(24,6) NOT NULL DEFAULT 0 COMMENT '金额',
  `remark`      VARCHAR(512)  NULL     COMMENT '备注',
  `creator`     VARCHAR(64)   NULL     COMMENT '创建者',
  `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`     VARCHAR(64)   NULL     COMMENT '更新者',
  `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     BIT(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`   BIGINT        NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_pre_receivable_id` (`pre_receivable_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 预收账款单明细';

-- ===================== 6. 其他应付单 =====================

DROP TABLE IF EXISTS `erp_other_payable`;
CREATE TABLE `erp_other_payable` (
  `id`              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键',
  `no`              VARCHAR(64)  NOT NULL COMMENT '单据编号',
  `status`          TINYINT      NOT NULL DEFAULT 10 COMMENT '审核状态：10=未审核 20=已审核 30=已反审',
  `biz_time`        DATETIME     NOT NULL COMMENT '业务日期',
  `party_type`      TINYINT      NOT NULL COMMENT '对方类型：1=客户 2=供应商 3=员工',
  `party_id`        BIGINT       NULL     COMMENT '对方ID',
  `party_name`      VARCHAR(128) NULL     COMMENT '对方名称',
  `account_id`      BIGINT       NULL     COMMENT '结算账户ID',
  `total_amount`    DECIMAL(24,6) NOT NULL DEFAULT 0 COMMENT '合计金额',
  `discount_amount` DECIMAL(24,6) NOT NULL DEFAULT 0 COMMENT '折让金额',
  `actual_amount`   DECIMAL(24,6) NOT NULL DEFAULT 0 COMMENT '实际金额',
  `remark`          VARCHAR(512) NULL     COMMENT '备注',
  `file_url`        VARCHAR(512) NULL     COMMENT '附件URL',
  `creator`         VARCHAR(64)  NULL     COMMENT '创建者',
  `create_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`         VARCHAR(64)  NULL     COMMENT '更新者',
  `update_time`     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`         BIT(1)       NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`       BIGINT       NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_no` (`no`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 其他应付单';

DROP TABLE IF EXISTS `erp_other_payable_item`;
CREATE TABLE `erp_other_payable_item` (
  `id`          BIGINT        NOT NULL AUTO_INCREMENT COMMENT '主键',
  `payable_id`  BIGINT        NOT NULL COMMENT '其他应付单ID',
  `summary`     VARCHAR(256)  NULL     COMMENT '摘要',
  `amount`      DECIMAL(24,6) NOT NULL DEFAULT 0 COMMENT '金额',
  `remark`      VARCHAR(512)  NULL     COMMENT '备注',
  `creator`     VARCHAR(64)   NULL     COMMENT '创建者',
  `create_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater`     VARCHAR(64)   NULL     COMMENT '更新者',
  `update_time` DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted`     BIT(1)        NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id`   BIGINT        NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`),
  KEY `idx_payable_id` (`payable_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 其他应付单明细';

-- ===================== 7. 菜单 =====================
-- parent_id 3030 = 财务核算目录（二期已建）
-- 菜单 ID 从 3070 开始，避免与二期 3030-3065 冲突

DELETE FROM `system_menu` WHERE `id` BETWEEN 3070 AND 3099;

INSERT INTO `system_menu`
(`id`, `name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
VALUES
-- 其他应收单
(3070, '其他应收单', '', 2, 10, 3030, 'other-receivable', 'ep:money',
 'erp/accounting/other-receivable/index', 'ErpOtherReceivable',
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3071, '其他应收单查询', 'erp:other-receivable:query', 3, 1, 3070, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3072, '其他应收单创建', 'erp:other-receivable:create', 3, 2, 3070, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3073, '其他应收单修改', 'erp:other-receivable:update', 3, 3, 3070, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3074, '其他应收单删除', 'erp:other-receivable:delete', 3, 4, 3070, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),

-- 预收款单
(3075, '预收款单', '', 2, 11, 3030, 'pre-receipt', 'ep:wallet',
 'erp/accounting/pre-receipt/index', 'ErpPreReceipt',
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3076, '预收款单查询', 'erp:pre-receipt:query', 3, 1, 3075, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3077, '预收款单创建', 'erp:pre-receipt:create', 3, 2, 3075, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3078, '预收款单修改', 'erp:pre-receipt:update', 3, 3, 3075, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3079, '预收款单删除', 'erp:pre-receipt:delete', 3, 4, 3075, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),

-- 预付款单
(3080, '预付款单', '', 2, 12, 3030, 'pre-payment', 'ep:credit-card',
 'erp/accounting/pre-payment/index', 'ErpPrePayment',
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3081, '预付款单查询', 'erp:pre-payment:query', 3, 1, 3080, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3082, '预付款单创建', 'erp:pre-payment:create', 3, 2, 3080, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3083, '预付款单修改', 'erp:pre-payment:update', 3, 3, 3080, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3084, '预付款单删除', 'erp:pre-payment:delete', 3, 4, 3080, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),

-- 预收账款单
(3085, '预收账款单', '', 2, 13, 3030, 'pre-receivable', 'ep:document',
 'erp/accounting/pre-receivable/index', 'ErpPreReceivable',
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3086, '预收账款单查询', 'erp:pre-receivable:query', 3, 1, 3085, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3087, '预收账款单创建', 'erp:pre-receivable:create', 3, 2, 3085, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3088, '预收账款单修改', 'erp:pre-receivable:update', 3, 3, 3085, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3089, '预收账款单删除', 'erp:pre-receivable:delete', 3, 4, 3085, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),

-- 其他应付单
(3090, '其他应付单', '', 2, 14, 3030, 'other-payable', 'ep:coin',
 'erp/accounting/other-payable/index', 'ErpOtherPayable',
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3091, '其他应付单查询', 'erp:other-payable:query', 3, 1, 3090, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3092, '其他应付单创建', 'erp:other-payable:create', 3, 2, 3090, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3093, '其他应付单修改', 'erp:other-payable:update', 3, 3, 3090, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'),
(3094, '其他应付单删除', 'erp:other-payable:delete', 3, 4, 3090, '', '', '', NULL,
 0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0');

-- 授权超管
INSERT IGNORE INTO `system_role_menu`
(`role_id`, `menu_id`, `creator`, `create_time`, `updater`, `update_time`, `deleted`, `tenant_id`)
VALUES
(1, 3070, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3071, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3072, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3073, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3074, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3075, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3076, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3077, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3078, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3079, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3080, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3081, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3082, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3083, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3084, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3085, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3086, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3087, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3088, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3089, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3090, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3091, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3092, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3093, '1', NOW(), '1', NOW(), b'0', 1),
(1, 3094, '1', NOW(), '1', NOW(), b'0', 1);
