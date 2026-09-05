package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutItemPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutItemDO;
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

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;

/**
 * ERP 销售出库项 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpSaleOutItemMapper extends BaseMapperX<ErpSaleOutItemDO> {

    default List<ErpSaleOutItemDO> selectListByOutId(Long outId) {
        return selectList(ErpSaleOutItemDO::getOutId, outId);
    }

    default PageResult<ErpSaleOutItemDO> selectPageByOutId(ErpSaleOutItemPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpSaleOutItemDO> query = new LambdaQueryWrapperX<ErpSaleOutItemDO>()
                .eq(ErpSaleOutItemDO::getOutId, reqVO.getOutId());
        SFunction<ErpSaleOutItemDO, ?> orderColumn = getOrderColumn(reqVO.getOrderField());
        if (orderColumn == null) {
            query.orderByAsc(ErpSaleOutItemDO::getId);
        } else if ("desc".equalsIgnoreCase(reqVO.getOrderDirection())) {
            query.orderByDesc(orderColumn);
        } else {
            query.orderByAsc(orderColumn);
        }
        if (orderColumn != null && !"id".equals(reqVO.getOrderField().trim())) {
            query.orderByAsc(ErpSaleOutItemDO::getId);
        }
        return selectPage(reqVO, query);
    }

    default List<ErpSaleOutItemDO> selectListByOutIds(Collection<Long> outIds) {
        return selectList(ErpSaleOutItemDO::getOutId, outIds);
    }

    default List<ErpSaleOutItemDO> selectListByIds(Collection<Long> ids) {
        return selectList(ErpSaleOutItemDO::getId, ids);
    }

    default int deleteByOutId(Long outId) {
        return delete(ErpSaleOutItemDO::getOutId, outId);
    }

    static SFunction<ErpSaleOutItemDO, ?> getOrderColumn(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "id":
                return ErpSaleOutItemDO::getId;
            case "orderItemId":
                return ErpSaleOutItemDO::getOrderItemId;
            case "productId":
            case "productCode":
            case "productName":
            case "productUnitName":
                return ErpSaleOutItemDO::getProductId;
            case "warehouseId":
                return ErpSaleOutItemDO::getWarehouseId;
            case "deptId":
                return ErpSaleOutItemDO::getDeptId;
            case "count":
                return ErpSaleOutItemDO::getCount;
            case "productPrice":
                return ErpSaleOutItemDO::getProductPrice;
            case "totalPrice":
                return ErpSaleOutItemDO::getTotalPrice;
            case "taxPercent":
                return ErpSaleOutItemDO::getTaxPercent;
            case "taxPrice":
                return ErpSaleOutItemDO::getTaxPrice;
            case "giftFlag":
                return ErpSaleOutItemDO::getGiftFlag;
            case "vehicleModel":
                return ErpSaleOutItemDO::getVehicleModel;
            case "standard":
                return ErpSaleOutItemDO::getStandard;
            case "featureCode":
                return ErpSaleOutItemDO::getFeatureCode;
            case "brand":
                return ErpSaleOutItemDO::getBrand;
            case "drawingNo":
                return ErpSaleOutItemDO::getDrawingNo;
            case "batchNo":
                return ErpSaleOutItemDO::getBatchNo;
            case "warehousePosition":
                return ErpSaleOutItemDO::getWarehousePosition;
            case "unitWeight":
                return ErpSaleOutItemDO::getUnitWeight;
            case "packageQty":
                return ErpSaleOutItemDO::getPackageQty;
            case "totalWeight":
                return ErpSaleOutItemDO::getTotalWeight;
            case "originPlace":
                return ErpSaleOutItemDO::getOriginPlace;
            case "remark":
                return ErpSaleOutItemDO::getRemark;
            default:
                return null;
        }
    }

    default Long selectCountByProductId(Long productId) {
        return selectCount(ErpSaleOutItemDO::getProductId, productId);
    }

    default Long selectCountByWarehouseId(Long warehouseId) {
        return selectCount(ErpSaleOutItemDO::getWarehouseId, warehouseId);
    }

    default Map<Long, BigDecimal> selectLatestSalePriceMap(Collection<Long> productIds) {
        if (CollUtil.isEmpty(productIds)) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = selectLatestSalePriceRows(productIds,
                ErpAuditStatus.APPROVE.getStatus());
        Map<Long, BigDecimal> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object productId = row.get("product_id");
            Object price = row.get("product_price");
            if (productId == null || price == null) {
                continue;
            }
            Long productIdValue = ((Number) productId).longValue();
            if (result.containsKey(productIdValue)) {
                continue;
            }
            result.put(productIdValue,
                    price instanceof BigDecimal ? (BigDecimal) price : new BigDecimal(price.toString()));
        }
        return result;
    }

    @Select({
            "<script>",
            "SELECT soi.product_id, soi.product_price",
            "  FROM erp_sale_out_items soi",
            " INNER JOIN erp_sale_out so ON so.id = soi.out_id",
            "   AND so.deleted = 0",
            "   AND so.status = #{status}",
            " WHERE soi.deleted = 0",
            "   AND soi.product_price IS NOT NULL",
            "   AND soi.product_id IN",
            "   <foreach collection='productIds' item='productId' open='(' separator=',' close=')'>",
            "     #{productId}",
            "   </foreach>",
            " ORDER BY soi.product_id ASC, so.out_time DESC, so.id DESC, soi.id DESC",
            "</script>"
    })
    List<Map<String, Object>> selectLatestSalePriceRows(@Param("productIds") Collection<Long> productIds,
                                                        @Param("status") Integer status);

    /**
     * 基于销售订单编号，查询每个销售订单项的出库数量之和
     *
     * @param outIds 出库订单项编号数组
     * @return key：销售订单项编号；value：出库数量之和
     */
    default Map<Long, BigDecimal> selectOrderItemCountSumMapByOutIds(Collection<Long> outIds) {
        if (CollUtil.isEmpty(outIds)) {
            return Collections.emptyMap();
        }
        // SQL sum 查询
        List<Map<String, Object>> result = selectMaps(new QueryWrapper<ErpSaleOutItemDO>()
                .select("order_item_id, SUM(count) AS sum_count")
                .groupBy("order_item_id")
                .in("out_id", outIds));
        // 获得数量
        return convertMap(result, obj -> (Long) obj.get("order_item_id"), obj -> (BigDecimal) obj.get("sum_count"));
    }

}
