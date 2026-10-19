package cn.iocoder.yudao.module.erp.dal.mysql.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust.ErpPurchasePriceAdjustItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoiceSourceItemPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustDO;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ERP 采购调价单明细 Mapper
 *
 * @author 汽配ERP
 */
@Mapper
public interface ErpPurchasePriceAdjustItemMapper extends BaseMapperX<ErpPurchasePriceAdjustItemDO> {

    default List<ErpPurchasePriceAdjustItemDO> selectListByAdjustId(Long adjustId) {
        return selectList(buildSortedQuery(adjustId, null, null, null));
    }

    default List<ErpPurchasePriceAdjustItemDO> selectListByAdjustId(Long adjustId, String orderField,
                                                                    String orderDirection) {
        return selectList(buildSortedQuery(adjustId, null, orderField, orderDirection));
    }

    default List<ErpPurchasePriceAdjustItemDO> selectListByAdjustIdForUpdate(Long adjustId) {
        return selectList(new LambdaQueryWrapperX<ErpPurchasePriceAdjustItemDO>()
                .eq(ErpPurchasePriceAdjustItemDO::getAdjustId, adjustId).last("FOR UPDATE"));
    }

    default PageResult<ErpPurchasePriceAdjustItemDO> selectPageByAdjustId(ErpPurchasePriceAdjustItemPageReqVO reqVO) {
        return selectPage(reqVO, buildSortedQuery(reqVO.getAdjustId(), reqVO.getExcludedItemIds(),
                reqVO.getOrderField(), reqVO.getOrderDirection()));
    }

    static QueryWrapper<ErpPurchasePriceAdjustItemDO> buildSortedQuery(Long adjustId,
                                                                        Collection<Long> excludedItemIds,
                                                                        String orderField,
                                                                        String orderDirection) {
        QueryWrapper<ErpPurchasePriceAdjustItemDO> query = new QueryWrapper<ErpPurchasePriceAdjustItemDO>()
                .eq("adjust_id", adjustId)
                .notIn(CollUtil.isNotEmpty(excludedItemIds), "id", excludedItemIds);
        return ErpPurchaseItemSortUtils.applyOrder(query, "erp_purchase_price_adjust_item", orderField,
                orderDirection, getOrderSpec(orderField));
    }

    default List<ErpPurchasePriceAdjustItemDO> selectListByAdjustIds(Collection<Long> adjustIds) {
        return selectList(ErpPurchasePriceAdjustItemDO::getAdjustId, adjustIds);
    }

    default PageResult<ErpPurchasePriceAdjustItemDO> selectInvoiceSourcePage(
            ErpPurchaseInvoiceSourceItemPageReqVO reqVO) {
        if (CollUtil.isEmpty(reqVO.getSourceDocIds())) {
            return PageResult.empty();
        }
        QueryWrapper<ErpPurchasePriceAdjustItemDO> query = new QueryWrapper<ErpPurchasePriceAdjustItemDO>()
                .in("adjust_id", reqVO.getSourceDocIds())
                .ne("adjust_price", 0)
                .apply("COALESCE(count,0)>COALESCE((SELECT SUM(COALESCE(a.allocated_count,0)) "
                        + "FROM erp_purchase_invoice_source_allocation a WHERE a.deleted=b'0' "
                        + "AND a.tenant_id=erp_purchase_price_adjust_item.tenant_id AND a.source_type=20 "
                        + "AND a.source_item_id=erp_purchase_price_adjust_item.id),0)")
                .orderByAsc("id");
        return selectPage(reqVO, query);
    }

    default PageResult<ErpPurchasePriceAdjustItemDO> selectInvoiceMixedSourcePage(
            ErpPurchaseInvoiceSourceItemPageReqVO reqVO) {
        if (CollUtil.isEmpty(reqVO.getSourceDocIds())) {
            return PageResult.empty();
        }
        MPJLambdaWrapperX<ErpPurchasePriceAdjustItemDO> query = new MPJLambdaWrapperX<>();
        query.selectAll(ErpPurchasePriceAdjustItemDO.class)
                .innerJoin(ErpPurchasePriceAdjustDO.class, ErpPurchasePriceAdjustDO::getId,
                        ErpPurchasePriceAdjustItemDO::getAdjustId)
                        .in(ErpPurchasePriceAdjustItemDO::getAdjustId, reqVO.getSourceDocIds())
                        .ne(ErpPurchasePriceAdjustItemDO::getAdjustPrice, BigDecimal.ZERO)
                        .apply("COALESCE(t.count,0)>COALESCE((SELECT SUM(COALESCE(a.allocated_count,0)) "
                                + "FROM erp_purchase_invoice_source_allocation a WHERE a.deleted=b'0' "
                                + "AND a.tenant_id=t.tenant_id AND a.source_type=20 AND a.source_item_id=t.id),0)")
                        .orderByDesc(ErpPurchasePriceAdjustDO::getAdjustTime)
                        .orderByDesc(ErpPurchasePriceAdjustItemDO::getAdjustId)
                .orderByAsc(ErpPurchasePriceAdjustItemDO::getId);
        return selectJoinPage(reqVO, ErpPurchasePriceAdjustItemDO.class, query);
    }

