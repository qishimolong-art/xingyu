package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnItemPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnItemDO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;

/**
 * ERP 销售退货项 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpSaleReturnItemMapper extends BaseMapperX<ErpSaleReturnItemDO> {

    default List<ErpSaleReturnItemDO> selectListByReturnId(Long returnId) {
        return selectList(ErpSaleReturnItemDO::getReturnId, returnId);
    }

    default PageResult<ErpSaleReturnItemDO> selectPageByReturnId(ErpSaleReturnItemPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpSaleReturnItemDO> query = new LambdaQueryWrapperX<ErpSaleReturnItemDO>()
                .eq(ErpSaleReturnItemDO::getReturnId, reqVO.getReturnId());
        SFunction<ErpSaleReturnItemDO, ?> orderColumn = getOrderColumn(reqVO.getOrderField());
        if (orderColumn == null) {
            query.orderByAsc(ErpSaleReturnItemDO::getId);
        } else if ("desc".equalsIgnoreCase(reqVO.getOrderDirection())) {
            query.orderByDesc(orderColumn);
        } else {
            query.orderByAsc(orderColumn);
        }
        if (orderColumn != null && !"id".equals(reqVO.getOrderField().trim())) {
            query.orderByAsc(ErpSaleReturnItemDO::getId);
        }
        return selectPage(reqVO, query);
    }

    default List<ErpSaleReturnItemDO> selectListByReturnIds(Collection<Long> returnIds) {
        return selectList(ErpSaleReturnItemDO::getReturnId, returnIds);
    }

    default int deleteByReturnId(Long returnId) {
        return delete(ErpSaleReturnItemDO::getReturnId, returnId);
    }

    static SFunction<ErpSaleReturnItemDO, ?> getOrderColumn(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "id":
                return ErpSaleReturnItemDO::getId;
            case "orderItemId":
                return ErpSaleReturnItemDO::getOrderItemId;
            case "sourceOutItemId":
                return ErpSaleReturnItemDO::getSourceOutItemId;
            case "productId":
            case "productCode":
            case "productName":
            case "productUnitName":
                return ErpSaleReturnItemDO::getProductId;
            case "warehouseId":
                return ErpSaleReturnItemDO::getWarehouseId;
            case "deptId":
                return ErpSaleReturnItemDO::getDeptId;
            case "batchNo":
                return ErpSaleReturnItemDO::getBatchNo;
            case "count":
                return ErpSaleReturnItemDO::getCount;
            case "productPrice":
                return ErpSaleReturnItemDO::getProductPrice;
            case "totalPrice":
                return ErpSaleReturnItemDO::getTotalPrice;
            case "taxPercent":
                return ErpSaleReturnItemDO::getTaxPercent;
            case "taxPrice":
                return ErpSaleReturnItemDO::getTaxPrice;
            case "weight":
                return ErpSaleReturnItemDO::getWeight;
            case "packageQty":
                return ErpSaleReturnItemDO::getPackageQty;
            case "returnReason":
                return ErpSaleReturnItemDO::getReturnReason;
            case "warehousePosition":
                return ErpSaleReturnItemDO::getWarehousePosition;
            case "remark":
                return ErpSaleReturnItemDO::getRemark;
            default:
                return null;
        }
    }

    default Long selectCountByProductId(Long productId) {
        return selectCount(ErpSaleReturnItemDO::getProductId, productId);
    }

    default Long selectCountByWarehouseId(Long warehouseId) {
        return selectCount(ErpSaleReturnItemDO::getWarehouseId, warehouseId);
    }

    /**
     * 基于销售订单编号，查询每个销售订单项的退货数量之和
     *
     * @param returnIds 出库订单项编号数组
     * @return key：销售订单项编号；value：退货数量之和
     */
    default Map<Long, BigDecimal> selectOrderItemCountSumMapByReturnIds(Collection<Long> returnIds) {
        if (CollUtil.isEmpty(returnIds)) {
            return Collections.emptyMap();
        }
        // SQL sum 查询
        List<Map<String, Object>> result = selectMaps(new QueryWrapper<ErpSaleReturnItemDO>()
                .select("order_item_id, SUM(count) AS sum_count")
                .groupBy("order_item_id")
                .in("return_id", returnIds));
        // 获得数量
        return convertMap(result, obj -> (Long) obj.get("order_item_id"), obj -> (BigDecimal) obj.get("sum_count"));
    }

    default Map<Long, BigDecimal> selectSourceOutItemCountSumMapByReturnIds(Collection<Long> returnIds) {
        if (CollUtil.isEmpty(returnIds)) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> result = selectMaps(new QueryWrapper<ErpSaleReturnItemDO>()
                .select("source_out_item_id, SUM(count) AS sum_count")
                .isNotNull("source_out_item_id")
                .groupBy("source_out_item_id")
                .in("return_id", returnIds));
        return convertMap(result, obj -> (Long) obj.get("source_out_item_id"), obj -> (BigDecimal) obj.get("sum_count"));
    }

    default Map<Long, BigDecimal> selectReturnedCountMapBySourceOutItemIds(Collection<Long> sourceOutItemIds) {
        if (CollUtil.isEmpty(sourceOutItemIds)) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> result = selectMaps(new QueryWrapper<ErpSaleReturnItemDO>()
                .select("source_out_item_id, SUM(count) AS sum_count")
                .isNotNull("source_out_item_id")
                .groupBy("source_out_item_id")
                .in("source_out_item_id", sourceOutItemIds));
        return convertMap(result, obj -> (Long) obj.get("source_out_item_id"), obj -> (BigDecimal) obj.get("sum_count"));
    }

}
