package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMoveItemPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseMoveItemDO;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface ErpWarehouseMoveItemMapper extends BaseMapperX<ErpWarehouseMoveItemDO> {

    default List<ErpWarehouseMoveItemDO> selectListByMoveId(Long moveId) {
        return selectList(ErpWarehouseMoveItemDO::getMoveId, moveId);
    }

    default PageResult<ErpWarehouseMoveItemDO> selectPageByMoveId(ErpWarehouseMoveItemPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpWarehouseMoveItemDO> query = new LambdaQueryWrapperX<ErpWarehouseMoveItemDO>()
                .eq(ErpWarehouseMoveItemDO::getMoveId, reqVO.getMoveId());
        SFunction<ErpWarehouseMoveItemDO, ?> orderColumn = getOrderColumn(reqVO.getOrderField());
        if (orderColumn == null) {
            query.orderByAsc(ErpWarehouseMoveItemDO::getId);
        } else if ("desc".equalsIgnoreCase(reqVO.getOrderDirection())) {
            query.orderByDesc(orderColumn);
        } else {
            query.orderByAsc(orderColumn);
        }
        if (orderColumn != null && !"id".equals(reqVO.getOrderField().trim())) {
            query.orderByAsc(ErpWarehouseMoveItemDO::getId);
        }
        return selectPage(reqVO, query);
    }

    default List<ErpWarehouseMoveItemDO> selectListByMoveIds(Collection<Long> moveIds) {
        if (moveIds == null || moveIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(ErpWarehouseMoveItemDO::getMoveId, moveIds);
    }

    default int deleteByMoveId(Long moveId) {
        return delete(ErpWarehouseMoveItemDO::getMoveId, moveId);
    }

    static SFunction<ErpWarehouseMoveItemDO, ?> getOrderColumn(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "id":
                return ErpWarehouseMoveItemDO::getId;
            case "productId":
            case "productCode":
            case "productName":
            case "productUnitName":
                return ErpWarehouseMoveItemDO::getProductId;
            case "fromWarehouseId":
                return ErpWarehouseMoveItemDO::getFromWarehouseId;
            case "toWarehouseId":
                return ErpWarehouseMoveItemDO::getToWarehouseId;
            case "count":
                return ErpWarehouseMoveItemDO::getCount;
            case "productPrice":
                return ErpWarehouseMoveItemDO::getProductPrice;
            case "totalPrice":
                return ErpWarehouseMoveItemDO::getTotalPrice;
            case "costPrice":
                return ErpWarehouseMoveItemDO::getCostPrice;
            case "costAmount":
                return ErpWarehouseMoveItemDO::getCostAmount;
            case "weight":
                return ErpWarehouseMoveItemDO::getWeight;
            case "packageQty":
                return ErpWarehouseMoveItemDO::getPackageQty;
            case "totalWeight":
                return ErpWarehouseMoveItemDO::getTotalWeight;
            case "batchNo":
                return ErpWarehouseMoveItemDO::getBatchNo;
            case "fromShelf":
                return ErpWarehouseMoveItemDO::getFromShelf;
            case "toShelf":
                return ErpWarehouseMoveItemDO::getToShelf;
            case "remark":
                return ErpWarehouseMoveItemDO::getRemark;
            default:
                return null;
        }
    }

}
