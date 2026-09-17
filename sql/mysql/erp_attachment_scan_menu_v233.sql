-- ERP attachment scan demo menu entry (v233)
-- Repeatable. Adds only the demo page menu so roles can grant the scan-upload showcase entry manually.
-- No role authorization is inserted here.
SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

SET @erp_root_id := (
  SELECT `id` FROM `system_menu`
  WHERE `name` = 'ERP 系统'
    AND `path` = '/erp'
    AND `type` = 1
    AND `deleted` = b'0'
  ORDER BY `id` DESC LIMIT 1
);

SET @attachment_scan_demo_id := (
  SELECT `id` FROM `system_menu`
  WHERE `deleted` = b'0'
    AND (
      `component` = 'erp/attachment-scan/demo'
      OR `component_name` = 'ErpAttachmentScanDemo'
    )
  ORDER BY `id` DESC LIMIT 1
);

INSERT INTO `system_menu`
(`name`, `permission`, `type`, `sort`, `parent_id`, `path`, `icon`, `component`, `component_name`,
 `status`, `visible`, `keep_alive`, `always_show`, `creator`, `create_time`, `updater`, `update_time`, `deleted`)
SELECT '扫码上传演示', '', 2, 99, @erp_root_id, 'attachment-scan/demo', 'ep:upload',
       'erp/attachment-scan/demo', 'ErpAttachmentScanDemo',
       0, b'1', b'1', b'1', '1', NOW(), '1', NOW(), b'0'
FROM DUAL
WHERE @erp_root_id IS NOT NULL
  AND @attachment_scan_demo_id IS NULL;

SET @attachment_scan_demo_id := (
  SELECT `id` FROM `system_menu`
  WHERE `deleted` = b'0'
    AND (
      `component` = 'erp/attachment-scan/demo'
      OR `component_name` = 'ErpAttachmentScanDemo'
    )
  ORDER BY `id` DESC LIMIT 1
);

-- Deployment check: should return one active demo page menu after execution.
SELECT `id`, `name`, `parent_id`, `path`, `component`, `component_name`, `visible`
FROM `system_menu`
WHERE `deleted` = b'0'
  AND (
    `component` = 'erp/attachment-scan/demo'
    OR `component_name` = 'ErpAttachmentScanDemo'
  )
ORDER BY `id`;
