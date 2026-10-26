package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockMoveItemPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveItemDO;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;

/**
 * ERP 库存调拨单项 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpStockMoveItemMapper extends BaseMapperX<ErpStockMoveItemDO> {

    default PageResult<ErpStockMoveItemDO> selectExportPageByMoveIds(Collection<Long> ids, int pageNo) {
        if (ids == null || ids.isEmpty()) return PageResult.empty();
        cn.iocoder.yudao.framework.common.pojo.PageParam page = new cn.iocoder.yudao.framework.common.pojo.PageParam();
        page.setPageNo(pageNo); page.setPageSize(500);
        StringBuilder order = new StringBuilder("CASE move_id ");
        int index = 0;
        for (Long id : ids) order.append("WHEN ").append(id).append(" THEN ").append(index++).append(' ');
        order.append("END");
        return selectPage(page, new QueryWrapper<ErpStockMoveItemDO>().in("move_id", ids).orderByAsc(order.toString(), "id"));
    }

    default List<ErpStockMoveItemDO> selectListByMoveId(Long moveId) {
        return selectList(buildSortedQuery(moveId, null, null, null));
    }

    default List<ErpStockMoveItemDO> selectListByMoveId(Long moveId, String orderField,
                                                        String orderDirection) {
        return selectList(buildSortedQuery(moveId, null, orderField, orderDirection));
    }

    default List<ErpStockMoveItemDO> selectListByMoveIdForUpdate(Long moveId) {
        return selectList(new LambdaQueryWrapperX<ErpStockMoveItemDO>()
                .eq(ErpStockMoveItemDO::getMoveId, moveId).last("FOR UPDATE"));
    }

    default PageResult<ErpStockMoveItemDO> selectPageByMoveId(ErpStockMoveItemPageReqVO reqVO) {
        return selectPage(reqVO, buildSortedQuery(reqVO.getMoveId(), reqVO.getExcludedItemIds(),
                reqVO.getOrderField(), reqVO.getOrderDirection()));
    }

    static QueryWrapper<ErpStockMoveItemDO> buildSortedQuery(Long moveId, Collection<Long> excludedItemIds,
                                                              String orderField, String orderDirection) {
        String item = "erp_stock_move_item";
        QueryWrapper<ErpStockMoveItemDO> query = new QueryWrapper<ErpStockMoveItemDO>()
                .eq("move_id", moveId)
                .notIn(CollUtil.isNotEmpty(excludedItemIds), "id", excludedItemIds);
        return ErpStockItemSortUtils.applyOrder(query, item, orderField, orderDirection,
                getOrderSpec(orderField));
    }

    default List<ErpStockMoveItemDO> selectListByMoveIds(Collection<Long> moveIds) {
        return selectList(ErpStockMoveItemDO::getMoveId, moveIds);
    }

    default List<ErpStockMoveItemDO> selectLightListByMoveIds(Collection<Long> moveIds) {
        if (cn.hutool.core.collection.CollUtil.isEmpty(moveIds)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<ErpStockMoveItemDO>()
                .select(ErpStockMoveItemDO::getId, ErpStockMoveItemDO::getMoveId,
                        ErpStockMoveItemDO::getFromWarehouseId, ErpStockMoveItemDO::getToWarehouseId,
                        ErpStockMoveItemDO::getFromDeptId, ErpStockMoveItemDO::getToDeptId)
                .in(ErpStockMoveItemDO::getMoveId, moveIds));
    }

    default Map<Long, Map<String, Object>> selectSummaryMapByMoveIds(Collection<Long> moveIds) {
        if (cn.hutool.core.collection.CollUtil.isEmpty(moveIds)) {
            return Collections.emptyMap();
        }
        return convertMap(selectSummaryRowsByMoveIds(moveIds), row -> toLong(row.get("moveId")), row -> row);
    }

    @Select({
            "<script>",
            "SELECT smi.move_id AS moveId,",
            "       COUNT(*) AS itemCount,",
            "       COALESCE(SUM(smi.count), 0) AS totalCount,",
            "       COALESCE(SUM(smi.total_price), 0) AS totalPrice,",
            "       COALESCE(SUM(smi.total_weight), 0) AS totalWeight,",
            "       GROUP_CONCAT(DISTINCT p.name ORDER BY p.name SEPARATOR ', ') AS productNames,",
            "       GROUP_CONCAT(DISTINCT p.code ORDER BY p.code SEPARATOR ', ') AS productCodes,",
            "       GROUP_CONCAT(DISTINCT fw.name ORDER BY fw.name SEPARATOR '、') AS fromWarehouseNames,",
            "       GROUP_CONCAT(DISTINCT tw.name ORDER BY tw.name SEPARATOR '、') AS toWarehouseNames",
            "  FROM erp_stock_move_item smi",
            "  LEFT JOIN erp_product p ON p.id = smi.product_id AND p.deleted = b'0'",
            "  LEFT JOIN erp_warehouse fw ON fw.id = smi.from_warehouse_id AND fw.deleted = b'0'",
            "  LEFT JOIN erp_warehouse tw ON tw.id = smi.to_warehouse_id AND tw.deleted = b'0'",
            " WHERE smi.deleted = b'0'",
            "   AND smi.move_id IN",
            " <foreach collection='moveIds' item='moveId' open='(' separator=',' close=')'>#{moveId}</foreach>",
            " GROUP BY smi.move_id",
            "</script>"
    })
    List<Map<String, Object>> selectSummaryRowsByMoveIds(@Param("moveIds") Collection<Long> moveIds);

    default int deleteByMoveId(Long moveId) {
        return delete(ErpStockMoveItemDO::getMoveId, moveId);
    }

    default Long selectCountByProductId(Long productId) {
        return selectCount(ErpStockMoveItemDO::getProductId, productId);
    }

    default Long selectCountByFromWarehouseId(Long warehouseId) {
        return selectCount(ErpStockMoveItemDO::getFromWarehouseId, warehouseId);
    }

    default Long selectCountByToWarehouseId(Long warehouseId) {
        return selectCount(ErpStockMoveItemDO::getToWarehouseId, warehouseId);
    }

    default ErpStockMoveItemDO selectFirstByFromWarehouseId(Long warehouseId) {
        return selectFirstOne(ErpStockMoveItemDO::getFromWarehouseId, warehouseId);
    }

    default ErpStockMoveItemDO selectFirstByToWarehouseId(Long warehouseId) {
        return selectFirstOne(ErpStockMoveItemDO::getToWarehouseId, warehouseId);
    }

    static boolean isSupportedOrderField(String orderField) {
        return getOrderSpec(orderField) != null;
    }

    static ErpStockItemSortUtils.OrderSpec getOrderSpec(String orderField) {
        if (orderField == null) {
            return null;
        }
        String item = "erp_stock_move_item";
        return switch (orderField.trim()) {
            case "id" -> spec(item + ".id", false);
            case "productId", "productName" -> spec(ErpStockItemSortUtils.productField(item, "name"), true);
            case "productCode" -> spec(ErpStockItemSortUtils.productField(item, "code"), true);
            case "productBarCode" -> spec(ErpStockItemSortUtils.productField(item, "bar_code"), true);
            case "productUnitName" -> spec(ErpStockItemSortUtils.itemUnitName(item), true);
            case "fromWarehouseId" -> spec(ErpStockItemSortUtils.warehouseName(item, "from_warehouse_id"), true);
            case "toWarehouseId" -> spec(ErpStockItemSortUtils.warehouseName(item, "to_warehouse_id"), true);
            case "fromDeptId", "fromWarehouseDeptName" -> spec(
                    ErpStockItemSortUtils.warehouseDeptName(item, "from_warehouse_id"), true);
            case "toDeptId", "toWarehouseDeptName" -> spec(
                    ErpStockItemSortUtils.warehouseDeptName(item, "to_warehouse_id"), true);
            case "stockCount" -> spec(ErpStockItemSortUtils.stockCount(item, "from_warehouse_id"), false);
            case "count" -> spec(item + ".count", false);
            case "productPrice" -> spec(item + ".product_price", false);
            case "totalPrice" -> spec(item + ".total_price", false);
            case "weight" -> spec(item + ".weight", false);
            case "packageQty" -> spec(item + ".package_qty", false);
            case "pieceCount" -> spec(ErpStockItemSortUtils.pieceCount(item), false);
            case "totalWeight" -> spec(item + ".total_weight", false);
            case "batchNo" -> spec(item + ".batch_no", true);
            case "fromShelf" -> spec(item + ".from_shelf", true);
            case "sourceCount" -> spec(item + ".source_count", false);
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

    default Map<Long, BigDecimal> selectMovedCountMapBySourceInItemIds(Collection<Long> sourceInItemIds) {
        return selectMovedCountMapBySourceInItemIds(sourceInItemIds, null);
    }

    default Map<Long, BigDecimal> selectMovedCountMapBySourceInItemIds(Collection<Long> sourceInItemIds,
                                                                       Long excludeMoveId) {
        if (cn.hutool.core.collection.CollUtil.isEmpty(sourceInItemIds)) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> result = selectMovedCountListBySourceInItemIds(sourceInItemIds, excludeMoveId);
        return convertMap(result,
                obj -> (Long) obj.get("source_in_item_id"),
                obj -> (BigDecimal) obj.get("sum_count"));
    }

    default Map<Long, BigDecimal> selectMovedCountMapBySourceSaleReturnItemIds(
            Collection<Long> sourceSaleReturnItemIds, Long excludeMoveId) {
        if (cn.hutool.core.collection.CollUtil.isEmpty(sourceSaleReturnItemIds)) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> result = selectMovedCountListBySourceSaleReturnItemIds(
                sourceSaleReturnItemIds, excludeMoveId);
        return convertMap(result,
                obj -> (Long) obj.get("source_sale_return_item_id"),
                obj -> (BigDecimal) obj.get("sum_count"));
    }

    @Select({
            "<script>",
            "SELECT smi.source_in_item_id, SUM(smi.count) AS sum_count",
            "  FROM erp_stock_move_item smi",
            " INNER JOIN erp_stock_move sm ON sm.id = smi.move_id",
            "   AND sm.deleted = 0",
            "   AND (sm.transfer_direction = 10 OR sm.transfer_direction IS NULL)",
            " WHERE smi.deleted = 0",
            "   AND smi.source_in_item_id IN",
            " <foreach collection='sourceInItemIds' item='sourceInItemId' open='(' separator=',' close=')'>",
            "   #{sourceInItemId}",
            " </foreach>",
            " <if test='excludeMoveId != null'>",
            "   AND smi.move_id &lt;&gt; #{excludeMoveId}",
            " </if>",
            " GROUP BY smi.source_in_item_id",
            "</script>"
    })
    List<Map<String, Object>> selectMovedCountListBySourceInItemIds(
            @Param("sourceInItemIds") Collection<Long> sourceInItemIds,
            @Param("excludeMoveId") Long excludeMoveId);

    @Select({
            "<script>",
            "SELECT smi.source_sale_return_item_id, SUM(smi.count) AS sum_count",
            "  FROM erp_stock_move_item smi",
            " INNER JOIN erp_stock_move sm ON sm.id = smi.move_id",
            "   AND sm.deleted = 0",
            "   AND (sm.transfer_direction = 10 OR sm.transfer_direction IS NULL)",
            " WHERE smi.deleted = 0",
            "   AND smi.source_sale_return_item_id IN",
            " <foreach collection='sourceSaleReturnItemIds' item='sourceSaleReturnItemId' open='(' separator=',' close=')'>",
            "   #{sourceSaleReturnItemId}",
            " </foreach>",
            " <if test='excludeMoveId != null'>",
            "   AND smi.move_id &lt;&gt; #{excludeMoveId}",
            " </if>",
            " GROUP BY smi.source_sale_return_item_id",
            "</script>"
    })
    List<Map<String, Object>> selectMovedCountListBySourceSaleReturnItemIds(
            @Param("sourceSaleReturnItemIds") Collection<Long> sourceSaleReturnItemIds,
            @Param("excludeMoveId") Long excludeMoveId);

}
