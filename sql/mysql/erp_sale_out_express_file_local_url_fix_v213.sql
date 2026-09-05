-- 修复销售单快递单和通用文件表中误保存的本机访问地址。
-- 背景：本地文件存储启用时，历史默认 domain 为 localhost/127.0.0.1，会导致非服务器本机用户无法查看图片。
-- 处理方式：统一改为 /admin-api 开头的相对地址，由前端站点或 nginx 的 /admin-api 代理转发到后端。

-- 预览销售单快递单历史地址。
SELECT id, no, express_file_url
FROM erp_sale_out
WHERE express_file_url LIKE 'http://localhost:48080/admin-api/%'
   OR express_file_url LIKE 'http://127.0.0.1:48080/admin-api/%';

-- 预览通用文件表历史地址。
SELECT id, name, url
FROM infra_file
WHERE url LIKE 'http://localhost:48080/admin-api/%'
   OR url LIKE 'http://127.0.0.1:48080/admin-api/%';

-- 修复销售单快递单历史地址。
UPDATE erp_sale_out
SET express_file_url = REPLACE(express_file_url, 'http://localhost:48080/admin-api/', '/admin-api/')
WHERE express_file_url LIKE 'http://localhost:48080/admin-api/%';

UPDATE erp_sale_out
SET express_file_url = REPLACE(express_file_url, 'http://127.0.0.1:48080/admin-api/', '/admin-api/')
WHERE express_file_url LIKE 'http://127.0.0.1:48080/admin-api/%';

-- 修复通用文件表历史地址。
UPDATE infra_file
SET url = REPLACE(url, 'http://localhost:48080/admin-api/', '/admin-api/')
WHERE url LIKE 'http://localhost:48080/admin-api/%';

UPDATE infra_file
SET url = REPLACE(url, 'http://127.0.0.1:48080/admin-api/', '/admin-api/')
WHERE url LIKE 'http://127.0.0.1:48080/admin-api/%';

-- 复核：下方结果应为空。
SELECT id, no, express_file_url
FROM erp_sale_out
WHERE express_file_url LIKE 'http://localhost:48080/admin-api/%'
   OR express_file_url LIKE 'http://127.0.0.1:48080/admin-api/%';

SELECT id, name, url
FROM infra_file
WHERE url LIKE 'http://localhost:48080/admin-api/%'
   OR url LIKE 'http://127.0.0.1:48080/admin-api/%';
