package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust.ErpSalePriceAdjustItemPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSalePriceAdjustItemDO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * ERP 销售调价单明细 Mapper
 *
 * @author 汽配ERP
 */
@Mapper
public interface ErpSalePriceAdjustItemMapper extends BaseMapperX<ErpSalePriceAdjustItemDO> {

    default List<ErpSalePriceAdjustItemDO> selectListByAdjustId(Long adjustId) {
        return selectList(buildSortedQuery(adjustId, null, null, null));
    }

    default List<ErpSalePriceAdjustItemDO> selectListByAdjustId(Long adjustId, String orderField,
                                                                String orderDirection) {
        return selectList(buildSortedQuery(adjustId, null, orderField, orderDirection));
    }

    default List<ErpSalePriceAdjustItemDO> selectListByAdjustIdForUpdate(Long adjustId) {
        return selectList(new LambdaQueryWrapperX<ErpSalePriceAdjustItemDO>()
                .eq(ErpSalePriceAdjustItemDO::getAdjustId, adjustId).last("FOR UPDATE"));
    }

    default PageResult<ErpSalePriceAdjustItemDO> selectPageByAdjustId(ErpSalePriceAdjustItemPageReqVO reqVO) {
        return selectPage(reqVO, buildSortedQuery(reqVO.getAdjustId(), reqVO.getExcludedItemIds(),
                reqVO.getOrderField(), reqVO.getOrderDirection()));
    }

    static QueryWrapper<ErpSalePriceAdjustItemDO> buildSortedQuery(Long adjustId,
                                                                    Collection<Long> excludedItemIds,
                                                                    String orderField,
                                                                    String orderDirection) {
        QueryWrapper<ErpSalePriceAdjustItemDO> query = new QueryWrapper<ErpSalePriceAdjustItemDO>()
                .eq("adjust_id", adjustId)
                .notIn(CollUtil.isNotEmpty(excludedItemIds), "id", excludedItemIds);
        return ErpSaleItemSortUtils.applyOrder(query, "erp_sale_price_adjust_item", orderField,
                orderDirection, getOrderSpec(orderField));
    }

    static boolean isSupportedOrderField(String orderField) {
        return getOrderSpec(orderField) != null;
    }

    static ErpSaleItemSortUtils.OrderSpec getOrderSpec(String orderField) {
        if (orderField == null) return null;
        String item = "erp_sale_price_adjust_item";
        String outItem = "(SELECT soi.%s FROM erp_sale_out_items soi WHERE soi.id = " + item
                + ".sale_out_item_id AND soi.tenant_id = " + item
                + ".tenant_id AND soi.deleted = 0 LIMIT 1)";
        return switch (orderField.trim()) {
            case "id" -> new ErpSaleItemSortUtils.OrderSpec(item + ".id", false);
            case "saleOutNo" -> new ErpSaleItemSortUtils.OrderSpec(item + ".sale_out_no", true);
            case "partCode" -> new ErpSaleItemSortUtils.OrderSpec(item + ".part_code", true);
            case "partName" -> new ErpSaleItemSortUtils.OrderSpec(item + ".part_name", true);
            case "oldPrice" -> new ErpSaleItemSortUtils.OrderSpec(item + ".old_price", false);
            case "newPrice" -> new ErpSaleItemSortUtils.OrderSpec(item + ".new_price", false);
            case "outCount" -> new ErpSaleItemSortUtils.OrderSpec(item + ".out_count", false);
            case "stockCount" -> new ErpSaleItemSortUtils.OrderSpec(
                    "(SELECT s.count FROM erp_stock s WHERE s.product_id = " + item
                            + ".product_id AND s.warehouse_id = " + String.format(outItem, "warehouse_id")
                            + " AND s.tenant_id = " + item + ".tenant_id AND s.deleted = 0 LIMIT 1)", false);
            case "warehouseName" -> new ErpSaleItemSortUtils.OrderSpec(
                    "(SELECT w.name FROM erp_warehouse w WHERE w.id = "
                            + String.format(outItem, "warehouse_id") + " AND w.tenant_id = " + item
                            + ".tenant_id AND w.deleted = 0 LIMIT 1)", true);
            case "batchNo" -> new ErpSaleItemSortUtils.OrderSpec(String.format(outItem, "batch_no"), true);
            case "vehicleModel" -> new ErpSaleItemSortUtils.OrderSpec(item + ".vehicle_model", true);
            case "originPlace" -> new ErpSaleItemSortUtils.OrderSpec(item + ".origin_place", true);
            case "brand" -> new ErpSaleItemSortUtils.OrderSpec(item + ".brand", true);
            case "unit" -> new ErpSaleItemSortUtils.OrderSpec(item + ".unit", true);
            case "weight" -> new ErpSaleItemSortUtils.OrderSpec(item + ".weight", false);
            case "packageQty" -> new ErpSaleItemSortUtils.OrderSpec(item + ".package_qty", false);
            case "pieceCount" -> new ErpSaleItemSortUtils.OrderSpec(
                    ErpSaleItemSortUtils.pieceCount(item, "out_count"), false);
            case "adjustAmount" -> new ErpSaleItemSortUtils.OrderSpec(
                    item + ".new_price * " + item + ".out_count", false);
            case "adjustPrice" -> new ErpSaleItemSortUtils.OrderSpec(item + ".adjust_price", false);
            case "adjustReason" -> new ErpSaleItemSortUtils.OrderSpec(item + ".adjust_reason", true);
            case "itemRemark" -> new ErpSaleItemSortUtils.OrderSpec(item + ".item_remark", true);
            default -> null;
        };
    }

