package cn.iocoder.yudao.module.erp.dal.mysql.base;

import com.baomidou.mybatisplus.annotation.InterceptorIgnore;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;

@Mapper
@InterceptorIgnore(tenantLine = "true", dataPermission = "true")
public interface ErpArchiveMergeReferenceMapper {

    @Update("<script>"
            + "UPDATE `${tableName}` "
            + "SET `${columnName}` = #{keepId}, updater = #{operatorId}, update_time = NOW() "
            + "WHERE `${columnName}` = #{sourceId} AND deleted = b'0' "
            + "<if test='tenantScoped'>"
            + "AND tenant_id = #{tenantId} "
            + "</if>"
            + "<if test='extraColumnName != null and extraColumnName != \"\"'>"
            + "AND `${extraColumnName}` = #{extraColumnValue} "
            + "</if>"
            + "</script>")
    int updateLongReference(@Param("tableName") String tableName,
                            @Param("columnName") String columnName,
                            @Param("sourceId") Long sourceId,
                            @Param("keepId") Long keepId,
                            @Param("extraColumnName") String extraColumnName,
                            @Param("extraColumnValue") Integer extraColumnValue,
                            @Param("operatorId") String operatorId,
                            @Param("tenantId") Long tenantId,
                            @Param("tenantScoped") boolean tenantScoped);

    @Update("<script>"
            + "UPDATE `${tableName}` source "
            + "JOIN `${tableName}` target "
            + "  ON target.`${columnName}` = #{keepId} "
            + " AND target.deleted = b'0' "
            + "<if test='tenantScoped'>"
            + " AND target.tenant_id = source.tenant_id "
            + "</if>"
            + "<foreach collection='matchColumns' item='matchColumn'>"
            + " AND target.`${matchColumn}` &lt;=&gt; source.`${matchColumn}` "
            + "</foreach>"
            + "SET source.deleted = b'1', source.updater = #{operatorId}, source.update_time = NOW() "
            + "WHERE source.`${columnName}` = #{sourceId} AND source.deleted = b'0' "
            + "<if test='tenantScoped'>"
            + "AND source.tenant_id = #{tenantId} "
            + "</if>"
            + "</script>")
    int deleteConflictingLongReference(@Param("tableName") String tableName,
                                       @Param("columnName") String columnName,
                                       @Param("sourceId") Long sourceId,
                                       @Param("keepId") Long keepId,
                                       @Param("matchColumns") Collection<String> matchColumns,
                                       @Param("operatorId") String operatorId,
                                       @Param("tenantId") Long tenantId,
                                       @Param("tenantScoped") boolean tenantScoped);

    @Update("<script>"
            + "UPDATE `${tableName}` source "
            + "JOIN `${tableName}` target "
            + "  ON target.`${columnName}` = #{keepValue} "
            + " AND target.deleted = b'0' "
            + "<if test='tenantScoped'>"
            + " AND target.tenant_id = source.tenant_id "
            + "</if>"
            + "<foreach collection='matchColumns' item='matchColumn'>"
            + " AND target.`${matchColumn}` &lt;=&gt; source.`${matchColumn}` "
            + "</foreach>"
            + "SET source.deleted = b'1', source.updater = #{operatorId}, source.update_time = NOW() "
            + "WHERE source.`${columnName}` = #{sourceValue} AND source.deleted = b'0' "
            + "<if test='tenantScoped'>"
            + "AND source.tenant_id = #{tenantId} "
            + "</if>"
            + "</script>")
    int deleteConflictingStringReference(@Param("tableName") String tableName,
                                         @Param("columnName") String columnName,
                                         @Param("sourceValue") String sourceValue,
                                         @Param("keepValue") String keepValue,
                                         @Param("matchColumns") Collection<String> matchColumns,
                                         @Param("operatorId") String operatorId,
                                         @Param("tenantId") Long tenantId,
                                         @Param("tenantScoped") boolean tenantScoped);

    @Update("<script>"
            + "UPDATE `${tableName}` "
            + "SET `${columnName}` = #{keepValue}, updater = #{operatorId}, update_time = NOW() "
            + "WHERE `${columnName}` = #{sourceValue} AND deleted = b'0' "
            + "<if test='tenantScoped'>"
            + "AND tenant_id = #{tenantId}"
            + "</if>"
            + "</script>")
    int updateStringReference(@Param("tableName") String tableName,
                              @Param("columnName") String columnName,
                              @Param("sourceValue") String sourceValue,
                              @Param("keepValue") String keepValue,
                              @Param("operatorId") String operatorId,
                              @Param("tenantId") Long tenantId,
                              @Param("tenantScoped") boolean tenantScoped);

    @Select("SELECT COUNT(1) FROM erp_stock source_stock "
            + "JOIN erp_stock keep_stock "
            + "  ON keep_stock.warehouse_id = source_stock.warehouse_id "
            + " AND keep_stock.product_id = #{keepId} "
            + " AND keep_stock.deleted = b'0' "
            + " AND keep_stock.tenant_id = source_stock.tenant_id "
            + "WHERE source_stock.product_id = #{sourceId} "
            + "  AND source_stock.deleted = b'0' "
            + "  AND source_stock.tenant_id = #{tenantId}")
    Long selectProductStockWarehouseConflictCount(@Param("sourceId") Long sourceId,
                                                  @Param("keepId") Long keepId,
                                                  @Param("tenantId") Long tenantId);

    @Select("SELECT COUNT(1) FROM erp_stock_lock source_lock "
            + "JOIN erp_stock_lock keep_lock "
            + "  ON keep_lock.warehouse_id = source_lock.warehouse_id "
            + " AND keep_lock.product_id = #{keepId} "
            + " AND keep_lock.status = source_lock.status "
            + " AND keep_lock.deleted = b'0' "
            + " AND keep_lock.tenant_id = source_lock.tenant_id "
            + "WHERE source_lock.product_id = #{sourceId} "
            + "  AND source_lock.deleted = b'0' "
            + "  AND source_lock.tenant_id = #{tenantId}")
    Long selectProductStockLockWarehouseConflictCount(@Param("sourceId") Long sourceId,
                                                      @Param("keepId") Long keepId,
                                                      @Param("tenantId") Long tenantId);

    @Select("SELECT COUNT(1) FROM information_schema.COLUMNS "
            + "WHERE TABLE_SCHEMA = DATABASE() "
            + "  AND TABLE_NAME = #{tableName} "
            + "  AND COLUMN_NAME = #{columnName}")
    Long selectTableColumnCount(@Param("tableName") String tableName,
                                @Param("columnName") String columnName);

}
