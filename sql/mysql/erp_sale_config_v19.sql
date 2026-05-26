-- erp_sale_config: 销售区域配置样例数据（v19），对齐客户v2 地区信息表
-- 无表结构变更，仅 INSERT
INSERT INTO erp_sale_config (config_type, code, name, status, sort, creator, create_time, updater, update_time, deleted, tenant_id)
VALUES
  ('SALES_AREA', 'east',        '华东区',        0, 1, 'system', NOW(), 'system', NOW(), 0, 1),
  ('SALES_AREA', 'south',       '华南区',        0, 2, 'system', NOW(), 'system', NOW(), 0, 1),
  ('SALES_AREA', 'north',       '华北区',        0, 3, 'system', NOW(), 'system', NOW(), 0, 1),
  ('SALES_AREA', 'central',     '华中区',        0, 4, 'system', NOW(), 'system', NOW(), 0, 1),
  ('SALES_AREA', 'southwest',   '西南区',        0, 5, 'system', NOW(), 'system', NOW(), 0, 1),
  ('SALES_AREA', 'northwest',   '西北区',        0, 6, 'system', NOW(), 'system', NOW(), 0, 1),
  ('SALES_AREA', 'northeast',   '东北区',        0, 7, 'system', NOW(), 'system', NOW(), 0, 1)
ON DUPLICATE KEY UPDATE name = VALUES(name);
