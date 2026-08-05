-- 上线后文件访问域名修复脚本。
-- 使用方式：
-- 1. 先把 @new_domain 改成线上用户可访问的后端公网前缀，例如 'https://erp.example.com'。
-- 2. 先执行预览 SELECT，确认命中记录后，再执行 UPDATE。
-- 3. 如果你的 nginx 直接暴露 /admin-api，这里不要在 @new_domain 后面追加 /admin-api。

SET @new_domain = TRIM(TRAILING '/' FROM 'http://47.109.30.86:48081');

-- 预览销售单快递单历史地址。
SELECT id, no, express_file_url
FROM erp_sale_out
WHERE express_file_url LIKE 'http://localhost:48080/%'
   OR express_file_url LIKE 'http://127.0.0.1:48080/%';

-- 预览通用文件表历史地址。
SELECT id, name, url
FROM infra_file
WHERE url LIKE 'http://localhost:48080/%'
   OR url LIKE 'http://127.0.0.1:48080/%';

-- 修复销售单快递单历史地址。
UPDATE erp_sale_out
SET express_file_url = REPLACE(express_file_url, 'http://localhost:48080', @new_domain)
WHERE @new_domain <> ''
  AND express_file_url LIKE 'http://localhost:48080/%';

UPDATE erp_sale_out
SET express_file_url = REPLACE(express_file_url, 'http://127.0.0.1:48080', @new_domain)
WHERE @new_domain <> ''
  AND express_file_url LIKE 'http://127.0.0.1:48080/%';

-- 修复通用文件表历史地址。
UPDATE infra_file
SET url = REPLACE(url, 'http://localhost:48080', @new_domain)
WHERE @new_domain <> ''
  AND url LIKE 'http://localhost:48080/%';

UPDATE infra_file
SET url = REPLACE(url, 'http://127.0.0.1:48080', @new_domain)
WHERE @new_domain <> ''
  AND url LIKE 'http://127.0.0.1:48080/%';

-- 如果这里仍能查到数据，说明 @new_domain 还没有替换成真实公网域名，或存在其它旧域名需要追加处理。
SELECT id, no, express_file_url
FROM erp_sale_out
WHERE express_file_url LIKE 'http://localhost:48080/%'
   OR express_file_url LIKE 'http://127.0.0.1:48080/%';

SELECT id, name, url
FROM infra_file
WHERE url LIKE 'http://localhost:48080/%'
   OR url LIKE 'http://127.0.0.1:48080/%';
