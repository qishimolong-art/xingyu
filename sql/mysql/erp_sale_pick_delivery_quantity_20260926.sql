-- 拣货/送货分批数量。目标：经确认的开发/测试库 ruoyi-vue-pro。
-- 部署窗口内暂停旧版本作业提交；先执行结构，回填时逐租户设置 @migration_tenant_id。
-- DDL 自动提交；失败立即停止。仅新增结构，回填只写尚未初始化的未删除明细。
-- 回滚应用前必须确认尚未产生分批数据；保留新增列/表，不执行破坏性结构回滚。

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='erp_sale_pick_delivery_item' AND column_name='picked_count')=0, 'ALTER TABLE `erp_sale_pick_delivery_item` ADD COLUMN `picked_count` decimal(24,6) NULL DEFAULT NULL', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='erp_sale_pick_delivery_item' AND column_name='delivered_count')=0, 'ALTER TABLE `erp_sale_pick_delivery_item` ADD COLUMN `delivered_count` decimal(24,6) NULL DEFAULT NULL', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='erp_sale_pick_delivery_submit' AND column_name='request_id')=0, 'ALTER TABLE `erp_sale_pick_delivery_submit` ADD COLUMN `request_id` varchar(64) CHARACTER SET ascii COLLATE ascii_bin NULL DEFAULT NULL', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='erp_sale_pick_delivery_submit' AND column_name='request_hash')=0, 'ALTER TABLE `erp_sale_pick_delivery_submit` ADD COLUMN `request_hash` varchar(64) NULL DEFAULT NULL', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='erp_sale_pick_delivery_submit' AND column_name='quantity_details')=0, 'ALTER TABLE `erp_sale_pick_delivery_submit` ADD COLUMN `quantity_details` bit(1) NOT NULL DEFAULT b''0''', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @ddl = IF((SELECT COUNT(*) FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='erp_sale_pick_delivery_submit' AND index_name='uk_quantity_request')=0,
 'ALTER TABLE erp_sale_pick_delivery_submit ADD UNIQUE KEY uk_quantity_request (tenant_id, order_id, type, request_id)', 'SELECT 1');
PREPARE stmt FROM @ddl;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS erp_sale_pick_delivery_submit_item (
 id bigint NOT NULL AUTO_INCREMENT,
 submit_id bigint NOT NULL,
 item_id bigint NOT NULL,
 product_code varchar(255) DEFAULT NULL,
 product_name varchar(255) DEFAULT NULL,
 quantity decimal(24,6) NOT NULL,
 creator varchar(64) DEFAULT '',
 create_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
 updater varchar(64) DEFAULT '',
 update_time datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
 deleted bit(1) NOT NULL DEFAULT b'0',
 tenant_id bigint NOT NULL DEFAULT 0,
 PRIMARY KEY (id),
 UNIQUE KEY uk_submit_item (tenant_id, submit_id, item_id),
 KEY idx_item (tenant_id, item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='拣货送货每次提交配件数量';

-- 回填：必须由执行方先赋值 @migration_tenant_id，事务内核对后再 COMMIT。
-- 不自动提交，便于执行方发现异常时 ROLLBACK；重复执行影响0行。
START TRANSACTION;
UPDATE erp_sale_pick_delivery_item
SET picked_count=CASE WHEN pick_status=30 THEN count ELSE 0 END,
    delivered_count=CASE WHEN delivery_status=30 THEN count ELSE 0 END,
    update_time=update_time
WHERE tenant_id=@migration_tenant_id AND deleted=b'0'
  AND picked_count IS NULL AND delivered_count IS NULL;
SELECT tenant_id, COUNT(*) AS rows_count,
 SUM(picked_count IS NULL OR delivered_count IS NULL) AS uninitialized,
 SUM(delivered_count>picked_count OR picked_count>count OR delivered_count<0 OR picked_count<0) AS invalid_count
FROM erp_sale_pick_delivery_item
WHERE tenant_id=@migration_tenant_id AND deleted=b'0'
GROUP BY tenant_id;
-- 执行方确认以上 uninitialized=0 且 invalid_count=0 后 COMMIT，否则 ROLLBACK。
