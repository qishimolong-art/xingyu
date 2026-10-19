package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMoveItemPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseMoveItemDO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;

@Mapper
public interface ErpWarehouseMoveItemMapper extends BaseMapperX<ErpWarehouseMoveItemDO> {

    default List<ErpWarehouseMoveItemDO> selectListByMoveId(Long moveId) {
        return selectList(buildSortedQuery(moveId, null, null, null));
    }

    default List<ErpWarehouseMoveItemDO> selectListByMoveId(Long moveId, String orderField,
                                                            String orderDirection) {
        return selectList(buildSortedQuery(moveId, null, orderField, orderDirection));
    }

    default List<ErpWarehouseMoveItemDO> selectListByMoveIdForUpdate(Long moveId) {
        return selectList(new LambdaQueryWrapperX<ErpWarehouseMoveItemDO>()
                .eq(ErpWarehouseMoveItemDO::getMoveId, moveId).last("FOR UPDATE"));
    }

    default PageResult<ErpWarehouseMoveItemDO> selectPageByMoveId(ErpWarehouseMoveItemPageReqVO reqVO) {
        return selectPage(reqVO, buildSortedQuery(reqVO.getMoveId(), reqVO.getExcludedItemIds(),
                reqVO.getOrderField(), reqVO.getOrderDirection()));
    }

    static QueryWrapper<ErpWarehouseMoveItemDO> buildSortedQuery(Long moveId, Collection<Long> excludedItemIds,
                                                                  String orderField, String orderDirection) {
        String item = "erp_warehouse_move_item";
        QueryWrapper<ErpWarehouseMoveItemDO> query = new QueryWrapper<ErpWarehouseMoveItemDO>()
                .eq("move_id", moveId)
                .notIn(CollUtil.isNotEmpty(excludedItemIds), "id", excludedItemIds);
        return ErpStockItemSortUtils.applyOrder(query, item, orderField, orderDirection,
                getOrderSpec(orderField));
    }

    default List<ErpWarehouseMoveItemDO> selectListByMoveIds(Collection<Long> moveIds) {
        if (moveIds == null || moveIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(ErpWarehouseMoveItemDO::getMoveId, moveIds);
    }

    default Map<Long, Map<String, Object>> selectSummaryMapByMoveIds(Collection<Long> moveIds) {
        if (CollUtil.isEmpty(moveIds)) {
            return Collections.emptyMap();
        }
        return convertMap(selectSummaryRowsByMoveIds(moveIds), row -> toLong(row.get("moveId")), row -> row);
    }

    @Select({
            "<script>",
            "SELECT wmi.move_id AS moveId,",
            "       COUNT(*) AS itemCount,",
            "       COALESCE(SUM(wmi.count), 0) AS totalCount,",
            "       COALESCE(SUM(wmi.total_price), 0) AS totalPrice,",
            "       COALESCE(SUM(wmi.cost_amount), 0) AS totalCostAmount,",
            "       COALESCE(SUM(wmi.total_weight), 0) AS totalWeight,",
            "       GROUP_CONCAT(DISTINCT p.name ORDER BY p.name SEPARATOR ', ') AS productNames,",
            "       GROUP_CONCAT(DISTINCT p.code ORDER BY p.code SEPARATOR ', ') AS productCodes,",
            "       GROUP_CONCAT(DISTINCT fw.name ORDER BY fw.name SEPARATOR '、') AS fromWarehouseNames,",
            "       GROUP_CONCAT(DISTINCT tw.name ORDER BY tw.name SEPARATOR '、') AS toWarehouseNames",
            "  FROM erp_warehouse_move_item wmi",
            "  LEFT JOIN erp_product p ON p.id = wmi.product_id AND p.deleted = b'0'",
            "  LEFT JOIN erp_warehouse fw ON fw.id = wmi.from_warehouse_id AND fw.deleted = b'0'",
            "  LEFT JOIN erp_warehouse tw ON tw.id = wmi.to_warehouse_id AND tw.deleted = b'0'",
            " WHERE wmi.deleted = b'0'",
            "   AND wmi.move_id IN",
            " <foreach collection='moveIds' item='moveId' open='(' separator=',' close=')'>#{moveId}</foreach>",
            " GROUP BY wmi.move_id",
            "</script>"
    })
    List<Map<String, Object>> selectSummaryRowsByMoveIds(@Param("moveIds") Collection<Long> moveIds);

    default int deleteByMoveId(Long moveId) {
        return delete(ErpWarehouseMoveItemDO::getMoveId, moveId);
    }

    static boolean isSupportedOrderField(String orderField) {
        return getOrderSpec(orderField) != null;
    }

    static ErpStockItemSortUtils.OrderSpec getOrderSpec(String orderField) {
        if (orderField == null) {
            return null;
        }
        String item = "erp_warehouse_move_item";
        return switch (orderField.trim()) {
            case "id" -> spec(item + ".id", false);
            case "productId", "productName" -> spec(ErpStockItemSortUtils.productField(item, "name"), true);
            case "productCode" -> spec(ErpStockItemSortUtils.productField(item, "code"), true);
            case "productBarCode" -> spec(ErpStockItemSortUtils.productField(item, "bar_code"), true);
            case "productUnitName" -> spec(ErpStockItemSortUtils.itemUnitName(item), true);
            case "vehicleModel" -> spec(ErpStockItemSortUtils.productField(item, "vehicle_model"), true);
            case "originPlace" -> spec(ErpStockItemSortUtils.productField(item, "origin_place"), true);
            case "standard" -> spec(ErpStockItemSortUtils.productField(item, "standard"), true);
            case "featureCode" -> spec(ErpStockItemSortUtils.productField(item, "feature_code"), true);
            case "brand" -> spec(ErpStockItemSortUtils.productField(item, "brand"), true);
            case "drawingNo" -> spec(ErpStockItemSortUtils.productField(item, "drawing_no"), true);
            case "fromWarehouseId", "fromWarehouseName" -> spec(
                    ErpStockItemSortUtils.warehouseName(item, "from_warehouse_id"), true);
            case "toWarehouseId", "toWarehouseName" -> spec(
                    ErpStockItemSortUtils.warehouseName(item, "to_warehouse_id"), true);
            case "fromStockCount" -> spec(ErpStockItemSortUtils.stockCount(item, "from_warehouse_id"), false);
            case "toStockCount" -> spec(ErpStockItemSortUtils.stockCount(item, "to_warehouse_id"), false);
            case "count" -> spec(item + ".count", false);
            case "productPrice" -> spec(item + ".product_price", false);
            case "totalPrice" -> spec(item + ".total_price", false);
            case "costPrice" -> spec(item + ".cost_price", false);
            case "costAmount" -> spec(item + ".cost_amount", false);
            case "weight" -> spec(item + ".weight", false);
            case "packageQty" -> spec(item + ".package_qty", false);
            case "pieceCount" -> spec(ErpStockItemSortUtils.pieceCount(item), false);
            case "totalWeight" -> spec(item + ".total_weight", false);
            case "batchNo" -> spec(item + ".batch_no", true);
            case "fromShelf" -> spec(item + ".from_shelf", true);
            case "toShelf" -> spec(item + ".to_shelf", true);
            case "remark" -> spec(item + ".remark", true);
            default -> null;
        };
    }

    private static ErpStockItemSortUtils.OrderSpec spec(String expression, boolean text) {
        return new ErpStockItemSortUtils.OrderSpec(expression, text);
    }

    static Long toLong(Object value) {
        return value instanceof Number ? ((Number) value).longValue() : null;
    }

}
