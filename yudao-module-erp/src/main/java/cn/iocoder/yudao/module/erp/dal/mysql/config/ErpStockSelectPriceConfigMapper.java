package cn.iocoder.yudao.module.erp.dal.mysql.config;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.config.ErpStockSelectPriceConfigDO;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Mapper
public interface ErpStockSelectPriceConfigMapper extends BaseMapperX<ErpStockSelectPriceConfigDO> {

    default List<ErpStockSelectPriceConfigDO> selectListByFieldKeys(Collection<String> fieldKeys) {
        if (fieldKeys == null || fieldKeys.isEmpty()) {
            return Collections.emptyList();
        }
        return selectList(new LambdaQueryWrapperX<ErpStockSelectPriceConfigDO>()
                .in(ErpStockSelectPriceConfigDO::getFieldKey, fieldKeys)
                .orderByAsc(ErpStockSelectPriceConfigDO::getFieldKey)
                .orderByAsc(ErpStockSelectPriceConfigDO::getBizType));
    }

    default List<ErpStockSelectPriceConfigDO> selectListByBizType(String bizType) {
        return selectList(new LambdaQueryWrapperX<ErpStockSelectPriceConfigDO>()
                .eq(ErpStockSelectPriceConfigDO::getBizType, bizType));
    }

    @Insert("INSERT INTO erp_stock_select_price_config "
            + "(field_key, biz_type, creator, create_time, updater, update_time, deleted, tenant_id) "
            + "VALUES (#{fieldKey}, #{bizType}, #{operator}, NOW(), #{operator}, NOW(), b'0', #{tenantId}) "
            + "ON DUPLICATE KEY UPDATE deleted = b'0', updater = VALUES(updater), update_time = NOW()")
    int restoreOrInsert(@Param("fieldKey") String fieldKey,
                        @Param("bizType") String bizType,
                        @Param("operator") String operator,
                        @Param("tenantId") Long tenantId);

    @Update("UPDATE erp_stock_select_price_config SET deleted = b'1', updater = #{operator}, update_time = NOW() "
            + "WHERE tenant_id = #{tenantId} AND field_key = #{fieldKey} AND biz_type = #{bizType} "
            + "AND deleted = b'0'")
    int softDelete(@Param("fieldKey") String fieldKey,
                   @Param("bizType") String bizType,
                   @Param("operator") String operator,
                   @Param("tenantId") Long tenantId);

    @Delete("<script>"
            + "DELETE FROM erp_stock_select_price_config WHERE tenant_id = #{tenantId} AND field_key IN "
            + "<foreach collection='fieldKeys' item='fieldKey' open='(' separator=',' close=')'>#{fieldKey}</foreach>"
            + "</script>")
    int physicalDeleteByFieldKeys(@Param("fieldKeys") Collection<String> fieldKeys,
                                  @Param("tenantId") Long tenantId);

}
