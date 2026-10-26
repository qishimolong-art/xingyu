package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.in.ErpStockInItemPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInItemDO;
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
 * ERP 其它入库单项 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpStockInItemMapper extends BaseMapperX<ErpStockInItemDO> {

    default PageResult<ErpStockInItemDO> selectExportPageByInIds(Collection<Long> ids, int pageNo) {
        if (CollUtil.isEmpty(ids)) return PageResult.empty();
        cn.iocoder.yudao.framework.common.pojo.PageParam page = new cn.iocoder.yudao.framework.common.pojo.PageParam();
        page.setPageNo(pageNo); page.setPageSize(500);
        StringBuilder order = new StringBuilder("CASE in_id ");
        int position = 0;
        for (Long id : ids) order.append("WHEN ").append(id.longValue()).append(" THEN ").append(position++).append(' ');
        order.append("END");
        return selectPage(page, new QueryWrapper<ErpStockInItemDO>().in("in_id", ids).orderByAsc(order.toString(), "id"));
    }

    default List<ErpStockInItemDO> selectListByInId(Long inId) {
        return selectList(buildSortedQuery(inId, null, null, null));
    }

    default List<ErpStockInItemDO> selectListByInId(Long inId, String orderField, String orderDirection) {
        return selectList(buildSortedQuery(inId, null, orderField, orderDirection));
    }

    default List<ErpStockInItemDO> selectListByInIdForUpdate(Long inId) {
        return selectList(new LambdaQueryWrapperX<ErpStockInItemDO>()
                .eq(ErpStockInItemDO::getInId, inId).last("FOR UPDATE"));
    }

    default PageResult<ErpStockInItemDO> selectPageByInId(ErpStockInItemPageReqVO reqVO) {
        return selectPage(reqVO, buildSortedQuery(reqVO.getInId(), reqVO.getExcludedItemIds(),
                reqVO.getOrderField(), reqVO.getOrderDirection()));
    }

    static QueryWrapper<ErpStockInItemDO> buildSortedQuery(Long inId, Collection<Long> excludedItemIds,
                                                            String orderField, String orderDirection) {
        String item = "erp_stock_in_item";
        QueryWrapper<ErpStockInItemDO> query = new QueryWrapper<ErpStockInItemDO>()
                .eq("in_id", inId)
                .notIn(CollUtil.isNotEmpty(excludedItemIds), "id", excludedItemIds);
        return ErpStockItemSortUtils.applyOrder(query, item, orderField, orderDirection,
                getOrderSpec(orderField));
    }

    default List<ErpStockInItemDO> selectListByInIds(Collection<Long> inIds) {
        return selectList(ErpStockInItemDO::getInId, inIds);
    }

    default Map<Long, Map<String, Object>> selectSummaryMapByInIds(Collection<Long> inIds) {
        if (CollUtil.isEmpty(inIds)) {
            return Collections.emptyMap();
        }
        return convertMap(selectSummaryRowsByInIds(inIds), row -> toLong(row.get("inId")), row -> row);
    }

    @Select({
            "<script>",
            "SELECT sii.in_id AS inId,",
            "       COUNT(*) AS itemCount,",
            "       COALESCE(SUM(sii.count), 0) AS totalCount,",
            "       COALESCE(SUM(sii.total_price), 0) AS totalPrice,",
            "       COALESCE(SUM(sii.total_weight), 0) AS totalWeight,",
            "       GROUP_CONCAT(DISTINCT p.name ORDER BY p.name SEPARATOR ', ') AS productNames,",
            "       GROUP_CONCAT(DISTINCT p.code ORDER BY p.code SEPARATOR ', ') AS productCodes,",
            "       GROUP_CONCAT(DISTINCT w.name ORDER BY w.name SEPARATOR ', ') AS warehouseNames",
            "  FROM erp_stock_in_item sii",
            "  LEFT JOIN erp_product p ON p.id = sii.product_id AND p.deleted = b'0'",
            "  LEFT JOIN erp_warehouse w ON w.id = sii.warehouse_id AND w.deleted = b'0'",
            " WHERE sii.deleted = b'0'",
            "   AND sii.in_id IN",
            " <foreach collection='inIds' item='inId' open='(' separator=',' close=')'>#{inId}</foreach>",
            " GROUP BY sii.in_id",
            "</script>"
    })
    List<Map<String, Object>> selectSummaryRowsByInIds(@Param("inIds") Collection<Long> inIds);

    default int deleteByInId(Long inId) {
        return delete(ErpStockInItemDO::getInId, inId);
    }

    default Long selectCountByProductId(Long productId) {
        return selectCount(ErpStockInItemDO::getProductId, productId);
    }

    default Long selectCountByWarehouseId(Long warehouseId) {
        return selectCount(ErpStockInItemDO::getWarehouseId, warehouseId);
    }

    default ErpStockInItemDO selectFirstByWarehouseId(Long warehouseId) {
        return selectFirstOne(ErpStockInItemDO::getWarehouseId, warehouseId);
    }

    static boolean isSupportedOrderField(String orderField) {
        return getOrderSpec(orderField) != null;
    }

    static ErpStockItemSortUtils.OrderSpec getOrderSpec(String orderField) {
        if (orderField == null) {
            return null;
        }
        String item = "erp_stock_in_item";
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
