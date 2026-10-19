package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order.ErpSaleOrderItemPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderItemDO;
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

/**
 * ERP 销售订单明项目 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpSaleOrderItemMapper extends BaseMapperX<ErpSaleOrderItemDO> {

    default List<ErpSaleOrderItemDO> selectListByOrderId(Long orderId) {
        return selectList(buildSortedQuery(orderId, null, null, null));
    }

    default List<ErpSaleOrderItemDO> selectListByOrderId(Long orderId, String orderField,
                                                         String orderDirection) {
        return selectList(buildSortedQuery(orderId, null, orderField, orderDirection));
    }

    default List<ErpSaleOrderItemDO> selectListByOrderIdForUpdate(Long orderId) {
        return selectList(new LambdaQueryWrapperX<ErpSaleOrderItemDO>()
                .eq(ErpSaleOrderItemDO::getOrderId, orderId).last("FOR UPDATE"));
    }

    default PageResult<ErpSaleOrderItemDO> selectPageByOrderId(ErpSaleOrderItemPageReqVO reqVO) {
        return selectPage(reqVO, buildSortedQuery(reqVO.getOrderId(), reqVO.getExcludedItemIds(),
                reqVO.getOrderField(), reqVO.getOrderDirection()));
    }

    static QueryWrapper<ErpSaleOrderItemDO> buildSortedQuery(Long orderId, Collection<Long> excludedItemIds,
                                                              String orderField, String orderDirection) {
        QueryWrapper<ErpSaleOrderItemDO> query = new QueryWrapper<ErpSaleOrderItemDO>()
                .eq("order_id", orderId)
                .notIn(CollUtil.isNotEmpty(excludedItemIds), "id", excludedItemIds);
        return ErpSaleItemSortUtils.applyOrder(query, "erp_sale_order_items", orderField, orderDirection,
                getOrderSpec(orderField));
    }

    static boolean isSupportedOrderField(String orderField) {
        return getOrderSpec(orderField) != null;
    }

    static ErpSaleItemSortUtils.OrderSpec getOrderSpec(String orderField) {
        if (orderField == null) {
            return null;
        }
        String item = "erp_sale_order_items";
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
            case "lastSalePrice" -> new ErpSaleItemSortUtils.OrderSpec(
                    ErpSaleItemSortUtils.lastSalePrice(item), false);
            case "pieceCount" -> new ErpSaleItemSortUtils.OrderSpec(
                    ErpSaleItemSortUtils.pieceCount(item, "count"), false);
            case "giftFlag" -> new ErpSaleItemSortUtils.OrderSpec(item + ".gift_flag", false);
            case "batchNo" -> new ErpSaleItemSortUtils.OrderSpec(item + ".batch_no", true);
            case "weight" -> new ErpSaleItemSortUtils.OrderSpec(item + ".weight", false);
            case "packageQty" -> new ErpSaleItemSortUtils.OrderSpec(item + ".package_qty", false);
            case "remark" -> new ErpSaleItemSortUtils.OrderSpec(item + ".remark", true);
            case "count" -> new ErpSaleItemSortUtils.OrderSpec(item + ".count", false);
            case "productPrice", "salePrice" -> new ErpSaleItemSortUtils.OrderSpec(
                    item + ".product_price", false);
            case "totalProductPrice" -> new ErpSaleItemSortUtils.OrderSpec(item + ".total_price", false);
            case "saleCostPrice" -> new ErpSaleItemSortUtils.OrderSpec(
                    "(SELECT CASE WHEN SUM(ABS(sr.count)) > 0 THEN SUM(ABS(sr.total_price)) / SUM(ABS(sr.count))"
                            + " ELSE NULL END FROM erp_sale_out_items soi INNER JOIN erp_stock_record sr"
                            + " ON sr.biz_item_id = soi.id AND sr.biz_type = 50 AND sr.tenant_id = soi.tenant_id"
                            + " AND sr.deleted = 0 WHERE soi.order_item_id = " + item
                            + ".id AND soi.tenant_id = " + item + ".tenant_id AND soi.deleted = 0)", false);
            case "saleCostAmount" -> new ErpSaleItemSortUtils.OrderSpec(
                    "(SELECT SUM(ABS(sr.total_price)) FROM erp_sale_out_items soi INNER JOIN erp_stock_record sr"
                            + " ON sr.biz_item_id = soi.id AND sr.biz_type = 50 AND sr.tenant_id = soi.tenant_id"
                            + " AND sr.deleted = 0 WHERE soi.order_item_id = " + item
                            + ".id AND soi.tenant_id = " + item + ".tenant_id AND soi.deleted = 0)", false);
            case "taxPercent" -> new ErpSaleItemSortUtils.OrderSpec(item + ".tax_percent", false);
            case "taxPrice" -> new ErpSaleItemSortUtils.OrderSpec(item + ".tax_price", false);
            case "totalPrice" -> new ErpSaleItemSortUtils.OrderSpec(
                    item + ".total_price + COALESCE(" + item + ".tax_price, 0)", false);
            default -> null;
        };
    }

    default List<ErpSaleOrderItemDO> selectListByOrderIds(Collection<Long> orderIds) {
        return selectList(ErpSaleOrderItemDO::getOrderId, orderIds);
    }

    default Map<Long, Integer> selectItemCountMapByOrderIds(Collection<Long> orderIds) {
        if (CollUtil.isEmpty(orderIds)) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = selectMaps(new QueryWrapper<ErpSaleOrderItemDO>()
                .select("order_id, COUNT(1) AS item_count")
                .in("order_id", orderIds)
                .groupBy("order_id"));
        Map<Long, Integer> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object orderId = row.get("order_id");
            Object count = row.get("item_count");
            if (orderId != null && count != null) {
                result.put(((Number) orderId).longValue(), ((Number) count).intValue());
            }
        }
        return result;
    }

    default Map<Long, String> selectProductNamesMapByOrderIds(Collection<Long> orderIds) {
        if (CollUtil.isEmpty(orderIds)) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = selectProductNamesRows(orderIds);
        Map<Long, String> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object orderId = row.get("order_id");
            Object productNames = row.get("product_names");
            if (orderId != null && productNames != null) {
                result.put(((Number) orderId).longValue(), productNames.toString());
            }
        }
        return result;
    }

    @Select({
            "<script>",
            "SELECT soi.order_id, GROUP_CONCAT(p.name ORDER BY soi.id SEPARATOR '，') AS product_names",
            "  FROM erp_sale_order_items soi",
            "  LEFT JOIN erp_product p ON p.id = soi.product_id AND p.deleted = 0",
            " WHERE soi.deleted = 0",
            "   AND soi.order_id IN",
            "   <foreach collection='orderIds' item='orderId' open='(' separator=',' close=')'>",
            "     #{orderId}",
            "   </foreach>",
            " GROUP BY soi.order_id",
            "</script>"
    })
    List<Map<String, Object>> selectProductNamesRows(@Param("orderIds") Collection<Long> orderIds);

    default int deleteByOrderId(Long orderId) {
        return delete(ErpSaleOrderItemDO::getOrderId, orderId);
    }

    static SFunction<ErpSaleOrderItemDO, ?> getOrderColumn(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "id":
                return ErpSaleOrderItemDO::getId;
            case "giftFlag":
                return ErpSaleOrderItemDO::getGiftFlag;
            case "productId":
            case "productCode":
            case "productName":
            case "productUnitName":
                return ErpSaleOrderItemDO::getProductId;
            case "warehouseId":
                return ErpSaleOrderItemDO::getWarehouseId;
            case "deptId":
                return ErpSaleOrderItemDO::getDeptId;
            case "count":
                return ErpSaleOrderItemDO::getCount;
            case "productPrice":
                return ErpSaleOrderItemDO::getProductPrice;
            case "totalPrice":
                return ErpSaleOrderItemDO::getTotalPrice;
            case "taxPercent":
                return ErpSaleOrderItemDO::getTaxPercent;
            case "taxPrice":
                return ErpSaleOrderItemDO::getTaxPrice;
            case "weight":
                return ErpSaleOrderItemDO::getWeight;
            case "packageQty":
                return ErpSaleOrderItemDO::getPackageQty;
            case "batchNo":
                return ErpSaleOrderItemDO::getBatchNo;
            case "outCount":
                return ErpSaleOrderItemDO::getOutCount;
            case "returnCount":
                return ErpSaleOrderItemDO::getReturnCount;
            case "remark":
                return ErpSaleOrderItemDO::getRemark;
            default:
                return null;
        }
    }

    default Long selectCountByProductId(Long productId) {
        return selectCount(ErpSaleOrderItemDO::getProductId, productId);
    }

    /**
     * 统计每个产品的"占用数"（= SUM(count - outCount)），按 product_id 分组
     */
    default Map<Long, BigDecimal> selectOccupiedCountMap(Collection<Long> productIds) {
        if (CollUtil.isEmpty(productIds)) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = selectMaps(
                new QueryWrapper<ErpSaleOrderItemDO>()
                        .select("product_id, SUM(COALESCE(count,0) - COALESCE(out_count,0)) AS occupied")
                        .in("product_id", productIds)
                        .groupBy("product_id"));
        Map<Long, BigDecimal> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long pid = (Long) row.get("product_id");
            Object val = row.get("occupied");
            BigDecimal amount = val == null ? BigDecimal.ZERO
                    : (val instanceof BigDecimal ? (BigDecimal) val : new BigDecimal(val.toString()));
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                result.put(pid, amount);
            }
        }
        return result;
    }

}
