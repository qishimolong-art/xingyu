package cn.iocoder.yudao.module.erp.dal.mysql.finance;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment.ErpFinancePaymentItemPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinancePaymentItemDO;
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
 * ERP 付款单项 Mapper
 *
 * @author 芋道源码
 */
@Mapper
public interface ErpFinancePaymentItemMapper extends BaseMapperX<ErpFinancePaymentItemDO> {

    static String effectivePaymentPriceSql(int bizType) {
        return "COALESCE((SELECT SUM(fpi.payment_price) FROM erp_finance_payment_item fpi "
                + "INNER JOIN erp_finance_payment fp ON fp.id = fpi.payment_id "
                + "AND fp.deleted = 0 AND fp.status = 20 AND fp.tenant_id = t.tenant_id "
                + "WHERE fpi.deleted = 0 AND fpi.write_off_status = 1 "
                + "AND fpi.tenant_id = t.tenant_id AND fpi.biz_type = " + bizType
                + " AND fpi.biz_id = t.id), 0)";
    }

    default List<ErpFinancePaymentItemDO> selectListByPaymentId(Long paymentId) {
        return selectList(new LambdaQueryWrapperX<ErpFinancePaymentItemDO>()
                .eq(ErpFinancePaymentItemDO::getPaymentId, paymentId)
                .orderByAsc(ErpFinancePaymentItemDO::getId));
    }

    default List<ErpFinancePaymentItemDO> selectListByPaymentIdForUpdate(Long paymentId) {
        return selectList(new LambdaQueryWrapperX<ErpFinancePaymentItemDO>()
                .eq(ErpFinancePaymentItemDO::getPaymentId, paymentId)
                .orderByAsc(ErpFinancePaymentItemDO::getId)
                .last("FOR UPDATE"));
    }

    default PageResult<ErpFinancePaymentItemDO> selectPageByPaymentId(ErpFinancePaymentItemPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpFinancePaymentItemDO> query = new LambdaQueryWrapperX<ErpFinancePaymentItemDO>()
                .eq(ErpFinancePaymentItemDO::getPaymentId, reqVO.getPaymentId());
        SFunction<ErpFinancePaymentItemDO, ?> orderColumn = getOrderColumn(reqVO.getOrderField());
        if (orderColumn == null) {
            query.orderByAsc(ErpFinancePaymentItemDO::getId);
        } else if ("desc".equalsIgnoreCase(reqVO.getOrderDirection())) {
            query.orderByDesc(orderColumn);
        } else {
            query.orderByAsc(orderColumn);
        }
        if (orderColumn != null && !"id".equals(reqVO.getOrderField().trim())) {
            query.orderByAsc(ErpFinancePaymentItemDO::getId);
        }
        return selectPage(reqVO, query);
    }

    default List<ErpFinancePaymentItemDO> selectListByPaymentIds(Collection<Long> paymentIds) {
        return selectList(ErpFinancePaymentItemDO::getPaymentId, paymentIds);
    }

    default List<ErpFinancePaymentItemDO> selectListByBizTypeAndBizId(Integer bizType, Long bizId) {
        return selectList(new LambdaQueryWrapperX<ErpFinancePaymentItemDO>()
                .eq(ErpFinancePaymentItemDO::getBizType, bizType)
                .eq(ErpFinancePaymentItemDO::getBizId, bizId)
                .inSql(ErpFinancePaymentItemDO::getPaymentId,
                        "SELECT id FROM erp_finance_payment WHERE deleted = 0 AND status = 20")
                .orderByDesc(ErpFinancePaymentItemDO::getWriteOffTime)
                .orderByDesc(ErpFinancePaymentItemDO::getId));
    }

    default int deleteByPaymentId(Long paymentId) {
        return delete(ErpFinancePaymentItemDO::getPaymentId, paymentId);
    }

    default BigDecimal selectPaymentPriceSumByBizIdAndBizType(Long bizId, Integer bizType) {
        // SQL sum 查询
        List<Map<String, Object>> result = selectMaps(new QueryWrapper<ErpFinancePaymentItemDO>()
                .select("COALESCE(SUM(payment_price), 0) AS payment_price_sum")
                .eq("biz_id", bizId)
                .eq("biz_type", bizType)
                .eq("write_off_status", ErpFinanceWriteOffStatusEnum.EFFECTIVE.getStatus())
                .inSql("payment_id", "SELECT id FROM erp_finance_payment WHERE deleted = 0 AND status = 20"));
        // 获得数量
        if (CollUtil.isEmpty(result) || result.get(0) == null) {
            return BigDecimal.ZERO;
        }
        return toBigDecimal(result.get(0).get("payment_price_sum"));
    }

