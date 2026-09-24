-- ERP cloud print integration for sw-aiot (v239).
-- Safe to rerun. This script creates cloud print device/task/callback tables
-- only; it does not delete or overwrite menu, role, permission, or business data.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `erp_cloud_print_device` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `devid` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '设备机器码',
  `dev_key` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '设备绑定校验码',
  `nickname` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '设备别名',
  `dev_type` tinyint NOT NULL COMMENT '设备类型：1=80mm热敏 2=标签 3=A4盒子 4=针式',
  `content_type` tinyint NOT NULL DEFAULT 9 COMMENT '内容类型：7=PDF 9=HTML',
  `print_width` int NOT NULL COMMENT '有效打印宽度(mm)',
  `print_height` int NULL DEFAULT NULL COMMENT '纸张高度(mm)，针式必填',
  `paper_type` tinyint NULL DEFAULT 4 COMMENT '纸张类型',
  `rotate` tinyint NULL DEFAULT 0 COMMENT '旋转参数',
  `copies` int NOT NULL DEFAULT 1 COMMENT '默认份数',
  `dept_id` bigint NULL DEFAULT NULL COMMENT '归属部门',
  `defaulted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否默认设备',
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0=启用 1=停用',
  `online_state` tinyint NULL DEFAULT NULL COMMENT '最近在线状态：0=离线 1=在线',
  `last_status_code` int NULL DEFAULT NULL COMMENT '最近设备状态码',
  `last_status_time` datetime NULL DEFAULT NULL COMMENT '最近状态时间',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '备注',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_devid_tenant` (`devid`, `tenant_id`, `deleted`) USING BTREE,
  KEY `idx_default_status` (`defaulted`, `status`, `tenant_id`, `deleted`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP 云打印设备';

CREATE TABLE IF NOT EXISTS `erp_cloud_print_task` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `reqid` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '云打印任务号',
  `biz_type` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '业务类型',
  `biz_id` bigint NOT NULL COMMENT '业务编号',
  `biz_no` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '业务单号',
  `warehouse_id` bigint NULL DEFAULT NULL COMMENT '仓库编号',
  `warehouse_name` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '仓库名称',
  `devid` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '设备机器码',
  `device_id` bigint NULL DEFAULT NULL COMMENT '设备档案编号',
  `template_id` bigint NULL DEFAULT NULL COMMENT '打印模板编号',
  `content_type` tinyint NOT NULL COMMENT '提交内容类型',
  `copies` int NOT NULL DEFAULT 1 COMMENT '份数',
  `file_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '文件地址',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '文件名',
  `status` tinyint NOT NULL COMMENT '状态：0待提交 1已入队 2成功 3失败 4提交失败 5未知 6超时 7取消',
  `submit_req` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '提交参数快照',
  `submit_resp` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '第三方响应',
  `submit_time` datetime NULL DEFAULT NULL COMMENT '提交时间',
  `callback_code` int NULL DEFAULT NULL COMMENT '回调状态码',
  `callback_msg` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '回调描述',
  `callback_time` datetime NULL DEFAULT NULL COMMENT '回调时间',
  `retry_of` bigint NULL DEFAULT NULL COMMENT '重打来源任务',
  `error_msg` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '错误信息',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_reqid_tenant` (`reqid`, `tenant_id`, `deleted`) USING BTREE,
  KEY `idx_biz` (`biz_type`, `biz_id`, `tenant_id`, `deleted`) USING BTREE,
  KEY `idx_biz_warehouse_status` (`biz_type`, `biz_id`, `warehouse_id`, `status`, `tenant_id`, `deleted`) USING BTREE,
  KEY `idx_template_id` (`template_id`, `tenant_id`, `deleted`) USING BTREE,
  KEY `idx_status_submit_time` (`status`, `submit_time`, `tenant_id`, `deleted`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP 云打印任务';

CREATE TABLE IF NOT EXISTS `erp_cloud_print_callback_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '编号',
  `raw_body` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '回调原文',
  `method` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '回调方法',
  `devid` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '设备机器码',
  `reqid` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '任务号',
  `code` int NULL DEFAULT NULL COMMENT '通知码',
  `matched` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否匹配本系统数据',
  `creator` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updater` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` bit(1) NOT NULL DEFAULT b'0' COMMENT '是否删除',
  `tenant_id` bigint NOT NULL DEFAULT 0 COMMENT '租户编号',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_reqid` (`reqid`, `tenant_id`, `deleted`) USING BTREE,
  KEY `idx_devid` (`devid`, `tenant_id`, `deleted`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='ERP 云打印回调日志';

-- 联调环境可按需执行以下设备初始化。纸张高度 print_height 需按二联纸实测填写，未知时不要提交打印。
-- INSERT INTO erp_cloud_print_device
-- (`devid`, `dev_key`, `nickname`, `dev_type`, `content_type`, `print_width`, `print_height`,
--  `paper_type`, `rotate`, `copies`, `defaulted`, `status`, `creator`, `updater`, `tenant_id`)
-- SELECT 'SW263000012', '<设备校验码>', '针式二联销售单打印机', 4, 9, 210, NULL,
--        4, 0, 1, b'1', 0, '1', '1', 1
-- WHERE NOT EXISTS (
--   SELECT 1 FROM erp_cloud_print_device
--    WHERE devid = 'SW263000012' AND tenant_id = 1 AND deleted = b'0'
-- );
