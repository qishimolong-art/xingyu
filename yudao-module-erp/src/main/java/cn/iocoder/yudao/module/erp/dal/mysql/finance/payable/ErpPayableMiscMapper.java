package cn.iocoder.yudao.module.erp.dal.mysql.finance.payable;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.misc.ErpPayableMiscPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableMiscDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceSortUtils;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mapper
public interface ErpPayableMiscMapper extends BaseMapperX<ErpPayableMiscDO> {

    default PageResult<ErpPayableMiscDO> selectPage(ErpPayableMiscPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpPayableMiscDO> wrapper = new LambdaQueryWrapperX<ErpPayableMiscDO>()
                .inIfPresent(ErpPayableMiscDO::getId, reqVO.getIds())
                .likeIfPresent(ErpPayableMiscDO::getNo, reqVO.getNo())
                .betweenIfPresent(ErpPayableMiscDO::getBizTime, reqVO.getBizTime())
                .eqIfPresent(ErpPayableMiscDO::getSupplierId, reqVO.getSupplierId())
                .eqIfPresent(ErpPayableMiscDO::getAccountId, reqVO.getAccountId())
                .eqIfPresent(ErpPayableMiscDO::getDeptId, reqVO.getDeptId())
                .eqIfPresent(ErpPayableMiscDO::getHandlerId, reqVO.getHandlerId())
                .eqIfPresent(ErpPayableMiscDO::getCreator, reqVO.getCreator())
                .eqIfPresent(ErpPayableMiscDO::getStatus, reqVO.getStatus())
                .likeIfPresent(ErpPayableMiscDO::getRemark, reqVO.getRemark());
        ErpKeywordQuery.appendWithDeptNameAndSupplier(wrapper, reqVO.getKeyword(),
                ErpPayableMiscDO::getNo,
                ErpPayableMiscDO::getRemark);
        ErpFinanceSortUtils.apply(wrapper, reqVO.getOrderField(), reqVO.getOrderDirection(), "erp_payable_misc",
                "no", "bizTime", "supplierId", "accountId", "deptId", "handlerId", "amount", "status",
                "creator", "updater", "createTime", "updateTime", "remark");
        return selectPage(reqVO, wrapper);
    }

    default ErpPayableMiscDO selectBySourceItem(String sourceType, Long sourceItemId) {
        if (sourceType == null || sourceItemId == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<ErpPayableMiscDO>()
                .eq(ErpPayableMiscDO::getSourceType, sourceType)
                .eq(ErpPayableMiscDO::getSourceItemId, sourceItemId));
    }

    default ErpPayableMiscDO selectBySourceDocument(String sourceType, Long sourceId) {
        if (sourceType == null || sourceId == null) {
            return null;
        }
        return selectOne(new LambdaQueryWrapperX<ErpPayableMiscDO>()
                .eq(ErpPayableMiscDO::getSourceType, sourceType)
                .eq(ErpPayableMiscDO::getSourceId, sourceId));
    }

    default Map<Long, BigDecimal> selectOffsetAmountSumMapBySourceMiscIds(
            Collection<Long> sourceMiscIds, String sourceType) {
        if (CollUtil.isEmpty(sourceMiscIds)) {
            return new HashMap<>();
        }
        List<Map<String, Object>> rows = selectMaps(new QueryWrapper<ErpPayableMiscDO>()
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

    default int updateByIdAndStatus(Long id, Integer status, ErpPayableMiscDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<ErpPayableMiscDO>()
                .eq(ErpPayableMiscDO::getId, id)
                .eq(ErpPayableMiscDO::getStatus, status));
    }

    default ErpPayableMiscDO selectByNo(String no) {
        return selectOne(ErpPayableMiscDO::getNo, no);
    }

    default ErpPayableMiscDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<ErpPayableMiscDO>()
                .eq(ErpPayableMiscDO::getId, id)
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
