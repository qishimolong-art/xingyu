-- 商城按收货地区定向发券：会员收货地址筛选索引
-- 目标：保留 idx_userId，并补充租户 + 逻辑删除 + 地区 + 会员的组合索引。
-- 本脚本不包含 DELETE、UPDATE、TRUNCATE、DROP 或权限数据变更，可重复执行。

SELECT COUNT(*) AS active_address_count,
       COUNT(DISTINCT user_id) AS active_user_count,
       COUNT(DISTINCT area_id) AS active_area_count
FROM member_address
WHERE deleted = 0;

SELECT COUNT(DISTINCT index_name) AS target_index_count
FROM information_schema.statistics
WHERE table_schema = DATABASE()
  AND table_name = 'member_address'
  AND index_name = 'idx_tenant_deleted_area_user';

SET @coupon_address_index_exists = (
    SELECT COUNT(DISTINCT index_name)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'member_address'
      AND index_name = 'idx_tenant_deleted_area_user'
);
SET @coupon_address_index_ddl = IF(
    @coupon_address_index_exists = 0,
    'ALTER TABLE member_address ADD INDEX idx_tenant_deleted_area_user (tenant_id, deleted, area_id, user_id)',
    'SELECT ''idx_tenant_deleted_area_user already exists'' AS index_result'
);
PREPARE coupon_address_index_stmt FROM @coupon_address_index_ddl;
EXECUTE coupon_address_index_stmt;
DEALLOCATE PREPARE coupon_address_index_stmt;

SHOW INDEX FROM member_address;

EXPLAIN SELECT DISTINCT u.id
FROM member_user u
INNER JOIN member_address a
        ON a.user_id = u.id
       AND a.tenant_id = u.tenant_id
       AND a.deleted = 0
WHERE u.tenant_id = 1
  AND u.deleted = 0
  AND u.status = 0
  AND a.area_id IN (510107)
ORDER BY u.id DESC;
