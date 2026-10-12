-- v253: 采购票据纳入采购入库、采购调价、采购退货，并支持按数量分批开票。
--
-- 安全边界：
-- 1. 不删除、不改写既有采购票据金额；仅补充通用来源字段和审核分摊台账。
-- 2. 历史已审核采购票据沿用旧规则，关联入库明细按“全部开票”迁移，避免升级后再次出现。
-- 3. 历史已开票入库对应的已审核调价明细同步标记为全部开票；历史退货仍保持可开票。
-- 4. DDL 会隐式提交。执行前必须把变量设置为已复核的目标数据库名。
-- 5. 客户端必须使用 utf8mb4，例如 mysql --default-character-set=utf8mb4，避免中文存储过程文本被错误转码。
--
-- SET @purchase_invoice_multi_source_schema = 'your_database';

SET NAMES utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 不预先 DROP 同名过程：若线上已存在同名对象，CREATE 会安全失败，避免误删已有过程。
DELIMITER $$
CREATE PROCEDURE migrate_purchase_invoice_multi_source_v253()
BEGIN
  DECLARE column_count INT DEFAULT 0;
  DECLARE allocation_column_count INT DEFAULT 0;
  DECLARE allocation_index_count INT DEFAULT 0;
  DECLARE invalid_count INT DEFAULT 0;
  DECLARE EXIT HANDLER FOR SQLEXCEPTION
  BEGIN
    ROLLBACK;
    RESIGNAL;
  END;

  IF @purchase_invoice_multi_source_schema IS NULL
      OR CAST(DATABASE() AS BINARY) <> CAST(@purchase_invoice_multi_source_schema AS BINARY) THEN
    SIGNAL SQLSTATE '45000'
      SET MESSAGE_TEXT = 'v253: set purchase_invoice_multi_source_schema to the reviewed target database';
  END IF;

  IF NOT EXISTS (SELECT 1 FROM information_schema.tables
      WHERE table_schema=DATABASE() AND table_name='erp_purchase_invoice' AND table_type='BASE TABLE')
    OR NOT EXISTS (SELECT 1 FROM information_schema.tables
      WHERE table_schema=DATABASE() AND table_name='erp_purchase_invoice_item' AND table_type='BASE TABLE')
    OR NOT EXISTS (SELECT 1 FROM information_schema.tables
      WHERE table_schema=DATABASE() AND table_name='erp_purchase_in_items' AND table_type='BASE TABLE')
    OR NOT EXISTS (SELECT 1 FROM information_schema.tables
      WHERE table_schema=DATABASE() AND table_name='erp_purchase_price_adjust_item' AND table_type='BASE TABLE')
    OR NOT EXISTS (SELECT 1 FROM information_schema.tables
      WHERE table_schema=DATABASE() AND table_name='erp_purchase_return_items' AND table_type='BASE TABLE') THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v253: required purchase invoice/source tables do not exist';
  END IF;

  -- 只读预检结果：执行人员应先核对历史规模，再继续观察迁移及后检输出。
  SELECT
    (SELECT COUNT(*) FROM erp_purchase_invoice WHERE deleted=b'0') AS invoice_rows,
    (SELECT COUNT(*) FROM erp_purchase_invoice_item WHERE deleted=b'0') AS invoice_item_rows,
    (SELECT COUNT(*) FROM erp_purchase_invoice_item
       WHERE deleted=b'0' AND source_in_item_id IS NOT NULL) AS legacy_source_item_rows,
    (SELECT COUNT(*) FROM erp_purchase_invoice_item pii
       INNER JOIN erp_purchase_invoice pinv ON pinv.id=pii.invoice_id AND pinv.tenant_id=pii.tenant_id
         AND pinv.deleted=b'0' AND pinv.status=20
       WHERE pii.deleted=b'0' AND pii.source_in_item_id IS NOT NULL) AS approved_legacy_source_rows,
    (SELECT COUNT(*) FROM (
       SELECT pii.tenant_id,pii.source_in_item_id
       FROM erp_purchase_invoice_item pii
       INNER JOIN erp_purchase_invoice pinv ON pinv.id=pii.invoice_id AND pinv.tenant_id=pii.tenant_id
         AND pinv.deleted=b'0' AND pinv.status=20
       WHERE pii.deleted=b'0' AND pii.source_in_item_id IS NOT NULL
       GROUP BY pii.tenant_id,pii.source_in_item_id
       HAVING COUNT(DISTINCT pii.invoice_id)>1
     ) duplicate_legacy_source) AS legacy_source_multi_invoice_groups,
    (SELECT COUNT(DISTINCT pai.id) FROM erp_purchase_price_adjust_item pai
       INNER JOIN erp_purchase_price_adjust pa ON pa.id=pai.adjust_id AND pa.tenant_id=pai.tenant_id
         AND pa.deleted=b'0' AND pa.status=20
       INNER JOIN erp_purchase_invoice_item pii ON pii.tenant_id=pai.tenant_id
         AND pii.deleted=b'0' AND pii.source_in_item_id=pai.in_item_id
       INNER JOIN erp_purchase_invoice pinv ON pinv.id=pii.invoice_id AND pinv.tenant_id=pii.tenant_id
         AND pinv.deleted=b'0' AND pinv.status=20
       WHERE pai.deleted=b'0' AND COALESCE(pai.adjust_price,0)<>0) AS historical_adjust_close_rows;

  -- 同名表若由中断脚本或手工操作留下残缺结构，必须先停止，不能让 IF NOT EXISTS 掩盖异常。
  IF EXISTS (SELECT 1 FROM information_schema.tables
      WHERE table_schema=DATABASE() AND table_name='erp_purchase_invoice_source_allocation'
        AND table_type='BASE TABLE') THEN
    SELECT COUNT(*) INTO allocation_column_count FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name='erp_purchase_invoice_source_allocation'
      AND column_name IN ('id','invoice_id','invoice_item_id','source_type','source_doc_id','source_doc_no',
        'source_item_id','source_total_count','source_total_amount','allocated_count','allocated_amount',
        'amount_direction','legacy_full_allocation','creator','create_time','updater','update_time','deleted','tenant_id');
    SELECT COUNT(DISTINCT index_name) INTO allocation_index_count FROM information_schema.statistics
    WHERE table_schema=DATABASE() AND table_name='erp_purchase_invoice_source_allocation'
      AND index_name IN ('PRIMARY','uk_invoice_source_item','idx_source_remaining','idx_invoice_id');
    IF allocation_column_count<>19 OR allocation_index_count<>4 THEN
      SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v253: incompatible source allocation table detected';
    END IF;
  END IF;

  SELECT COUNT(*) INTO column_count FROM information_schema.columns
  WHERE table_schema=DATABASE() AND table_name='erp_purchase_invoice_item'
    AND column_name IN ('source_type','source_doc_id','source_doc_no','source_item_id','amount_direction');
  IF column_count=0 THEN
    ALTER TABLE erp_purchase_invoice_item
      ADD COLUMN source_type TINYINT NULL COMMENT '来源类型：10入库 20调价 30退货' AFTER source_in_item_id,
      ADD COLUMN source_doc_id BIGINT NULL COMMENT '通用来源单据ID' AFTER source_type,
      ADD COLUMN source_doc_no VARCHAR(64) NULL COMMENT '通用来源单号' AFTER source_doc_id,
      ADD COLUMN source_item_id BIGINT NULL COMMENT '通用来源明细ID' AFTER source_doc_no,
      ADD COLUMN amount_direction TINYINT NOT NULL DEFAULT 1 COMMENT '金额方向：1正向 -1负向' AFTER source_item_id;
  ELSEIF column_count<>5 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v253: partial generic source columns detected';
  END IF;

  CREATE TABLE IF NOT EXISTS erp_purchase_invoice_source_allocation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    invoice_id BIGINT NOT NULL COMMENT '采购票据ID',
    invoice_item_id BIGINT NULL COMMENT '采购票据明细ID；历史调价关闭记录可为空',
    source_type TINYINT NOT NULL COMMENT '来源类型：10入库 20调价 30退货',
    source_doc_id BIGINT NOT NULL COMMENT '来源单据ID',
    source_doc_no VARCHAR(64) NOT NULL COMMENT '来源单号',
    source_item_id BIGINT NOT NULL COMMENT '来源明细ID',
    source_total_count DECIMAL(24,6) NOT NULL DEFAULT 0 COMMENT '来源原始数量',
    source_total_amount DECIMAL(24,6) NOT NULL DEFAULT 0 COMMENT '来源原始金额（带正负方向）',
    allocated_count DECIMAL(24,6) NOT NULL DEFAULT 0 COMMENT '本票据已审核分摊数量',
    allocated_amount DECIMAL(24,6) NOT NULL DEFAULT 0 COMMENT '本票据已审核分摊金额（带正负方向）',
    amount_direction TINYINT NOT NULL DEFAULT 1 COMMENT '金额方向：1正向 -1负向',
    legacy_full_allocation BIT(1) NOT NULL DEFAULT b'0' COMMENT '是否历史整项关闭记录',
    creator VARCHAR(64) DEFAULT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updater VARCHAR(64) DEFAULT NULL,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted BIT(1) NOT NULL DEFAULT b'0',
    tenant_id BIGINT NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    UNIQUE KEY uk_invoice_source_item (tenant_id,invoice_id,source_type,source_item_id,deleted),
    KEY idx_source_remaining (tenant_id,deleted,source_type,source_item_id),
    KEY idx_invoice_id (tenant_id,deleted,invoice_id)
  ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='ERP 采购票据审核来源分摊';

  IF NOT EXISTS (SELECT 1 FROM information_schema.statistics
      WHERE table_schema=DATABASE() AND table_name='erp_purchase_invoice_item'
        AND index_name='idx_invoice_generic_source') THEN
    ALTER TABLE erp_purchase_invoice_item ADD INDEX idx_invoice_generic_source
      (tenant_id,deleted,source_type,source_item_id,invoice_id);
  END IF;

  -- DDL 已完成并隐式提交；下面的历史回填和占用台账写入使用独立事务，任一异常整体回滚。
  START TRANSACTION;

  -- 兼容旧字段：只补空值，不改变票据数量、单价和金额。
  UPDATE erp_purchase_invoice_item
  SET source_type=10,
      source_doc_id=source_in_id,
      source_doc_no=source_in_no,
      source_item_id=source_in_item_id,
      amount_direction=1
  WHERE deleted=b'0' AND source_in_item_id IS NOT NULL AND source_type IS NULL;

  -- 旧版本审批即把整张入库单标记已开票，因此历史来源按原入库项总数量全部关闭。
  INSERT INTO erp_purchase_invoice_source_allocation
    (invoice_id,invoice_item_id,source_type,source_doc_id,source_doc_no,source_item_id,
     source_total_count,source_total_amount,allocated_count,allocated_amount,amount_direction,
     legacy_full_allocation,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT MIN(pii.invoice_id),
         CASE WHEN COUNT(DISTINCT pii.invoice_id)>1 THEN NULL ELSE MIN(pii.id) END,
         10,MIN(pii.source_in_id),MIN(pii.source_in_no),pii.source_in_item_id,
         COALESCE(source_item.count,0),
         ROUND(COALESCE(source_item.count,0)*COALESCE(source_item.original_product_price,source_item.product_price,0),6),
         COALESCE(source_item.count,0),
         CASE WHEN COUNT(DISTINCT pii.invoice_id)>1
           THEN ROUND(COALESCE(source_item.count,0)
             * COALESCE(source_item.original_product_price,source_item.product_price,0),6)
           ELSE SUM(COALESCE(pii.total_price,0)) END,
         1,b'1',
         COALESCE(MIN(pii.creator),'1'),COALESCE(MIN(pii.create_time),NOW()),COALESCE(MIN(pii.updater),'1'),NOW(),b'0',pii.tenant_id
  FROM erp_purchase_invoice_item pii
  INNER JOIN erp_purchase_invoice pinv ON pinv.id=pii.invoice_id AND pinv.tenant_id=pii.tenant_id
    AND pinv.deleted=b'0' AND pinv.status=20
  INNER JOIN erp_purchase_in_items source_item ON source_item.id=pii.source_in_item_id
    AND source_item.tenant_id=pii.tenant_id AND source_item.deleted=b'0'
  WHERE pii.deleted=b'0' AND pii.source_in_item_id IS NOT NULL
    AND NOT EXISTS (SELECT 1 FROM erp_purchase_invoice_source_allocation existing
      WHERE existing.tenant_id=pii.tenant_id AND existing.deleted=b'0'
        AND existing.source_type=10 AND existing.source_item_id=pii.source_in_item_id)
  GROUP BY pii.tenant_id,pii.source_in_item_id,source_item.count,
           source_item.original_product_price,source_item.product_price;

  -- 若历史入库在开票前已有已审核调价，按旧整单开票语义一并关闭调价来源，避免重复出现。
  INSERT INTO erp_purchase_invoice_source_allocation
    (invoice_id,invoice_item_id,source_type,source_doc_id,source_doc_no,source_item_id,
     source_total_count,source_total_amount,allocated_count,allocated_amount,amount_direction,
     legacy_full_allocation,creator,create_time,updater,update_time,deleted,tenant_id)
  SELECT MIN(in_alloc.invoice_id),NULL,20,pai.adjust_id,pa.no,pai.id,
         COALESCE(pai.count,0),COALESCE(pai.adjust_price,0),COALESCE(pai.count,0),COALESCE(pai.adjust_price,0),
         CASE WHEN COALESCE(pai.adjust_price,0)<0 THEN -1 ELSE 1 END,b'1','1',NOW(),'1',NOW(),b'0',pai.tenant_id
  FROM erp_purchase_price_adjust_item pai
  INNER JOIN erp_purchase_price_adjust pa ON pa.id=pai.adjust_id AND pa.tenant_id=pai.tenant_id
    AND pa.deleted=b'0' AND pa.status=20
  INNER JOIN erp_purchase_invoice_source_allocation in_alloc
    ON in_alloc.tenant_id=pai.tenant_id AND in_alloc.deleted=b'0'
    AND in_alloc.source_type=10 AND in_alloc.source_item_id=pai.in_item_id
  WHERE pai.deleted=b'0' AND COALESCE(pai.adjust_price,0)<>0
    AND NOT EXISTS (SELECT 1 FROM erp_purchase_invoice_source_allocation existing
      WHERE existing.tenant_id=pai.tenant_id AND existing.deleted=b'0'
        AND existing.source_type=20 AND existing.source_item_id=pai.id)
  GROUP BY pai.tenant_id,pai.adjust_id,pa.no,pai.id,pai.count,pai.adjust_price;

  SELECT COUNT(*) INTO invalid_count
  FROM (
    SELECT tenant_id,source_type,source_item_id
    FROM erp_purchase_invoice_source_allocation
    WHERE deleted=b'0'
    GROUP BY tenant_id,source_type,source_item_id
    HAVING SUM(allocated_count)>MAX(source_total_count)
  ) over_allocated;
  IF invalid_count<>0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v253: source allocation exceeds source total count';
  END IF;

  SELECT COUNT(*) INTO invalid_count
  FROM erp_purchase_invoice_item
  WHERE deleted=b'0' AND source_in_item_id IS NOT NULL
    AND (source_type IS NULL OR source_doc_id IS NULL OR source_doc_no IS NULL
      OR source_item_id IS NULL OR amount_direction IS NULL);
  IF invalid_count<>0 THEN
    SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'v253: legacy invoice source backfill is incomplete';
  END IF;

  COMMIT;
END$$
DELIMITER ;

CALL migrate_purchase_invoice_multi_source_v253();
DROP PROCEDURE migrate_purchase_invoice_multi_source_v253;

-- 后检：不应出现超分摊、非法类型或同来源重复台账。
SELECT source_type,COUNT(*) allocation_rows,
       SUM(allocated_count) allocated_count,SUM(allocated_amount) allocated_amount
FROM erp_purchase_invoice_source_allocation
WHERE deleted=b'0' GROUP BY source_type ORDER BY source_type;

SELECT source_type,source_item_id,SUM(allocated_count) allocated_count,MAX(source_total_count) source_total_count
FROM erp_purchase_invoice_source_allocation
WHERE deleted=b'0'
GROUP BY tenant_id,source_type,source_item_id
HAVING allocated_count>source_total_count;

SELECT source_type,COUNT(*) invalid_count
FROM erp_purchase_invoice_source_allocation
WHERE deleted=b'0' AND source_type NOT IN (10,20,30)
GROUP BY source_type;

SELECT COUNT(*) legacy_source_without_generic
FROM erp_purchase_invoice_item
WHERE deleted=b'0' AND source_in_item_id IS NOT NULL
  AND (source_type IS NULL OR source_doc_id IS NULL OR source_doc_no IS NULL
    OR source_item_id IS NULL OR amount_direction IS NULL);

SELECT COUNT(*) duplicate_allocation_groups
FROM (
  SELECT tenant_id,invoice_id,source_type,source_item_id,deleted
  FROM erp_purchase_invoice_source_allocation
  GROUP BY tenant_id,invoice_id,source_type,source_item_id,deleted
  HAVING COUNT(*)>1
) duplicate_groups;

SELECT
  (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema=DATABASE() AND table_name='erp_purchase_invoice_item'
      AND column_name IN ('source_type','source_doc_id','source_doc_no','source_item_id','amount_direction'))
    AS generic_source_column_count,
  (SELECT COUNT(DISTINCT index_name) FROM information_schema.statistics
    WHERE table_schema=DATABASE() AND table_name='erp_purchase_invoice_item'
      AND index_name='idx_invoice_generic_source') AS generic_source_index_count,
  (SELECT COUNT(*) FROM information_schema.tables
    WHERE table_schema=DATABASE() AND table_name='erp_purchase_invoice_source_allocation'
      AND table_type='BASE TABLE') AS allocation_table_count;
