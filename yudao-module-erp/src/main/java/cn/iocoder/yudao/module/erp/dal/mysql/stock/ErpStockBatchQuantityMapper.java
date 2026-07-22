package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockBatchQuantityDO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * 按产品、仓库、批次统计占用、未入和在途数量。
 *
 * <p>所有来源都将空白批次标准化为 {@code null}，没有批次字段的历史业务来源也归入未指定批次。</p>
 */
@Mapper
public interface ErpStockBatchQuantityMapper {

    // 各业务表的 batch_no 可能使用不同的 utf8mb4 排序规则，进入 UNION 前必须统一，
    // 否则 MySQL 会抛出 Illegal mix of collations for operation 'UNION'。
    @Select({
            "<script>",
            "SELECT d.product_id, d.warehouse_id, NULLIF(TRIM(d.batch_no), '') AS batch_no, SUM(d.count) AS count",
            "  FROM (",
            " SELECT sci.product_id, sci.warehouse_id, CONVERT(sci.batch_no USING utf8mb4) COLLATE utf8mb4_unicode_ci AS batch_no, COALESCE(sci.count, 0) AS count",
            "   FROM erp_sale_cart_items sci INNER JOIN erp_sale_cart sc ON sc.id = sci.cart_id",
            "    AND sc.deleted = 0 AND sc.status IN",
            "    <foreach collection='cartStatuses' item='status' open='(' separator=',' close=')'>#{status}</foreach>",
            "  WHERE sci.deleted = 0 AND NOT EXISTS",
            "        (SELECT 1 FROM erp_sale_out so WHERE so.deleted = 0 AND so.source_id = sc.id)",
            " UNION ALL",
            " SELECT soi.product_id, soi.warehouse_id, CONVERT(soi.batch_no USING utf8mb4) COLLATE utf8mb4_unicode_ci, COALESCE(soi.count, 0)",
            "   FROM erp_sale_out_items soi INNER JOIN erp_sale_out so ON so.id = soi.out_id",
            "    AND so.deleted = 0 AND so.status = #{processStatus} WHERE soi.deleted = 0",
            " UNION ALL",
            " SELECT pri.product_id, pri.warehouse_id, CONVERT(pri.batch_no USING utf8mb4) COLLATE utf8mb4_unicode_ci, COALESCE(pri.count, 0)",
            "   FROM erp_purchase_return_items pri INNER JOIN erp_purchase_return pr ON pr.id = pri.return_id",
            "    AND pr.deleted = 0 AND pr.status = #{processStatus} WHERE pri.deleted = 0",
            " UNION ALL",
            " SELECT soi2.product_id, soi2.warehouse_id, NULL AS batch_no, COALESCE(soi2.count, 0)",
            "   FROM erp_stock_out_item soi2 INNER JOIN erp_stock_out so2 ON so2.id = soi2.out_id",
            "    AND so2.deleted = 0 AND so2.status = #{processStatus} WHERE soi2.deleted = 0",
            " UNION ALL",
            " SELECT smi.product_id, smi.from_warehouse_id, CONVERT(smi.batch_no USING utf8mb4) COLLATE utf8mb4_unicode_ci, COALESCE(smi.count, 0)",
            "   FROM erp_stock_move_item smi INNER JOIN erp_stock_move sm ON sm.id = smi.move_id",
            "    AND sm.deleted = 0 AND sm.status = #{processStatus}",
            "    AND sm.transfer_direction = #{transferOutDirection} WHERE smi.deleted = 0",
            " UNION ALL",
            " SELECT wmi.product_id, wmi.from_warehouse_id, CONVERT(wmi.batch_no USING utf8mb4) COLLATE utf8mb4_unicode_ci, COALESCE(wmi.count, 0)",
            "   FROM erp_warehouse_move_item wmi INNER JOIN erp_warehouse_move wm ON wm.id = wmi.move_id",
            "    AND wm.deleted = 0 AND wm.status = #{processStatus} WHERE wmi.deleted = 0",
            " UNION ALL",
            " SELECT sci2.product_id, sci2.warehouse_id, CONVERT(sci2.batch_no USING utf8mb4) COLLATE utf8mb4_unicode_ci, ABS(COALESCE(sci2.count, 0))",
            "   FROM erp_stock_check_item sci2 INNER JOIN erp_stock_check sc2 ON sc2.id = sci2.check_id",
            "    AND sc2.deleted = 0 AND sc2.status = #{processStatus} AND sc2.check_type = #{checkType}",
            "  WHERE sci2.deleted = 0 AND sci2.count &lt; 0",
            " UNION ALL",
            " SELECT sobi.product_id, sobi.warehouse_id, CONVERT(sobi.batch_no USING utf8mb4) COLLATE utf8mb4_unicode_ci,",
            "        GREATEST(COALESCE(sobi.count, 0) - COALESCE(sobi.picked_count, 0), 0)",
            "   FROM erp_stock_out_bill_item sobi INNER JOIN erp_stock_out_bill sob ON sob.id = sobi.bill_id",
            "    AND sob.deleted = 0 AND sob.status IN (10, 20) WHERE sobi.deleted = 0",
            "       ) d",
            " WHERE d.product_id IN",
            " <foreach collection='productIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "   AND d.warehouse_id IN",
            " <foreach collection='warehouseIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            " GROUP BY d.product_id, d.warehouse_id, NULLIF(TRIM(d.batch_no), '')",
            "HAVING SUM(d.count) &lt;&gt; 0",
            "</script>"
    })
    List<ErpStockBatchQuantityDO> selectOccupiedList(@Param("productIds") Collection<Long> productIds,
                                                      @Param("warehouseIds") Collection<Long> warehouseIds,
                                                      @Param("processStatus") Integer processStatus,
                                                      @Param("cartStatuses") Collection<Integer> cartStatuses,
                                                      @Param("checkType") Integer checkType,
                                                      @Param("transferOutDirection") Integer transferOutDirection);

