package cn.iocoder.yudao.module.erp.dal.mysql.finance;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceReceiptItemDO;
import cn.iocoder.yudao.module.erp.enums.finance.ErpFinanceWriteOffStatusEnum;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ERP 收款单项 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpFinanceReceiptItemMapper extends BaseMapperX<ErpFinanceReceiptItemDO> {

    default List<ErpFinanceReceiptItemDO> selectListByReceiptId(Long receiptId) {
        return selectList(new LambdaQueryWrapperX<ErpFinanceReceiptItemDO>()
                .eq(ErpFinanceReceiptItemDO::getReceiptId, receiptId)
                .orderByAsc(ErpFinanceReceiptItemDO::getId));
    }

    default List<ErpFinanceReceiptItemDO> selectListByReceiptIds(Collection<Long> receiptIds) {
        return selectList(ErpFinanceReceiptItemDO::getReceiptId, receiptIds);
    }

    default BigDecimal selectReceiptPriceSumByBizIdAndBizType(Long bizId, Integer bizType) {
        List<Map<String, Object>> result = selectMaps(new QueryWrapper<ErpFinanceReceiptItemDO>()
                .select("COALESCE(SUM(receipt_price), 0) AS receipt_price_sum")
                .eq("biz_id", bizId)
                .eq("biz_type", bizType)
                .eq("write_off_status", ErpFinanceWriteOffStatusEnum.EFFECTIVE.getStatus())
                .inSql("receipt_id", "SELECT id FROM erp_finance_receipt WHERE deleted = 0 AND status = 20"));
        if (CollUtil.isEmpty(result) || result.get(0) == null) {
            return BigDecimal.ZERO;
        }
        return toBigDecimal(result.get(0).get("receipt_price_sum"));
    }

    default Map<Long, BigDecimal> selectReceiptPriceSumMapByBizIdsAndBizType(Collection<Long> bizIds, Integer bizType) {
        if (CollUtil.isEmpty(bizIds)) {
            return new HashMap<>();
        }
        List<Map<String, Object>> result = selectMaps(new QueryWrapper<ErpFinanceReceiptItemDO>()
                .select("biz_id, SUM(receipt_price) AS receipt_price_sum")
                .eq("biz_type", bizType)
                .eq("write_off_status", ErpFinanceWriteOffStatusEnum.EFFECTIVE.getStatus())
                .inSql("receipt_id", "SELECT id FROM erp_finance_receipt WHERE deleted = 0 AND status = 20")
                .in("biz_id", bizIds)
                .groupBy("biz_id"));
        Map<Long, BigDecimal> resultMap = new HashMap<>();
        for (Map<String, Object> row : result) {
            Number bizId = (Number) row.get("biz_id");
            if (bizId != null) {
                resultMap.put(bizId.longValue(), toBigDecimal(row.get("receipt_price_sum")));
            }
        }
        return resultMap;
    }

    default Map<Long, BigDecimal> selectEffectivePriceSumMapByReceiptIds(Collection<Long> receiptIds) {
        if (CollUtil.isEmpty(receiptIds)) {
            return new HashMap<>();
        }
        List<Map<String, Object>> result = selectMaps(new QueryWrapper<ErpFinanceReceiptItemDO>()
                .select("receipt_id, SUM(receipt_price) AS receipt_price_sum")
                .eq("write_off_status", ErpFinanceWriteOffStatusEnum.EFFECTIVE.getStatus())
                .inSql("receipt_id", "SELECT id FROM erp_finance_receipt WHERE deleted = 0 AND status = 20")
                .in("receipt_id", receiptIds)
                .groupBy("receipt_id"));
        Map<Long, BigDecimal> resultMap = new HashMap<>();
        for (Map<String, Object> row : result) {
            Number receiptId = (Number) row.get("receipt_id");
            if (receiptId != null) {
                resultMap.put(receiptId.longValue(), toBigDecimal(row.get("receipt_price_sum")));
            }
        }
        return resultMap;
    }

    default ErpFinanceReceiptItemDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<ErpFinanceReceiptItemDO>()
                .eq(ErpFinanceReceiptItemDO::getId, id)
                .last("FOR UPDATE"));
    }

    static BigDecimal toBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value instanceof BigDecimal) {
            return (BigDecimal) value;
        }
        return new BigDecimal(value.toString());
    }

}
