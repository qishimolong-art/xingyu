package cn.iocoder.yudao.module.erp.dal.mysql.purchase;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockInTransitDetailRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderItemDO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ERP 采购订单明项目 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpPurchaseOrderItemMapper extends BaseMapperX<ErpPurchaseOrderItemDO> {

    default List<ErpPurchaseOrderItemDO> selectListByOrderId(Long orderId) {
        return selectList(ErpPurchaseOrderItemDO::getOrderId, orderId);
    }

    default List<ErpPurchaseOrderItemDO> selectListByOrderIds(Collection<Long> orderIds) {
        return selectList(ErpPurchaseOrderItemDO::getOrderId, orderIds);
    }

    default int deleteByOrderId(Long orderId) {
        return delete(ErpPurchaseOrderItemDO::getOrderId, orderId);
    }

    default Long selectCountByProductId(Long productId) {
        return selectCount(ErpPurchaseOrderItemDO::getProductId, productId);
    }

    default Long selectCountByWarehouseId(Long warehouseId) {
        return selectCount(ErpPurchaseOrderItemDO::getWarehouseId, warehouseId);
    }

    /**
     * 统计每个产品的"未入数"（= SUM(count - inCount)），按 product_id 分组
     */
    default Map<Long, BigDecimal> selectPendingInCountMap(Collection<Long> productIds) {
        if (CollUtil.isEmpty(productIds)) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = selectMaps(
                new QueryWrapper<ErpPurchaseOrderItemDO>()
                        .select("product_id, SUM(COALESCE(count,0) - COALESCE(in_count,0)) AS pending")
                        .in("product_id", productIds)
                        .groupBy("product_id"));
        Map<Long, BigDecimal> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long pid = (Long) row.get("product_id");
            Object val = row.get("pending");
            BigDecimal amount = val == null ? BigDecimal.ZERO
                    : (val instanceof BigDecimal ? (BigDecimal) val : new BigDecimal(val.toString()));
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                result.put(pid, amount);
            }
        }
        return result;
    }

    default Map<String, BigDecimal> selectInTransitCountMap(Collection<Long> productIds,
                                                            Collection<Long> warehouseIds,
                                                            Collection<Integer> statuses) {
        if (CollUtil.isEmpty(productIds) || CollUtil.isEmpty(warehouseIds) || CollUtil.isEmpty(statuses)) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = selectInTransitCountRows(productIds, warehouseIds, statuses);
        Map<String, BigDecimal> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object productId = row.get("product_id");
            Object warehouseId = row.get("warehouse_id");
            Object value = row.get("in_transit");
            if (productId == null || warehouseId == null || value == null) {
                continue;
            }
            BigDecimal amount = value instanceof BigDecimal ? (BigDecimal) value : new BigDecimal(value.toString());
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                result.put(productId + "_" + warehouseId, amount);
            }
        }
        return result;
    }

    @Select({
            "<script>",
            "SELECT poi.product_id, poi.warehouse_id,",
            "       SUM(GREATEST(COALESCE(poi.count, 0) - COALESCE(poi.in_count, 0), 0)) AS in_transit",
            "  FROM erp_purchase_order_items poi",
            " INNER JOIN erp_purchase_order po ON po.id = poi.order_id",
            "   AND po.deleted = 0",
            "   AND po.status IN",
            "   <foreach collection='statuses' item='status' open='(' separator=',' close=')'>",
            "     #{status}",
            "   </foreach>",
            " WHERE poi.deleted = 0",
            "   AND poi.product_id IN",
            "   <foreach collection='productIds' item='productId' open='(' separator=',' close=')'>",
            "     #{productId}",
            "   </foreach>",
            "   AND poi.warehouse_id IN",
            "   <foreach collection='warehouseIds' item='warehouseId' open='(' separator=',' close=')'>",
            "     #{warehouseId}",
            "   </foreach>",
            " GROUP BY poi.product_id, poi.warehouse_id",
            "HAVING SUM(GREATEST(COALESCE(poi.count, 0) - COALESCE(poi.in_count, 0), 0)) > 0",
            "</script>"
    })
    List<Map<String, Object>> selectInTransitCountRows(@Param("productIds") Collection<Long> productIds,
                                                       @Param("warehouseIds") Collection<Long> warehouseIds,
                                                       @Param("statuses") Collection<Integer> statuses);

    default List<ErpStockInTransitDetailRespVO> selectInTransitDetails(Long productId,
                                                                       Long warehouseId,
                                                                       Collection<Integer> statuses,
                                                                       String batchNo,
                                                                       Boolean unassignedBatch) {
        if (productId == null || warehouseId == null || CollUtil.isEmpty(statuses)) {
            return Collections.emptyList();
        }
        return selectInTransitDetailRows(productId, warehouseId, statuses, batchNo, unassignedBatch);
    }

    @Select({
            "<script>",
            "SELECT po.id AS orderId,",
            "       poi.id AS itemId,",
            "       po.no AS no,",
            "       poi.product_id AS productId,",
            "       p.code AS productCode,",
            "       p.name AS productName,",
            "       poi.warehouse_id AS warehouseId,",
            "       w.name AS warehouseName,",
            "       NULLIF(TRIM(poi.batch_no), '') AS batchNo,",
            "       GREATEST(COALESCE(poi.count, 0) - COALESCE(poi.in_count, 0), 0) AS count,",
            "       COALESCE(poi.vehicle_model, p.vehicle_model) AS vehicleModel,",
            "       COALESCE(poi.origin_place, p.origin_place) AS originPlace,",
            "       COALESCE(poi.drawing_no, p.drawing_no) AS drawingNo,",
            "       COALESCE(poi.standard, p.standard) AS standard,",
            "       po.create_time AS createTime,",
            "       po.creator AS creator",
            "  FROM erp_purchase_order_items poi",
            " INNER JOIN erp_purchase_order po ON po.id = poi.order_id",
            "   AND po.deleted = 0",
            "   AND po.status IN",
            "   <foreach collection='statuses' item='status' open='(' separator=',' close=')'>",
            "     #{status}",
            "   </foreach>",
            "  LEFT JOIN erp_product p ON p.id = poi.product_id AND p.deleted = 0",
            "  LEFT JOIN erp_warehouse w ON w.id = poi.warehouse_id AND w.deleted = 0",
            " WHERE poi.deleted = 0",
            "   AND poi.product_id = #{productId}",
            "   AND poi.warehouse_id = #{warehouseId}",
            "   <if test='unassignedBatch != null and unassignedBatch'>",
            "     AND (poi.batch_no IS NULL OR TRIM(poi.batch_no) = '')",
            "   </if>",
            "   <if test='(unassignedBatch == null or !unassignedBatch) and batchNo != null and batchNo.trim() != \"\"'>",
            "     AND TRIM(poi.batch_no) = TRIM(#{batchNo})",
            "   </if>",
            "   AND GREATEST(COALESCE(poi.count, 0) - COALESCE(poi.in_count, 0), 0) > 0",
            " ORDER BY po.create_time DESC, po.id DESC, poi.id DESC",
            "</script>"
    })
    List<ErpStockInTransitDetailRespVO> selectInTransitDetailRows(@Param("productId") Long productId,
                                                                  @Param("warehouseId") Long warehouseId,
                                                                  @Param("statuses") Collection<Integer> statuses,
                                                                  @Param("batchNo") String batchNo,
                                                                  @Param("unassignedBatch") Boolean unassignedBatch);

}