    default List<ErpPurchasePriceAdjustItemDO> selectListByIdsForUpdate(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<ErpPurchasePriceAdjustItemDO>()
                .in(ErpPurchasePriceAdjustItemDO::getId, ids)
                .orderByAsc(ErpPurchasePriceAdjustItemDO::getId).last("FOR UPDATE"));
    }

    default Map<Long, Integer> selectItemCountMapByAdjustIds(Collection<Long> adjustIds) {
        if (CollUtil.isEmpty(adjustIds)) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = selectMaps(new QueryWrapper<ErpPurchasePriceAdjustItemDO>()
                .select("adjust_id, COUNT(1) AS item_count")
                .in("adjust_id", adjustIds)
                .groupBy("adjust_id"));
        Map<Long, Integer> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object adjustId = row.get("adjust_id");
            Object count = row.get("item_count");
            if (adjustId != null && count != null) {
                result.put(Long.valueOf(adjustId.toString()), Integer.valueOf(count.toString()));
            }
        }
        return result;
    }

    default void deleteByAdjustId(Long adjustId) {
        delete(ErpPurchasePriceAdjustItemDO::getAdjustId, adjustId);
    }

    default Long selectCountByWarehouseId(Long warehouseId) {
        return selectCount(ErpPurchasePriceAdjustItemDO::getWarehouseId, warehouseId);
    }

    static boolean isSupportedOrderField(String orderField) {
        return getOrderSpec(orderField) != null;
    }

    static ErpPurchaseItemSortUtils.OrderSpec getOrderSpec(String orderField) {
        if (orderField == null) {
            return null;
        }
        String item = "erp_purchase_price_adjust_item";
        return switch (orderField.trim()) {
            case "id" -> spec(item + ".id", false);
            case "inId" -> spec(item + ".in_id", false);
            case "inNo" -> spec(item + ".in_no", true);
            case "inItemId" -> spec(item + ".in_item_id", false);
            case "productId", "productName" -> spec("COALESCE(" + item + ".product_name, "
                    + ErpPurchaseItemSortUtils.productField(item, "name") + ")", true);
            case "productCode" -> spec("COALESCE(" + item + ".product_code, "
                    + ErpPurchaseItemSortUtils.productField(item, "code") + ")", true);
            case "batchNo" -> spec(item + ".batch_no", true);
            case "productUnitName" -> spec(item + ".product_unit_name", true);
            case "weight" -> spec(item + ".weight", false);
            case "packageQty" -> spec(item + ".package_qty", false);
            case "pieceCount" -> spec(ErpPurchaseItemSortUtils.pieceCount(item + ".count",
                    item + ".package_qty"), false);
            case "stockCount" -> spec(ErpPurchaseItemSortUtils.stockCount(item), false);
            case "warehouseId" -> spec(ErpPurchaseItemSortUtils.warehouseName(item), true);
            case "deptId" -> spec(ErpPurchaseItemSortUtils.deptName(item), true);
            case "oldPrice" -> spec(item + ".old_price", false);
            case "newPrice" -> spec(item + ".new_price", false);
            case "count" -> spec(item + ".count", false);
            case "adjustRatio" -> spec(item + ".adjust_ratio", false);
            case "adjustPrice" -> spec(item + ".adjust_price", false);
            case "vehicleModel" -> spec(item + ".vehicle_model", true);
            case "standard" -> spec(item + ".standard", true);
            case "featureCode" -> spec(item + ".feature_code", true);
            case "originPlace" -> spec(item + ".origin_place", true);
            case "brand" -> spec(item + ".brand", true);
            case "drawingNo" -> spec(item + ".drawing_no", true);
            case "warehousePosition" -> spec(item + ".warehouse_position", true);
            default -> null;
        };
    }

    private static ErpPurchaseItemSortUtils.OrderSpec spec(String expression, boolean text) {
        return new ErpPurchaseItemSortUtils.OrderSpec(expression, text);
    }

    /**
     * 查询指定入库项是否已被调过价（M2 审批流程用于校验"添加明细"方式）
     */
    default List<ErpPurchasePriceAdjustItemDO> selectListByInItemIds(Collection<Long> inItemIds) {
        return selectList(ErpPurchasePriceAdjustItemDO::getInItemId, inItemIds);
    }

}
