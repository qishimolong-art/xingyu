-- 00H 必需部署前提：手推车到实际调出单的定位，不产生库存/成本/账务。
-- 新旧核算模式都须安装。暂停手推车、报价转手推车、调拨相关写入后执行。
-- DDL 非原子；只按真实关系填充缺失定位，复跑校验既有定位，绝不覆盖冲突证据。
-- 任一语句失败立即停止；临时表仅当前会话，关闭会话后重试。保留原业务数据。
CREATE TABLE IF NOT EXISTS erp_sale_cart_transfer_link (
    tenant_id BIGINT NOT NULL,
    cart_id BIGINT NOT NULL,
    out_ids JSON NOT NULL,
    PRIMARY KEY (tenant_id, cart_id)
) ENGINE=InnoDB COMMENT='00H手推车真实调出单定位（不含入库镜像）';

CREATE TEMPORARY TABLE erp_cart_link_guard_20260909 (
    valid_state INT NOT NULL,
    CONSTRAINT chk_cart_link_20260909 CHECK (valid_state=1)
);
-- 同名表不能用错误列结构或无唯一定位的定义冒充本次部署。
INSERT INTO erp_cart_link_guard_20260909 SELECT IF(
    (SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=DATABASE()
       AND table_name='erp_sale_cart_transfer_link' AND is_nullable='NO'
       AND ((column_name IN ('tenant_id','cart_id') AND data_type='bigint')
            OR (column_name='out_ids' AND data_type='json')))=3
    AND EXISTS (SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE()
       AND table_name='erp_sale_cart_transfer_link' AND index_name='PRIMARY'
       GROUP BY index_name HAVING GROUP_CONCAT(column_name ORDER BY seq_in_index)='tenant_id,cart_id'),1,0);
-- 孤儿来源保留原状并要求核对，不创建虚构手推车或空关联。
INSERT INTO erp_cart_link_guard_20260909 SELECT IF(NOT EXISTS (
    SELECT 1 FROM erp_stock_move m LEFT JOIN erp_sale_cart c
      ON c.tenant_id=m.tenant_id AND c.id=m.source_id
    WHERE m.source_type=30 AND m.deleted=b'0'
      AND (m.transfer_direction=10 OR m.transfer_direction IS NULL) AND c.id IS NULL
),1,0);

CREATE TEMPORARY TABLE erp_cart_link_expected_20260909 (
    tenant_id BIGINT NOT NULL, cart_id BIGINT NOT NULL, out_ids JSON NOT NULL,
    PRIMARY KEY (tenant_id,cart_id)
);
INSERT INTO erp_cart_link_expected_20260909(tenant_id,cart_id,out_ids)
SELECT c.tenant_id,c.id,COALESCE(m.out_ids,JSON_ARRAY())
FROM erp_sale_cart c LEFT JOIN (
    SELECT tenant_id,source_id,JSON_ARRAYAGG(id) AS out_ids
    FROM erp_stock_move WHERE source_type=30 AND deleted=b'0'
      AND (transfer_direction=10 OR transfer_direction IS NULL)
    GROUP BY tenant_id,source_id
) m ON m.tenant_id=c.tenant_id AND m.source_id=c.id;

INSERT INTO erp_cart_link_guard_20260909 SELECT IF(NOT EXISTS (
    SELECT 1 FROM erp_sale_cart_transfer_link l LEFT JOIN erp_cart_link_expected_20260909 e
      ON e.tenant_id=l.tenant_id AND e.cart_id=l.cart_id
    WHERE e.cart_id IS NULL OR JSON_TYPE(l.out_ids)<>'ARRAY'
      OR JSON_LENGTH(l.out_ids)<>JSON_LENGTH(e.out_ids)
      OR NOT JSON_CONTAINS(l.out_ids,e.out_ids) OR NOT JSON_CONTAINS(e.out_ids,l.out_ids)
),1,0);
INSERT INTO erp_sale_cart_transfer_link(tenant_id,cart_id,out_ids)
SELECT e.tenant_id,e.cart_id,e.out_ids FROM erp_cart_link_expected_20260909 e
LEFT JOIN erp_sale_cart_transfer_link l ON l.tenant_id=e.tenant_id AND l.cart_id=e.cart_id
WHERE l.cart_id IS NULL;
DROP TEMPORARY TABLE erp_cart_link_expected_20260909;
DROP TEMPORARY TABLE erp_cart_link_guard_20260909;
