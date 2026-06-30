package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpUserWarehousePermissionDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * ERP user warehouse permission mapper.
 */
@Mapper
public interface ErpUserWarehousePermissionMapper extends BaseMapperX<ErpUserWarehousePermissionDO> {

    default List<ErpUserWarehousePermissionDO> selectListByUserId(Long userId) {
        return selectList(new LambdaQueryWrapperX<ErpUserWarehousePermissionDO>()
                .eq(ErpUserWarehousePermissionDO::getUserId, userId)
                .eq(ErpUserWarehousePermissionDO::getTenantId, TenantContextHolder.getRequiredTenantId()));
    }

    default List<ErpUserWarehousePermissionDO> selectListByWarehouseId(Long warehouseId) {
        return selectList(new LambdaQueryWrapperX<ErpUserWarehousePermissionDO>()
                .eq(ErpUserWarehousePermissionDO::getWarehouseId, warehouseId)
                .eq(ErpUserWarehousePermissionDO::getTenantId, TenantContextHolder.getRequiredTenantId()));
    }

    @Delete("DELETE FROM erp_user_warehouse_permission WHERE user_id = #{userId} AND tenant_id = #{tenantId}")
    void deleteListByUserId(@Param("userId") Long userId, @Param("tenantId") Long tenantId);

    @Delete("DELETE FROM erp_user_warehouse_permission WHERE warehouse_id = #{warehouseId} AND tenant_id = #{tenantId}")
    void deleteListByWarehouseId(@Param("warehouseId") Long warehouseId, @Param("tenantId") Long tenantId);

    @Delete("<script>"
            + "DELETE FROM erp_user_warehouse_permission "
            + "WHERE tenant_id = #{tenantId} AND warehouse_id IN "
            + "<foreach collection='warehouseIds' item='warehouseId' open='(' separator=',' close=')'>"
            + "#{warehouseId}"
            + "</foreach>"
            + "</script>")
    void deleteListByWarehouseIds(@Param("warehouseIds") Collection<Long> warehouseIds,
                                  @Param("tenantId") Long tenantId);

    @Insert("INSERT IGNORE INTO erp_user_warehouse_permission "
            + "(user_id, warehouse_id, creator, create_time, updater, update_time, deleted, tenant_id) "
            + "VALUES (#{userId}, #{warehouseId}, '1', NOW(), '1', NOW(), b'0', #{tenantId})")
    int insertIgnore(@Param("userId") Long userId, @Param("warehouseId") Long warehouseId,
                     @Param("tenantId") Long tenantId);

}
