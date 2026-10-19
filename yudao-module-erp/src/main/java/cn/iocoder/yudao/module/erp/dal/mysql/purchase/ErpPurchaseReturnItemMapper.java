package cn.iocoder.yudao.module.erp.dal.mysql.purchase;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoiceSourceItemPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnDO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;

/**
 * ERP 采购退货项 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpPurchaseReturnItemMapper extends BaseMapperX<ErpPurchaseReturnItemDO> {

    default int updateNormalizedSources(Long returnId, ErpPurchaseReturnItemDO item) {
        return update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<ErpPurchaseReturnItemDO>()
                .eq(ErpPurchaseReturnItemDO::getId, item.getId()).eq(ErpPurchaseReturnItemDO::getReturnId, returnId)
                .set(ErpPurchaseReturnItemDO::getSourceInId, item.getSourceInId())
                .set(ErpPurchaseReturnItemDO::getSourceInItemId, item.getSourceInItemId())
                .set(ErpPurchaseReturnItemDO::getSourceInNo, item.getSourceInNo())
                .set(ErpPurchaseReturnItemDO::getSourceSaleReturnId, item.getSourceSaleReturnId())
                .set(ErpPurchaseReturnItemDO::getSourceSaleReturnItemId, item.getSourceSaleReturnItemId())
                .set(ErpPurchaseReturnItemDO::getSourceSaleReturnNo, item.getSourceSaleReturnNo()));
    }

    default List<ErpPurchaseReturnItemDO> selectListByReturnIdForUpdate(Long id) {
        return selectList(new cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX<ErpPurchaseReturnItemDO>()
                .eq(ErpPurchaseReturnItemDO::getReturnId, id).last("FOR UPDATE"));
    }

    default List<ErpPurchaseReturnItemDO> selectListByReturnId(Long returnId) {
        return selectList(buildSortedQuery(returnId, null, null, null));
    }

    default List<ErpPurchaseReturnItemDO> selectListByReturnId(Long returnId, String orderField,
                                                               String orderDirection) {
        return selectList(buildSortedQuery(returnId, null, orderField, orderDirection));
    }

    default PageResult<ErpPurchaseReturnItemDO> selectPageByReturnId(ErpPurchaseReturnItemPageReqVO reqVO) {
        return selectPage(reqVO, buildSortedQuery(reqVO.getReturnId(), reqVO.getExcludedItemIds(),
                reqVO.getOrderField(), reqVO.getOrderDirection()));
    }

    static QueryWrapper<ErpPurchaseReturnItemDO> buildSortedQuery(Long returnId, Collection<Long> excludedItemIds,
                                                                   String orderField, String orderDirection) {
        QueryWrapper<ErpPurchaseReturnItemDO> query = new QueryWrapper<ErpPurchaseReturnItemDO>()
                .eq("return_id", returnId)
                .notIn(CollUtil.isNotEmpty(excludedItemIds), "id", excludedItemIds);
        return ErpPurchaseItemSortUtils.applyOrder(query, "erp_purchase_return_items", orderField, orderDirection,
                getOrderSpec(orderField));
    }

    default List<ErpPurchaseReturnItemDO> selectListByReturnIds(Collection<Long> returnIds) {
        return selectList(ErpPurchaseReturnItemDO::getReturnId, returnIds);
    }

    default PageResult<ErpPurchaseReturnItemDO> selectInvoiceSourcePage(
            ErpPurchaseInvoiceSourceItemPageReqVO reqVO) {
        if (CollUtil.isEmpty(reqVO.getSourceDocIds())) {
            return PageResult.empty();
        }
        QueryWrapper<ErpPurchaseReturnItemDO> query = new QueryWrapper<ErpPurchaseReturnItemDO>()
                .in("return_id", reqVO.getSourceDocIds())
                .apply("COALESCE(count,0)>COALESCE((SELECT SUM(COALESCE(a.allocated_count,0)) "
                        + "FROM erp_purchase_invoice_source_allocation a WHERE a.deleted=b'0' "
                        + "AND a.tenant_id=erp_purchase_return_items.tenant_id AND a.source_type=30 "
                        + "AND a.source_item_id=erp_purchase_return_items.id),0)")
                .orderByAsc("id");
        return selectPage(reqVO, query);
    }

    default PageResult<ErpPurchaseReturnItemDO> selectInvoiceMixedSourcePage(
            ErpPurchaseInvoiceSourceItemPageReqVO reqVO) {
        if (CollUtil.isEmpty(reqVO.getSourceDocIds())) {
            return PageResult.empty();
        }
        MPJLambdaWrapperX<ErpPurchaseReturnItemDO> query = new MPJLambdaWrapperX<>();
        query.selectAll(ErpPurchaseReturnItemDO.class)
                .innerJoin(ErpPurchaseReturnDO.class, ErpPurchaseReturnDO::getId,
                        ErpPurchaseReturnItemDO::getReturnId)
                .in(ErpPurchaseReturnItemDO::getReturnId, reqVO.getSourceDocIds())
                .apply("COALESCE(t.count,0)>COALESCE((SELECT SUM(COALESCE(a.allocated_count,0)) "
                        + "FROM erp_purchase_invoice_source_allocation a WHERE a.deleted=b'0' "
                        + "AND a.tenant_id=t.tenant_id AND a.source_type=30 AND a.source_item_id=t.id),0)")
                .orderByDesc(ErpPurchaseReturnDO::getReturnTime)
                .orderByDesc(ErpPurchaseReturnItemDO::getReturnId)
                .orderByAsc(ErpPurchaseReturnItemDO::getId);
        return selectJoinPage(reqVO, ErpPurchaseReturnItemDO.class, query);
    }

    default List<ErpPurchaseReturnItemDO> selectListByIdsForUpdate(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<ErpPurchaseReturnItemDO>()
                .in(ErpPurchaseReturnItemDO::getId, ids)
                .orderByAsc(ErpPurchaseReturnItemDO::getId).last("FOR UPDATE"));
    }

    default Map<Long, Integer> selectItemCountMapByReturnIds(Collection<Long> returnIds) {
        if (CollUtil.isEmpty(returnIds)) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = selectMaps(new QueryWrapper<ErpPurchaseReturnItemDO>()
                .select("return_id, COUNT(1) AS item_count")
                .in("return_id", returnIds)
                .groupBy("return_id"));
        return convertMap(rows,
                row -> Long.valueOf(row.get("return_id").toString()),
                row -> Integer.valueOf(row.get("item_count").toString()));
    }

    default int deleteByReturnId(Long returnId) {
        return delete(ErpPurchaseReturnItemDO::getReturnId, returnId);
    }

    default Long selectCountByProductId(Long productId) {
        return selectCount(ErpPurchaseReturnItemDO::getProductId, productId);
    }

    default Long selectCountByWarehouseId(Long warehouseId) {
        return selectCount(ErpPurchaseReturnItemDO::getWarehouseId, warehouseId);
    }

    default Long selectCountByOrderItemIds(Collection<Long> orderItemIds) {
        if (CollUtil.isEmpty(orderItemIds)) {
            return 0L;
        }
        return selectCount(new QueryWrapper<ErpPurchaseReturnItemDO>().in("order_item_id", orderItemIds));
    }

    static boolean isSupportedOrderField(String orderField) {
        return getOrderSpec(orderField) != null;
    }

    static ErpPurchaseItemSortUtils.OrderSpec getOrderSpec(String orderField) {
        if (orderField == null) {
            return null;
        }
        String item = "erp_purchase_return_items";
        return switch (orderField.trim()) {
            case "id" -> spec(item + ".id", false);
            case "productId", "productName" -> spec(ErpPurchaseItemSortUtils.productField(item, "name"), true);
            case "productCode" -> spec(ErpPurchaseItemSortUtils.productField(item, "code"), true);
            case "productBarCode" -> spec(ErpPurchaseItemSortUtils.productField(item, "bar_code"), true);
            case "productUnitName" -> spec(ErpPurchaseItemSortUtils.itemUnitName(item), true);
            case "lastPurchasePrice" -> spec(ErpPurchaseItemSortUtils.productField(item, "last_purchase_price"), false);
            case "warehouseId" -> spec(ErpPurchaseItemSortUtils.warehouseName(item), true);
            case "deptId" -> spec(ErpPurchaseItemSortUtils.deptName(item), true);
            case "stockCount" -> spec(ErpPurchaseItemSortUtils.stockCount(item), false);
            case "count" -> spec(item + ".count", false);
            case "productPrice" -> spec(item + ".product_price", false);
            case "totalProductPrice" -> spec(ErpPurchaseItemSortUtils.productAmount(item), false);
            case "taxPercent" -> spec(item + ".tax_percent", false);
            case "taxPrice" -> spec(item + ".tax_price", false);
            case "totalPrice" -> spec(item + ".total_price", false);
            case "batchNo" -> spec(item + ".batch_no", true);
            case "sourceInNo" -> spec(item + ".source_in_no", true);
            case "inCount" -> spec("(SELECT pii.count FROM erp_purchase_in_items pii WHERE pii.id = "
                    + item + ".source_in_item_id AND pii.tenant_id = " + item
                    + ".tenant_id AND pii.deleted = 0 LIMIT 1)", false);
            case "returnableCount" -> spec("((SELECT pii.count FROM erp_purchase_in_items pii WHERE pii.id = "
                    + item + ".source_in_item_id AND pii.tenant_id = " + item
                    + ".tenant_id AND pii.deleted = 0 LIMIT 1) - "
                    + ErpPurchaseItemSortUtils.returnedCount(item, item + ".source_in_item_id") + ")", false);
            case "partCode" -> spec(item + ".part_code", true);
            case "partName" -> spec(item + ".part_name", true);
            case "vehicleModel" -> spec(item + ".vehicle_model", true);
            case "originPlace" -> spec(item + ".origin_place", true);
            case "weight" -> spec(item + ".weight", false);
            case "packageQty" -> spec(item + ".package_qty", false);
            case "pieceCount" -> spec(ErpPurchaseItemSortUtils.pieceCount(item + ".count",
                    item + ".package_qty"), false);
            case "wholeQty" -> spec(item + ".whole_qty", false);
            case "warehousePosition" -> spec(item + ".warehouse_position", true);
            case "drawingNo" -> spec(item + ".drawing_no", true);
            case "brand" -> spec(item + ".brand", true);
            case "remark" -> spec(item + ".remark", true);
            default -> null;
        };
    }

    private static ErpPurchaseItemSortUtils.OrderSpec spec(String expression, boolean text) {
        return new ErpPurchaseItemSortUtils.OrderSpec(expression, text);
    }

    /**
     * 基于采购订单编号，查询每个采购订单项的退货数量之和
     *
     * @param returnIds 入库订单项编号数组
     * @return key：采购订单项编号；value：退货数量之和
     */
    default Map<Long, BigDecimal> selectOrderItemCountSumMapByReturnIds(Collection<Long> returnIds) {
        if (CollUtil.isEmpty(returnIds)) {
            return Collections.emptyMap();
        }
        // SQL sum 查询
        List<Map<String, Object>> result = selectMaps(new QueryWrapper<ErpPurchaseReturnItemDO>()
                .select("order_item_id, SUM(count) AS sum_count")
                .groupBy("order_item_id")
                .in("return_id", returnIds));
        // 获得数量
        return convertMap(result, obj -> (Long) obj.get("order_item_id"), obj -> (BigDecimal) obj.get("sum_count"));
    }

    /**
     * 按 source_in_item_id 分组，查询每个原入库项已累计退货的数量（仅统计已审批单据）
     *
     * @param sourceInItemIds 原入库项 ID 集合
     * @return Map&lt;sourceInItemId, 已退数量合计&gt;
     */
    default Map<Long, BigDecimal> selectReturnedCountMapBySourceInItemIds(Collection<Long> sourceInItemIds) {
        if (CollUtil.isEmpty(sourceInItemIds)) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> result = selectApprovedReturnedCountListBySourceInItemIds(
                sourceInItemIds, null, ErpAuditStatus.APPROVE.getStatus());
        return convertMap(result,
                obj -> (Long) obj.get("source_in_item_id"),
                obj -> (BigDecimal) obj.get("sum_count"));
    }

    /**
     * 按 source_in_item_id 分组，查询其他退货单（排除 excludeReturnId）的累计退货数量
     *
     * @param sourceInItemIds 原入库项 ID 集合
     * @param excludeReturnId 要排除的当前退货单 ID（新建场景传 null）
     * @return Map&lt;sourceInItemId, 其他退货单累计退货数量&gt;
     */
    default Map<Long, BigDecimal> selectReturnedCountMapBySourceInItemIdsExcludeReturn(
            Collection<Long> sourceInItemIds, Long excludeReturnId) {
        if (CollUtil.isEmpty(sourceInItemIds)) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> result = selectApprovedReturnedCountListBySourceInItemIds(
                sourceInItemIds, excludeReturnId, ErpAuditStatus.APPROVE.getStatus());
        return convertMap(result,
                obj -> (Long) obj.get("source_in_item_id"),
                obj -> (BigDecimal) obj.get("sum_count"));
    }

    /**
     * 按 source_in_item_id 分组，查询指定状态退货单的累计退货数量。
     */
    default Map<Long, BigDecimal> selectReturnedCountMapBySourceInItemIdsExcludeReturnAndStatuses(
            Collection<Long> sourceInItemIds, Long excludeReturnId, Collection<Integer> statuses) {
        if (CollUtil.isEmpty(sourceInItemIds) || CollUtil.isEmpty(statuses)) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> result = selectReturnedCountListBySourceInItemIdsExcludeReturnAndStatuses(
                sourceInItemIds, excludeReturnId, statuses);
        return convertMap(result,
                obj -> (Long) obj.get("source_in_item_id"),
                obj -> (BigDecimal) obj.get("sum_count"));
    }

    default Map<Long, BigDecimal> selectProcessingAndApprovedReturnedCountMapBySourceInItemIdsExcludeReturn(
            Collection<Long> sourceInItemIds, Long excludeReturnId) {
        return selectReturnedCountMapBySourceInItemIdsExcludeReturnAndStatuses(sourceInItemIds, excludeReturnId,
                Arrays.asList(ErpAuditStatus.PROCESS.getStatus(), ErpAuditStatus.APPROVE.getStatus()));
    }

    default Map<Long, BigDecimal> selectReturnedCountMapBySourceSaleReturnItemIds(
            Collection<Long> sourceSaleReturnItemIds, Long excludeReturnId) {
        if (CollUtil.isEmpty(sourceSaleReturnItemIds)) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> result = selectReturnedCountListBySourceSaleReturnItemIds(
                sourceSaleReturnItemIds, excludeReturnId);
        return convertMap(result,
                obj -> (Long) obj.get("source_sale_return_item_id"),
                obj -> (BigDecimal) obj.get("sum_count"));
    }

    @Select({
            "<script>",
            "SELECT pri.source_in_item_id, SUM(pri.count) AS sum_count",
            "  FROM erp_purchase_return_items pri",
            " INNER JOIN erp_purchase_return pr ON pr.id = pri.return_id",
            "   AND pr.deleted = 0 AND pr.status = #{status}",
            " WHERE pri.deleted = 0",
            "   AND pri.source_in_item_id IN",
            " <foreach collection='sourceInItemIds' item='sourceInItemId' open='(' separator=',' close=')'>",
            "   #{sourceInItemId}",
            " </foreach>",
            " <if test='excludeReturnId != null'>",
            "   AND pri.return_id != #{excludeReturnId}",
            " </if>",
            " GROUP BY pri.source_in_item_id",
            "</script>"
    })
    List<Map<String, Object>> selectApprovedReturnedCountListBySourceInItemIds(
            @Param("sourceInItemIds") Collection<Long> sourceInItemIds,
            @Param("excludeReturnId") Long excludeReturnId,
            @Param("status") Integer status);

    @Select({
            "<script>",
            "SELECT pri.source_in_item_id, SUM(pri.count) AS sum_count",
            "  FROM erp_purchase_return_items pri",
            " INNER JOIN erp_purchase_return pr ON pr.id = pri.return_id",
            "   AND pr.deleted = 0 AND pr.status IN",
            " <foreach collection='statuses' item='status' open='(' separator=',' close=')'>",
            "   #{status}",
            " </foreach>",
            " WHERE pri.deleted = 0",
            "   AND pri.source_in_item_id IN",
            " <foreach collection='sourceInItemIds' item='sourceInItemId' open='(' separator=',' close=')'>",
            "   #{sourceInItemId}",
            " </foreach>",
            " <if test='excludeReturnId != null'>",
            "   AND pri.return_id != #{excludeReturnId}",
            " </if>",
            " GROUP BY pri.source_in_item_id",
            "</script>"
    })
    List<Map<String, Object>> selectReturnedCountListBySourceInItemIdsExcludeReturnAndStatuses(
            @Param("sourceInItemIds") Collection<Long> sourceInItemIds,
            @Param("excludeReturnId") Long excludeReturnId,
            @Param("statuses") Collection<Integer> statuses);

    @Select({
            "<script>",
            "SELECT pri.source_sale_return_item_id, SUM(pri.count) AS sum_count",
            "  FROM erp_purchase_return_items pri",
            " INNER JOIN erp_purchase_return pr ON pr.id = pri.return_id",
            "   AND pr.deleted = 0",
            " WHERE pri.deleted = 0",
            "   AND pri.source_sale_return_item_id IN",
            " <foreach collection='sourceSaleReturnItemIds' item='sourceSaleReturnItemId' open='(' separator=',' close=')'>",
            "   #{sourceSaleReturnItemId}",
            " </foreach>",
            " <if test='excludeReturnId != null'>",
            "   AND pri.return_id != #{excludeReturnId}",
            " </if>",
            " GROUP BY pri.source_sale_return_item_id",
            "</script>"
    })
    List<Map<String, Object>> selectReturnedCountListBySourceSaleReturnItemIds(
            @Param("sourceSaleReturnItemIds") Collection<Long> sourceSaleReturnItemIds,
            @Param("excludeReturnId") Long excludeReturnId);

}
