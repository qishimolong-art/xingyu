package cn.iocoder.yudao.module.erp.dal.mysql.purchase;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDeptDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

@Mapper
public interface ErpSupplierDeptMapper extends BaseMapperX<ErpSupplierDeptDO> {

    default List<ErpSupplierDeptDO> selectListBySupplierId(Long supplierId) {
        return selectList(ErpSupplierDeptDO::getSupplierId, supplierId);
    }

    default List<ErpSupplierDeptDO> selectListBySupplierIds(Collection<Long> supplierIds) {
        return selectList(ErpSupplierDeptDO::getSupplierId, supplierIds);
    }

    default int deleteBySupplierId(Long supplierId) {
        return deleteBySupplierId(supplierId, TenantContextHolder.getRequiredTenantId());
    }

    @Delete("DELETE FROM erp_supplier_dept WHERE supplier_id = #{supplierId} AND tenant_id = #{tenantId}")
    int deleteBySupplierId(@Param("supplierId") Long supplierId, @Param("tenantId") Long tenantId);

    default int deleteBySupplierIds(Collection<Long> supplierIds) {
        if (CollUtil.isEmpty(supplierIds)) {
            return 0;
        }
        return deleteBySupplierIds0(supplierIds, TenantContextHolder.getRequiredTenantId());
    }

    @Delete("<script>"
            + "DELETE FROM erp_supplier_dept WHERE tenant_id = #{tenantId} AND supplier_id IN "
            + "<foreach collection='supplierIds' item='supplierId' open='(' separator=',' close=')'>"
            + "#{supplierId}"
            + "</foreach>"
            + "</script>")
    int deleteBySupplierIds0(@Param("supplierIds") Collection<Long> supplierIds, @Param("tenantId") Long tenantId);

}
