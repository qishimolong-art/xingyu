package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDeptDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ErpCustomerDeptMapper extends BaseMapperX<ErpCustomerDeptDO> {

    default List<ErpCustomerDeptDO> selectListByCustomerId(Long customerId) {
        return selectList(ErpCustomerDeptDO::getCustomerId, customerId);
    }

    default List<ErpCustomerDeptDO> selectListByCustomerIds(Collection<Long> customerIds) {
        return selectList(ErpCustomerDeptDO::getCustomerId, customerIds);
    }

    default int deleteByCustomerId(Long customerId) {
        return deleteByCustomerId(customerId, TenantContextHolder.getRequiredTenantId());
    }

    @Delete("DELETE FROM erp_customer_dept WHERE customer_id = #{customerId} AND tenant_id = #{tenantId}")
    int deleteByCustomerId(@Param("customerId") Long customerId, @Param("tenantId") Long tenantId);

    default int deleteByCustomerIds(Collection<Long> customerIds) {
        if (CollUtil.isEmpty(customerIds)) {
            return 0;
        }
        return deleteByCustomerIds0(customerIds, TenantContextHolder.getRequiredTenantId());
    }

    @Delete("<script>"
            + "DELETE FROM erp_customer_dept WHERE tenant_id = #{tenantId} AND customer_id IN "
            + "<foreach collection='customerIds' item='customerId' open='(' separator=',' close=')'>"
            + "#{customerId}"
            + "</foreach>"
            + "</script>")
    int deleteByCustomerIds0(@Param("customerIds") Collection<Long> customerIds, @Param("tenantId") Long tenantId);

}