    default Map<Long, BigDecimal> selectPaymentPriceSumMapByBizIdsAndBizType(Collection<Long> bizIds, Integer bizType) {
        if (CollUtil.isEmpty(bizIds)) {
            return new HashMap<>();
        }
        List<Map<String, Object>> result = selectMaps(new QueryWrapper<ErpFinancePaymentItemDO>()
                .select("biz_id, SUM(payment_price) AS payment_price_sum")
                .eq("biz_type", bizType)
                .eq("write_off_status", ErpFinanceWriteOffStatusEnum.EFFECTIVE.getStatus())
                .inSql("payment_id", "SELECT id FROM erp_finance_payment WHERE deleted = 0 AND status = 20")
                .in("biz_id", bizIds)
                .groupBy("biz_id"));
        Map<Long, BigDecimal> resultMap = new HashMap<>();
        for (Map<String, Object> row : result) {
            Number bizId = (Number) row.get("biz_id");
            if (bizId == null) {
                continue;
            }
            resultMap.put(bizId.longValue(), toBigDecimal(row.get("payment_price_sum")));
        }
        return resultMap;
    }

    default Map<Long, BigDecimal> selectEffectivePriceSumMapByPaymentIds(Collection<Long> paymentIds) {
        if (CollUtil.isEmpty(paymentIds)) {
            return new HashMap<>();
        }
        List<Map<String, Object>> result = selectMaps(new QueryWrapper<ErpFinancePaymentItemDO>()
                .select("payment_id, SUM(payment_price) AS payment_price_sum")
                .eq("write_off_status", ErpFinanceWriteOffStatusEnum.EFFECTIVE.getStatus())
                .inSql("payment_id", "SELECT id FROM erp_finance_payment WHERE deleted = 0 AND status = 20")
                .in("payment_id", paymentIds)
                .groupBy("payment_id"));
        Map<Long, BigDecimal> resultMap = new HashMap<>();
        for (Map<String, Object> row : result) {
            Number paymentId = (Number) row.get("payment_id");
            if (paymentId != null) {
                resultMap.put(paymentId.longValue(), toBigDecimal(row.get("payment_price_sum")));
            }
        }
        return resultMap;
    }

    default Map<Long, Long> selectEffectiveCountMapByPaymentIds(Collection<Long> paymentIds) {
        if (CollUtil.isEmpty(paymentIds)) {
            return new HashMap<>();
        }
        List<Map<String, Object>> result = selectMaps(new QueryWrapper<ErpFinancePaymentItemDO>()
                .select("payment_id, COUNT(1) AS item_count")
                .eq("write_off_status", ErpFinanceWriteOffStatusEnum.EFFECTIVE.getStatus())
                .inSql("payment_id", "SELECT id FROM erp_finance_payment WHERE deleted = 0 AND status = 20")
                .in("payment_id", paymentIds)
                .groupBy("payment_id"));
        Map<Long, Long> resultMap = new HashMap<>();
        for (Map<String, Object> row : result) {
            Number paymentId = (Number) row.get("payment_id");
            Number itemCount = (Number) row.get("item_count");
            if (paymentId != null) {
                resultMap.put(paymentId.longValue(), itemCount == null ? 0L : itemCount.longValue());
            }
        }
        return resultMap;
    }

    default ErpFinancePaymentItemDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<ErpFinancePaymentItemDO>()
                .eq(ErpFinancePaymentItemDO::getId, id)
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

    static SFunction<ErpFinancePaymentItemDO, ?> getOrderColumn(String orderField) {
        if (orderField == null) {
            return null;
        }
        switch (orderField.trim()) {
            case "id":
                return ErpFinancePaymentItemDO::getId;
            case "bizType":
                return ErpFinancePaymentItemDO::getBizType;
            case "bizId":
                return ErpFinancePaymentItemDO::getBizId;
            case "bizNo":
                return ErpFinancePaymentItemDO::getBizNo;
            case "totalPrice":
                return ErpFinancePaymentItemDO::getTotalPrice;
            case "paidPrice":
                return ErpFinancePaymentItemDO::getPaidPrice;
            case "paymentPrice":
                return ErpFinancePaymentItemDO::getPaymentPrice;
            case "writeOffStatus":
                return ErpFinancePaymentItemDO::getWriteOffStatus;
            case "remark":
                return ErpFinancePaymentItemDO::getRemark;
            default:
                return null;
        }
    }

}
