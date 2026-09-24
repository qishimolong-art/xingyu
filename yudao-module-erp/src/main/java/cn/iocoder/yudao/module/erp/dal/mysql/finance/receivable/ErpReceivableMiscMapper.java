package cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.misc.ErpReceivableMiscPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableMiscDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceSortUtils;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mapper
public interface ErpReceivableMiscMapper extends BaseMapperX<ErpReceivableMiscDO> {

    default PageResult<ErpReceivableMiscDO> selectPage(ErpReceivableMiscPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpReceivableMiscDO> wrapper = new LambdaQueryWrapperX<ErpReceivableMiscDO>()
                .inIfPresent(ErpReceivableMiscDO::getId, reqVO.getIds())
                .likeIfPresent(ErpReceivableMiscDO::getNo, reqVO.getNo())
                .betweenIfPresent(ErpReceivableMiscDO::getBizTime, reqVO.getBizTime())
                .eqIfPresent(ErpReceivableMiscDO::getCustomerId, reqVO.getCustomerId())
                .eqIfPresent(ErpReceivableMiscDO::getAccountId, reqVO.getAccountId())
                .eqIfPresent(ErpReceivableMiscDO::getDeptId, reqVO.getDeptId())
                .eqIfPresent(ErpReceivableMiscDO::getHandlerId, reqVO.getHandlerId())
                .eqIfPresent(ErpReceivableMiscDO::getCreator, reqVO.getCreator())
                .eqIfPresent(ErpReceivableMiscDO::getStatus, reqVO.getStatus())
                .likeIfPresent(ErpReceivableMiscDO::getRemark, reqVO.getRemark());
        ErpKeywordQuery.appendWithDeptNameAndCustomer(wrapper, reqVO.getKeyword(),
                ErpReceivableMiscDO::getNo,
                ErpReceivableMiscDO::getRemark);
        ErpFinanceSortUtils.apply(wrapper, reqVO.getOrderField(), reqVO.getOrderDirection(), "erp_receivable_misc",
                "no", "bizTime", "customerId", "accountId", "deptId", "handlerId", "amount", "status",
                "creator", "updater", "createTime", "updateTime", "remark");
        return selectPage(reqVO, wrapper);
    }

    default ErpReceivableMiscDO selectBySourceItem(String sourceType, Long sourceItemId) {
        if (sourceType == null || sourceItemId == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<ErpReceivableMiscDO>()
                .eq(ErpReceivableMiscDO::getSourceType, sourceType)
                .eq(ErpReceivableMiscDO::getSourceItemId, sourceItemId));
    }

    default ErpReceivableMiscDO selectBySourceDocument(String sourceType, Long sourceId) {
        if (sourceType == null || sourceId == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<ErpReceivableMiscDO>()
                .eq(ErpReceivableMiscDO::getSourceType, sourceType)
                .eq(ErpReceivableMiscDO::getSourceId, sourceId));
    }

    default Map<Long, BigDecimal> selectOffsetAmountSumMapBySourceMiscIds(
            Collection<Long> sourceMiscIds, String sourceType) {
        if (CollUtil.isEmpty(sourceMiscIds)) {
            return new HashMap<>();
        }
        List<Map<String, Object>> rows = selectMaps(new QueryWrapper<ErpReceivableMiscDO>()
                .select("source_misc_id, SUM(ABS(amount)) AS offset_amount_sum")
                .eq("source_type", sourceType)
                .eq("status", ErpAuditStatus.APPROVE.getStatus())
                .lt("amount", BigDecimal.ZERO)
                .in("source_misc_id", sourceMiscIds)
                .groupBy("source_misc_id"));
        Map<Long, BigDecimal> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Number sourceMiscId = (Number) row.get("source_misc_id");
            if (sourceMiscId != null) {
                result.put(sourceMiscId.longValue(), toBigDecimal(row.get("offset_amount_sum")));
            }
        }
        return result;
    }

    default int updateByIdAndStatus(Long id, Integer status, ErpReceivableMiscDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<ErpReceivableMiscDO>()
                .eq(ErpReceivableMiscDO::getId, id)
                .eq(ErpReceivableMiscDO::getStatus, status));
    }

    default ErpReceivableMiscDO selectByNo(String no) {
        return selectOne(ErpReceivableMiscDO::getNo, no);
    }

    default ErpReceivableMiscDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<ErpReceivableMiscDO>()
                .eq(ErpReceivableMiscDO::getId, id)
                .last("FOR UPDATE"));
    }

    default Long selectCountByCustomerId(Long customerId) {
        return selectCount(ErpReceivableMiscDO::getCustomerId, customerId);
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
