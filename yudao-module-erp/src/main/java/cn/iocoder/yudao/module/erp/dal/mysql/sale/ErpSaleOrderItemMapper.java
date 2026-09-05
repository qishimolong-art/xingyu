package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order.ErpSaleOrderItemPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOrderItemDO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ERP 销售订单明项目 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpSaleOrderItemMapper extends BaseMapperX<ErpSaleOrderItemDO> {

    default List<ErpSaleOrderItemDO> selectListByOrderId(Long orderId) {
        return selectList(ErpSaleOrderItemDO::getOrderId, orderId);
    }

    default PageResult<ErpSaleOrderItemDO> selectPageByOrderId(ErpSaleOrderItemPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpSaleOrderItemDO> query = new LambdaQueryWrapperX<ErpSaleOrderItemDO>()
                .eq(ErpSaleOrderItemDO::getOrderId, reqVO.getOrderId());
        SFunction<ErpSaleOrderItemDO, ?> orderColumn = getOrderColumn(reqVO.getOrderField());
        if (orderColumn == null) {
            query.orderByAsc(ErpSaleOrderItemDO::getId);
        } else if ("desc".equalsIgnoreCase(reqVO.getOrderDirection())) {
            query.orderByDesc(orderColumn);
        } else {
            query.orderByAsc(orderColumn);
        }
        if (orderColumn != null && !"id".equals(reqVO.getOrderField().trim())) {
            query.orderByAsc(ErpSaleOrderItemDO::getId);
        }
        return selectPage(reqVO, query);
    }

    default List<ErpSaleOrderItemDO> selectListByOrderIds(Collection<Long> orderIds) {
        return selectList(ErpSaleOrderItemDO::getOrderId, orderIds);
    }

    default int deleteByOrderId(Long orderId) {
        return delete(ErpSaleOrderItemDO::getOrderId, orderId);
    }

    static SFunction<ErpSaleOrderItemDO, ?> getOrderColumn(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "id":
                return ErpSaleOrderItemDO::getId;
            case "giftFlag":
                return ErpSaleOrderItemDO::getGiftFlag;
            case "productId":
            case "productCode":
            case "productName":
            case "productUnitName":
                return ErpSaleOrderItemDO::getProductId;
            case "warehouseId":
                return ErpSaleOrderItemDO::getWarehouseId;
            case "deptId":
                return ErpSaleOrderItemDO::getDeptId;
            case "count":
                return ErpSaleOrderItemDO::getCount;
            case "productPrice":
                return ErpSaleOrderItemDO::getProductPrice;
            case "totalPrice":
                return ErpSaleOrderItemDO::getTotalPrice;
            case "taxPercent":
                return ErpSaleOrderItemDO::getTaxPercent;
            case "taxPrice":
                return ErpSaleOrderItemDO::getTaxPrice;
            case "weight":
                return ErpSaleOrderItemDO::getWeight;
            case "packageQty":
                return ErpSaleOrderItemDO::getPackageQty;
            case "batchNo":
                return ErpSaleOrderItemDO::getBatchNo;
            case "outCount":
                return ErpSaleOrderItemDO::getOutCount;
            case "returnCount":
                return ErpSaleOrderItemDO::getReturnCount;
            case "remark":
                return ErpSaleOrderItemDO::getRemark;
            default:
                return null;
        }
    }

    default Long selectCountByProductId(Long productId) {
        return selectCount(ErpSaleOrderItemDO::getProductId, productId);
    }

    /**
     * 统计每个产品的"占用数"（= SUM(count - outCount)），按 product_id 分组
     */
    default Map<Long, BigDecimal> selectOccupiedCountMap(Collection<Long> productIds) {
        if (CollUtil.isEmpty(productIds)) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = selectMaps(
                new QueryWrapper<ErpSaleOrderItemDO>()
                        .select("product_id, SUM(COALESCE(count,0) - COALESCE(out_count,0)) AS occupied")
                        .in("product_id", productIds)
                        .groupBy("product_id"));
        Map<Long, BigDecimal> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Long pid = (Long) row.get("product_id");
            Object val = row.get("occupied");
            BigDecimal amount = val == null ? BigDecimal.ZERO
                    : (val instanceof BigDecimal ? (BigDecimal) val : new BigDecimal(val.toString()));
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                result.put(pid, amount);
            }
        }
        return result;
    }

}
