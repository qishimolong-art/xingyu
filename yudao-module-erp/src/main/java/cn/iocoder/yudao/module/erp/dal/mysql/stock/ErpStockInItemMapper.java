package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.in.ErpStockInItemPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInItemDO;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * ERP 其它入库单项 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpStockInItemMapper extends BaseMapperX<ErpStockInItemDO> {

    default List<ErpStockInItemDO> selectListByInId(Long inId) {
        return selectList(ErpStockInItemDO::getInId, inId);
    }

    default PageResult<ErpStockInItemDO> selectPageByInId(ErpStockInItemPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpStockInItemDO> query = new LambdaQueryWrapperX<ErpStockInItemDO>()
                .eq(ErpStockInItemDO::getInId, reqVO.getInId());
        SFunction<ErpStockInItemDO, ?> orderColumn = getOrderColumn(reqVO.getOrderField());
        if (orderColumn == null) {
            query.orderByAsc(ErpStockInItemDO::getId);
        } else if ("desc".equalsIgnoreCase(reqVO.getOrderDirection())) {
            query.orderByDesc(orderColumn);
        } else {
            query.orderByAsc(orderColumn);
        }
        if (orderColumn != null && !"id".equals(reqVO.getOrderField().trim())) {
            query.orderByAsc(ErpStockInItemDO::getId);
        }
        return selectPage(reqVO, query);
    }

    default List<ErpStockInItemDO> selectListByInIds(Collection<Long> inIds) {
        return selectList(ErpStockInItemDO::getInId, inIds);
    }

    default int deleteByInId(Long inId) {
        return delete(ErpStockInItemDO::getInId, inId);
    }

    default Long selectCountByProductId(Long productId) {
        return selectCount(ErpStockInItemDO::getProductId, productId);
    }

    default Long selectCountByWarehouseId(Long warehouseId) {
        return selectCount(ErpStockInItemDO::getWarehouseId, warehouseId);
    }

    default ErpStockInItemDO selectFirstByWarehouseId(Long warehouseId) {
        return selectFirstOne(ErpStockInItemDO::getWarehouseId, warehouseId);
    }

    static SFunction<ErpStockInItemDO, ?> getOrderColumn(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "id":
                return ErpStockInItemDO::getId;
            case "productId":
            case "productCode":
            case "productName":
            case "productUnitName":
                return ErpStockInItemDO::getProductId;
            case "warehouseId":
                return ErpStockInItemDO::getWarehouseId;
            case "count":
                return ErpStockInItemDO::getCount;
            case "productPrice":
                return ErpStockInItemDO::getProductPrice;
            case "totalPrice":
                return ErpStockInItemDO::getTotalPrice;
            case "weight":
                return ErpStockInItemDO::getWeight;
            case "packageQty":
                return ErpStockInItemDO::getPackageQty;
            case "totalWeight":
                return ErpStockInItemDO::getTotalWeight;
            case "batchNo":
                return ErpStockInItemDO::getBatchNo;
            case "remark":
                return ErpStockInItemDO::getRemark;
            default:
                return null;
        }
    }

}
