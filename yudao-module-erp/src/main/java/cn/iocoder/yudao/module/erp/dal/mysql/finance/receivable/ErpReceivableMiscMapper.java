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
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpMiscSettlementLedgerRow;
import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import cn.iocoder.yudao.framework.datapermission.core.annotation.DataPermission;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.ErpMiscSettlementRespVO;

import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpMiscSettlementSql;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mapper
public interface ErpReceivableMiscMapper extends BaseMapperX<ErpReceivableMiscDO> {

    @Select("SELECT * FROM " + ErpMiscSettlementSql.RECEIVABLE_LEDGER + " misc_ledger ${ew.customSqlSegment}")
    List<ErpMiscSettlementLedgerRow> selectSettlementLedger(@Param(Constants.WRAPPER) Wrapper<ErpReceivableMiscDO> query);

    @Select({"SELECT f.id AS documentId, f.no, f.receipt_time AS bizTime, s.amount,",
            " f.account_id, f.dept_id, f.finance_user_id AS handlerId, f.status, f.remark",
            " FROM erp_receivable_misc m INNER JOIN (" + ErpMiscSettlementSql.RECEIVABLE_ROWS + ") s",
            " ON s.misc_id = m.id AND s.tenant_id = m.tenant_id",
            " INNER JOIN erp_finance_receipt f ON f.id = s.document_id AND f.tenant_id = s.tenant_id",
            " WHERE m.id = #{id} AND m.deleted = 0 AND f.deleted = 0 AND f.status = 20",
            " ORDER BY f.receipt_time DESC, f.id DESC"})
    Page<ErpMiscSettlementRespVO> selectSettlementPage(Page<ErpMiscSettlementRespVO> page, @Param("id") Long id);

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
        wrapper.and(q -> q.isNull(ErpReceivableMiscDO::getSourceType).or().ne(ErpReceivableMiscDO::getSourceType,
                        cn.iocoder.yudao.module.erp.service.finance.ErpMiscTransferOffsetConstants.RECEIPT_OFFSET_SOURCE_TYPE));
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

    // Callers supply ids obtained from the permission-filtered original documents. Count all
    // associated settlements for integrity, even if a finance document has another department.
    @DataPermission(enable = false)
    @Select({"<script>SELECT m.id AS misc_id, COALESCE(s.settled_amount, 0) AS settled_amount FROM erp_receivable_misc m",
            " LEFT JOIN " + ErpMiscSettlementSql.RECEIVABLE_TOTALS + " s ON s.misc_id = m.id AND s.tenant_id = m.tenant_id",
            " WHERE m.deleted = 0 AND m.id IN <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach></script>"})
    List<Map<String, Object>> selectSettlementTotals(@Param("ids") Collection<Long> ids);

    default Map<Long, BigDecimal> selectSettlementAmountSumMapBySourceMiscIds(
            Collection<Long> sourceMiscIds, String sourceType) {
        if (CollUtil.isEmpty(sourceMiscIds)) {
            return new HashMap<>();
        }
        List<Map<String, Object>> rows = selectSettlementTotals(sourceMiscIds);
        Map<Long, BigDecimal> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Number id = (Number) row.get("misc_id");
            if (id != null) result.put(id.longValue(), toBigDecimal(row.get("settled_amount")));
        }
        return result;
    }


    // Integrity queries retain tenant isolation but include reservations in other departments.
    @DataPermission(enable = false)
    @Select({"<script>SELECT m.id AS misc_id, COALESCE(SUM(f.receipt_price), 0) AS pending_amount, m.amount AS original_amount,",
            " SUM(CASE WHEN f.id IS NOT NULL AND (f.receipt_price IS NULL OR f.receipt_price = 0 OR SIGN(f.receipt_price) != SIGN(m.amount)) THEN 1 ELSE 0 END) AS invalid_count",
            " FROM erp_receivable_misc m LEFT JOIN erp_finance_receipt f",
            " ON f.source_receivable_misc_id = m.id AND f.tenant_id = m.tenant_id",
            " AND f.deleted = 0 AND f.status = 10",
            " <if test='excludeId != null'>AND f.id != #{excludeId}</if>",
            " WHERE m.deleted = 0 AND m.id IN <foreach collection='ids' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            " GROUP BY m.id, m.amount</script>"})
    List<Map<String, Object>> selectPendingTransferTotals(@Param("ids") Collection<Long> ids,
                                                        @Param("excludeId") Long excludeId);

    default Map<Long, BigDecimal> selectPendingTransferAmounts(Collection<Long> ids, Long excludeId) {
        Map<Long, BigDecimal> result = new HashMap<>();
        if (CollUtil.isEmpty(ids)) return result;
        for (Map<String, Object> row : selectPendingTransferTotals(ids, excludeId)) {
            Number id = (Number) row.get("misc_id");
            if (id != null) result.put(id.longValue(), toBigDecimal(row.get("pending_amount")));
        }
        return result;
    }

    default java.util.Set<Long> selectInvalidPendingSourceIds(Collection<Long> ids, Long excludeId) {
        java.util.Set<Long> invalid = new java.util.HashSet<>();
        if (CollUtil.isEmpty(ids)) return invalid;
        for (Map<String, Object> row : selectPendingTransferTotals(ids, excludeId)) {
            Number count = (Number) row.get("invalid_count");
            if (count != null && count.longValue() > 0) invalid.add(((Number) row.get("misc_id")).longValue());
        }
        return invalid;
    }

    default BigDecimal selectPendingTransferAmount(Long id, Long excludeId) {
        return selectPendingTransferAmounts(java.util.Collections.singleton(id), excludeId)
                .getOrDefault(id, BigDecimal.ZERO);
    }

    default Map<Long, BigDecimal> selectOccupiedAmounts(Collection<Long> ids, String sourceType) {
        Map<Long, BigDecimal> result = selectSettlementAmountSumMapBySourceMiscIds(ids, sourceType);
        if (CollUtil.isEmpty(ids)) return result;
        for (Map<String, Object> row : selectPendingTransferTotals(ids, null)) {
            Long id = ((Number) row.get("misc_id")).longValue();
            Number invalid = (Number) row.get("invalid_count");
            if (invalid != null && invalid.longValue() > 0) result.put(id, toBigDecimal(row.get("original_amount")));
            else result.merge(id, toBigDecimal(row.get("pending_amount")), BigDecimal::add);
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
