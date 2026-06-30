package cn.iocoder.yudao.module.erp.dal.mysql.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplier.ErpSupplierPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import org.apache.ibatis.annotations.Mapper;

import java.util.Collection;
import java.util.List;

/**
 * ERP 供应商 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpSupplierMapper extends BaseMapperX<ErpSupplierDO> {

    default PageResult<ErpSupplierDO> selectPage(ErpSupplierPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpSupplierDO> wrapper = new LambdaQueryWrapperX<ErpSupplierDO>()
                .likeIfPresent(ErpSupplierDO::getName, reqVO.getName())
                .likeIfPresent(ErpSupplierDO::getMobile, reqVO.getMobile())
                .likeIfPresent(ErpSupplierDO::getTelephone, reqVO.getTelephone())
                .eqIfPresent(ErpSupplierDO::getDeptId, reqVO.getDeptId());
        orderByIfPresent(wrapper, reqVO);
        return selectPage(reqVO, wrapper);
    }

    default PageResult<ErpSupplierDO> selectVisiblePage(ErpSupplierPageReqVO reqVO, Collection<Long> deptIds,
                                                       Long selfUserId, boolean all) {
        LambdaQueryWrapperX<ErpSupplierDO> wrapper = new LambdaQueryWrapperX<ErpSupplierDO>()
                .likeIfPresent(ErpSupplierDO::getName, reqVO.getName())
                .likeIfPresent(ErpSupplierDO::getMobile, reqVO.getMobile())
                .likeIfPresent(ErpSupplierDO::getTelephone, reqVO.getTelephone())
                .eqIfPresent(ErpSupplierDO::getDeptId, reqVO.getDeptId());
        applyVisibleScope(wrapper, deptIds, selfUserId, all);
        orderByIfPresent(wrapper, reqVO);
        return selectPage(reqVO, wrapper);
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
        return selectList(ErpSupplierDO::getStatus, status);
    }

    default List<ErpSupplierDO> selectVisibleListByStatus(Integer status, Collection<Long> deptIds, Long selfUserId, boolean all) {
        LambdaQueryWrapperX<ErpSupplierDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.eqIfPresent(ErpSupplierDO::getStatus, status);
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
        return selectList(new LambdaQueryWrapperX<ErpSupplierDO>()
                .like(ErpSupplierDO::getName, name));
    }

    default List<ErpSupplierDO> selectVisibleListByNameLike(String name, Collection<Long> deptIds, Long selfUserId, boolean all) {
        LambdaQueryWrapperX<ErpSupplierDO> wrapper = new LambdaQueryWrapperX<>();
        wrapper.like(ErpSupplierDO::getName, name);
        applyVisibleScope(wrapper, deptIds, selfUserId, all);
        return selectList(wrapper);
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
