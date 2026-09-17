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

    @Update("<script>"
            + "UPDATE `${tableName}` "
            + "SET `${codeColumnName}` = #{keepCode}, "
            + "    `${nameColumnName}` = #{keepName}, "
            + "    updater = #{operatorId}, "
            + "    update_time = NOW() "
            + "WHERE `${productIdColumnName}` = #{sourceId} AND deleted = b'0' "
            + "<if test='tenantScoped'>"
            + "AND tenant_id = #{tenantId}"
            + "</if>"
            + "</script>")
    int updateProductStoredIdentityReference(@Param("tableName") String tableName,
                                             @Param("productIdColumnName") String productIdColumnName,
                                             @Param("codeColumnName") String codeColumnName,
                                             @Param("nameColumnName") String nameColumnName,
                                             @Param("sourceId") Long sourceId,
                                             @Param("keepCode") String keepCode,
                                             @Param("keepName") String keepName,
                                             @Param("operatorId") String operatorId,
                                             @Param("tenantId") Long tenantId,
                                             @Param("tenantScoped") boolean tenantScoped);

    @Update("UPDATE erp_stock keep_stock "
            + "JOIN erp_stock source_stock "
            + "  ON source_stock.product_id = #{sourceId} "
            + " AND source_stock.warehouse_id = keep_stock.warehouse_id "
            + " AND source_stock.deleted = b'0' "
            + " AND source_stock.tenant_id = keep_stock.tenant_id "
            + "SET keep_stock.count = COALESCE(keep_stock.count, 0) + COALESCE(source_stock.count, 0), "
            + "    keep_stock.lock_count = COALESCE(keep_stock.lock_count, 0) + COALESCE(source_stock.lock_count, 0), "
            + "    keep_stock.occupied_count = COALESCE(keep_stock.occupied_count, 0) + COALESCE(source_stock.occupied_count, 0), "
            + "    keep_stock.pending_in_count = COALESCE(keep_stock.pending_in_count, 0) + COALESCE(source_stock.pending_in_count, 0), "
            + "    keep_stock.in_transit_count = COALESCE(keep_stock.in_transit_count, 0) + COALESCE(source_stock.in_transit_count, 0), "
            + "    keep_stock.cost_amount = COALESCE(keep_stock.cost_amount, 0) + COALESCE(source_stock.cost_amount, 0), "
            + "    keep_stock.cost_price = CASE "
            + "        WHEN COALESCE(keep_stock.count, 0) + COALESCE(source_stock.count, 0) = 0 THEN 0 "
            + "        ELSE (COALESCE(keep_stock.cost_amount, 0) + COALESCE(source_stock.cost_amount, 0)) "
            + "             / (COALESCE(keep_stock.count, 0) + COALESCE(source_stock.count, 0)) "
            + "    END, "
            + "    keep_stock.purchase_price = COALESCE(keep_stock.purchase_price, source_stock.purchase_price), "
            + "    keep_stock.updater = #{operatorId}, "
            + "    keep_stock.update_time = NOW() "
            + "WHERE keep_stock.product_id = #{keepId} "
            + "  AND keep_stock.deleted = b'0' "
            + "  AND keep_stock.tenant_id = #{tenantId}")
    int mergeProductStockConflict(@Param("sourceId") Long sourceId,
                                  @Param("keepId") Long keepId,
                                  @Param("operatorId") String operatorId,
                                  @Param("tenantId") Long tenantId);

    @Update("UPDATE erp_stock source_stock "
            + "JOIN erp_stock keep_stock "
            + "  ON keep_stock.product_id = #{keepId} "
            + " AND keep_stock.warehouse_id = source_stock.warehouse_id "
            + " AND keep_stock.deleted = b'0' "
            + " AND keep_stock.tenant_id = source_stock.tenant_id "
            + "SET source_stock.deleted = b'1', "
            + "    source_stock.updater = #{operatorId}, "
            + "    source_stock.update_time = NOW() "
            + "WHERE source_stock.product_id = #{sourceId} "
            + "  AND source_stock.deleted = b'0' "
            + "  AND source_stock.tenant_id = #{tenantId}")
    int deleteMergedProductStockConflict(@Param("sourceId") Long sourceId,
                                         @Param("keepId") Long keepId,
                                         @Param("operatorId") String operatorId,
                                         @Param("tenantId") Long tenantId);

    @Update("UPDATE erp_stock_lock keep_lock "
            + "JOIN erp_stock_lock source_lock "
            + "  ON source_lock.product_id = #{sourceId} "
            + " AND source_lock.warehouse_id = keep_lock.warehouse_id "
            + " AND source_lock.biz_type = keep_lock.biz_type "
            + " AND source_lock.biz_id = keep_lock.biz_id "
            + " AND source_lock.biz_item_id = keep_lock.biz_item_id "
            + " AND (source_lock.status = keep_lock.status "
            + "      OR (source_lock.status IS NULL AND keep_lock.status IS NULL)) "
            + " AND source_lock.deleted = b'0' "
            + " AND source_lock.tenant_id = keep_lock.tenant_id "
            + "SET keep_lock.lock_count = COALESCE(keep_lock.lock_count, 0) + COALESCE(source_lock.lock_count, 0), "
            + "    keep_lock.updater = #{operatorId}, "
            + "    keep_lock.update_time = NOW() "
            + "WHERE keep_lock.product_id = #{keepId} "
            + "  AND keep_lock.deleted = b'0' "
            + "  AND keep_lock.tenant_id = #{tenantId}")
    int mergeProductStockLockConflict(@Param("sourceId") Long sourceId,
                                      @Param("keepId") Long keepId,
                                      @Param("operatorId") String operatorId,
                                      @Param("tenantId") Long tenantId);

    @Update("UPDATE erp_stock_lock source_lock "
            + "JOIN erp_stock_lock keep_lock "
            + "  ON keep_lock.product_id = #{keepId} "
            + " AND keep_lock.warehouse_id = source_lock.warehouse_id "
            + " AND keep_lock.biz_type = source_lock.biz_type "
            + " AND keep_lock.biz_id = source_lock.biz_id "
            + " AND keep_lock.biz_item_id = source_lock.biz_item_id "
            + " AND (keep_lock.status = source_lock.status "
            + "      OR (keep_lock.status IS NULL AND source_lock.status IS NULL)) "
            + " AND keep_lock.deleted = b'0' "
            + " AND keep_lock.tenant_id = source_lock.tenant_id "
            + "SET source_lock.deleted = b'1', "
            + "    source_lock.updater = #{operatorId}, "
            + "    source_lock.update_time = NOW() "
            + "WHERE source_lock.product_id = #{sourceId} "
            + "  AND source_lock.deleted = b'0' "
            + "  AND source_lock.tenant_id = #{tenantId}")
    int deleteMergedProductStockLockConflict(@Param("sourceId") Long sourceId,
                                             @Param("keepId") Long keepId,
                                             @Param("operatorId") String operatorId,
                                             @Param("tenantId") Long tenantId);

    @Select("SELECT COUNT(1) FROM information_schema.COLUMNS "
            + "WHERE TABLE_SCHEMA = DATABASE() "
            + "  AND TABLE_NAME = #{tableName} "
            + "  AND COLUMN_NAME = #{columnName}")
    Long selectTableColumnCount(@Param("tableName") String tableName,
                                @Param("columnName") String columnName);

}
