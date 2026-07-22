package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseSaleDeptPermissionDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * ERP warehouse sale department permission mapper.
 */
@Mapper
public interface ErpWarehouseSaleDeptPermissionMapper extends BaseMapperX<ErpWarehouseSaleDeptPermissionDO> {

    default List<ErpWarehouseSaleDeptPermissionDO> selectListByWarehouseId(Long warehouseId) {
        return selectList(new LambdaQueryWrapperX<ErpWarehouseSaleDeptPermissionDO>()
                .eq(ErpWarehouseSaleDeptPermissionDO::getWarehouseId, warehouseId)
                .eq(ErpWarehouseSaleDeptPermissionDO::getTenantId, TenantContextHolder.getRequiredTenantId()));
    }

    default List<ErpWarehouseSaleDeptPermissionDO> selectListByDeptId(Long deptId) {
        return selectList(new LambdaQueryWrapperX<ErpWarehouseSaleDeptPermissionDO>()
                .eq(ErpWarehouseSaleDeptPermissionDO::getDeptId, deptId)
                .eq(ErpWarehouseSaleDeptPermissionDO::getTenantId, TenantContextHolder.getRequiredTenantId()));
    }

    default List<ErpWarehouseSaleDeptPermissionDO> selectListByDeptIds(Collection<Long> deptIds) {
        if (deptIds == null || deptIds.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<ErpWarehouseSaleDeptPermissionDO>()
                .in(ErpWarehouseSaleDeptPermissionDO::getDeptId, deptIds)
                .eq(ErpWarehouseSaleDeptPermissionDO::getTenantId, TenantContextHolder.getRequiredTenantId()));
    }

    default Long selectCountByWarehouseIdAndDeptId(Long warehouseId, Long deptId) {
        return selectCount(new LambdaQueryWrapperX<ErpWarehouseSaleDeptPermissionDO>()
                .eq(ErpWarehouseSaleDeptPermissionDO::getWarehouseId, warehouseId)
                .eq(ErpWarehouseSaleDeptPermissionDO::getDeptId, deptId)
                .eq(ErpWarehouseSaleDeptPermissionDO::getTenantId, TenantContextHolder.getRequiredTenantId()));
    }

    @Delete("DELETE FROM erp_warehouse_sale_dept_permission "
            + "WHERE warehouse_id = #{warehouseId} AND tenant_id = #{tenantId}")
    void deleteListByWarehouseId(@Param("warehouseId") Long warehouseId, @Param("tenantId") Long tenantId);

    @Insert("INSERT IGNORE INTO erp_warehouse_sale_dept_permission "
            + "(warehouse_id, dept_id, creator, create_time, updater, update_time, deleted, tenant_id) "
            + "VALUES (#{warehouseId}, #{deptId}, '1', NOW(), '1', NOW(), b'0', #{tenantId})")
    int insertIgnore(@Param("warehouseId") Long warehouseId, @Param("deptId") Long deptId,
                     @Param("tenantId") Long tenantId);

}