    @Select({
            "<script>",
            "SELECT d.product_id, d.warehouse_id, NULLIF(TRIM(d.batch_no), '') AS batch_no, SUM(d.count) AS count",
            "  FROM (",
            " SELECT pii.product_id, pii.warehouse_id, CONVERT(pii.batch_no USING utf8mb4) COLLATE utf8mb4_unicode_ci AS batch_no, COALESCE(pii.count, 0) AS count",
            "   FROM erp_purchase_in_items pii INNER JOIN erp_purchase_in pi ON pi.id = pii.in_id",
            "    AND pi.deleted = 0 AND pi.status = #{processStatus} WHERE pii.deleted = 0",
            " UNION ALL",
            " SELECT sri.product_id, sri.warehouse_id, NULL AS batch_no, COALESCE(sri.count, 0)",
            "   FROM erp_sale_return_items sri INNER JOIN erp_sale_return sr ON sr.id = sri.return_id",
            "    AND sr.deleted = 0 AND sr.status = #{processStatus} WHERE sri.deleted = 0",
            " UNION ALL",
            " SELECT sii.product_id, sii.warehouse_id, NULL AS batch_no, COALESCE(sii.count, 0)",
            "   FROM erp_stock_in_item sii INNER JOIN erp_stock_in si ON si.id = sii.in_id",
            "    AND si.deleted = 0 AND si.status = #{processStatus} WHERE sii.deleted = 0",
            " UNION ALL",
            " SELECT smi.product_id, smi.to_warehouse_id, CONVERT(smi.batch_no USING utf8mb4) COLLATE utf8mb4_unicode_ci, COALESCE(smi.count, 0)",
            "   FROM erp_stock_move_item smi INNER JOIN erp_stock_move sm ON sm.id = smi.move_id",
            "    AND sm.deleted = 0 AND sm.status = #{processStatus}",
            "    AND sm.transfer_direction = #{transferInDirection} WHERE smi.deleted = 0",
            " UNION ALL",
            " SELECT wmi.product_id, wmi.to_warehouse_id, CONVERT(wmi.batch_no USING utf8mb4) COLLATE utf8mb4_unicode_ci, COALESCE(wmi.count, 0)",
            "   FROM erp_warehouse_move_item wmi INNER JOIN erp_warehouse_move wm ON wm.id = wmi.move_id",
            "    AND wm.deleted = 0 AND wm.status = #{processStatus} WHERE wmi.deleted = 0",
            " UNION ALL",
            " SELECT sci.product_id, sci.warehouse_id, CONVERT(sci.batch_no USING utf8mb4) COLLATE utf8mb4_unicode_ci, COALESCE(sci.count, 0)",
            "   FROM erp_stock_check_item sci INNER JOIN erp_stock_check sc ON sc.id = sci.check_id",
            "    AND sc.deleted = 0 AND sc.status = #{processStatus} AND sc.check_type = #{checkType}",
            "  WHERE sci.deleted = 0 AND sci.count &gt; 0",
            " UNION ALL",
            " SELECT sibi.product_id, sibi.warehouse_id, CONVERT(sibi.batch_no USING utf8mb4) COLLATE utf8mb4_unicode_ci,",
            "        GREATEST(COALESCE(sibi.count, 0) - COALESCE(sibi.picked_count, 0), 0)",
            "   FROM erp_stock_in_bill_item sibi INNER JOIN erp_stock_in_bill sib ON sib.id = sibi.bill_id",
            "    AND sib.deleted = 0 AND sib.status IN (10, 20) WHERE sibi.deleted = 0",
            "       ) d",
            " WHERE d.product_id IN",
            " <foreach collection='productIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "   AND d.warehouse_id IN",
            " <foreach collection='warehouseIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            " GROUP BY d.product_id, d.warehouse_id, NULLIF(TRIM(d.batch_no), '')",
            "HAVING SUM(d.count) &lt;&gt; 0",
            "</script>"
    })
    List<ErpStockBatchQuantityDO> selectPendingInList(@Param("productIds") Collection<Long> productIds,
                                                       @Param("warehouseIds") Collection<Long> warehouseIds,
                                                       @Param("processStatus") Integer processStatus,
                                                       @Param("checkType") Integer checkType,
                                                       @Param("transferInDirection") Integer transferInDirection);

    @Select({
            "<script>",
            "SELECT poi.product_id, poi.warehouse_id, NULLIF(TRIM(poi.batch_no), '') AS batch_no,",
            "       SUM(GREATEST(COALESCE(poi.count, 0) - COALESCE(poi.in_count, 0), 0)) AS count",
            "  FROM erp_purchase_order_items poi INNER JOIN erp_purchase_order po ON po.id = poi.order_id",
            "   AND po.deleted = 0 AND po.status IN",
            "   <foreach collection='statuses' item='status' open='(' separator=',' close=')'>#{status}</foreach>",
            " WHERE poi.deleted = 0 AND poi.product_id IN",
            " <foreach collection='productIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "   AND poi.warehouse_id IN",
            " <foreach collection='warehouseIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            " GROUP BY poi.product_id, poi.warehouse_id, NULLIF(TRIM(poi.batch_no), '')",
            "HAVING SUM(GREATEST(COALESCE(poi.count, 0) - COALESCE(poi.in_count, 0), 0)) &gt; 0",
            "</script>"
    })
    List<ErpStockBatchQuantityDO> selectInTransitList(@Param("productIds") Collection<Long> productIds,
                                                       @Param("warehouseIds") Collection<Long> warehouseIds,
                                                       @Param("statuses") Collection<Integer> statuses);

}
