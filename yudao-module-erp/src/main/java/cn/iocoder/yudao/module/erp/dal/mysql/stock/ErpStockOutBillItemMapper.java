package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.outbill.ErpStockOutBillItemPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutBillItemDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

@Mapper
public interface ErpStockOutBillItemMapper extends BaseMapperX<ErpStockOutBillItemDO> {

    default List<ErpStockOutBillItemDO> selectListByBillId(Long billId) {
        return selectList(new LambdaQueryWrapperX<ErpStockOutBillItemDO>()
                .eq(ErpStockOutBillItemDO::getBillId, billId)
                .orderByAsc(ErpStockOutBillItemDO::getId));
    }

    default PageResult<ErpStockOutBillItemDO> selectPageByBillId(ErpStockOutBillItemPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpStockOutBillItemDO> query = new LambdaQueryWrapperX<ErpStockOutBillItemDO>()
                .eq(ErpStockOutBillItemDO::getBillId, reqVO.getBillId());
        SFunction<ErpStockOutBillItemDO, ?> orderColumn = getOrderColumn(reqVO.getOrderField());
        if (orderColumn == null) {
            query.orderByAsc(ErpStockOutBillItemDO::getId);
        } else if ("desc".equalsIgnoreCase(reqVO.getOrderDirection())) {
            query.orderByDesc(orderColumn);
        } else {
            query.orderByAsc(orderColumn);
        }
        if (orderColumn != null && !"id".equals(reqVO.getOrderField().trim())) {
            query.orderByAsc(ErpStockOutBillItemDO::getId);
        }
        return selectPage(reqVO, query);
    }

    default List<ErpStockOutBillItemDO> selectListByBillIds(Collection<Long> billIds) {
        return selectList(ErpStockOutBillItemDO::getBillId, billIds);
    }

    default int updatePickedCountByIdAndPickedCount(Long id, BigDecimal oldPickedCount,
                                                    BigDecimal newPickedCount, Integer status) {
        return update(new ErpStockOutBillItemDO()
                        .setPickedCount(newPickedCount)
                        .setStatus(status),
                new LambdaUpdateWrapper<ErpStockOutBillItemDO>()
                        .eq(ErpStockOutBillItemDO::getId, id)
                        .eq(ErpStockOutBillItemDO::getPickedCount, oldPickedCount));
    }

    static SFunction<ErpStockOutBillItemDO, ?> getOrderColumn(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "id":
                return ErpStockOutBillItemDO::getId;
            case "productId":
            case "productCode":
            case "productName":
            case "productUnitName":
                return ErpStockOutBillItemDO::getProductId;
            case "warehouseId":
                return ErpStockOutBillItemDO::getWarehouseId;
            case "count":
                return ErpStockOutBillItemDO::getCount;
            case "pickedCount":
                return ErpStockOutBillItemDO::getPickedCount;
            case "status":
                return ErpStockOutBillItemDO::getStatus;
            case "productPrice":
                return ErpStockOutBillItemDO::getProductPrice;
            case "weight":
                return ErpStockOutBillItemDO::getWeight;
            case "packageQty":
                return ErpStockOutBillItemDO::getPackageQty;
            case "totalWeight":
                return ErpStockOutBillItemDO::getTotalWeight;
            case "sourceNo":
                return ErpStockOutBillItemDO::getSourceNo;
            case "batchNo":
                return ErpStockOutBillItemDO::getBatchNo;
            case "barCode":
                return ErpStockOutBillItemDO::getBarCode;
            case "brand":
                return ErpStockOutBillItemDO::getBrand;
            case "remark":
                return ErpStockOutBillItemDO::getRemark;
            default:
                return null;
        }
    }

}
