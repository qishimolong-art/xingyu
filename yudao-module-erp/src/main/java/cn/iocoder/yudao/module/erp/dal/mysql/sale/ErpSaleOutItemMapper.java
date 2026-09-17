package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutReturnableItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust.ErpSalePriceAdjustableItemPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutItemDO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
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
 * ERP 销售出库项 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpSaleOutItemMapper extends BaseMapperX<ErpSaleOutItemDO> {

    String SALE_ITEM_PRODUCT_KEYWORD_SQL = "("
            + "EXISTS (SELECT 1 FROM erp_product p "
            + "WHERE p.id = erp_sale_out_items.product_id AND p.deleted = b'0' "
            + "AND p.tenant_id = erp_sale_out_items.tenant_id "
            + "AND (p.code LIKE {0} OR p.name LIKE {0} OR p.pinyin_code LIKE {0} "
            + "OR p.wubi_code LIKE {0} OR p.bar_code LIKE {0} OR p.vehicle_model LIKE {0} "
            + "OR p.factory_code LIKE {0} OR p.standard LIKE {0} OR p.brand LIKE {0} "
            + "OR p.drawing_no LIKE {0})) "
            + "OR erp_sale_out_items.vehicle_model LIKE {0} "
            + "OR erp_sale_out_items.standard LIKE {0} "
            + "OR erp_sale_out_items.feature_code LIKE {0} "
            + "OR erp_sale_out_items.brand LIKE {0} "
            + "OR erp_sale_out_items.drawing_no LIKE {0} "
            + "OR erp_sale_out_items.batch_no LIKE {0})";

    /** 调用方先持有销售单父锁，保持当前读而不追加动态排序。 */
    default List<ErpSaleOutItemDO> selectListByOutIdForUpdate(Long outId) {
        return selectList(new LambdaQueryWrapperX<ErpSaleOutItemDO>()
                .eq(ErpSaleOutItemDO::getOutId, outId).last("FOR UPDATE"));
    }

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

    default PageResult<ErpSaleOutItemDO> selectAdjustableItemPage(
            ErpSalePriceAdjustableItemPageReqVO reqVO) {
        if (reqVO.getCustomerId() == null) {
            return PageResult.empty();
        }
        QueryWrapper<ErpSaleOutItemDO> query = new QueryWrapper<ErpSaleOutItemDO>()
                .apply("EXISTS (SELECT 1 FROM erp_sale_out so "
                                + "WHERE so.id = erp_sale_out_items.out_id AND so.deleted = b'0' "
                                + "AND so.tenant_id = erp_sale_out_items.tenant_id "
                                + "AND so.customer_id = {0} AND so.status = {1})",
                        reqVO.getCustomerId(), ErpAuditStatus.APPROVE.getStatus());
        if (reqVO.getSaleOutId() != null) {
            query.eq("out_id", reqVO.getSaleOutId());
        }
        if (Boolean.TRUE.equals(reqVO.getExcludeAdjusted()) || reqVO.getExcludeAdjusted() == null) {
            query.and(wrapper -> wrapper.isNull("adjusted").or().eq("adjusted", false));
        }
        appendProductKeyword(query, reqVO.getProductKeyword());
        query.orderByDesc("out_id").orderByAsc("id");
        return selectPage(reqVO, query);
    }

    default PageResult<ErpSaleOutItemDO> selectReturnableItemPage(
            ErpSaleOutReturnableItemPageReqVO reqVO) {
        QueryWrapper<ErpSaleOutItemDO> query = new QueryWrapper<ErpSaleOutItemDO>()
                .eq("out_id", reqVO.getOutId())
                .apply("COALESCE(count, 0) > COALESCE((SELECT SUM(COALESCE(sri.count, 0)) "
                                + "FROM erp_sale_return_items sri "
                                + "INNER JOIN erp_sale_return sr ON sr.id = sri.return_id "
                                + "AND sr.deleted = b'0' AND sr.tenant_id = sri.tenant_id "
                                + "AND sr.status = {0} "
                                + "WHERE sri.deleted = b'0' "
                                + "AND sri.tenant_id = erp_sale_out_items.tenant_id "
                                + "AND sri.source_out_item_id = erp_sale_out_items.id), 0)",
                        ErpAuditStatus.APPROVE.getStatus());
        appendProductKeyword(query, reqVO.getProductKeyword());
        query.orderByAsc("id");
        return selectPage(reqVO, query);
    }

    default List<ErpSaleOutItemDO> selectListByOutIds(Collection<Long> outIds) {
        return selectList(ErpSaleOutItemDO::getOutId, outIds);
    }

    default List<ErpSaleOutItemDO> selectLightListByOutIds(Collection<Long> outIds) {
        if (CollUtil.isEmpty(outIds)) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<ErpSaleOutItemDO>()
                .select(ErpSaleOutItemDO::getId, ErpSaleOutItemDO::getOutId,
                        ErpSaleOutItemDO::getCount, ErpSaleOutItemDO::getAdjusted)
                .in(ErpSaleOutItemDO::getOutId, outIds));
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

    static void appendProductKeyword(QueryWrapper<ErpSaleOutItemDO> query, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return;
        }
        String likeValue = "%" + keyword.trim().replaceAll("\\s+", "%") + "%";
        query.and(wrapper -> wrapper.apply(SALE_ITEM_PRODUCT_KEYWORD_SQL, likeValue));
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
