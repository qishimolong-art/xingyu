package cn.iocoder.yudao.module.erp.dal.mysql.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * ERP 供应�?Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpSupplierMapper extends BaseMapperX<ErpSupplierDO> {

    default PageResult<ErpSupplierDO> selectPage(ErpSupplierPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpSupplierDO> wrapper = buildPageQuery(reqVO);
        wrapper.ne(ErpSupplierDO::getMergedFlag, Boolean.TRUE);
        orderByIfPresent(wrapper, reqVO);
        return selectPage(reqVO, wrapper);
    }

    default PageResult<ErpSupplierDO> selectVisiblePage(ErpSupplierPageReqVO reqVO, Collection<Long> deptIds,
                                                       Long selfUserId, boolean all) {
        LambdaQueryWrapperX<ErpSupplierDO> wrapper = buildPageQuery(reqVO);
        wrapper.ne(ErpSupplierDO::getMergedFlag, Boolean.TRUE);
        applyVisibleScope(wrapper, deptIds, selfUserId, all);
        orderByIfPresent(wrapper, reqVO);
        return selectPage(reqVO, wrapper);
    }

    static LambdaQueryWrapperX<ErpSupplierDO> buildPageQuery(ErpSupplierPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpSupplierDO> wrapper = new LambdaQueryWrapperX<ErpSupplierDO>()
                .likeIfPresent(ErpSupplierDO::getCode, reqVO.getCode())
                .likeIfPresent(ErpSupplierDO::getName, reqVO.getName())
                .likeIfPresent(ErpSupplierDO::getMobile, reqVO.getMobile())
                .likeIfPresent(ErpSupplierDO::getTelephone, reqVO.getTelephone())
                .eqIfPresent(ErpSupplierDO::getDeptId, reqVO.getDeptId())
                .likeIfPresent(ErpSupplierDO::getRegion, reqVO.getRegion())
                .eqIfPresent(ErpSupplierDO::getSettleMethod, reqVO.getSettleMethod())
                .eqIfPresent(ErpSupplierDO::getPurchaser, reqVO.getPurchaser())
                .eqIfPresent(ErpSupplierDO::getCategory, reqVO.getCategory())
                .likeIfPresent(ErpSupplierDO::getAddress, reqVO.getAddress())
                .likeIfPresent(ErpSupplierDO::getRemark, reqVO.getRemark())
                .eqIfPresent(ErpSupplierDO::getCreator, reqVO.getCreator());
        if (StrUtil.isNotBlank(reqVO.getContactInfo())) {
            wrapper.and(contact -> contact
                    .like(ErpSupplierDO::getMobile, reqVO.getContactInfo())
                    .or()
                    .like(ErpSupplierDO::getTelephone, reqVO.getContactInfo()));
        }
        ErpKeywordQuery.appendWithDeptName(wrapper, reqVO.getKeyword(),
                ErpSupplierDO::getCode, ErpSupplierDO::getName, ErpSupplierDO::getShortName,
                ErpSupplierDO::getOldCode, ErpSupplierDO::getForeignName, ErpSupplierDO::getContact,
                ErpSupplierDO::getMobile, ErpSupplierDO::getTelephone, ErpSupplierDO::getEmail,
                ErpSupplierDO::getFax, ErpSupplierDO::getRegion, ErpSupplierDO::getCategory,
                ErpSupplierDO::getAccount, ErpSupplierDO::getSettleMethod, ErpSupplierDO::getSupplierType,
                ErpSupplierDO::getTransportMethod, ErpSupplierDO::getFreightType, ErpSupplierDO::getWubiCode,
                ErpSupplierDO::getPinyinCode, ErpSupplierDO::getPurchaser, ErpSupplierDO::getCompanyNature,
                ErpSupplierDO::getInvoiceType, ErpSupplierDO::getLogisticsCompany, ErpSupplierDO::getArrivalPoint,
                ErpSupplierDO::getPostalCode, ErpSupplierDO::getAddress, ErpSupplierDO::getProvince,
                ErpSupplierDO::getCity, ErpSupplierDO::getDistrict, ErpSupplierDO::getWebsite,
                ErpSupplierDO::getLegalPerson, ErpSupplierDO::getCreditCode, ErpSupplierDO::getPurchaseControl,
                ErpSupplierDO::getFloatUpdateLastPrice, ErpSupplierDO::getTaxpayerId, ErpSupplierDO::getTaxNo,
                ErpSupplierDO::getBankName, ErpSupplierDO::getBankAccount, ErpSupplierDO::getBankAddress,
                ErpSupplierDO::getInvoiceBank, ErpSupplierDO::getInvoiceBankAccount,
                ErpSupplierDO::getInvoiceAddress, ErpSupplierDO::getInvoicePhone,
                ErpSupplierDO::getInvoiceCompany, ErpSupplierDO::getFinancePhone,
                ErpSupplierDO::getPerformanceProfitRef, ErpSupplierDO::getRemark);
        return wrapper;
    }

    static void orderByIfPresent(LambdaQueryWrapperX<ErpSupplierDO> wrapper, ErpSupplierPageReqVO reqVO) {
        SFunction<ErpSupplierDO, ?> orderColumn = getOrderColumn(reqVO.getOrderField());
        if (orderColumn == null) {
            wrapper.orderByDesc(ErpSupplierDO::getId);
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
        wrapper.orderByDesc(ErpSupplierDO::getId);
    }

    static SFunction<ErpSupplierDO, ?> getOrderColumn(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "code":
                return ErpSupplierDO::getCode;
            case "name":
                return ErpSupplierDO::getName;
            case "status":
                return ErpSupplierDO::getStatus;
            case "region":
                return ErpSupplierDO::getRegion;
            case "address":
                return ErpSupplierDO::getAddress;
            case "remark":
                return ErpSupplierDO::getRemark;
            case "createTime":
                return ErpSupplierDO::getCreateTime;
            case "mobile":
                return ErpSupplierDO::getMobile;
            case "telephone":
                return ErpSupplierDO::getTelephone;
            case "purchaser":
                return ErpSupplierDO::getPurchaser;
            default:
                return null;
        }
    }

    default List<ErpSupplierDO> selectListByStatus(Integer status) {
        return selectList(new LambdaQueryWrapperX<ErpSupplierDO>()
                .eq(ErpSupplierDO::getStatus, status)
                .ne(ErpSupplierDO::getMergedFlag, Boolean.TRUE));
    }

    default List<ErpSupplierDO> selectVisibleListByStatus(Integer status, Collection<Long> deptIds, Long selfUserId, boolean all) {
        LambdaQueryWrapperX<ErpSupplierDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eqIfPresent(ErpSupplierDO::getStatus, status);
        wrapper.ne(ErpSupplierDO::getMergedFlag, Boolean.TRUE);
        applyVisibleScope(wrapper, deptIds, selfUserId, all);
        wrapper.orderByDesc(ErpSupplierDO::getId);
        return selectList(wrapper);
    }

    default List<ErpSupplierDO> selectVisibleListByIds(Collection<Long> ids, Collection<Long> deptIds, Long selfUserId, boolean all) {
        LambdaQueryWrapperX<ErpSupplierDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.in(ErpSupplierDO::getId, ids);
        applyVisibleScope(wrapper, deptIds, selfUserId, all);
        return selectList(wrapper);
    }

    default ErpSupplierDO selectVisibleById(Long id, Collection<Long> deptIds, Long selfUserId, boolean all) {
        LambdaQueryWrapperX<ErpSupplierDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eq(ErpSupplierDO::getId, id);
        applyVisibleScope(wrapper, deptIds, selfUserId, all);
        return selectOne(wrapper);
    }

    default String selectMaxCode() {
        ErpSupplierDO supplier = selectOne(new LambdaQueryWrapperX<ErpSupplierDO>()
                .likeRight(ErpSupplierDO::getCode, "GYS")
                .orderByDesc(ErpSupplierDO::getCode)
                .last("LIMIT 1"));
        return supplier != null ? supplier.getCode() : null;
    }

    default ErpSupplierDO selectByCodeExcludeId(String code, Long excludeId) {
        return selectOne(new LambdaQueryWrapperX<ErpSupplierDO>()
                .eq(ErpSupplierDO::getCode, code)
                .neIfPresent(ErpSupplierDO::getId, excludeId));
    }

    default List<ErpSupplierDO> selectListByNameLike(String name) {
        LambdaQueryWrapperX<ErpSupplierDO> wrapper = new LambdaQueryWrapperX<>();
        appendSimpleKeyword(wrapper, name);
        wrapper.ne(ErpSupplierDO::getMergedFlag, Boolean.TRUE);
        return selectList(wrapper);
    }

    default List<ErpSupplierDO> selectVisibleListByNameLike(String name, Collection<Long> deptIds, Long selfUserId, boolean all) {
        LambdaQueryWrapperX<ErpSupplierDO> wrapper = new LambdaQueryWrapperX<>();
        appendSimpleKeyword(wrapper, name);
        wrapper.ne(ErpSupplierDO::getMergedFlag, Boolean.TRUE);
        applyVisibleScope(wrapper, deptIds, selfUserId, all);
        return selectList(wrapper);
    }

    static void appendSimpleKeyword(LambdaQueryWrapperX<ErpSupplierDO> wrapper, String keyword) {
        String value = ErpKeywordQuery.normalize(keyword);
        if (StrUtil.isBlank(value)) {
            return;
        }
        wrapper.and(w -> w.like(ErpSupplierDO::getName, value)
                .or().like(ErpSupplierDO::getCode, value)
                .or().like(ErpSupplierDO::getShortName, value)
                .or().like(ErpSupplierDO::getContact, value)
                .or().like(ErpSupplierDO::getMobile, value)
                .or().like(ErpSupplierDO::getTelephone, value)
                .or().like(ErpSupplierDO::getPinyinCode, value)
                .or().like(ErpSupplierDO::getWubiCode, value));
    }

    static void applyVisibleScope(LambdaQueryWrapper<ErpSupplierDO> wrapper, Collection<Long> deptIds, Long selfUserId, boolean all) {
        if (all) {
            return;
        }
        wrapper.and(scope -> {
            if (CollUtil.isNotEmpty(deptIds)) {
                scope.in(ErpSupplierDO::getDeptId, deptIds)
                        .or(shared -> shared.eq(ErpSupplierDO::getAllowMultiDept, true)
                                .exists("SELECT 1 FROM erp_supplier_dept esd "
                                        + "WHERE esd.supplier_id = erp_supplier.id "
                                        + "AND esd.deleted = b'0' "
                                        + "AND esd.tenant_id = erp_supplier.tenant_id "
                                        + "AND esd.dept_id IN (" + CollUtil.join(deptIds, ",") + ")"));
            } else {
                scope.apply("1 = 0");
            }
            if (selfUserId != null) {
                scope.or().eq(ErpSupplierDO::getCreator, String.valueOf(selfUserId));
            }
        });
    }

}
