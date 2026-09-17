package cn.iocoder.yudao.module.erp.dal.mysql.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust.ErpPurchasePriceAdjustItemPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustItemDO;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ERP 采购调价单明细 Mapper
 *
 * @author 汽配ERP
 */
@Mapper
public interface ErpPurchasePriceAdjustItemMapper extends BaseMapperX<ErpPurchasePriceAdjustItemDO> {

    default List<ErpPurchasePriceAdjustItemDO> selectListByAdjustId(Long adjustId) {
        return selectList(ErpPurchasePriceAdjustItemDO::getAdjustId, adjustId);
    }

    default List<ErpPurchasePriceAdjustItemDO> selectListByAdjustIdForUpdate(Long adjustId) {
        return selectList(new LambdaQueryWrapperX<ErpPurchasePriceAdjustItemDO>()
                .eq(ErpPurchasePriceAdjustItemDO::getAdjustId, adjustId).last("FOR UPDATE"));
    }

    default PageResult<ErpPurchasePriceAdjustItemDO> selectPageByAdjustId(ErpPurchasePriceAdjustItemPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpPurchasePriceAdjustItemDO> query = new LambdaQueryWrapperX<ErpPurchasePriceAdjustItemDO>()
                .eq(ErpPurchasePriceAdjustItemDO::getAdjustId, reqVO.getAdjustId());
        SFunction<ErpPurchasePriceAdjustItemDO, ?> orderColumn = getOrderColumn(reqVO.getOrderField());
        if (orderColumn == null) {
            query.orderByAsc(ErpPurchasePriceAdjustItemDO::getId);
        } else if ("desc".equalsIgnoreCase(reqVO.getOrderDirection())) {
            query.orderByDesc(orderColumn);
        } else {
            query.orderByAsc(orderColumn);
        }
        if (orderColumn != null && !"id".equals(reqVO.getOrderField().trim())) {
            query.orderByAsc(ErpPurchasePriceAdjustItemDO::getId);
        }
        return selectPage(reqVO, query);
    }

    default List<ErpPurchasePriceAdjustItemDO> selectListByAdjustIds(Collection<Long> adjustIds) {
        return selectList(ErpPurchasePriceAdjustItemDO::getAdjustId, adjustIds);
    }

    default Map<Long, Integer> selectItemCountMapByAdjustIds(Collection<Long> adjustIds) {
        if (CollUtil.isEmpty(adjustIds)) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = selectMaps(new QueryWrapper<ErpPurchasePriceAdjustItemDO>()
                .select("adjust_id, COUNT(1) AS item_count")
                .in("adjust_id", adjustIds)
                .groupBy("adjust_id"));
        Map<Long, Integer> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object adjustId = row.get("adjust_id");
            Object count = row.get("item_count");
            if (adjustId != null && count != null) {
                result.put(Long.valueOf(adjustId.toString()), Integer.valueOf(count.toString()));
            }
        }
        return result;
    }

    default void deleteByAdjustId(Long adjustId) {
        delete(ErpPurchasePriceAdjustItemDO::getAdjustId, adjustId);
    }

    default Long selectCountByWarehouseId(Long warehouseId) {
        return selectCount(ErpPurchasePriceAdjustItemDO::getWarehouseId, warehouseId);
    }

    static SFunction<ErpPurchasePriceAdjustItemDO, ?> getOrderColumn(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "id":
                return ErpPurchasePriceAdjustItemDO::getId;
            case "inId":
            case "inNo":
                return ErpPurchasePriceAdjustItemDO::getInId;
            case "inItemId":
                return ErpPurchasePriceAdjustItemDO::getInItemId;
            case "productId":
            case "productCode":
            case "productName":
            case "productUnitName":
            case "weight":
            case "packageQty":
                return ErpPurchasePriceAdjustItemDO::getProductId;
            case "warehouseId":
                return ErpPurchasePriceAdjustItemDO::getWarehouseId;
            case "deptId":
                return ErpPurchasePriceAdjustItemDO::getDeptId;
            case "oldPrice":
                return ErpPurchasePriceAdjustItemDO::getOldPrice;
            case "newPrice":
                return ErpPurchasePriceAdjustItemDO::getNewPrice;
            case "count":
                return ErpPurchasePriceAdjustItemDO::getCount;
            case "adjustRatio":
                return ErpPurchasePriceAdjustItemDO::getAdjustRatio;
            case "adjustPrice":
                return ErpPurchasePriceAdjustItemDO::getAdjustPrice;
            case "vehicleModel":
                return ErpPurchasePriceAdjustItemDO::getVehicleModel;
            case "standard":
                return ErpPurchasePriceAdjustItemDO::getStandard;
            case "featureCode":
                return ErpPurchasePriceAdjustItemDO::getFeatureCode;
            case "originPlace":
                return ErpPurchasePriceAdjustItemDO::getOriginPlace;
            case "brand":
                return ErpPurchasePriceAdjustItemDO::getBrand;
            case "drawingNo":
                return ErpPurchasePriceAdjustItemDO::getDrawingNo;
            case "warehousePosition":
                return ErpPurchasePriceAdjustItemDO::getWarehousePosition;
            default:
                return null;
        }
    }

    /**
     * 查询指定入库项是否已被调过价（M2 审批流程用于校验"添加明细"方式）
     */
    default List<ErpPurchasePriceAdjustItemDO> selectListByInItemIds(Collection<Long> inItemIds) {
        return selectList(ErpPurchasePriceAdjustItemDO::getInItemId, inItemIds);
    }

}
