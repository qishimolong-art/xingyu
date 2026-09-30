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
public interface ErpPayableMiscMapper extends BaseMapperX<ErpPayableMiscDO> {

    @Select("SELECT * FROM " + ErpMiscSettlementSql.PAYABLE_LEDGER + " misc_ledger ${ew.customSqlSegment}")
    List<ErpMiscSettlementLedgerRow> selectSettlementLedger(@Param(Constants.WRAPPER) Wrapper<ErpPayableMiscDO> query);

    @Select({"SELECT f.id AS documentId, f.no, f.payment_time AS bizTime, s.amount,",
            " f.account_id, f.dept_id, f.finance_user_id AS handlerId, f.status, f.remark",
            " FROM erp_payable_misc m INNER JOIN (" + ErpMiscSettlementSql.PAYABLE_ROWS + ") s",
            " ON s.misc_id = m.id AND s.tenant_id = m.tenant_id",
            " INNER JOIN erp_finance_payment f ON f.id = s.document_id AND f.tenant_id = s.tenant_id",
            " WHERE m.id = #{id} AND m.deleted = 0 AND f.deleted = 0 AND f.status = 20",
            " ORDER BY f.payment_time DESC, f.id DESC"})
    Page<ErpMiscSettlementRespVO> selectSettlementPage(Page<ErpMiscSettlementRespVO> page, @Param("id") Long id);

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
        wrapper.and(q -> q.isNull(ErpPayableMiscDO::getSourceType).or().ne(ErpPayableMiscDO::getSourceType,
                        cn.iocoder.yudao.module.erp.service.finance.ErpMiscTransferOffsetConstants.PAYMENT_OFFSET_SOURCE_TYPE));
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

    // Callers supply ids obtained from the permission-filtered original documents. Count all
    // associated settlements for integrity, even if a finance document has another department.
    @DataPermission(enable = false)
    @Select({"<script>SELECT m.id AS misc_id, COALESCE(s.settled_amount, 0) AS settled_amount FROM erp_payable_misc m",
            " LEFT JOIN " + ErpMiscSettlementSql.PAYABLE_TOTALS + " s ON s.misc_id = m.id AND s.tenant_id = m.tenant_id",
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
    @Select({"<script>SELECT m.id AS misc_id, COALESCE(SUM(f.payment_price), 0) AS pending_amount, m.amount AS original_amount,",
            " SUM(CASE WHEN f.id IS NOT NULL AND (f.payment_price IS NULL OR f.payment_price = 0 OR SIGN(f.payment_price) != SIGN(m.amount)) THEN 1 ELSE 0 END) AS invalid_count",
            " FROM erp_payable_misc m LEFT JOIN erp_finance_payment f",
            " ON f.source_payable_misc_id = m.id AND f.tenant_id = m.tenant_id",
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

    default Long selectCountBySupplierId(Long supplierId) {
        return selectCount(ErpPayableMiscDO::getSupplierId, supplierId);
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
