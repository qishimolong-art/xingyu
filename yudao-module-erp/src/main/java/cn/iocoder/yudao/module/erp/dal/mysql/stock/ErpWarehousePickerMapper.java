package cn.iocoder.yudao.module.erp.dal.mysql.stock;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehousePickerDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;
import java.util.List;

/**
 * ERP warehouse picker mapper.
 */
@Mapper
public interface ErpWarehousePickerMapper extends BaseMapperX<ErpWarehousePickerDO> {

    default List<ErpWarehousePickerDO> selectListByUserId(Long userId) {
        return selectList(new LambdaQueryWrapperX<ErpWarehousePickerDO>()
                .eq(ErpWarehousePickerDO::getUserId, userId)
                .eq(ErpWarehousePickerDO::getTenantId, TenantContextHolder.getRequiredTenantId()));
    }

    default List<ErpWarehousePickerDO> selectListByWarehouseId(Long warehouseId) {
        return selectList(new LambdaQueryWrapperX<ErpWarehousePickerDO>()
                .eq(ErpWarehousePickerDO::getWarehouseId, warehouseId)
                .eq(ErpWarehousePickerDO::getTenantId, TenantContextHolder.getRequiredTenantId()));
    }

    @Delete("DELETE FROM erp_warehouse_picker WHERE warehouse_id = #{warehouseId} AND tenant_id = #{tenantId}")
    void deleteListByWarehouseId(@Param("warehouseId") Long warehouseId, @Param("tenantId") Long tenantId);

    @Delete("<script>"
            + "DELETE FROM erp_warehouse_picker "
            + "WHERE tenant_id = #{tenantId} AND warehouse_id IN "
            + "<foreach collection='warehouseIds' item='warehouseId' open='(' separator=',' close=')'>"
            + "#{warehouseId}"
            + "</foreach>"
            + "</script>")
    void deleteListByWarehouseIds(@Param("warehouseIds") Collection<Long> warehouseIds,
                                  @Param("tenantId") Long tenantId);

    @Insert("INSERT IGNORE INTO erp_warehouse_picker "
            + "(warehouse_id, user_id, creator, create_time, updater, update_time, deleted, tenant_id) "
            + "VALUES (#{warehouseId}, #{userId}, '1', NOW(), '1', NOW(), b'0', #{tenantId})")
    int insertIgnore(@Param("warehouseId") Long warehouseId, @Param("userId") Long userId,
                     @Param("tenantId") Long tenantId);

}
