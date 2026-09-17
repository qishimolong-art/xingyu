-- MySQL 8：经营报表跨库存期间查询索引。非唯一DDL，不修改业务数据/授权。
-- 先执行双成本基础表DDL。DDL有隐式提交，不能声称事务回滚；大表请评估建索引时长/锁等待。
-- 多biz_type按全局posted_at排序仍可能filesort，小夹具EXPLAIN不代表生产规模性能。
DROP PROCEDURE IF EXISTS erp_trade_report_period_index_v226;
DELIMITER $$
CREATE PROCEDURE erp_trade_report_period_index_v226()
BEGIN
 DECLARE locked INT DEFAULT 0;
 DECLARE named_count INT DEFAULT 0;
 DECLARE valid_named INT DEFAULT 0;
 DECLARE equivalent_count INT DEFAULT 0;
 DECLARE EXIT HANDLER FOR SQLEXCEPTION
 BEGIN
  IF locked=1 THEN DO RELEASE_LOCK('erp_trade_report_period_index_v226'); END IF;
  RESIGNAL;
 END;
 SELECT GET_LOCK('erp_trade_report_period_index_v226',10) INTO locked;
 IF locked IS NULL OR locked<>1 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v226: index migration lock unavailable'; END IF;
 IF (SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name='erp_stock_dual_cost_posting' AND table_type='BASE TABLE')<>1
 THEN SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v226: dual cost posting table is missing'; END IF;
 SELECT COUNT(*) INTO named_count FROM information_schema.statistics
 WHERE table_schema=DATABASE() AND table_name='erp_stock_dual_cost_posting' AND index_name='idx_tenant_biz_posted';
 SELECT COUNT(*) INTO valid_named FROM (
  SELECT index_name FROM information_schema.statistics
  WHERE table_schema=DATABASE() AND table_name='erp_stock_dual_cost_posting' AND index_name='idx_tenant_biz_posted' AND seq_in_index<=4
  GROUP BY index_name HAVING COUNT(*)=4
   AND GROUP_CONCAT(column_name ORDER BY seq_in_index SEPARATOR ',')='tenant_id,biz_type,posted_at,id'
   AND SUM(sub_part IS NOT NULL)=0 AND MIN(is_visible)='YES' AND MIN(index_type)='BTREE'
 ) valid_named_index;
 IF named_count>0 AND valid_named=0 THEN
  SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT='v226: existing idx_tenant_biz_posted conflicts; review manually, no index replaced';
 END IF;
 SELECT COUNT(*) INTO equivalent_count FROM (
  SELECT index_name FROM information_schema.statistics
  WHERE table_schema=DATABASE() AND table_name='erp_stock_dual_cost_posting' AND seq_in_index<=4
  GROUP BY index_name HAVING COUNT(*)=4
   AND GROUP_CONCAT(column_name ORDER BY seq_in_index SEPARATOR ',')='tenant_id,biz_type,posted_at,id'
   AND SUM(sub_part IS NOT NULL)=0 AND MIN(is_visible)='YES' AND MIN(index_type)='BTREE'
 ) equivalent_index;
 IF equivalent_count=0 THEN
  ALTER TABLE erp_stock_dual_cost_posting ADD INDEX idx_tenant_biz_posted(tenant_id,biz_type,posted_at,id);
 END IF;
 DO RELEASE_LOCK('erp_trade_report_period_index_v226');
END$$
DELIMITER ;
CALL erp_trade_report_period_index_v226();
DROP PROCEDURE erp_trade_report_period_index_v226;
