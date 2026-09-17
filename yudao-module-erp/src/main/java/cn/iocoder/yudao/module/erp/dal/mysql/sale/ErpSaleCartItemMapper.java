package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockOccupiedDetailRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartItemDO;
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
        return selectList(ErpSaleCartItemDO::getCartId, cartId);
    }

    /** 写入流程在父行锁后读取完整源行，不使用已有 RR 快照；排序由服务执行。 */
    default List<ErpSaleCartItemDO> selectListByCartIdForUpdate(Long cartId) {
        return selectList(new LambdaQueryWrapperX<ErpSaleCartItemDO>()
                .eq(ErpSaleCartItemDO::getCartId, cartId).last("FOR UPDATE"));
    }

    default PageResult<ErpSaleCartItemDO> selectPageByCartId(ErpSaleCartItemPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpSaleCartItemDO> query = new LambdaQueryWrapperX<ErpSaleCartItemDO>()
                .eq(ErpSaleCartItemDO::getCartId, reqVO.getCartId());
        SFunction<ErpSaleCartItemDO, ?> orderColumn = getOrderColumn(reqVO.getOrderField());
        if (orderColumn == null) {
            query.orderByAsc(ErpSaleCartItemDO::getId);
        } else if ("desc".equalsIgnoreCase(reqVO.getOrderDirection())) {
            query.orderByDesc(orderColumn);
        } else {
            query.orderByAsc(orderColumn);
        }
        if (orderColumn != null && !"id".equals(reqVO.getOrderField().trim())) {
            query.orderByAsc(ErpSaleCartItemDO::getId);
        }
        return selectPage(reqVO, query);
    }

    default List<ErpSaleCartItemDO> selectListByCartIds(Collection<Long> cartIds) {
        return selectList(ErpSaleCartItemDO::getCartId, cartIds);
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
