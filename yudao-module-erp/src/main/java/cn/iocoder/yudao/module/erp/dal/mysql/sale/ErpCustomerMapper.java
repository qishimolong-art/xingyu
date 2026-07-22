package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * ERP 客户 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpCustomerMapper extends BaseMapperX<ErpCustomerDO> {

    default PageResult<ErpCustomerDO> selectPage(ErpCustomerPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpCustomerDO> wrapper = buildPageQuery(reqVO);
        wrapper.ne(ErpCustomerDO::getMergedFlag, Boolean.TRUE);
        orderByIfPresent(wrapper, reqVO);
        return selectPage(reqVO, wrapper);
    }

    default PageResult<ErpCustomerDO> selectVisiblePage(ErpCustomerPageReqVO reqVO, Collection<Long> deptIds,
                                                       Long selfUserId, boolean all) {
        LambdaQueryWrapperX<ErpCustomerDO> wrapper = buildPageQuery(reqVO);
        wrapper.ne(ErpCustomerDO::getMergedFlag, Boolean.TRUE);
        applyVisibleScope(wrapper, deptIds, selfUserId, all);
        orderByIfPresent(wrapper, reqVO);
        return selectPage(reqVO, wrapper);
    }

    static LambdaQueryWrapperX<ErpCustomerDO> buildPageQuery(ErpCustomerPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpCustomerDO> wrapper = new LambdaQueryWrapperX<ErpCustomerDO>()
                .likeIfPresent(ErpCustomerDO::getName, reqVO.getName())
                .likeIfPresent(ErpCustomerDO::getCode, reqVO.getCode())
                .likeIfPresent(ErpCustomerDO::getContact, reqVO.getContact())
                .eqIfPresent(ErpCustomerDO::getMobile, reqVO.getMobile())
                .eqIfPresent(ErpCustomerDO::getTelephone, reqVO.getTelephone())
                .eqIfPresent(ErpCustomerDO::getCustomerType, reqVO.getCustomerType())
                .eqIfPresent(ErpCustomerDO::getDeptId, reqVO.getDeptId())
                .eqIfPresent(ErpCustomerDO::getSettleMethod, reqVO.getSettleMethod())
                .eqIfPresent(ErpCustomerDO::getPriceLevel, reqVO.getPriceLevel())
                .eqIfPresent(ErpCustomerDO::getAreaId, reqVO.getAreaId())
                .eqIfPresent(ErpCustomerDO::getRouteId, reqVO.getRouteId())
                .eqIfPresent(ErpCustomerDO::getSaleUserId, reqVO.getSaleUserId())
                .likeIfPresent(ErpCustomerDO::getRemark, reqVO.getRemark())
                .eqIfPresent(ErpCustomerDO::getCreator, reqVO.getCreator())
                .inIfPresent(ErpCustomerDO::getId, reqVO.getIds());
        if (StrUtil.isNotBlank(reqVO.getAddress())) {
            wrapper.and(address -> address
                    .like(ErpCustomerDO::getDetailAddress, reqVO.getAddress())
                    .or()
                    .like(ErpCustomerDO::getAddress, reqVO.getAddress()));
        }
        ErpKeywordQuery.appendWithDeptName(wrapper, reqVO.getKeyword(),
                ErpCustomerDO::getCode, ErpCustomerDO::getName, ErpCustomerDO::getShortName,
                ErpCustomerDO::getContact, ErpCustomerDO::getMobile, ErpCustomerDO::getTelephone,
                ErpCustomerDO::getEmail, ErpCustomerDO::getFinanceTelephone, ErpCustomerDO::getAddress,
                ErpCustomerDO::getDetailAddress, ErpCustomerDO::getPostCode, ErpCustomerDO::getRemark,
                ErpCustomerDO::getTaxNo, ErpCustomerDO::getAccountName, ErpCustomerDO::getBankName,
                ErpCustomerDO::getBankAccount, ErpCustomerDO::getBankAddress, ErpCustomerDO::getUnifiedCreditCode,
                ErpCustomerDO::getInvoiceBankName, ErpCustomerDO::getInvoiceBankAccount,
                ErpCustomerDO::getInvoiceAddress, ErpCustomerDO::getInvoiceTelephone,
                ErpCustomerDO::getInvoiceCompany, ErpCustomerDO::getWubiCode, ErpCustomerDO::getPinyinCode,
                ErpCustomerDO::getMemberCode, ErpCustomerDO::getPlatformCode);
        return wrapper;
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
            case "priceLevel":
            case "priceLevelName":
                return ErpCustomerDO::getPriceLevel;
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
        return selectList(new LambdaQueryWrapperX<ErpCustomerDO>()
                .eq(ErpCustomerDO::getStatus, status)
                .ne(ErpCustomerDO::getMergedFlag, Boolean.TRUE));
    }

    default List<ErpCustomerDO> selectVisibleListByStatus(Integer status, Collection<Long> deptIds, Long selfUserId, boolean all) {
        LambdaQueryWrapperX<ErpCustomerDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eqIfPresent(ErpCustomerDO::getStatus, status);
        wrapper.ne(ErpCustomerDO::getMergedFlag, Boolean.TRUE);
        applyVisibleScope(wrapper, deptIds, selfUserId, all);
        wrapper.orderByDesc(ErpCustomerDO::getId);
        return selectList(wrapper);
    }

    default List<ErpCustomerDO> selectVisibleListByIds(Collection<Long> ids, Collection<Long> deptIds, Long selfUserId, boolean all) {
        LambdaQueryWrapperX<ErpCustomerDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.in(ErpCustomerDO::getId, ids);
        applyVisibleScope(wrapper, deptIds, selfUserId, all);
        return selectList(wrapper);
    }

    default ErpCustomerDO selectVisibleById(Long id, Collection<Long> deptIds, Long selfUserId, boolean all) {
        LambdaQueryWrapperX<ErpCustomerDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(ErpCustomerDO::getId, id);
        applyVisibleScope(wrapper, deptIds, selfUserId, all);
        return selectOne(wrapper);
    }

    default List<ErpCustomerDO> selectListByNameLike(String name) {
        return selectList(new LambdaQueryWrapperX<ErpCustomerDO>()
                .like(ErpCustomerDO::getName, name)
                .ne(ErpCustomerDO::getMergedFlag, Boolean.TRUE));
    }

    default List<ErpCustomerDO> selectVisibleListByNameLike(String name, Collection<Long> deptIds, Long selfUserId, boolean all) {
        LambdaQueryWrapperX<ErpCustomerDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.like(ErpCustomerDO::getName, name);
        wrapper.ne(ErpCustomerDO::getMergedFlag, Boolean.TRUE);
        applyVisibleScope(wrapper, deptIds, selfUserId, all);
        return selectList(wrapper);
    }

    default ErpCustomerDO selectByCodeExcludeId(String code, Long excludeId) {
        return selectOne(new LambdaQueryWrapperX<ErpCustomerDO>()
                .eq(ErpCustomerDO::getCode, code)
                .neIfPresent(ErpCustomerDO::getId, excludeId));
    }

    static void applyVisibleScope(LambdaQueryWrapper<ErpCustomerDO> wrapper, Collection<Long> deptIds, Long selfUserId, boolean all) {
        if (all) {
            return;
        }
        wrapper.and(scope -> {
            if (CollUtil.isNotEmpty(deptIds)) {
                scope.in(ErpCustomerDO::getDeptId, deptIds)
                        .or(shared -> shared.eq(ErpCustomerDO::getAllowMultiDept, true)
                                .exists("SELECT 1 FROM erp_customer_dept ecd "
                                        + "WHERE ecd.customer_id = erp_customer.id "
                                        + "AND ecd.deleted = b'0' "
                                        + "AND ecd.tenant_id = erp_customer.tenant_id "
                                        + "AND ecd.dept_id IN (" + CollUtil.join(deptIds, ",") + ")"));
            } else {
                scope.apply("1 = 0");
            }
            if (selfUserId != null) {
                scope.or().eq(ErpCustomerDO::getCreator, String.valueOf(selfUserId));
            }
        });
    }

}
