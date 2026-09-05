package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteItemPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteItemDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * ERP 报价订单项 Mapper
 */
@Mapper
public interface ErpSaleQuoteItemMapper extends BaseMapperX<ErpSaleQuoteItemDO> {

    default List<ErpSaleQuoteItemDO> selectListByQuoteId(Long quoteId) {
        return selectList(ErpSaleQuoteItemDO::getQuoteId, quoteId);
    }

    default PageResult<ErpSaleQuoteItemDO> selectPageByQuoteId(ErpSaleQuoteItemPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpSaleQuoteItemDO> query = new LambdaQueryWrapperX<ErpSaleQuoteItemDO>()
                .eq(ErpSaleQuoteItemDO::getQuoteId, reqVO.getQuoteId());
        SFunction<ErpSaleQuoteItemDO, ?> orderColumn = getOrderColumn(reqVO.getOrderField());
        if (orderColumn == null) {
            query.orderByAsc(ErpSaleQuoteItemDO::getId);
        } else if ("desc".equalsIgnoreCase(reqVO.getOrderDirection())) {
            query.orderByDesc(orderColumn);
        } else {
            query.orderByAsc(orderColumn);
        }
        if (orderColumn != null && !"id".equals(reqVO.getOrderField().trim())) {
            query.orderByAsc(ErpSaleQuoteItemDO::getId);
        }
        return selectPage(reqVO, query);
    }

    default List<ErpSaleQuoteItemDO> selectListByQuoteIds(Collection<Long> quoteIds) {
        return selectList(ErpSaleQuoteItemDO::getQuoteId, quoteIds);
    }

    default int deleteByQuoteId(Long quoteId) {
        return delete(ErpSaleQuoteItemDO::getQuoteId, quoteId);
    }

    static SFunction<ErpSaleQuoteItemDO, ?> getOrderColumn(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "id":
                return ErpSaleQuoteItemDO::getId;
            case "giftFlag":
                return ErpSaleQuoteItemDO::getGiftFlag;
            case "productId":
            case "productCode":
            case "productName":
            case "productUnitName":
            case "salePrice":
            case "lastSalePrice":
                return ErpSaleQuoteItemDO::getProductId;
            case "warehouseId":
                return ErpSaleQuoteItemDO::getWarehouseId;
            case "deptId":
                return ErpSaleQuoteItemDO::getDeptId;
            case "count":
                return ErpSaleQuoteItemDO::getCount;
            case "convertedCount":
                return ErpSaleQuoteItemDO::getConvertedCount;
            case "productPrice":
                return ErpSaleQuoteItemDO::getProductPrice;
            case "totalPrice":
                return ErpSaleQuoteItemDO::getTotalPrice;
            case "taxPercent":
                return ErpSaleQuoteItemDO::getTaxPercent;
            case "taxPrice":
                return ErpSaleQuoteItemDO::getTaxPrice;
            case "vehicleModel":
                return ErpSaleQuoteItemDO::getVehicleModel;
            case "standard":
                return ErpSaleQuoteItemDO::getStandard;
            case "weight":
                return ErpSaleQuoteItemDO::getWeight;
            case "packageQty":
                return ErpSaleQuoteItemDO::getPackageQty;
            case "warehousePosition":
                return ErpSaleQuoteItemDO::getWarehousePosition;
            case "drawingNo":
                return ErpSaleQuoteItemDO::getDrawingNo;
            case "batchNo":
                return ErpSaleQuoteItemDO::getBatchNo;
            case "brand":
                return ErpSaleQuoteItemDO::getBrand;
            case "originPlace":
                return ErpSaleQuoteItemDO::getOriginPlace;
            case "remark":
                return ErpSaleQuoteItemDO::getRemark;
            default:
                return null;
        }
    }

    default Long selectCountByProductId(Long productId) {
        return selectCount(ErpSaleQuoteItemDO::getProductId, productId);
    }

    default Long selectCountByWarehouseId(Long warehouseId) {
        return selectCount(ErpSaleQuoteItemDO::getWarehouseId, warehouseId);
    }

    default int updateWarehouseDeptByIds(Collection<Long> ids, Long warehouseId, Long deptId, boolean clearBatchNo) {
        LambdaUpdateWrapper<ErpSaleQuoteItemDO> wrapper = new LambdaUpdateWrapper<ErpSaleQuoteItemDO>()
                .in(ErpSaleQuoteItemDO::getId, ids)
                .set(warehouseId != null, ErpSaleQuoteItemDO::getWarehouseId, warehouseId)
                .set(ErpSaleQuoteItemDO::getDeptId, deptId);
        if (clearBatchNo) {
            wrapper.set(ErpSaleQuoteItemDO::getBatchNo, null);
        }
        return update(null, wrapper);
    }

}
