package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartItemMatchReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockOccupiedDetailRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartItemDO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
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

/**
 * ERP 销售手推车项 Mapper
 */
@Mapper
public interface ErpSaleCartItemMapper extends BaseMapperX<ErpSaleCartItemDO> {

    default List<ErpSaleCartItemDO> selectListByCartId(Long cartId) {
        return selectList(buildSortedQuery(cartId, null, null, null));
    }

    default List<ErpSaleCartItemDO> selectListByCartId(Long cartId, String orderField,
                                                       String orderDirection) {
        return selectList(buildSortedQuery(cartId, null, orderField, orderDirection));
    }

    /** 写入流程在父行锁后读取完整源行，不使用已有 RR 快照；排序由服务执行。 */
    default List<ErpSaleCartItemDO> selectListByCartIdForUpdate(Long cartId) {
        return selectList(new LambdaQueryWrapperX<ErpSaleCartItemDO>()
                .eq(ErpSaleCartItemDO::getCartId, cartId).last("FOR UPDATE"));
    }

    default PageResult<ErpSaleCartItemDO> selectPageByCartId(ErpSaleCartItemPageReqVO reqVO) {
        QueryWrapper<ErpSaleCartItemDO> query = buildSortedQuery(reqVO.getCartId(),
                reqVO.getExcludedItemIds(), reqVO.getOrderField(), reqVO.getOrderDirection());
        return selectPage(reqVO, query);
    }

    static QueryWrapper<ErpSaleCartItemDO> buildSortedQuery(Long cartId, Collection<Long> excludedItemIds,
                                                             String orderField, String orderDirection) {
        QueryWrapper<ErpSaleCartItemDO> query = new QueryWrapper<ErpSaleCartItemDO>()
                .eq("cart_id", cartId)
                .notIn(CollUtil.isNotEmpty(excludedItemIds), "id", excludedItemIds);
        OrderSpec orderSpec = getOrderSpec(orderField);
        if (orderSpec == null) {
            return query.orderByAsc("erp_sale_cart_items.id");
        }
        String blankOrder = "CASE WHEN " + orderSpec.expression() + " IS NULL"
                + (orderSpec.text() ? " OR TRIM(" + orderSpec.expression() + ") = ''" : "")
                + " THEN 1 ELSE 0 END";
        query.orderByAsc(blankOrder)
                .orderBy(true, !"desc".equalsIgnoreCase(orderDirection), orderSpec.expression());
        if (!"id".equals(orderField == null ? null : orderField.trim())) {
            query.orderByAsc("erp_sale_cart_items.id");
        }
        return query;
    }

    static boolean isSupportedOrderField(String orderField) {
        return getOrderSpec(orderField) != null;
    }

