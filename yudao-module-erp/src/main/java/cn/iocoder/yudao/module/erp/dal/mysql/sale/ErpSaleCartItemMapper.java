package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockOccupiedDetailRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartItemDO;
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

    default List<ErpSaleCartItemDO> selectListByCartIds(Collection<Long> cartIds) {
        return selectList(ErpSaleCartItemDO::getCartId, cartIds);
    }

    default int deleteByCartId(Long cartId) {
        return delete(ErpSaleCartItemDO::getCartId, cartId);
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
