package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

/**
 * ERP 客户 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpCustomerMapper extends BaseMapperX<ErpCustomerDO> {

    default PageResult<ErpCustomerDO> selectPage(ErpCustomerPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpCustomerDO> wrapper = new LambdaQueryWrapperX<ErpCustomerDO>()
                .likeIfPresent(ErpCustomerDO::getName, reqVO.getName())
                .likeIfPresent(ErpCustomerDO::getCode, reqVO.getCode())
                .likeIfPresent(ErpCustomerDO::getContact, reqVO.getContact())
                .eqIfPresent(ErpCustomerDO::getMobile, reqVO.getMobile())
                .eqIfPresent(ErpCustomerDO::getTelephone, reqVO.getTelephone())
                .eqIfPresent(ErpCustomerDO::getCustomerType, reqVO.getCustomerType())
                .eqIfPresent(ErpCustomerDO::getAreaId, reqVO.getAreaId())
                .eqIfPresent(ErpCustomerDO::getRouteId, reqVO.getRouteId())
                .eqIfPresent(ErpCustomerDO::getSaleUserId, reqVO.getSaleUserId())
                .inIfPresent(ErpCustomerDO::getId, reqVO.getIds());
        orderByIfPresent(wrapper, reqVO);
        return selectPage(reqVO, wrapper);
    }

    static void orderByIfPresent(LambdaQueryWrapperX<ErpCustomerDO> wrapper, ErpCustomerPageReqVO reqVO) {
        SFunction<ErpCustomerDO, ?> orderColumn = getOrderColumn(reqVO.getOrderField());
        if (orderColumn == null) {
            wrapper.orderByDesc(ErpCustomerDO::getId);
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
        wrapper.orderByDesc(ErpCustomerDO::getId);
    }

    static SFunction<ErpCustomerDO, ?> getOrderColumn(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "code":
                return ErpCustomerDO::getCode;
            case "name":
                return ErpCustomerDO::getName;
            case "enterpriseMatchStatus":
                return ErpCustomerDO::getEnterpriseMatchStatus;
            case "status":
                return ErpCustomerDO::getStatus;
            case "settleMethod":
                return ErpCustomerDO::getSettleMethod;
            case "wechatService":
                return ErpCustomerDO::getWechatService;
            case "areaId":
            case "areaName":
                return ErpCustomerDO::getAreaId;
            case "routeId":
            case "routeName":
                return ErpCustomerDO::getRouteId;
            case "saleUserId":
            case "saleUserName":
                return ErpCustomerDO::getSaleUserId;
            case "customerType":
                return ErpCustomerDO::getCustomerType;
            case "detailAddress":
                return ErpCustomerDO::getDetailAddress;
            case "address":
                return ErpCustomerDO::getAddress;
            case "remark":
                return ErpCustomerDO::getRemark;
            case "customerTag":
                return ErpCustomerDO::getCustomerTag;
            case "creator":
            case "creatorName":
                return ErpCustomerDO::getCreator;
            case "createTime":
                return ErpCustomerDO::getCreateTime;
            case "mobile":
            case "customerTel":
                return ErpCustomerDO::getMobile;
            default:
                return null;
        }
    }

    default List<ErpCustomerDO> selectListByStatus(Integer status) {
        return selectList(ErpCustomerDO::getStatus, status);
    }

    default List<ErpCustomerDO> selectListByNameLike(String name) {
        return selectList(new LambdaQueryWrapperX<ErpCustomerDO>()
                .like(ErpCustomerDO::getName, name));
    }

}
