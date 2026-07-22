package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockPendingInDetailRespVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** Queries every document source included by the stock pending-in aggregate. */
@Mapper
public interface ErpStockPendingInDetailMapper {

    // Legacy ERP tables do not necessarily share one utf8mb4 collation. Normalize every text column
    // participating in UNION ALL so opening a batch detail cannot fail with "Illegal mix of collations".
    @Select({
            "<script>",
            "SELECT d.*, p.code AS productCode, p.name AS productName, w.name AS warehouseName,",
            "       p.vehicle_model AS vehicleModel, p.origin_place AS originPlace,",
            "       p.drawing_no AS drawingNo, p.standard AS standard",
            "  FROM (",
            " SELECT 'PURCHASE_IN' AS documentType, pi.id AS documentId, pii.id AS itemId,",
            "        CONVERT(pi.no USING utf8mb4) COLLATE utf8mb4_unicode_ci AS no,",
            "        pii.product_id AS productId, pii.warehouse_id AS warehouseId,",
            "        NULLIF(TRIM(CONVERT(pii.batch_no USING utf8mb4) COLLATE utf8mb4_unicode_ci), '') AS batchNo, COALESCE(pii.count, 0) AS count,",
            "        pi.create_time AS createTime,",
            "        CONVERT(pi.creator USING utf8mb4) COLLATE utf8mb4_unicode_ci AS creator",
            "   FROM erp_purchase_in_items pii INNER JOIN erp_purchase_in pi ON pi.id = pii.in_id",
            "    AND pi.deleted = 0 AND pi.status = #{processStatus}",
            "  WHERE pii.deleted = 0 AND pii.product_id = #{productId} AND pii.warehouse_id = #{warehouseId}",
            " UNION ALL",
            " SELECT 'SALE_RETURN', sr.id, sri.id, CONVERT(sr.no USING utf8mb4) COLLATE utf8mb4_unicode_ci,",
            "        sri.product_id, sri.warehouse_id, NULL, COALESCE(sri.count, 0), sr.create_time,",
            "        CONVERT(sr.creator USING utf8mb4) COLLATE utf8mb4_unicode_ci",
            "   FROM erp_sale_return_items sri INNER JOIN erp_sale_return sr ON sr.id = sri.return_id",
            "    AND sr.deleted = 0 AND sr.status = #{processStatus}",
            "  WHERE sri.deleted = 0 AND sri.product_id = #{productId} AND sri.warehouse_id = #{warehouseId}",
            " UNION ALL",
            " SELECT 'STOCK_IN', si.id, sii.id, CONVERT(si.no USING utf8mb4) COLLATE utf8mb4_unicode_ci,",
            "        sii.product_id, sii.warehouse_id, NULL, COALESCE(sii.count, 0), si.create_time,",
            "        CONVERT(si.creator USING utf8mb4) COLLATE utf8mb4_unicode_ci",
            "   FROM erp_stock_in_item sii INNER JOIN erp_stock_in si ON si.id = sii.in_id",
            "    AND si.deleted = 0 AND si.status = #{processStatus}",
            "  WHERE sii.deleted = 0 AND sii.product_id = #{productId} AND sii.warehouse_id = #{warehouseId}",
            " UNION ALL",
            " SELECT 'STOCK_TRANSFER_IN', sm.id, smi.id, CONVERT(sm.no USING utf8mb4) COLLATE utf8mb4_unicode_ci,",
            "        smi.product_id, smi.to_warehouse_id,",
            "        NULLIF(TRIM(CONVERT(smi.batch_no USING utf8mb4) COLLATE utf8mb4_unicode_ci), ''), COALESCE(smi.count, 0),",
            "        sm.create_time, CONVERT(sm.creator USING utf8mb4) COLLATE utf8mb4_unicode_ci",
            "   FROM erp_stock_move_item smi INNER JOIN erp_stock_move sm ON sm.id = smi.move_id",
            "    AND sm.deleted = 0 AND sm.status = #{processStatus} AND sm.transfer_direction = #{transferInDirection}",
            "  WHERE smi.deleted = 0 AND smi.product_id = #{productId} AND smi.to_warehouse_id = #{warehouseId}",
            " UNION ALL",
            " SELECT 'WAREHOUSE_MOVE', wm.id, wmi.id, CONVERT(wm.no USING utf8mb4) COLLATE utf8mb4_unicode_ci,",
            "        wmi.product_id, wmi.to_warehouse_id,",
            "        NULLIF(TRIM(CONVERT(wmi.batch_no USING utf8mb4) COLLATE utf8mb4_unicode_ci), ''), COALESCE(wmi.count, 0),",
            "        wm.create_time, CONVERT(wm.creator USING utf8mb4) COLLATE utf8mb4_unicode_ci",
            "   FROM erp_warehouse_move_item wmi INNER JOIN erp_warehouse_move wm ON wm.id = wmi.move_id",
            "    AND wm.deleted = 0 AND wm.status = #{processStatus}",
            "  WHERE wmi.deleted = 0 AND wmi.product_id = #{productId} AND wmi.to_warehouse_id = #{warehouseId}",
            " UNION ALL",
            " SELECT 'STOCK_CHECK', sc.id, sci.id, CONVERT(sc.no USING utf8mb4) COLLATE utf8mb4_unicode_ci,",
            "        sci.product_id, sci.warehouse_id,",
            "        NULLIF(TRIM(CONVERT(sci.batch_no USING utf8mb4) COLLATE utf8mb4_unicode_ci), ''), COALESCE(sci.count, 0),",
            "        sc.create_time, CONVERT(sc.creator USING utf8mb4) COLLATE utf8mb4_unicode_ci",
            "   FROM erp_stock_check_item sci INNER JOIN erp_stock_check sc ON sc.id = sci.check_id",
            "    AND sc.deleted = 0 AND sc.status = #{processStatus} AND sc.check_type = #{checkType}",
            "  WHERE sci.deleted = 0 AND sci.count &gt; 0 AND sci.product_id = #{productId} AND sci.warehouse_id = #{warehouseId}",
            " UNION ALL",
            " SELECT 'STOCK_IN_BILL', sib.id, sibi.id, CONVERT(sib.no USING utf8mb4) COLLATE utf8mb4_unicode_ci,",
            "        sibi.product_id, sibi.warehouse_id,",
            "        NULLIF(TRIM(CONVERT(sibi.batch_no USING utf8mb4) COLLATE utf8mb4_unicode_ci), ''),",
            "        GREATEST(COALESCE(sibi.count, 0) - COALESCE(sibi.picked_count, 0), 0), sib.create_time,",
            "        CONVERT(sib.creator USING utf8mb4) COLLATE utf8mb4_unicode_ci",
            "   FROM erp_stock_in_bill_item sibi INNER JOIN erp_stock_in_bill sib ON sib.id = sibi.bill_id",
            "    AND sib.deleted = 0 AND sib.status IN (10, 20)",
            "  WHERE sibi.deleted = 0 AND sibi.product_id = #{productId} AND sibi.warehouse_id = #{warehouseId}",
            "    AND GREATEST(COALESCE(sibi.count, 0) - COALESCE(sibi.picked_count, 0), 0) &gt; 0",
            "       ) d",
            "  LEFT JOIN erp_product p ON p.id = d.productId AND p.deleted = 0",
            "  LEFT JOIN erp_warehouse w ON w.id = d.warehouseId AND w.deleted = 0",
            " <where>",
            "   <if test='unassignedBatch != null and unassignedBatch'>",
            "     (d.batchNo IS NULL OR TRIM(d.batchNo) = '')",
            "   </if>",
            "   <if test='(unassignedBatch == null or !unassignedBatch) and batchNo != null and batchNo.trim() != \"\"'>",
            "     TRIM(d.batchNo) = TRIM(#{batchNo})",
            "   </if>",
            " </where>",
            " ORDER BY d.createTime DESC, d.documentId DESC, d.itemId DESC",
            "</script>"
    })
    List<ErpStockPendingInDetailRespVO> selectList(@Param("productId") Long productId,
                                                   @Param("warehouseId") Long warehouseId,
                                                   @Param("processStatus") Integer processStatus,
                                                   @Param("checkType") Integer checkType,
                                                   @Param("transferInDirection") Integer transferInDirection,
                                                   @Param("batchNo") String batchNo,
                                                   @Param("unassignedBatch") Boolean unassignedBatch);

}
