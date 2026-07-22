package cn.iocoder.yudao.module.erp.dal.mysql.sale;

import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleBizSourceTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleConvertTypeEnum;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleConvertRecordDO;
import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ERP 销售单据转换记录 Mapper
 */
@Mapper
public interface ErpSaleConvertRecordMapper extends BaseMapperX<ErpSaleConvertRecordDO> {

    default Map<Long, BigDecimal> selectPurchaseInToCartCountMapBySourceItemIds(Collection<Long> sourceItemIds) {
        if (CollUtil.isEmpty(sourceItemIds)) {
            return Collections.emptyMap();
        }
        List<Map<String, Object>> rows = selectPurchaseInToCartCountRows(sourceItemIds,
                ErpSaleConvertTypeEnum.PURCHASE_IN_TO_CART.getType(),
                ErpSaleBizSourceTypeEnum.PURCHASE_IN.getType(),
                ErpSaleBizSourceTypeEnum.CART.getType());
        Map<Long, BigDecimal> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object sourceItemId = row.get("source_item_id");
            Object value = row.get("converted_count");
            if (sourceItemId == null || value == null) {
                continue;
            }
            BigDecimal amount = value instanceof BigDecimal ? (BigDecimal) value : new BigDecimal(value.toString());
            result.put(Long.valueOf(sourceItemId.toString()), amount);
        }
        return result;
    }

    @Select({
            "<script>",
            "SELECT r.source_item_id, SUM(COALESCE(r.count, 0)) AS converted_count",
            "  FROM erp_sale_convert_record r",
            " INNER JOIN erp_sale_cart c ON c.id = r.target_id",
            "   AND c.deleted = 0",
            " WHERE r.deleted = 0",
            "   AND r.convert_type = #{convertType}",
            "   AND r.source_type = #{sourceType}",
            "   AND r.target_type = #{targetType}",
            "   AND r.source_item_id IN",
            "   <foreach collection='sourceItemIds' item='sourceItemId' open='(' separator=',' close=')'>",
            "     #{sourceItemId}",
            "   </foreach>",
            " GROUP BY r.source_item_id",
            "</script>"
    })
    List<Map<String, Object>> selectPurchaseInToCartCountRows(@Param("sourceItemIds") Collection<Long> sourceItemIds,
                                                              @Param("convertType") Integer convertType,
                                                              @Param("sourceType") Integer sourceType,
                                                              @Param("targetType") Integer targetType);

    default int deleteByTarget(Integer targetType, Long targetId) {
        return delete(new LambdaQueryWrapper<ErpSaleConvertRecordDO>()
                .eq(ErpSaleConvertRecordDO::getTargetType, targetType)
                .eq(ErpSaleConvertRecordDO::getTargetId, targetId));
    }

}
