package cn.iocoder.yudao.module.erp.dal.mysql.purchase;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderInableItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockInTransitDetailRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderItemDO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.springframework.util.StringUtils;

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

    String PURCHASE_ORDER_ITEM_PRODUCT_KEYWORD_SQL = "("
            + "EXISTS (SELECT 1 FROM erp_product p "
            + "WHERE p.id = erp_purchase_order_items.product_id AND p.deleted = b'0' "
            + "AND p.tenant_id = erp_purchase_order_items.tenant_id "
            + "AND (p.code LIKE {0} OR p.name LIKE {0} OR p.pinyin_code LIKE {0} "
            + "OR p.wubi_code LIKE {0} OR p.bar_code LIKE {0} OR p.vehicle_model LIKE {0} "
            + "OR p.factory_code LIKE {0} OR p.standard LIKE {0} OR p.brand LIKE {0} "
            + "OR p.drawing_no LIKE {0})) "
            + "OR erp_purchase_order_items.vehicle_model LIKE {0} "
            + "OR erp_purchase_order_items.factory_code LIKE {0} "
            + "OR erp_purchase_order_items.standard LIKE {0} "
            + "OR erp_purchase_order_items.feature_code LIKE {0} "
            + "OR erp_purchase_order_items.brand LIKE {0} "
            + "OR erp_purchase_order_items.drawing_no LIKE {0} "
            + "OR erp_purchase_order_items.batch_no LIKE {0})";

    default List<ErpPurchaseOrderItemDO> selectListByOrderId(Long orderId) {
        return selectList(buildSortedQuery(orderId, null, null, null));
    }

    default List<ErpPurchaseOrderItemDO> selectListByOrderId(Long orderId, String orderField,
                                                             String orderDirection) {
        return selectList(buildSortedQuery(orderId, null, orderField, orderDirection));
    }

    default PageResult<ErpPurchaseOrderItemDO> selectInableItemPage(ErpPurchaseOrderInableItemPageReqVO reqVO) {
        QueryWrapper<ErpPurchaseOrderItemDO> query = new QueryWrapper<ErpPurchaseOrderItemDO>()
                .eq("order_id", reqVO.getOrderId())
                .apply("COALESCE(count, 0) > COALESCE(in_count, 0)");
        appendProductKeyword(query, reqVO.getProductKeyword());
        query.orderByAsc("id");
        return selectPage(reqVO, query);
    }

    default List<ErpPurchaseOrderItemDO> selectListByOrderIdForUpdate(Long orderId) {
        return selectList(new LambdaQueryWrapperX<ErpPurchaseOrderItemDO>()
                .eq(ErpPurchaseOrderItemDO::getOrderId, orderId).last("FOR UPDATE"));
    }

    default PageResult<ErpPurchaseOrderItemDO> selectPageByOrderId(ErpPurchaseOrderItemPageReqVO reqVO) {
        return selectPage(reqVO, buildSortedQuery(reqVO.getOrderId(), reqVO.getExcludedItemIds(),
                reqVO.getOrderField(), reqVO.getOrderDirection()));
    }

    static QueryWrapper<ErpPurchaseOrderItemDO> buildSortedQuery(Long orderId, Collection<Long> excludedItemIds,
                                                                  String orderField, String orderDirection) {
        QueryWrapper<ErpPurchaseOrderItemDO> query = new QueryWrapper<ErpPurchaseOrderItemDO>()
                .eq("order_id", orderId)
                .notIn(CollUtil.isNotEmpty(excludedItemIds), "id", excludedItemIds);
        return ErpPurchaseItemSortUtils.applyOrder(query, "erp_purchase_order_items", orderField, orderDirection,
                getOrderSpec(orderField));
    }

    default List<ErpPurchaseOrderItemDO> selectListByOrderIds(Collection<Long> orderIds) {
        return selectList(ErpPurchaseOrderItemDO::getOrderId, orderIds);
    }

    default Map<Long, Integer> selectItemCountMapByOrderIds(Collection<Long> orderIds) {
        if (CollUtil.isEmpty(orderIds)) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = selectMaps(new QueryWrapper<ErpPurchaseOrderItemDO>()
                .select("order_id, COUNT(1) AS item_count")
                .in("order_id", orderIds)
                .groupBy("order_id"));
        Map<Long, Integer> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object orderId = row.get("order_id");
            Object count = row.get("item_count");
            if (orderId != null && count != null) {
                result.put(Long.valueOf(orderId.toString()), Integer.valueOf(count.toString()));
            }
        }
        return result;
    }

    default int deleteByOrderId(Long orderId) {
        return delete(ErpPurchaseOrderItemDO::getOrderId, orderId);
    }

    static boolean isSupportedOrderField(String orderField) {
        return getOrderSpec(orderField) != null;
    }

    static ErpPurchaseItemSortUtils.OrderSpec getOrderSpec(String orderField) {
        if (orderField == null) {
            return null;
        }
        String item = "erp_purchase_order_items";
        return switch (orderField.trim()) {
            case "id" -> spec(item + ".id", false);
            case "gift" -> spec(item + ".gift", false);
            case "productId", "productName" -> spec(ErpPurchaseItemSortUtils.productField(item, "name"), true);
            case "productCode" -> spec(ErpPurchaseItemSortUtils.productField(item, "code"), true);
            case "lastPurchasePrice" -> spec(ErpPurchaseItemSortUtils.productField(item, "last_purchase_price"), false);
            case "productUnitName" -> spec(ErpPurchaseItemSortUtils.itemUnitName(item), true);
            case "weight" -> spec(ErpPurchaseItemSortUtils.productField(item, "weight"), false);
            case "packageQty" -> spec(ErpPurchaseItemSortUtils.productPackageQty(item), false);
            case "pieceCount" -> spec(ErpPurchaseItemSortUtils.pieceCount(item + ".count",
                    ErpPurchaseItemSortUtils.productPackageQty(item)), false);
            case "stockCount" -> spec(ErpPurchaseItemSortUtils.stockCount(item), false);
            case "warehouseId" -> spec(ErpPurchaseItemSortUtils.warehouseName(item), true);
            case "deptId" -> spec(ErpPurchaseItemSortUtils.deptName(item), true);
            case "count" -> spec(item + ".count", false);
            case "productPrice" -> spec(item + ".product_price", false);
            case "totalProductPrice" -> spec(ErpPurchaseItemSortUtils.productAmount(item), false);
            case "arrivalCount" -> spec(item + ".arrival_count", false);
            case "vehicleModel" -> spec(item + ".vehicle_model", true);
            case "standard" -> spec(item + ".standard", true);
            case "featureCode" -> spec(item + ".feature_code", true);
            case "warehousePosition" -> spec(item + ".warehouse_position", true);
            case "drawingNo" -> spec(item + ".drawing_no", true);
            case "batchNo" -> spec(item + ".batch_no", true);
            case "factoryCode" -> spec(item + ".factory_code", true);
            case "brand" -> spec(item + ".brand", true);
            case "remark" -> spec(item + ".remark", true);
            case "inStatus" -> spec("(CASE WHEN COALESCE(" + item + ".in_count, 0) <= 0 THEN 0"
                    + " WHEN COALESCE(" + item + ".in_count, 0) >= COALESCE(" + item
                    + ".count, 0) THEN 2 ELSE 1 END)", false);
            default -> null;
        };
    }

    private static ErpPurchaseItemSortUtils.OrderSpec spec(String expression, boolean text) {
        return new ErpPurchaseItemSortUtils.OrderSpec(expression, text);
    }

    default Long selectCountByProductId(Long productId) {
        return selectCount(ErpPurchaseOrderItemDO::getProductId, productId);
    }

    default Long selectCountByWarehouseId(Long warehouseId) {
        return selectCount(ErpPurchaseOrderItemDO::getWarehouseId, warehouseId);
    }

    default int updateStockCountAtOrderIfNull(Long id, BigDecimal stockCount) {
        return update(null, new LambdaUpdateWrapper<ErpPurchaseOrderItemDO>()
                .eq(ErpPurchaseOrderItemDO::getId, id)
                .isNull(ErpPurchaseOrderItemDO::getStockCountAtOrder)
                .set(ErpPurchaseOrderItemDO::getStockCountAtOrder, stockCount));
    }

    default int clearStockCountAtOrder(Long id) {
        return update(null, new LambdaUpdateWrapper<ErpPurchaseOrderItemDO>()
                .eq(ErpPurchaseOrderItemDO::getId, id)
                .set(ErpPurchaseOrderItemDO::getStockCountAtOrder, null));
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

    static void appendProductKeyword(QueryWrapper<ErpPurchaseOrderItemDO> query, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return;
        }
        String likeValue = "%" + keyword.trim().replaceAll("\\s+", "%") + "%";
        query.and(wrapper -> wrapper.apply(PURCHASE_ORDER_ITEM_PRODUCT_KEYWORD_SQL, likeValue));
    }

}
