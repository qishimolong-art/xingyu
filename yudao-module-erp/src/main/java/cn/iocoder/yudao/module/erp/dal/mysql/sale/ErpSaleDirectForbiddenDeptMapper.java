package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleDirectForbiddenDeptDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * ERP 销售直接开单禁用部门 Mapper
 */
@Mapper
public interface ErpSaleDirectForbiddenDeptMapper extends BaseMapperX<ErpSaleDirectForbiddenDeptDO> {

    default List<ErpSaleDirectForbiddenDeptDO> selectListByDeptIds(Collection<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<ErpSaleDirectForbiddenDeptDO>()
                .in(ErpSaleDirectForbiddenDeptDO::getDeptId, deptIds)
                .eq(ErpSaleDirectForbiddenDeptDO::getTenantId, TenantContextHolder.getRequiredTenantId()));
    }

    default List<ErpSaleDirectForbiddenDeptDO> selectListByCurrentTenant() {
        return selectList(new LambdaQueryWrapperX<ErpSaleDirectForbiddenDeptDO>()
                .eq(ErpSaleDirectForbiddenDeptDO::getTenantId, TenantContextHolder.getRequiredTenantId())
                .orderByAsc(ErpSaleDirectForbiddenDeptDO::getDeptId));
    }

    @Delete("DELETE FROM erp_sale_direct_forbidden_dept WHERE tenant_id = #{tenantId}")
    void deleteListByTenantId(@Param("tenantId") Long tenantId);

    @Insert("INSERT IGNORE INTO erp_sale_direct_forbidden_dept "
            + "(dept_id, creator, create_time, updater, update_time, deleted, tenant_id) "
            + "VALUES (#{deptId}, '1', NOW(), '1', NOW(), b'0', #{tenantId})")
    int insertIgnore(@Param("deptId") Long deptId, @Param("tenantId") Long tenantId);

}
