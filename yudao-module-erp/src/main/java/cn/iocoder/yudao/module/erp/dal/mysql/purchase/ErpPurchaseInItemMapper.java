package cn.iocoder.yudao.module.erp.dal.mysql.purchase;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInAdjustableItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInReturnableItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInSaleCartableItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInTransferOutableItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoiceSourceInItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoiceSourceItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockPendingInDetailRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleBizSourceTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleConvertTypeEnum;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
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

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;

/**
 * ERP 采购入库项 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpPurchaseInItemMapper extends BaseMapperX<ErpPurchaseInItemDO> {

    String PURCHASE_ITEM_PRODUCT_KEYWORD_SQL = "("
            + "EXISTS (SELECT 1 FROM erp_product p "
            + "WHERE p.id = erp_purchase_in_items.product_id AND p.deleted = b'0' "
            + "AND p.tenant_id = erp_purchase_in_items.tenant_id "
            + "AND (p.code LIKE {0} OR p.name LIKE {0} OR p.pinyin_code LIKE {0} "
            + "OR p.wubi_code LIKE {0} OR p.bar_code LIKE {0} OR p.vehicle_model LIKE {0} "
            + "OR p.factory_code LIKE {0} OR p.standard LIKE {0} OR p.brand LIKE {0} "
            + "OR p.drawing_no LIKE {0})) "
            + "OR erp_purchase_in_items.bar_code LIKE {0} "
            + "OR erp_purchase_in_items.vehicle_model LIKE {0} "
            + "OR erp_purchase_in_items.brand LIKE {0} "
            + "OR erp_purchase_in_items.drawing_no LIKE {0} "
            + "OR erp_purchase_in_items.batch_no LIKE {0})";

    default List<ErpPurchaseInItemDO> selectListByInIdForUpdate(Long inId) {
        return selectList(new LambdaQueryWrapperX<ErpPurchaseInItemDO>()
                .eq(ErpPurchaseInItemDO::getInId,inId).last("FOR UPDATE"));
    }

    default List<ErpPurchaseInItemDO> selectListByInId(Long inId) {
        return selectList(buildSortedQuery(inId, null, null, null));
    }

    default List<ErpPurchaseInItemDO> selectListByInId(Long inId, String orderField, String orderDirection) {
        return selectList(buildSortedQuery(inId, null, orderField, orderDirection));
    }

    default PageResult<ErpPurchaseInItemDO> selectPageByInId(ErpPurchaseInItemPageReqVO reqVO) {
        return selectPage(reqVO, buildSortedQuery(reqVO.getInId(), reqVO.getExcludedItemIds(),
                reqVO.getOrderField(), reqVO.getOrderDirection()));
    }

    static QueryWrapper<ErpPurchaseInItemDO> buildSortedQuery(Long inId, Collection<Long> excludedItemIds,
                                                               String orderField, String orderDirection) {
        QueryWrapper<ErpPurchaseInItemDO> query = new QueryWrapper<ErpPurchaseInItemDO>()
                .eq("in_id", inId)
                .notIn(CollUtil.isNotEmpty(excludedItemIds), "id", excludedItemIds);
        return ErpPurchaseItemSortUtils.applyOrder(query, "erp_purchase_in_items", orderField, orderDirection,
                getOrderSpec(orderField));
    }

    default PageResult<ErpPurchaseInItemDO> selectPageByInIds(ErpPurchaseInvoiceSourceInItemPageReqVO reqVO) {
        if (CollUtil.isEmpty(reqVO.getSourceInIds())) {
            return PageResult.empty();
        }
        QueryWrapper<ErpPurchaseInItemDO> query = new QueryWrapper<ErpPurchaseInItemDO>()
                .in("in_id", reqVO.getSourceInIds());
        ErpPurchaseItemSortUtils.applyOrder(query, "erp_purchase_in_items", reqVO.getOrderField(),
                reqVO.getOrderDirection(), getOrderSpec(reqVO.getOrderField()));
        return selectPage(reqVO, query);
    }

    default PageResult<ErpPurchaseInItemDO> selectInvoiceSourcePage(ErpPurchaseInvoiceSourceItemPageReqVO reqVO) {
        if (CollUtil.isEmpty(reqVO.getSourceDocIds())) {
            return PageResult.empty();
        }
        QueryWrapper<ErpPurchaseInItemDO> query = new QueryWrapper<ErpPurchaseInItemDO>()
                .in("in_id", reqVO.getSourceDocIds())
                .apply("COALESCE(count,0)>COALESCE((SELECT SUM(COALESCE(a.allocated_count,0)) "
                        + "FROM erp_purchase_invoice_source_allocation a WHERE a.deleted=b'0' "
                        + "AND a.tenant_id=erp_purchase_in_items.tenant_id AND a.source_type=10 "
                        + "AND a.source_item_id=erp_purchase_in_items.id),0)")
                .orderByAsc("id");
        return selectPage(reqVO, query);
    }

    default PageResult<ErpPurchaseInItemDO> selectInvoiceMixedSourcePage(
            ErpPurchaseInvoiceSourceItemPageReqVO reqVO) {
        if (CollUtil.isEmpty(reqVO.getSourceDocIds())) {
            return PageResult.empty();
        }
        MPJLambdaWrapperX<ErpPurchaseInItemDO> query = new MPJLambdaWrapperX<>();
        query.selectAll(ErpPurchaseInItemDO.class)
                .innerJoin(ErpPurchaseInDO.class, ErpPurchaseInDO::getId, ErpPurchaseInItemDO::getInId)
                .in(ErpPurchaseInItemDO::getInId, reqVO.getSourceDocIds())
                .apply("COALESCE(t.count,0)>COALESCE((SELECT SUM(COALESCE(a.allocated_count,0)) "
                        + "FROM erp_purchase_invoice_source_allocation a WHERE a.deleted=b'0' "
                        + "AND a.tenant_id=t.tenant_id AND a.source_type=10 AND a.source_item_id=t.id),0)")
                .orderByDesc(ErpPurchaseInDO::getInTime)
                .orderByDesc(ErpPurchaseInItemDO::getInId)
                .orderByAsc(ErpPurchaseInItemDO::getId);
        return selectJoinPage(reqVO, ErpPurchaseInItemDO.class, query);
    }

    default List<ErpPurchaseInItemDO> selectListByIdsForUpdate(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<ErpPurchaseInItemDO>()
                .in(ErpPurchaseInItemDO::getId, ids).orderByAsc(ErpPurchaseInItemDO::getId).last("FOR UPDATE"));
    }

    default PageResult<ErpPurchaseInItemDO> selectSaleCartablePageByInId(
            ErpPurchaseInSaleCartableItemPageReqVO reqVO) {
        QueryWrapper<ErpPurchaseInItemDO> query = new QueryWrapper<ErpPurchaseInItemDO>()
                .eq("in_id", reqVO.getInId())
                .apply("COALESCE(count, 0) > COALESCE((SELECT SUM(COALESCE(r.count, 0)) "
                        + "FROM erp_sale_convert_record r "
                        + "INNER JOIN erp_sale_cart c ON c.id = r.target_id AND c.deleted = 0 "
                        + "WHERE r.deleted = 0 "
                        + "AND r.convert_type = {0} "
                        + "AND r.source_type = {1} "
                        + "AND r.target_type = {2} "
                        + "AND r.source_item_id = erp_purchase_in_items.id), 0)",
                        ErpSaleConvertTypeEnum.PURCHASE_IN_TO_CART.getType(),
                        ErpSaleBizSourceTypeEnum.PURCHASE_IN.getType(),
                        ErpSaleBizSourceTypeEnum.CART.getType());
        String orderColumn = getSaleCartableOrderColumn(reqVO.getOrderField());
        if (orderColumn == null) {
            query.orderByAsc("id");
        } else if ("desc".equalsIgnoreCase(reqVO.getOrderDirection())) {
            query.orderByDesc(orderColumn);
        } else {
            query.orderByAsc(orderColumn);
        }
        if (orderColumn != null && !"id".equals(reqVO.getOrderField().trim())) {
            query.orderByAsc("id");
        }
        return selectPage(reqVO, query);
    }

    default PageResult<ErpPurchaseInItemDO> selectAdjustableItemPage(
            ErpPurchaseInAdjustableItemPageReqVO reqVO) {
        if (reqVO.getSupplierId() == null) {
            return PageResult.empty();
        }
        QueryWrapper<ErpPurchaseInItemDO> query = new QueryWrapper<ErpPurchaseInItemDO>()
                .apply("EXISTS (SELECT 1 FROM erp_purchase_in pi "
                                + "WHERE pi.id = erp_purchase_in_items.in_id AND pi.deleted = b'0' "
                                + "AND pi.tenant_id = erp_purchase_in_items.tenant_id "
                                + "AND pi.supplier_id = {0} AND pi.status = {1})",
                        reqVO.getSupplierId(), ErpAuditStatus.APPROVE.getStatus());
        if (CollUtil.isNotEmpty(reqVO.getInIds())) {
            query.in("in_id", reqVO.getInIds());
        }
        if (Boolean.TRUE.equals(reqVO.getExcludeAdjusted())) {
            query.and(wrapper -> wrapper.isNull("adjusted").or().eq("adjusted", false));
        }
        if (Boolean.TRUE.equals(reqVO.getExcludeInvoiced())) {
            query.apply("NOT EXISTS (SELECT 1 FROM erp_purchase_invoice_item pii "
                    + "INNER JOIN erp_purchase_invoice pio ON pio.id = pii.invoice_id "
                    + "AND pio.deleted = b'0' AND pio.tenant_id = pii.tenant_id "
                    + "AND pio.status = {0} "
                    + "WHERE pii.deleted = b'0' AND pii.tenant_id = erp_purchase_in_items.tenant_id "
                    + "AND pii.source_in_id = erp_purchase_in_items.in_id)", ErpAuditStatus.APPROVE.getStatus());
        }
        appendProductKeyword(query, reqVO.getProductKeyword());
        query.orderByDesc("in_id").orderByAsc("id");
        return selectPage(reqVO, query);
    }

    default PageResult<ErpPurchaseInItemDO> selectReturnableItemPage(
            ErpPurchaseInReturnableItemPageReqVO reqVO) {
        QueryWrapper<ErpPurchaseInItemDO> query = new QueryWrapper<ErpPurchaseInItemDO>()
                .eq("in_id", reqVO.getInId())
                .apply("COALESCE(count, 0) > COALESCE((SELECT SUM(COALESCE(pri.count, 0)) "
                                + "FROM erp_purchase_return_items pri "
                                + "INNER JOIN erp_purchase_return pr ON pr.id = pri.return_id "
                                + "AND pr.deleted = 0 AND pr.tenant_id = pri.tenant_id "
                                + "AND pr.status IN ({0}, {1}) "
                                + "WHERE pri.deleted = 0 "
                                + "AND pri.tenant_id = erp_purchase_in_items.tenant_id "
                                + "AND pri.source_in_item_id = erp_purchase_in_items.id), 0)",
                        ErpAuditStatus.PROCESS.getStatus(), ErpAuditStatus.APPROVE.getStatus());
        appendProductKeyword(query, reqVO.getProductKeyword());
        query.orderByAsc("id");
        return selectPage(reqVO, query);
    }

    default PageResult<ErpPurchaseInItemDO> selectTransferOutableItemPage(
            ErpPurchaseInTransferOutableItemPageReqVO reqVO) {
        QueryWrapper<ErpPurchaseInItemDO> query = new QueryWrapper<ErpPurchaseInItemDO>()
                .eq("in_id", reqVO.getInId())
                .apply("COALESCE(count, 0) > COALESCE((SELECT SUM(COALESCE(smi.count, 0)) "
                        + "FROM erp_stock_move_item smi "
                        + "INNER JOIN erp_stock_move sm ON sm.id = smi.move_id "
                        + "AND sm.deleted = b'0' AND sm.tenant_id = smi.tenant_id "
                        + "AND (sm.transfer_direction = 10 OR sm.transfer_direction IS NULL) "
                        + "WHERE smi.deleted = b'0' "
                        + "AND smi.tenant_id = erp_purchase_in_items.tenant_id "
                        + "AND smi.source_in_item_id = erp_purchase_in_items.id), 0)");
        appendProductKeyword(query, reqVO.getProductKeyword());
        query.orderByAsc("id");
        return selectPage(reqVO, query);
    }

    default List<ErpPurchaseInItemDO> selectListByInIds(Collection<Long> inIds) {
        return selectList(ErpPurchaseInItemDO::getInId, inIds);
    }

    default List<ErpPurchaseInItemDO> selectLightListByInIds(Collection<Long> inIds) {
        if (CollUtil.isEmpty(inIds)) {
            return Collections.emptyList();
        }
        return selectList(new QueryWrapper<ErpPurchaseInItemDO>()
                .select("id", "in_id", "count", "adjusted")
                .in("in_id", inIds));
    }

    default int deleteByInId(Long inId) {
        return delete(ErpPurchaseInItemDO::getInId, inId);
    }

    default Long selectCountByProductId(Long productId) {
        return selectCount(ErpPurchaseInItemDO::getProductId, productId);
    }

    default Long selectCountByWarehouseId(Long warehouseId) {
        return selectCount(ErpPurchaseInItemDO::getWarehouseId, warehouseId);
    }

    default Long selectCountByOrderItemIds(Collection<Long> orderItemIds) {
        if (CollUtil.isEmpty(orderItemIds)) {
            return 0L;
        }
        return selectCount(new QueryWrapper<ErpPurchaseInItemDO>().in("order_item_id", orderItemIds));
    }

    static boolean isSupportedOrderField(String orderField) {
        return getOrderSpec(orderField) != null;
    }

    static ErpPurchaseItemSortUtils.OrderSpec getOrderSpec(String orderField) {
        if (orderField == null) {
            return null;
        }
        String item = "erp_purchase_in_items";
        return switch (orderField.trim()) {
            case "id" -> spec(item + ".id", false);
            case "sourceInId" -> spec(item + ".in_id", false);
            case "gift" -> spec(item + ".gift", false);
            case "productId", "productName" -> spec(ErpPurchaseItemSortUtils.productField(item, "name"), true);
            case "productCode" -> spec(ErpPurchaseItemSortUtils.productField(item, "code"), true);
            case "productBarCode" -> spec(ErpPurchaseItemSortUtils.productField(item, "bar_code"), true);
            case "productUnitName" -> spec(ErpPurchaseItemSortUtils.itemUnitName(item), true);
            case "weight" -> spec(ErpPurchaseItemSortUtils.productField(item, "weight"), false);
            case "lastPurchasePrice" -> spec(ErpPurchaseItemSortUtils.productField(item, "last_purchase_price"), false);
            case "warehouseId" -> spec(ErpPurchaseItemSortUtils.warehouseName(item), true);
            case "deptId" -> spec(ErpPurchaseItemSortUtils.deptName(item), true);
            case "stockCount" -> spec(ErpPurchaseItemSortUtils.stockCount(item), false);
            case "count" -> spec(item + ".count", false);
            case "productPrice" -> spec(item + ".product_price", false);
            case "totalProductPrice", "productCostAmount" -> spec(ErpPurchaseItemSortUtils.productAmount(item), false);
            case "productCostPrice" -> spec(item + ".product_price", false);
            case "taxPercent" -> spec(item + ".tax_percent", false);
            case "taxPrice" -> spec(item + ".tax_price", false);
            case "totalPrice" -> spec(item + ".total_price", false);
            case "packageQty" -> spec(item + ".package_qty", false);
            case "pieceCount" -> spec(ErpPurchaseItemSortUtils.pieceCount(item + ".count",
                    item + ".package_qty"), false);
            case "wholeQty" -> spec(item + ".whole_qty", false);
            case "vehicleModel" -> spec(item + ".vehicle_model", true);
            case "originPlace" -> spec(item + ".origin_place", true);
            case "warehousePosition" -> spec(item + ".warehouse_position", true);
            case "drawingNo" -> spec(item + ".drawing_no", true);
            case "batchNo" -> spec(item + ".batch_no", true);
            case "barCode" -> spec(item + ".bar_code", true);
            case "brand" -> spec(item + ".brand", true);
            case "remark" -> spec(item + ".remark", true);
            case "adjustStatus" -> spec("(CASE WHEN " + item + ".adjusted = 1 THEN 1 ELSE 0 END)", false);
            case "returnCount" -> spec(ErpPurchaseItemSortUtils.returnedCount(item, item + ".id"), false);
            case "returnStatus" -> {
                String returned = ErpPurchaseItemSortUtils.returnedCount(item, item + ".id");
                yield spec("(CASE WHEN " + returned + " <= 0 THEN 0 WHEN " + returned + " >= COALESCE("
                        + item + ".count, 0) THEN 2 ELSE 1 END)", false);
            }
            case "invoiceStatus" -> spec("(SELECT CASE WHEN COALESCE(SUM(a.allocated_count), 0) <= 0 THEN 0"
                    + " WHEN COALESCE(SUM(a.allocated_count), 0) >= COALESCE(" + item + ".count, 0) THEN 2 ELSE 1 END"
                    + " FROM erp_purchase_invoice_source_allocation a WHERE a.source_type = 10"
                    + " AND a.source_item_id = " + item + ".id AND a.tenant_id = " + item
                    + ".tenant_id AND a.deleted = 0)", false);
            case "stockInBillStatus" -> spec("(SELECT COALESCE(MAX(sb.status), 0) FROM erp_stock_in_bill sb"
                    + " WHERE sb.source_biz_type = 10 AND sb.source_id = " + item + ".in_id AND sb.tenant_id = "
                    + item + ".tenant_id AND sb.deleted = 0)", false);
            default -> null;
        };
    }

    private static ErpPurchaseItemSortUtils.OrderSpec spec(String expression, boolean text) {
        return new ErpPurchaseItemSortUtils.OrderSpec(expression, text);
    }

    static void appendProductKeyword(QueryWrapper<ErpPurchaseInItemDO> query, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return;
        }
        String likeValue = "%" + keyword.trim().replaceAll("\\s+", "%") + "%";
        query.and(wrapper -> wrapper.apply(PURCHASE_ITEM_PRODUCT_KEYWORD_SQL, likeValue));
    }

    static String getSaleCartableOrderColumn(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "id":
                return "id";
            case "productCode":
            case "productName":
                return "product_id";
            case "warehouseId":
                return "warehouse_id";
            case "count":
            case "inCount":
            case "saleCartableCount":
                return "count";
            case "productPrice":
            case "purchasePrice":
                return "product_price";
            case "warehousePosition":
                return "warehouse_position";
            case "batchNo":
                return "batch_no";
            case "brand":
                return "brand";
            case "remark":
                return "remark";
            default:
                return null;
        }
    }

    /**
     * 基于采购订单编号，查询每个采购订单项的入库数量之和
     *
     * @param inIds 入库订单项编号数组
     * @return key：采购订单项编号；value：入库数量之和
     */
    default Map<Long, BigDecimal> selectOrderItemCountSumMapByInIds(Collection<Long> inIds) {
        if (CollUtil.isEmpty(inIds)) {
            return Collections.emptyMap();
        }
        // SQL sum 查询
        List<Map<String, Object>> result = selectMaps(new QueryWrapper<ErpPurchaseInItemDO>()
                .select("order_item_id, SUM(count) AS sum_count")
                .groupBy("order_item_id")
                .in("in_id", inIds));
        // 获得数量
        return convertMap(result, obj -> (Long) obj.get("order_item_id"), obj -> (BigDecimal) obj.get("sum_count"));
    }

    /**
     * 批量汇总存在原始价格快照的采购入库商品金额。
     *
     * <p>仅返回至少一条明细存在 original_product_price 的入库单；未返回的入库单继续使用主单 total_price。
     * 这样付款候选分页无需把全部入库明细加载到 JVM。</p>
     */
    default Map<Long, BigDecimal> selectOriginalSettlementProductPriceMap(Collection<Long> inIds) {
        if (CollUtil.isEmpty(inIds)) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = selectMaps(new QueryWrapper<ErpPurchaseInItemDO>()
                .select("in_id AS in_id",
                        "SUM(ROUND(COALESCE(original_product_price, product_price) * count, 2)) AS product_price")
                .in("in_id", inIds)
                .groupBy("in_id")
                .having("SUM(CASE WHEN original_product_price IS NOT NULL THEN 1 ELSE 0 END) > 0"));
        return convertMap(rows, row -> ((Number) row.get("in_id")).longValue(),
                row -> row.get("product_price") instanceof BigDecimal
                        ? (BigDecimal) row.get("product_price")
                        : new BigDecimal(row.get("product_price").toString()));
    }

    default Map<String, BigDecimal> selectPendingInCountMap(Collection<Long> productIds,
                                                           Collection<Long> warehouseIds,
                                                           Collection<Integer> statuses) {
        if (CollUtil.isEmpty(productIds) || CollUtil.isEmpty(warehouseIds) || CollUtil.isEmpty(statuses)) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = selectPendingInCountRows(productIds, warehouseIds, statuses);
        Map<String, BigDecimal> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object productId = row.get("product_id");
            Object warehouseId = row.get("warehouse_id");
            Object value = row.get("pending");
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
            "SELECT pii.product_id, pii.warehouse_id, SUM(COALESCE(pii.count, 0)) AS pending",
            "  FROM erp_purchase_in_items pii",
            " INNER JOIN erp_purchase_in pi ON pi.id = pii.in_id",
            "   AND pi.deleted = 0",
            "   AND pi.status IN",
            "   <foreach collection='statuses' item='status' open='(' separator=',' close=')'>",
            "     #{status}",
            "   </foreach>",
            " WHERE pii.deleted = 0",
            "   AND pii.product_id IN",
            "   <foreach collection='productIds' item='productId' open='(' separator=',' close=')'>",
            "     #{productId}",
            "   </foreach>",
            "   AND pii.warehouse_id IN",
            "   <foreach collection='warehouseIds' item='warehouseId' open='(' separator=',' close=')'>",
            "     #{warehouseId}",
            "   </foreach>",
            " GROUP BY pii.product_id, pii.warehouse_id",
            "</script>"
    })
    List<Map<String, Object>> selectPendingInCountRows(@Param("productIds") Collection<Long> productIds,
                                                       @Param("warehouseIds") Collection<Long> warehouseIds,
                                                       @Param("statuses") Collection<Integer> statuses);

    default List<ErpStockPendingInDetailRespVO> selectPendingInDetails(Long productId,
                                                                       Long warehouseId,
                                                                       Collection<Integer> statuses) {
        if (productId == null || warehouseId == null || CollUtil.isEmpty(statuses)) {
            return Collections.emptyList();
        }
        return selectPendingInDetailRows(productId, warehouseId, statuses);
    }

    @Select({
            "<script>",
            "SELECT pi.id AS inId,",
            "       pii.id AS itemId,",
            "       pi.no AS no,",
            "       pii.product_id AS productId,",
            "       p.code AS productCode,",
            "       p.name AS productName,",
            "       pii.warehouse_id AS warehouseId,",
            "       w.name AS warehouseName,",
            "       COALESCE(pii.count, 0) AS count,",
            "       COALESCE(pii.vehicle_model, p.vehicle_model) AS vehicleModel,",
            "       COALESCE(pii.origin_place, p.origin_place) AS originPlace,",
            "       COALESCE(pii.drawing_no, p.drawing_no) AS drawingNo,",
            "       p.standard AS standard,",
            "       pi.create_time AS createTime,",
            "       pi.creator AS creator",
            "  FROM erp_purchase_in_items pii",
            " INNER JOIN erp_purchase_in pi ON pi.id = pii.in_id",
            "   AND pi.deleted = 0",
            "   AND pi.status IN",
            "   <foreach collection='statuses' item='status' open='(' separator=',' close=')'>",
            "     #{status}",
            "   </foreach>",
            "  LEFT JOIN erp_product p ON p.id = pii.product_id AND p.deleted = 0",
            "  LEFT JOIN erp_warehouse w ON w.id = pii.warehouse_id AND w.deleted = 0",
            " WHERE pii.deleted = 0",
            "   AND pii.product_id = #{productId}",
            "   AND pii.warehouse_id = #{warehouseId}",
            " ORDER BY pi.create_time DESC, pi.id DESC, pii.id DESC",
            "</script>"
    })
    List<ErpStockPendingInDetailRespVO> selectPendingInDetailRows(@Param("productId") Long productId,
                                                                  @Param("warehouseId") Long warehouseId,
                                                                  @Param("statuses") Collection<Integer> statuses);

    @Select({
            "<script>",
            "SELECT t.batch_no,",
            "       t.in_count - COALESCE(o.out_count, 0) AS available_count,",
            "       t.first_in_time",
            "  FROM (",
            "        SELECT pii.batch_no,",
            "               SUM(pii.count) AS in_count,",
            "               MIN(pi.in_time) AS first_in_time",
            "          FROM erp_purchase_in_items pii",
            "         INNER JOIN erp_purchase_in pi ON pi.id = pii.in_id",
            "           AND pi.deleted = 0",
            "           AND pi.status = #{approveStatus}",
            "         WHERE pii.deleted = 0",
            "           AND pii.product_id = #{productId}",
            "           AND pii.warehouse_id = #{warehouseId}",
            "           AND pii.batch_no IS NOT NULL",
            "           AND pii.batch_no != ''",
            "         GROUP BY pii.batch_no",
            "       ) t",
            "  LEFT JOIN (",
            "        SELECT soi.batch_no,",
            "               SUM(soi.count) AS out_count",
            "          FROM erp_sale_out_items soi",
            "         INNER JOIN erp_sale_out so ON so.id = soi.out_id",
            "           AND so.deleted = 0",
            "           AND so.status = #{approveStatus}",
            "         WHERE soi.deleted = 0",
            "           AND soi.product_id = #{productId}",
            "           AND soi.warehouse_id = #{warehouseId}",
            "           AND soi.batch_no IS NOT NULL",
            "           AND soi.batch_no != ''",
            "         GROUP BY soi.batch_no",
            "       ) o ON o.batch_no = t.batch_no",
            " WHERE t.in_count - COALESCE(o.out_count, 0) > 0",
            "<if test='keyword != null and keyword != &quot;&quot;'> AND t.batch_no LIKE CONCAT('%', #{keyword}, '%')</if>",
            " ORDER BY t.first_in_time ASC, t.batch_no ASC",
            "</script>"
    })
    com.baomidou.mybatisplus.core.metadata.IPage<Map<String, Object>> selectAvailableBatchNoPage(
            com.baomidou.mybatisplus.core.metadata.IPage<Map<String, Object>> page,
            @Param("productId") Long productId, @Param("warehouseId") Long warehouseId,
            @Param("approveStatus") Integer approveStatus, @Param("keyword") String keyword);

    @Select({
            "SELECT t.batch_no,",
            "       t.in_count - COALESCE(o.out_count, 0) AS available_count,",
            "       t.first_in_time",
            "  FROM (",
            "        SELECT pii.batch_no,",
            "               SUM(pii.count) AS in_count,",
            "               MIN(pi.in_time) AS first_in_time",
            "          FROM erp_purchase_in_items pii",
            "         INNER JOIN erp_purchase_in pi ON pi.id = pii.in_id",
            "           AND pi.deleted = 0",
            "           AND pi.status = #{approveStatus}",
            "         WHERE pii.deleted = 0",
            "           AND pii.product_id = #{productId}",
            "           AND pii.warehouse_id = #{warehouseId}",
            "           AND pii.batch_no IS NOT NULL",
            "           AND pii.batch_no != ''",
            "         GROUP BY pii.batch_no",
            "       ) t",
            "  LEFT JOIN (",
            "        SELECT soi.batch_no,",
            "               SUM(soi.count) AS out_count",
            "          FROM erp_sale_out_items soi",
            "         INNER JOIN erp_sale_out so ON so.id = soi.out_id",
            "           AND so.deleted = 0",
            "           AND so.status = #{approveStatus}",
            "         WHERE soi.deleted = 0",
            "           AND soi.product_id = #{productId}",
            "           AND soi.warehouse_id = #{warehouseId}",
            "           AND soi.batch_no IS NOT NULL",
            "           AND soi.batch_no != ''",
            "         GROUP BY soi.batch_no",
            "       ) o ON o.batch_no = t.batch_no",
            " WHERE t.in_count - COALESCE(o.out_count, 0) > 0",
            " ORDER BY t.first_in_time ASC, t.batch_no ASC"
    })
    List<Map<String, Object>> selectAvailableBatchNoList(@Param("productId") Long productId,
                                                         @Param("warehouseId") Long warehouseId,
                                                         @Param("approveStatus") Integer approveStatus);

    @Select({
            "<script>",
            "SELECT t.product_id,",
            "       t.warehouse_id,",
            "       t.batch_no,",
            "       t.in_count - COALESCE(o.out_count, 0) AS available_count,",
            "       t.first_in_time",
            "  FROM (",
            "        SELECT pii.product_id,",
            "               pii.warehouse_id,",
            "               pii.batch_no,",
            "               SUM(pii.count) AS in_count,",
            "               MIN(pi.in_time) AS first_in_time",
            "          FROM erp_purchase_in_items pii",
            "         INNER JOIN erp_purchase_in pi ON pi.id = pii.in_id",
            "           AND pi.deleted = 0",
            "           AND pi.status = #{approveStatus}",
            "         WHERE pii.deleted = 0",
            "           AND (",
            "           <foreach collection='stocks' item='stock' separator=' OR '>",
            "             (pii.product_id = #{stock.productId} AND pii.warehouse_id = #{stock.warehouseId})",
            "           </foreach>",
            "           )",
            "           AND pii.batch_no IS NOT NULL",
            "           AND pii.batch_no != ''",
            "         GROUP BY pii.product_id, pii.warehouse_id, pii.batch_no",
            "       ) t",
            "  LEFT JOIN (",
            "        SELECT soi.product_id,",
            "               soi.warehouse_id,",
            "               soi.batch_no,",
            "               SUM(soi.count) AS out_count",
            "          FROM erp_sale_out_items soi",
            "         INNER JOIN erp_sale_out so ON so.id = soi.out_id",
            "           AND so.deleted = 0",
            "           AND so.status = #{approveStatus}",
            "         WHERE soi.deleted = 0",
            "           AND (",
            "           <foreach collection='stocks' item='stock' separator=' OR '>",
            "             (soi.product_id = #{stock.productId} AND soi.warehouse_id = #{stock.warehouseId})",
            "           </foreach>",
            "           )",
            "           AND soi.batch_no IS NOT NULL",
            "           AND soi.batch_no != ''",
            "         GROUP BY soi.product_id, soi.warehouse_id, soi.batch_no",
            "       ) o ON o.product_id = t.product_id",
            "          AND o.warehouse_id = t.warehouse_id",
            "          AND o.batch_no = t.batch_no",
            " WHERE t.in_count - COALESCE(o.out_count, 0) > 0",
            " ORDER BY t.product_id ASC, t.warehouse_id ASC, t.first_in_time ASC, t.batch_no ASC",
            "</script>"
    })
    List<Map<String, Object>> selectAvailableBatchNoListByProductAndWarehouseIds(
            @Param("stocks") Collection<ErpStockDO> stocks,
            @Param("approveStatus") Integer approveStatus);

}
