package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust.ErpSalePriceAdjustItemPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSalePriceAdjustItemDO;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * ERP 销售调价单明细 Mapper
 *
 * @author 汽配ERP
 */
@Mapper
public interface ErpSalePriceAdjustItemMapper extends BaseMapperX<ErpSalePriceAdjustItemDO> {

    default List<ErpSalePriceAdjustItemDO> selectListByAdjustId(Long adjustId) {
        return selectList(ErpSalePriceAdjustItemDO::getAdjustId, adjustId);
    }

    default PageResult<ErpSalePriceAdjustItemDO> selectPageByAdjustId(ErpSalePriceAdjustItemPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpSalePriceAdjustItemDO> query = new LambdaQueryWrapperX<ErpSalePriceAdjustItemDO>()
                .eq(ErpSalePriceAdjustItemDO::getAdjustId, reqVO.getAdjustId());
        SFunction<ErpSalePriceAdjustItemDO, ?> orderColumn = getOrderColumn(reqVO.getOrderField());
        if (orderColumn == null) {
            query.orderByAsc(ErpSalePriceAdjustItemDO::getId);
        } else if ("desc".equalsIgnoreCase(reqVO.getOrderDirection())) {
            query.orderByDesc(orderColumn);
        } else {
            query.orderByAsc(orderColumn);
        }
        if (orderColumn != null && !"id".equals(reqVO.getOrderField().trim())) {
            query.orderByAsc(ErpSalePriceAdjustItemDO::getId);
        }
        return selectPage(reqVO, query);
    }

    default List<ErpSalePriceAdjustItemDO> selectListByAdjustIds(Collection<Long> adjustIds) {
        return selectList(ErpSalePriceAdjustItemDO::getAdjustId, adjustIds);
    }

    default List<ErpSalePriceAdjustItemDO> selectListBySaleOutItemIds(Collection<Long> saleOutItemIds) {
        return selectList(ErpSalePriceAdjustItemDO::getSaleOutItemId, saleOutItemIds);
    }

    default void deleteByAdjustId(Long adjustId) {
        delete(ErpSalePriceAdjustItemDO::getAdjustId, adjustId);
    }

    static SFunction<ErpSalePriceAdjustItemDO, ?> getOrderColumn(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "id":
                return ErpSalePriceAdjustItemDO::getId;
            case "saleOutNo":
                return ErpSalePriceAdjustItemDO::getSaleOutNo;
            case "partCode":
                return ErpSalePriceAdjustItemDO::getPartCode;
            case "partName":
                return ErpSalePriceAdjustItemDO::getPartName;
            case "vehicleModel":
                return ErpSalePriceAdjustItemDO::getVehicleModel;
            case "originPlace":
                return ErpSalePriceAdjustItemDO::getOriginPlace;
            case "brand":
                return ErpSalePriceAdjustItemDO::getBrand;
            case "unit":
                return ErpSalePriceAdjustItemDO::getUnit;
            case "weight":
                return ErpSalePriceAdjustItemDO::getWeight;
            case "packageQty":
                return ErpSalePriceAdjustItemDO::getPackageQty;
            case "outCount":
                return ErpSalePriceAdjustItemDO::getOutCount;
            case "oldPrice":
                return ErpSalePriceAdjustItemDO::getOldPrice;
            case "newPrice":
                return ErpSalePriceAdjustItemDO::getNewPrice;
            case "adjustPrice":
                return ErpSalePriceAdjustItemDO::getAdjustPrice;
            case "productId":
                return ErpSalePriceAdjustItemDO::getProductId;
            case "deptId":
                return ErpSalePriceAdjustItemDO::getDeptId;
            case "saleOutItemId":
                return ErpSalePriceAdjustItemDO::getSaleOutItemId;
            case "saleOutId":
                return ErpSalePriceAdjustItemDO::getSaleOutId;
            case "adjustReason":
                return ErpSalePriceAdjustItemDO::getAdjustReason;
            case "itemRemark":
                return ErpSalePriceAdjustItemDO::getItemRemark;
            default:
                return null;
        }
    }

}