    static OrderSpec getOrderSpec(String orderField) {
        if (orderField == null) {
            return null;
        }
        String item = "erp_sale_cart_items";
        String productName = "(SELECT p.name FROM erp_product p WHERE p.id = " + item
                + ".product_id AND p.tenant_id = " + item + ".tenant_id AND p.deleted = 0 LIMIT 1)";
        String productCode = "(SELECT p.code FROM erp_product p WHERE p.id = " + item
                + ".product_id AND p.tenant_id = " + item + ".tenant_id AND p.deleted = 0 LIMIT 1)";
        String productUnitName = "(SELECT u.name FROM erp_product p LEFT JOIN erp_product_unit u"
                + " ON u.id = p.unit_id AND u.tenant_id = p.tenant_id AND u.deleted = 0"
                + " WHERE p.id = " + item + ".product_id AND p.tenant_id = " + item
                + ".tenant_id AND p.deleted = 0 LIMIT 1)";
        String warehouseName = "(SELECT w.name FROM erp_warehouse w WHERE w.id = " + item
                + ".warehouse_id AND w.tenant_id = " + item + ".tenant_id AND w.deleted = 0 LIMIT 1)";
        String warehouseDeptName = "(SELECT d.name FROM erp_warehouse w LEFT JOIN system_dept d"
                + " ON d.id = w.dept_id AND d.tenant_id = w.tenant_id AND d.deleted = 0"
                + " WHERE w.id = " + item + ".warehouse_id AND w.tenant_id = " + item
                + ".tenant_id AND w.deleted = 0 LIMIT 1)";
        String lastSalePrice = "(SELECT soi.product_price FROM erp_sale_out_items soi"
                + " INNER JOIN erp_sale_out so ON so.id = soi.out_id AND so.tenant_id = soi.tenant_id"
                + " AND so.deleted = 0 AND so.status = " + ErpAuditStatus.APPROVE.getStatus()
                + " WHERE soi.product_id = " + item + ".product_id AND soi.tenant_id = " + item
                + ".tenant_id AND soi.deleted = 0 AND soi.product_price IS NOT NULL"
                + " ORDER BY so.out_time DESC, so.id DESC, soi.id DESC LIMIT 1)";
        String pieceCount = "(CASE WHEN " + item + ".package_qty > 0 THEN " + item
                + ".count / " + item + ".package_qty ELSE NULL END)";
        return switch (orderField.trim()) {
            case "id" -> new OrderSpec(item + ".id", false);
            case "giftFlag" -> new OrderSpec(item + ".gift_flag", false);
            case "productId", "productName" -> new OrderSpec(productName, true);
            case "productCode" -> new OrderSpec(productCode, true);
            case "productUnitName" -> new OrderSpec(productUnitName, true);
            case "warehouseId" -> new OrderSpec(warehouseName, true);
            case "warehouseDeptName" -> new OrderSpec(warehouseDeptName, true);
            case "deptId" -> new OrderSpec(item + ".dept_id", false);
            case "count" -> new OrderSpec(item + ".count", false);
            case "productPrice" -> new OrderSpec(item + ".product_price", false);
            case "lastSalePrice" -> new OrderSpec(lastSalePrice, false);
            case "totalPrice" -> new OrderSpec(item + ".total_price", false);
            case "taxPercent" -> new OrderSpec(item + ".tax_percent", false);
            case "taxPrice" -> new OrderSpec(item + ".tax_price", false);
            case "vehicleModel" -> new OrderSpec(item + ".vehicle_model", true);
            case "standard" -> new OrderSpec(item + ".standard", true);
            case "weight" -> new OrderSpec(item + ".weight", false);
            case "packageQty" -> new OrderSpec(item + ".package_qty", false);
            case "pieceCount" -> new OrderSpec(pieceCount, false);
            case "stockCount" -> new OrderSpec(item + ".stock_count", false);
            case "warehousePosition" -> new OrderSpec(item + ".warehouse_position", true);
            case "drawingNo" -> new OrderSpec(item + ".drawing_no", true);
            case "batchNo" -> new OrderSpec(item + ".batch_no", true);
            case "brand" -> new OrderSpec(item + ".brand", true);
            case "originPlace" -> new OrderSpec(item + ".origin_place", true);
            case "remark" -> new OrderSpec(item + ".remark", true);
            default -> null;
        };
    }

    record OrderSpec(String expression, boolean text) {
    }

    default List<ErpSaleCartItemDO> selectListByCartIds(Collection<Long> cartIds) {
        return selectList(ErpSaleCartItemDO::getCartId, cartIds);
    }

    default List<ErpSaleCartItemDO> selectListByCartIdAndStockKeys(
            Long cartId, Collection<ErpSaleCartItemMatchReqVO.Key> stockKeys) {
        if (cartId == null || CollUtil.isEmpty(stockKeys)) {
            return Collections.emptyList();
        }
        QueryWrapper<ErpSaleCartItemDO> query = new QueryWrapper<ErpSaleCartItemDO>()
                .eq("cart_id", cartId);
        query.and(group -> {
            boolean first = true;
            for (ErpSaleCartItemMatchReqVO.Key key : stockKeys) {
                if (!first) {
                    group.or();
                }
                group.nested(item -> item.eq("product_id", key.getProductId())
                        .eq("warehouse_id", key.getWarehouseId())
                        .apply("TRIM(COALESCE(batch_no, '')) = {0}",
                                key.getBatchNo() == null ? "" : key.getBatchNo().trim())
                        .apply("COALESCE(gift_flag, 0) = {0}", Boolean.TRUE.equals(key.getGiftFlag()) ? 1 : 0));
                first = false;
            }
        }).orderByAsc("id");
        return selectList(query);
    }

