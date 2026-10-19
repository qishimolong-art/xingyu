package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnItemPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnItemDO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;

/**
 * ERP 销售退货项 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpSaleReturnItemMapper extends BaseMapperX<ErpSaleReturnItemDO> {

    default List<ErpSaleReturnItemDO> selectListByReturnIdForUpdate(Long returnId) {
        // 已持父锁；当前读避免外层旧RR快照，不在FOR UPDATE前拼接会被拦截器错误重排的ORDER BY。
        return selectList(new LambdaQueryWrapperX<ErpSaleReturnItemDO>()
                .eq(ErpSaleReturnItemDO::getReturnId, returnId).last("FOR UPDATE"));
    }

    default int clearOriginalReferencesByReturnId(Long returnId) {
        return update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ErpSaleReturnItemDO>()
                .eq(ErpSaleReturnItemDO::getReturnId, returnId)
                .set(ErpSaleReturnItemDO::getSourceOutItemId, null).set(ErpSaleReturnItemDO::getOrderItemId, null));
    }

    default List<ErpSaleReturnItemDO> selectListByReturnId(Long returnId) {
        return selectList(buildSortedQuery(returnId, null, null, null));
    }

    default List<ErpSaleReturnItemDO> selectListByReturnId(Long returnId, String orderField,
                                                           String orderDirection) {
        return selectList(buildSortedQuery(returnId, null, orderField, orderDirection));
    }

    default PageResult<ErpSaleReturnItemDO> selectPageByReturnId(ErpSaleReturnItemPageReqVO reqVO) {
        return selectPage(reqVO, buildSortedQuery(reqVO.getReturnId(), reqVO.getExcludedItemIds(),
                reqVO.getOrderField(), reqVO.getOrderDirection()));
    }

    static QueryWrapper<ErpSaleReturnItemDO> buildSortedQuery(Long returnId, Collection<Long> excludedItemIds,
                                                               String orderField, String orderDirection) {
        QueryWrapper<ErpSaleReturnItemDO> query = new QueryWrapper<ErpSaleReturnItemDO>()
                .eq("return_id", returnId)
                .notIn(CollUtil.isNotEmpty(excludedItemIds), "id", excludedItemIds);
        return ErpSaleItemSortUtils.applyOrder(query, "erp_sale_return_items", orderField, orderDirection,
                getOrderSpec(orderField));
    }

    static boolean isSupportedOrderField(String orderField) {
        return getOrderSpec(orderField) != null;
    }

    static ErpSaleItemSortUtils.OrderSpec getOrderSpec(String orderField) {
        if (orderField == null) return null;
        String item = "erp_sale_return_items";
        return switch (orderField.trim()) {
            case "id" -> new ErpSaleItemSortUtils.OrderSpec(item + ".id", false);
            case "productId", "productName" -> new ErpSaleItemSortUtils.OrderSpec(
                    ErpSaleItemSortUtils.productField(item, "name"), true);
            case "productCode" -> new ErpSaleItemSortUtils.OrderSpec(
                    ErpSaleItemSortUtils.productField(item, "code"), true);
            case "productBarCode" -> new ErpSaleItemSortUtils.OrderSpec(
                    ErpSaleItemSortUtils.productField(item, "bar_code"), true);
            case "productUnitName" -> new ErpSaleItemSortUtils.OrderSpec(
                    ErpSaleItemSortUtils.productUnitName(item), true);
            case "warehouseId" -> new ErpSaleItemSortUtils.OrderSpec(
                    ErpSaleItemSortUtils.warehouseName(item, "warehouse_id"), true);
            case "deptId" -> new ErpSaleItemSortUtils.OrderSpec(
                    ErpSaleItemSortUtils.deptName(item, "dept_id"), true);
            case "stockCount" -> new ErpSaleItemSortUtils.OrderSpec(ErpSaleItemSortUtils.stockCount(item), false);
            case "pieceCount" -> new ErpSaleItemSortUtils.OrderSpec(
                    ErpSaleItemSortUtils.pieceCount(item, "count"), false);
            case "batchNo" -> new ErpSaleItemSortUtils.OrderSpec(item + ".batch_no", true);
            case "count" -> new ErpSaleItemSortUtils.OrderSpec(item + ".count", false);
            case "productPrice" -> new ErpSaleItemSortUtils.OrderSpec(item + ".product_price", false);
            case "totalPrice", "totalProductPrice" -> new ErpSaleItemSortUtils.OrderSpec(
                    item + ".total_price", false);
            case "taxPercent" -> new ErpSaleItemSortUtils.OrderSpec(item + ".tax_percent", false);
            case "taxPrice" -> new ErpSaleItemSortUtils.OrderSpec(item + ".tax_price", false);
            case "weight" -> new ErpSaleItemSortUtils.OrderSpec(item + ".weight", false);
            case "packageQty" -> new ErpSaleItemSortUtils.OrderSpec(item + ".package_qty", false);
            case "returnReason" -> new ErpSaleItemSortUtils.OrderSpec(item + ".return_reason", true);
            case "warehousePosition" -> new ErpSaleItemSortUtils.OrderSpec(item + ".warehouse_position", true);
            case "remark" -> new ErpSaleItemSortUtils.OrderSpec(item + ".remark", true);
            case "outCount" -> new ErpSaleItemSortUtils.OrderSpec(
                    "(SELECT COALESCE(soi.count, 0) FROM erp_sale_out_items soi WHERE soi.id = "
                            + item + ".source_out_item_id AND soi.tenant_id = " + item
                            + ".tenant_id AND soi.deleted = 0 LIMIT 1)", false);
            case "returnCount" -> new ErpSaleItemSortUtils.OrderSpec(item + ".count", false);
            default -> null;
        };
    }

    default List<ErpSaleReturnItemDO> selectListByReturnIds(Collection<Long> returnIds) {
        return selectList(ErpSaleReturnItemDO::getReturnId, returnIds);
    }

    default Map<Long, Integer> selectItemCountMapByReturnIds(Collection<Long> returnIds) {
        if (CollUtil.isEmpty(returnIds)) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = selectMaps(new QueryWrapper<ErpSaleReturnItemDO>()
                .select("return_id, COUNT(1) AS item_count")
                .in("return_id", returnIds)
                .groupBy("return_id"));
        Map<Long, Integer> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object returnId = row.get("return_id");
            Object count = row.get("item_count");
            if (returnId != null && count != null) {
                result.put(((Number) returnId).longValue(), ((Number) count).intValue());
            }
        }
        return result;
    }

    default Map<Long, String> selectProductNamesMapByReturnIds(Collection<Long> returnIds) {
        if (CollUtil.isEmpty(returnIds)) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = selectProductNamesRows(returnIds);
        Map<Long, String> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object returnId = row.get("return_id");
            Object productNames = row.get("product_names");
            if (returnId != null && productNames != null) {
                result.put(((Number) returnId).longValue(), productNames.toString());
            }
        }
        return result;
    }

    @Select({
            "<script>",
            "SELECT sri.return_id, GROUP_CONCAT(p.name ORDER BY sri.id SEPARATOR '，') AS product_names",
            "  FROM erp_sale_return_items sri",
            "  LEFT JOIN erp_product p ON p.id = sri.product_id AND p.deleted = 0",
            " WHERE sri.deleted = 0",
            "   AND sri.return_id IN",
            "   <foreach collection='returnIds' item='returnId' open='(' separator=',' close=')'>",
            "     #{returnId}",
            "   </foreach>",
            " GROUP BY sri.return_id",
            "</script>"
    })
    List<Map<String, Object>> selectProductNamesRows(@Param("returnIds") Collection<Long> returnIds);

    @Select({
            "SELECT COALESCE(SUM(sri.count), 0) AS totalCount,",
            "       COALESCE(SUM(sri.total_price), 0) AS totalProductPrice,",
            "       COALESCE(SUM(sri.tax_price), 0) AS totalTaxPrice",
            "  FROM erp_sale_return_items sri",
            " WHERE sri.deleted = 0",
            "   AND sri.return_id = #{returnId}"
    })
    Map<String, Object> selectDocumentSummaryByReturnId(@Param("returnId") Long returnId);

    default int deleteByReturnId(Long returnId) {
        return delete(ErpSaleReturnItemDO::getReturnId, returnId);
    }

    static SFunction<ErpSaleReturnItemDO, ?> getOrderColumn(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "id":
                return ErpSaleReturnItemDO::getId;
            case "orderItemId":
                return ErpSaleReturnItemDO::getOrderItemId;
            case "sourceOutItemId":
                return ErpSaleReturnItemDO::getSourceOutItemId;
            case "productId":
            case "productCode":
            case "productName":
            case "productUnitName":
                return ErpSaleReturnItemDO::getProductId;
            case "warehouseId":
                return ErpSaleReturnItemDO::getWarehouseId;
            case "deptId":
                return ErpSaleReturnItemDO::getDeptId;
            case "batchNo":
                return ErpSaleReturnItemDO::getBatchNo;
            case "count":
                return ErpSaleReturnItemDO::getCount;
            case "productPrice":
                return ErpSaleReturnItemDO::getProductPrice;
            case "totalPrice":
                return ErpSaleReturnItemDO::getTotalPrice;
            case "taxPercent":
                return ErpSaleReturnItemDO::getTaxPercent;
            case "taxPrice":
                return ErpSaleReturnItemDO::getTaxPrice;
            case "weight":
                return ErpSaleReturnItemDO::getWeight;
            case "packageQty":
                return ErpSaleReturnItemDO::getPackageQty;
            case "returnReason":
                return ErpSaleReturnItemDO::getReturnReason;
            case "warehousePosition":
                return ErpSaleReturnItemDO::getWarehousePosition;
            case "remark":
                return ErpSaleReturnItemDO::getRemark;
            default:
                return null;
        }
    }

    default Long selectCountByProductId(Long productId) {
        return selectCount(ErpSaleReturnItemDO::getProductId, productId);
    }

    default Long selectCountByWarehouseId(Long warehouseId) {
        return selectCount(ErpSaleReturnItemDO::getWarehouseId, warehouseId);
    }

    /**
     * 基于销售订单编号，查询每个销售订单项的退货数量之和
     *
     * @param returnIds 出库订单项编号数组
     * @return key：销售订单项编号；value：退货数量之和
     */
    default Map<Long, BigDecimal> selectOrderItemCountSumMapByReturnIds(Collection<Long> returnIds) {
        if (CollUtil.isEmpty(returnIds)) {
            return Collections.emptyMap();
        }
        // SQL sum 查询
        List<Map<String, Object>> result = selectMaps(new QueryWrapper<ErpSaleReturnItemDO>()
                .select("order_item_id, SUM(count) AS sum_count")
                .groupBy("order_item_id")
                .in("return_id", returnIds));
        // 获得数量
        return convertMap(result, obj -> (Long) obj.get("order_item_id"), obj -> (BigDecimal) obj.get("sum_count"));
    }

    default Map<Long, BigDecimal> selectSourceOutItemCountSumMapByReturnIds(Collection<Long> returnIds) {
        if (CollUtil.isEmpty(returnIds)) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> result = selectMaps(new QueryWrapper<ErpSaleReturnItemDO>()
                .select("source_out_item_id, SUM(count) AS sum_count")
                .isNotNull("source_out_item_id")
                .groupBy("source_out_item_id")
                .in("return_id", returnIds));
        return convertMap(result, obj -> (Long) obj.get("source_out_item_id"), obj -> (BigDecimal) obj.get("sum_count"));
    }

    default Map<Long, BigDecimal> selectReturnedCountMapBySourceOutItemIds(Collection<Long> sourceOutItemIds) {
        if (CollUtil.isEmpty(sourceOutItemIds)) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> result = selectMaps(new QueryWrapper<ErpSaleReturnItemDO>()
                .select("source_out_item_id, SUM(count) AS sum_count")
                .isNotNull("source_out_item_id")
                .groupBy("source_out_item_id")
                .in("source_out_item_id", sourceOutItemIds));
        return convertMap(result, obj -> (Long) obj.get("source_out_item_id"), obj -> (BigDecimal) obj.get("sum_count"));
    }

}