    default List<ErpSalePriceAdjustItemDO> selectListByAdjustIds(Collection<Long> adjustIds) {
        return selectList(ErpSalePriceAdjustItemDO::getAdjustId, adjustIds);
    }

    default Map<Long, Integer> selectItemCountMapByAdjustIds(Collection<Long> adjustIds) {
        if (CollUtil.isEmpty(adjustIds)) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = selectMaps(new QueryWrapper<ErpSalePriceAdjustItemDO>()
                .select("adjust_id, COUNT(1) AS item_count")
                .in("adjust_id", adjustIds)
                .groupBy("adjust_id"));
        Map<Long, Integer> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object adjustId = row.get("adjust_id");
            Object count = row.get("item_count");
            if (adjustId != null && count != null) {
                result.put(((Number) adjustId).longValue(), ((Number) count).intValue());
            }
        }
        return result;
    }

    default List<ErpSalePriceAdjustItemDO> selectSummaryListByAdjustIds(Collection<Long> adjustIds) {
        if (CollUtil.isEmpty(adjustIds)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<ErpSalePriceAdjustItemDO>()
                .select(ErpSalePriceAdjustItemDO::getAdjustId, ErpSalePriceAdjustItemDO::getSaleOutNo,
                        ErpSalePriceAdjustItemDO::getOutCount, ErpSalePriceAdjustItemDO::getOldPrice,
                        ErpSalePriceAdjustItemDO::getNewPrice)
                .in(ErpSalePriceAdjustItemDO::getAdjustId, adjustIds));
    }

    @Select({
            "SELECT COALESCE(SUM(spai.out_count * spai.old_price), 0) AS totalOriginalPrice,",
            "       COALESCE(SUM(spai.out_count * spai.new_price), 0) AS totalAdjustedPrice,",
            "       COALESCE(SUM(spai.adjust_price), 0) AS totalAdjustPrice,",
            "       GROUP_CONCAT(DISTINCT spai.sale_out_no ORDER BY spai.sale_out_no SEPARATOR '、') AS sourceNo",
            "  FROM erp_sale_price_adjust_item spai",
            " WHERE spai.deleted = 0",
            "   AND spai.adjust_id = #{adjustId}"
    })
    Map<String, Object> selectDocumentSummaryByAdjustId(@Param("adjustId") Long adjustId);

    default List<ErpSalePriceAdjustItemDO> selectListBySaleOutItemIds(Collection<Long> saleOutItemIds) {
        return selectList(ErpSalePriceAdjustItemDO::getSaleOutItemId, saleOutItemIds);
    }

    default void deleteByAdjustId(Long adjustId) {
        delete(ErpSalePriceAdjustItemDO::getAdjustId, adjustId);
    }

    static SFunction<ErpSalePriceAdjustItemDO, ?> getOrderColumn(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "id":
                return ErpSalePriceAdjustItemDO::getId;
            case "saleOutNo":
                return ErpSalePriceAdjustItemDO::getSaleOutNo;
            case "partCode":
                return ErpSalePriceAdjustItemDO::getPartCode;
            case "partName":
                return ErpSalePriceAdjustItemDO::getPartName;
            case "vehicleModel":
                return ErpSalePriceAdjustItemDO::getVehicleModel;
            case "originPlace":
                return ErpSalePriceAdjustItemDO::getOriginPlace;
            case "brand":
                return ErpSalePriceAdjustItemDO::getBrand;
            case "unit":
                return ErpSalePriceAdjustItemDO::getUnit;
            case "weight":
                return ErpSalePriceAdjustItemDO::getWeight;
            case "packageQty":
                return ErpSalePriceAdjustItemDO::getPackageQty;
            case "outCount":
                return ErpSalePriceAdjustItemDO::getOutCount;
            case "oldPrice":
                return ErpSalePriceAdjustItemDO::getOldPrice;
            case "newPrice":
                return ErpSalePriceAdjustItemDO::getNewPrice;
            case "adjustPrice":
                return ErpSalePriceAdjustItemDO::getAdjustPrice;
            case "productId":
                return ErpSalePriceAdjustItemDO::getProductId;
            case "deptId":
                return ErpSalePriceAdjustItemDO::getDeptId;
            case "saleOutItemId":
                return ErpSalePriceAdjustItemDO::getSaleOutItemId;
            case "saleOutId":
                return ErpSalePriceAdjustItemDO::getSaleOutId;
            case "adjustReason":
                return ErpSalePriceAdjustItemDO::getAdjustReason;
            case "itemRemark":
                return ErpSalePriceAdjustItemDO::getItemRemark;
            default:
                return null;
        }
    }

}
