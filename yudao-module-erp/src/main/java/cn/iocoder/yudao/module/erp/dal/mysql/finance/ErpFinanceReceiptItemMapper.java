package cn.iocoder.yudao.module.erp.dal.mysql.finance;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt.ErpFinanceReceiptItemPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceReceiptItemDO;
import cn.iocoder.yudao.module.erp.enums.finance.ErpFinanceWriteOffStatusEnum;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
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

    static String effectiveReceiptPriceSql(int bizType) {
        return "COALESCE((SELECT SUM(fri.receipt_price) FROM erp_finance_receipt_item fri "
                + "INNER JOIN erp_finance_receipt fr ON fr.id = fri.receipt_id "
                + "AND fr.deleted = 0 AND fr.status = 20 AND fr.tenant_id = t.tenant_id "
                + "WHERE fri.deleted = 0 AND fri.write_off_status = 1 "
                + "AND fri.tenant_id = t.tenant_id AND fri.biz_type = " + bizType
                + " AND fri.biz_id = t.id), 0)";
    }

    default List<ErpFinanceReceiptItemDO> selectListByReceiptId(Long receiptId) {
        return selectList(new LambdaQueryWrapperX<ErpFinanceReceiptItemDO>()
                .eq(ErpFinanceReceiptItemDO::getReceiptId, receiptId)
                .orderByAsc(ErpFinanceReceiptItemDO::getId));
    }

    default PageResult<ErpFinanceReceiptItemDO> selectPageByReceiptId(ErpFinanceReceiptItemPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpFinanceReceiptItemDO> query = new LambdaQueryWrapperX<ErpFinanceReceiptItemDO>()
                .eq(ErpFinanceReceiptItemDO::getReceiptId, reqVO.getReceiptId());
        SFunction<ErpFinanceReceiptItemDO, ?> orderColumn = getOrderColumn(reqVO.getOrderField());
        if (orderColumn == null) {
            query.orderByAsc(ErpFinanceReceiptItemDO::getId);
        } else if ("desc".equalsIgnoreCase(reqVO.getOrderDirection())) {
            query.orderByDesc(orderColumn);
        } else {
            query.orderByAsc(orderColumn);
        }
        if (orderColumn != null && !"id".equals(reqVO.getOrderField().trim())) {
            query.orderByAsc(ErpFinanceReceiptItemDO::getId);
        }
        return selectPage(reqVO, query);
    }

    default List<ErpFinanceReceiptItemDO> selectListByReceiptIds(Collection<Long> receiptIds) {
        return selectList(ErpFinanceReceiptItemDO::getReceiptId, receiptIds);
    }

    default int deleteByReceiptId(Long receiptId) {
        return delete(ErpFinanceReceiptItemDO::getReceiptId, receiptId);
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

    static SFunction<ErpFinanceReceiptItemDO, ?> getOrderColumn(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "id":
                return ErpFinanceReceiptItemDO::getId;
            case "bizType":
                return ErpFinanceReceiptItemDO::getBizType;
            case "bizId":
                return ErpFinanceReceiptItemDO::getBizId;
            case "bizNo":
                return ErpFinanceReceiptItemDO::getBizNo;
            case "totalPrice":
                return ErpFinanceReceiptItemDO::getTotalPrice;
            case "receiptedPrice":
                return ErpFinanceReceiptItemDO::getReceiptedPrice;
            case "receiptPrice":
                return ErpFinanceReceiptItemDO::getReceiptPrice;
            case "writeOffStatus":
                return ErpFinanceReceiptItemDO::getWriteOffStatus;
            case "remark":
                return ErpFinanceReceiptItemDO::getRemark;
            default:
                return null;
        }
    }

}
