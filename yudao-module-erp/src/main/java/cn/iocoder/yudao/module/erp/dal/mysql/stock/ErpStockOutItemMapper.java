package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.out.ErpStockOutItemPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutItemDO;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;

/**
 * ERP 其它出库单项 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpStockOutItemMapper extends BaseMapperX<ErpStockOutItemDO> {

    default List<ErpStockOutItemDO> selectListByOutId(Long outId) {
        return selectList(buildSortedQuery(outId, null, null, null));
    }

    default List<ErpStockOutItemDO> selectListByOutId(Long outId, String orderField, String orderDirection) {
        return selectList(buildSortedQuery(outId, null, orderField, orderDirection));
    }

    default List<ErpStockOutItemDO> selectListByOutIdForUpdate(Long outId) {
        return selectList(new LambdaQueryWrapperX<ErpStockOutItemDO>()
                .eq(ErpStockOutItemDO::getOutId, outId).last("FOR UPDATE"));
    }

    default PageResult<ErpStockOutItemDO> selectPageByOutId(ErpStockOutItemPageReqVO reqVO) {
        return selectPage(reqVO, buildSortedQuery(reqVO.getOutId(), reqVO.getExcludedItemIds(),
                reqVO.getOrderField(), reqVO.getOrderDirection()));
    }

    static QueryWrapper<ErpStockOutItemDO> buildSortedQuery(Long outId, Collection<Long> excludedItemIds,
                                                             String orderField, String orderDirection) {
        String item = "erp_stock_out_item";
        QueryWrapper<ErpStockOutItemDO> query = new QueryWrapper<ErpStockOutItemDO>()
                .eq("out_id", outId)
                .notIn(CollUtil.isNotEmpty(excludedItemIds), "id", excludedItemIds);
        return ErpStockItemSortUtils.applyOrder(query, item, orderField, orderDirection,
                getOrderSpec(orderField));
    }

    default List<ErpStockOutItemDO> selectListByOutIds(Collection<Long> outIds) {
        return selectList(ErpStockOutItemDO::getOutId, outIds);
    }

    default Map<Long, Map<String, Object>> selectSummaryMapByOutIds(Collection<Long> outIds) {
        if (CollUtil.isEmpty(outIds)) {
            return Collections.emptyMap();
        }
        return convertMap(selectSummaryRowsByOutIds(outIds), row -> toLong(row.get("outId")), row -> row);
    }

    @Select({
            "<script>",
            "SELECT soi.out_id AS outId,",
            "       COUNT(*) AS itemCount,",
            "       COALESCE(SUM(soi.count), 0) AS totalCount,",
            "       COALESCE(SUM(soi.total_price), 0) AS totalPrice,",
            "       COALESCE(SUM(soi.total_weight), 0) AS totalWeight,",
            "       GROUP_CONCAT(DISTINCT p.name ORDER BY p.name SEPARATOR ', ') AS productNames,",
            "       GROUP_CONCAT(DISTINCT p.code ORDER BY p.code SEPARATOR ', ') AS productCodes,",
            "       GROUP_CONCAT(DISTINCT w.name ORDER BY w.name SEPARATOR ', ') AS warehouseNames",
            "  FROM erp_stock_out_item soi",
            "  LEFT JOIN erp_product p ON p.id = soi.product_id AND p.deleted = b'0'",
            "  LEFT JOIN erp_warehouse w ON w.id = soi.warehouse_id AND w.deleted = b'0'",
            " WHERE soi.deleted = b'0'",
            "   AND soi.out_id IN",
            " <foreach collection='outIds' item='outId' open='(' separator=',' close=')'>#{outId}</foreach>",
            " GROUP BY soi.out_id",
            "</script>"
    })
    List<Map<String, Object>> selectSummaryRowsByOutIds(@Param("outIds") Collection<Long> outIds);

    default int deleteByOutId(Long outId) {
        return delete(ErpStockOutItemDO::getOutId, outId);
    }

    default Long selectCountByProductId(Long productId) {
        return selectCount(ErpStockOutItemDO::getProductId, productId);
    }

    default Long selectCountByWarehouseId(Long warehouseId) {
        return selectCount(ErpStockOutItemDO::getWarehouseId, warehouseId);
    }

    default ErpStockOutItemDO selectFirstByWarehouseId(Long warehouseId) {
        return selectFirstOne(ErpStockOutItemDO::getWarehouseId, warehouseId);
    }

    static boolean isSupportedOrderField(String orderField) {
        return getOrderSpec(orderField) != null;
    }

    static ErpStockItemSortUtils.OrderSpec getOrderSpec(String orderField) {
        if (orderField == null) {
            return null;
        }
        String item = "erp_stock_out_item";
        return switch (orderField.trim()) {
            case "id" -> spec(item + ".id", false);
            case "productId", "productName" -> spec(ErpStockItemSortUtils.productField(item, "name"), true);
            case "productCode" -> spec(ErpStockItemSortUtils.productField(item, "code"), true);
            case "productBarCode" -> spec(ErpStockItemSortUtils.productField(item, "bar_code"), true);
            case "productUnitName" -> spec(ErpStockItemSortUtils.itemUnitName(item), true);
            case "warehouseId" -> spec(ErpStockItemSortUtils.warehouseName(item, "warehouse_id"), true);
            case "warehouseDeptName" -> spec(ErpStockItemSortUtils.warehouseDeptName(item, "warehouse_id"), true);
            case "stockCount" -> spec(ErpStockItemSortUtils.stockCount(item, "warehouse_id"), false);
            case "count" -> spec(item + ".count", false);
            case "productPrice" -> spec(item + ".product_price", false);
            case "totalPrice" -> spec(item + ".total_price", false);
            case "weight" -> spec(item + ".weight", false);
            case "packageQty" -> spec(item + ".package_qty", false);
            case "pieceCount" -> spec(ErpStockItemSortUtils.pieceCount(item), false);
            case "totalWeight" -> spec(item + ".total_weight", false);
            case "batchNo" -> spec(item + ".batch_no", true);
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
