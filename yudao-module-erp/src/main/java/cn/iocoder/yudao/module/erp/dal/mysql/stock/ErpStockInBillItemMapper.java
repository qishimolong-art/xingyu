package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.inbill.ErpStockInBillItemPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInBillItemDO;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ErpStockInBillItemMapper extends BaseMapperX<ErpStockInBillItemDO> {

    default List<ErpStockInBillItemDO> selectListByBillId(Long billId) {
        return selectList(new LambdaQueryWrapperX<ErpStockInBillItemDO>()
                .eq(ErpStockInBillItemDO::getBillId, billId)
                .orderByAsc(ErpStockInBillItemDO::getId));
    }

    default PageResult<ErpStockInBillItemDO> selectPageByBillId(ErpStockInBillItemPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpStockInBillItemDO> query = new LambdaQueryWrapperX<ErpStockInBillItemDO>()
                .eq(ErpStockInBillItemDO::getBillId, reqVO.getBillId());
        SFunction<ErpStockInBillItemDO, ?> orderColumn = getOrderColumn(reqVO.getOrderField());
        if (orderColumn == null) {
            query.orderByAsc(ErpStockInBillItemDO::getId);
        } else if ("desc".equalsIgnoreCase(reqVO.getOrderDirection())) {
            query.orderByDesc(orderColumn);
        } else {
            query.orderByAsc(orderColumn);
        }
        if (orderColumn != null && !"id".equals(reqVO.getOrderField().trim())) {
            query.orderByAsc(ErpStockInBillItemDO::getId);
        }
        return selectPage(reqVO, query);
    }

    default List<ErpStockInBillItemDO> selectListByBillIds(Collection<Long> billIds) {
        return selectList(ErpStockInBillItemDO::getBillId, billIds);
    }

    static SFunction<ErpStockInBillItemDO, ?> getOrderColumn(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "id":
                return ErpStockInBillItemDO::getId;
            case "productId":
            case "productCode":
            case "productName":
            case "productUnitName":
                return ErpStockInBillItemDO::getProductId;
            case "warehouseId":
                return ErpStockInBillItemDO::getWarehouseId;
            case "count":
                return ErpStockInBillItemDO::getCount;
            case "pickedCount":
                return ErpStockInBillItemDO::getPickedCount;
            case "status":
                return ErpStockInBillItemDO::getStatus;
            case "productPrice":
                return ErpStockInBillItemDO::getProductPrice;
            case "weight":
                return ErpStockInBillItemDO::getWeight;
            case "packageQty":
                return ErpStockInBillItemDO::getPackageQty;
            case "totalWeight":
                return ErpStockInBillItemDO::getTotalWeight;
            case "sourceNo":
                return ErpStockInBillItemDO::getSourceNo;
            case "batchNo":
                return ErpStockInBillItemDO::getBatchNo;
            case "barCode":
                return ErpStockInBillItemDO::getBarCode;
            case "brand":
                return ErpStockInBillItemDO::getBrand;
            case "remark":
                return ErpStockInBillItemDO::getRemark;
            default:
                return null;
        }
    }

}
