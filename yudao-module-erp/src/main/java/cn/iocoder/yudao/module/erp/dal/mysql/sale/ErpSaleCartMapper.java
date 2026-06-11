package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleCartStatusEnum;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;

/**
 * ERP 销售手推车 Mapper
 */
@Mapper
public interface ErpSaleCartMapper extends BaseMapperX<ErpSaleCartDO> {

    default PageResult<ErpSaleCartDO> selectPage(ErpSaleCartPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpSaleCartDO> queryWrapper = new LambdaQueryWrapperX<ErpSaleCartDO>()
                .likeIfPresent(ErpSaleCartDO::getNo, reqVO.getNo())
                .eqIfPresent(ErpSaleCartDO::getCustomerId, reqVO.getCustomerId())
                .eqIfPresent(ErpSaleCartDO::getSaleUserId, reqVO.getSaleUserId())
                .eqIfPresent(ErpSaleCartDO::getDeptId, reqVO.getDeptId())
                .betweenIfPresent(ErpSaleCartDO::getCartTime, reqVO.getCartTime())
                .eqIfPresent(ErpSaleCartDO::getStatus, reqVO.getStatus())
                .likeIfPresent(ErpSaleCartDO::getRemark, reqVO.getRemark())
                .inIfPresent(ErpSaleCartDO::getId, reqVO.getIds());
        if (reqVO.getStatus() == null && Boolean.TRUE.equals(reqVO.getIncludeCompleted())) {
            queryWrapper.in(ErpSaleCartDO::getStatus,
                    ErpSaleCartStatusEnum.FINAL_APPROVE.getStatus(),
                    ErpSaleCartStatusEnum.GENERATED_SALE_OUT.getStatus());
        } else if (reqVO.getStatus() == null) {
            queryWrapper.notIn(ErpSaleCartDO::getStatus,
                    ErpSaleCartStatusEnum.FINAL_APPROVE.getStatus(),
                    ErpSaleCartStatusEnum.GENERATED_SALE_OUT.getStatus());
        }
        orderByIfPresent(queryWrapper, reqVO);
        return selectPage(reqVO, queryWrapper);
    }

    static void orderByIfPresent(LambdaQueryWrapperX<ErpSaleCartDO> wrapper, ErpSaleCartPageReqVO reqVO) {
        SFunction<ErpSaleCartDO, ?> orderColumn = getOrderColumn(reqVO.getOrderField());
        if (orderColumn == null) {
            wrapper.orderByDesc(ErpSaleCartDO::getId);
            return;
        }
        if ("asc".equalsIgnoreCase(reqVO.getOrderDirection())) {
            wrapper.orderByAsc(orderColumn);
            return;
        }
        if ("desc".equalsIgnoreCase(reqVO.getOrderDirection())) {
            wrapper.orderByDesc(orderColumn);
            return;
        }
        wrapper.orderByDesc(ErpSaleCartDO::getId);
    }

    static SFunction<ErpSaleCartDO, ?> getOrderColumn(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "no":
                return ErpSaleCartDO::getNo;
            case "cartTime":
                return ErpSaleCartDO::getCartTime;
            case "customerId":
            case "customerName":
                return ErpSaleCartDO::getCustomerId;
            case "creator":
            case "creatorName":
                return ErpSaleCartDO::getCreator;
            case "totalProductPrice":
                return ErpSaleCartDO::getTotalProductPrice;
            case "discountPrice":
                return ErpSaleCartDO::getDiscountPrice;
            case "totalPrice":
                return ErpSaleCartDO::getTotalPrice;
            case "totalFreight":
                return ErpSaleCartDO::getTotalFreight;
            case "settleMethod":
                return ErpSaleCartDO::getSettleMethod;
            case "deliveryMethod":
                return ErpSaleCartDO::getDeliveryMethod;
            case "logisticsCompany":
                return ErpSaleCartDO::getLogisticsCompany;
            case "saleUserId":
            case "saleUserName":
                return ErpSaleCartDO::getSaleUserId;
            case "deptId":
            case "deptName":
                return ErpSaleCartDO::getDeptId;
            case "priority":
                return ErpSaleCartDO::getPriority;
            case "invoiceType":
                return ErpSaleCartDO::getInvoiceType;
            case "status":
                return ErpSaleCartDO::getStatus;
            case "createTime":
                return ErpSaleCartDO::getCreateTime;
            default:
                return null;
        }
    }

    default ErpSaleCartDO selectByNo(String no) {
        return selectOne(ErpSaleCartDO::getNo, no);
    }

    default Long selectCountByCustomerId(Long customerId) {
        return selectCount(ErpSaleCartDO::getCustomerId, customerId);
    }

    default int updateByIdAndStatus(Long id, Integer status, ErpSaleCartDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<ErpSaleCartDO>()
                .eq(ErpSaleCartDO::getId, id).eq(ErpSaleCartDO::getStatus, status));
    }

}