    default long selectDefaultRankByCartIdAndItemId(Long cartId, Long itemId) {
        if (cartId == null || itemId == null) {
            return 0L;
        }
        return selectCount(new LambdaQueryWrapperX<ErpSaleCartItemDO>()
                .eq(ErpSaleCartItemDO::getCartId, cartId)
                .le(ErpSaleCartItemDO::getId, itemId));
    }

    default Map<Long, Integer> selectItemCountMapByCartIds(Collection<Long> cartIds) {
        if (CollUtil.isEmpty(cartIds)) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = selectMaps(new QueryWrapper<ErpSaleCartItemDO>()
                .select("cart_id, COUNT(1) AS item_count")
                .in("cart_id", cartIds)
                .groupBy("cart_id"));
        Map<Long, Integer> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object cartId = row.get("cart_id");
            Object count = row.get("item_count");
            if (cartId != null && count != null) {
                result.put(((Number) cartId).longValue(), ((Number) count).intValue());
            }
        }
        return result;
    }

    default int deleteByCartId(Long cartId) {
        return delete(ErpSaleCartItemDO::getCartId, cartId);
    }

    static SFunction<ErpSaleCartItemDO, ?> getOrderColumn(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "id":
                return ErpSaleCartItemDO::getId;
            case "giftFlag":
                return ErpSaleCartItemDO::getGiftFlag;
            case "productId":
            case "productCode":
            case "productName":
            case "productUnitName":
                return ErpSaleCartItemDO::getProductId;
            case "warehouseId":
                return ErpSaleCartItemDO::getWarehouseId;
            case "deptId":
                return ErpSaleCartItemDO::getDeptId;
            case "count":
                return ErpSaleCartItemDO::getCount;
            case "productPrice":
                return ErpSaleCartItemDO::getProductPrice;
            case "totalPrice":
                return ErpSaleCartItemDO::getTotalPrice;
            case "taxPercent":
                return ErpSaleCartItemDO::getTaxPercent;
            case "taxPrice":
                return ErpSaleCartItemDO::getTaxPrice;
            case "vehicleModel":
                return ErpSaleCartItemDO::getVehicleModel;
            case "standard":
                return ErpSaleCartItemDO::getStandard;
            case "weight":
                return ErpSaleCartItemDO::getWeight;
            case "packageQty":
                return ErpSaleCartItemDO::getPackageQty;
            case "warehousePosition":
                return ErpSaleCartItemDO::getWarehousePosition;
            case "drawingNo":
                return ErpSaleCartItemDO::getDrawingNo;
            case "batchNo":
                return ErpSaleCartItemDO::getBatchNo;
            case "brand":
                return ErpSaleCartItemDO::getBrand;
            case "originPlace":
                return ErpSaleCartItemDO::getOriginPlace;
            case "remark":
                return ErpSaleCartItemDO::getRemark;
            default:
                return null;
        }
    }

    default Long selectCountByProductId(Long productId) {
        return selectCount(ErpSaleCartItemDO::getProductId, productId);
    }

    default Long selectCountByWarehouseId(Long warehouseId) {
        return selectCount(ErpSaleCartItemDO::getWarehouseId, warehouseId);
    }

    default Map<String, BigDecimal> selectOccupiedCountMap(Collection<Long> productIds,
                                                          Collection<Long> warehouseIds,
                                                          Collection<Integer> statuses) {
        if (CollUtil.isEmpty(productIds) || CollUtil.isEmpty(warehouseIds) || CollUtil.isEmpty(statuses)) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = selectOccupiedCountRows(productIds, warehouseIds, statuses);
        Map<String, BigDecimal> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object productId = row.get("product_id");
            Object warehouseId = row.get("warehouse_id");
            Object value = row.get("occupied");
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
            "SELECT sci.product_id, sci.warehouse_id, SUM(COALESCE(sci.count, 0)) AS occupied",
            "  FROM erp_sale_cart_items sci",
            " INNER JOIN erp_sale_cart sc ON sc.id = sci.cart_id",
            "   AND sc.deleted = 0",
            "   AND sc.status IN",
            "   <foreach collection='statuses' item='status' open='(' separator=',' close=')'>",
            "     #{status}",
            "   </foreach>",
            "   AND (sc.status &lt;&gt; 20 OR sc.billing_confirm_time IS NULL)",
            " WHERE sci.deleted = 0",
            "   AND sci.product_id IN",
            "   <foreach collection='productIds' item='productId' open='(' separator=',' close=')'>",
            "     #{productId}",
            "   </foreach>",
            "   AND sci.warehouse_id IN",
            "   <foreach collection='warehouseIds' item='warehouseId' open='(' separator=',' close=')'>",
            "     #{warehouseId}",
            "   </foreach>",
            " GROUP BY sci.product_id, sci.warehouse_id",
            "</script>"
    })
    List<Map<String, Object>> selectOccupiedCountRows(@Param("productIds") Collection<Long> productIds,
                                                      @Param("warehouseIds") Collection<Long> warehouseIds,
                                                      @Param("statuses") Collection<Integer> statuses);

    default List<ErpStockOccupiedDetailRespVO> selectOccupiedDetails(Long productId,
                                                                     Long warehouseId,
                                                                     Collection<Integer> statuses) {
        if (productId == null || warehouseId == null || CollUtil.isEmpty(statuses)) {
            return Collections.emptyList();
        }
        return selectOccupiedDetailRows(productId, warehouseId, statuses);
    }

    @Select({
            "<script>",
            "SELECT sc.id AS cartId,",
            "       sci.id AS itemId,",
            "       sc.no AS no,",
            "       sci.product_id AS productId,",
            "       p.code AS productCode,",
            "       p.name AS productName,",
            "       sci.warehouse_id AS warehouseId,",
            "       w.name AS warehouseName,",
            "       COALESCE(sci.count, 0) AS count,",
            "       COALESCE(sci.vehicle_model, p.vehicle_model) AS vehicleModel,",
            "       COALESCE(sci.origin_place, p.origin_place) AS originPlace,",
            "       COALESCE(sci.drawing_no, p.drawing_no) AS drawingNo,",
            "       COALESCE(sci.standard, p.standard) AS standard,",
            "       sc.create_time AS createTime,",
            "       sc.creator AS creator",
            "  FROM erp_sale_cart_items sci",
            " INNER JOIN erp_sale_cart sc ON sc.id = sci.cart_id",
            "   AND sc.deleted = 0",
            "   AND sc.status IN",
            "   <foreach collection='statuses' item='status' open='(' separator=',' close=')'>",
            "     #{status}",
            "   </foreach>",
            "   AND (sc.status &lt;&gt; 20 OR sc.billing_confirm_time IS NULL)",
            "  LEFT JOIN erp_product p ON p.id = sci.product_id AND p.deleted = 0",
            "  LEFT JOIN erp_warehouse w ON w.id = sci.warehouse_id AND w.deleted = 0",
            " WHERE sci.deleted = 0",
            "   AND sci.product_id = #{productId}",
            "   AND sci.warehouse_id = #{warehouseId}",
            " ORDER BY sc.create_time DESC, sc.id DESC, sci.id DESC",
            "</script>"
    })
    List<ErpStockOccupiedDetailRespVO> selectOccupiedDetailRows(@Param("productId") Long productId,
                                                                @Param("warehouseId") Long warehouseId,
                                                                @Param("statuses") Collection<Integer> statuses);

}
