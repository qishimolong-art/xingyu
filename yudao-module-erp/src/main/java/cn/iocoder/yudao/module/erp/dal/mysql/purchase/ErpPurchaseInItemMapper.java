package cn.iocoder.yudao.module.erp.dal.mysql.purchase;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInSaleCartableItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoiceSourceInItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockPendingInDetailRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleBizSourceTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleConvertTypeEnum;
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

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;

/**
 * ERP 采购入库项 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpPurchaseInItemMapper extends BaseMapperX<ErpPurchaseInItemDO> {

    default List<ErpPurchaseInItemDO> selectListByInId(Long inId) {
        return selectList(ErpPurchaseInItemDO::getInId, inId);
    }

    default PageResult<ErpPurchaseInItemDO> selectPageByInId(ErpPurchaseInItemPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpPurchaseInItemDO> query = new LambdaQueryWrapperX<ErpPurchaseInItemDO>()
                .eq(ErpPurchaseInItemDO::getInId, reqVO.getInId());
        SFunction<ErpPurchaseInItemDO, ?> orderColumn = getOrderColumn(reqVO.getOrderField());
        if (orderColumn == null) {
            query.orderByAsc(ErpPurchaseInItemDO::getId);
        } else if ("desc".equalsIgnoreCase(reqVO.getOrderDirection())) {
            query.orderByDesc(orderColumn);
        } else {
            query.orderByAsc(orderColumn);
        }
        if (orderColumn != null && !"id".equals(reqVO.getOrderField().trim())) {
            query.orderByAsc(ErpPurchaseInItemDO::getId);
        }
        return selectPage(reqVO, query);
    }

    default PageResult<ErpPurchaseInItemDO> selectPageByInIds(ErpPurchaseInvoiceSourceInItemPageReqVO reqVO) {
        if (CollUtil.isEmpty(reqVO.getSourceInIds())) {
            return PageResult.empty();
        }
        LambdaQueryWrapperX<ErpPurchaseInItemDO> query = new LambdaQueryWrapperX<ErpPurchaseInItemDO>()
                .in(ErpPurchaseInItemDO::getInId, reqVO.getSourceInIds());
        SFunction<ErpPurchaseInItemDO, ?> orderColumn = getOrderColumn(reqVO.getOrderField());
        if (orderColumn == null) {
            query.orderByAsc(ErpPurchaseInItemDO::getId);
        } else if ("desc".equalsIgnoreCase(reqVO.getOrderDirection())) {
            query.orderByDesc(orderColumn);
        } else {
            query.orderByAsc(orderColumn);
        }
        if (orderColumn != null && !"id".equals(reqVO.getOrderField().trim())) {
            query.orderByAsc(ErpPurchaseInItemDO::getId);
        }
        return selectPage(reqVO, query);
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

    default List<ErpPurchaseInItemDO> selectListByInIds(Collection<Long> inIds) {
        return selectList(ErpPurchaseInItemDO::getInId, inIds);
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

    static SFunction<ErpPurchaseInItemDO, ?> getOrderColumn(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "id":
                return ErpPurchaseInItemDO::getId;
            case "sourceInId":
                return ErpPurchaseInItemDO::getInId;
            case "gift":
                return ErpPurchaseInItemDO::getGift;
            case "productId":
            case "productCode":
            case "productName":
            case "productUnitName":
            case "weight":
                return ErpPurchaseInItemDO::getProductId;
            case "warehouseId":
                return ErpPurchaseInItemDO::getWarehouseId;
            case "deptId":
                return ErpPurchaseInItemDO::getDeptId;
            case "count":
                return ErpPurchaseInItemDO::getCount;
            case "productPrice":
            case "totalProductPrice":
                return ErpPurchaseInItemDO::getProductPrice;
            case "totalPrice":
                return ErpPurchaseInItemDO::getTotalPrice;
            case "packageQty":
                return ErpPurchaseInItemDO::getPackageQty;
            case "wholeQty":
                return ErpPurchaseInItemDO::getWholeQty;
            case "vehicleModel":
                return ErpPurchaseInItemDO::getVehicleModel;
            case "warehousePosition":
                return ErpPurchaseInItemDO::getWarehousePosition;
            case "drawingNo":
                return ErpPurchaseInItemDO::getDrawingNo;
            case "batchNo":
                return ErpPurchaseInItemDO::getBatchNo;
            case "brand":
                return ErpPurchaseInItemDO::getBrand;
            case "remark":
                return ErpPurchaseInItemDO::getRemark;
            case "returnStatus":
            case "stockInBillStatus":
            default:
                return null;
        }
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
