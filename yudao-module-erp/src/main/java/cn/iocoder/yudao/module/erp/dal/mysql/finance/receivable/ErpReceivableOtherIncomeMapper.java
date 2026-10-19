package cn.iocoder.yudao.module.erp.dal.mysql.finance.receivable;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.MPJLambdaWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.ErpFinanceQuerySummaryRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherincome.ErpReceivableOtherIncomePageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableOtherIncomeDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceSortUtils;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ErpReceivableOtherIncomeMapper extends BaseMapperX<ErpReceivableOtherIncomeDO> {

    default PageResult<ErpReceivableOtherIncomeDO> selectPage(ErpReceivableOtherIncomePageReqVO reqVO) {
        MPJLambdaWrapperX<ErpReceivableOtherIncomeDO> wrapper = buildQuery(reqVO);
        ErpFinanceSortUtils.apply(wrapper, reqVO.getOrderField(), reqVO.getOrderDirection(), "erp_receivable_other_income",
                "no", "bizTime", "incomeType", "accountId", "deptId", "handlerId", "party",
                "totalAmount", "status", "remark", "creator", "relatedBiz", "updater", "createTime", "updateTime");
        return selectJoinPage(reqVO, ErpReceivableOtherIncomeDO.class, wrapper);
    }

    default ErpFinanceQuerySummaryRespVO selectSummary(ErpReceivableOtherIncomePageReqVO reqVO) {
        MPJLambdaWrapperX<ErpReceivableOtherIncomeDO> wrapper = buildQuery(reqVO)
                .selectAs("COUNT(*)", ErpFinanceQuerySummaryRespVO::getDocumentCount)
                .selectAs("COALESCE(SUM(t.total_amount), 0)", ErpFinanceQuerySummaryRespVO::getTotalAmount);
        return selectJoinOne(ErpFinanceQuerySummaryRespVO.class, wrapper);
    }

    static MPJLambdaWrapperX<ErpReceivableOtherIncomeDO> buildQuery(ErpReceivableOtherIncomePageReqVO reqVO) {
        MPJLambdaWrapperX<ErpReceivableOtherIncomeDO> wrapper = new MPJLambdaWrapperX<ErpReceivableOtherIncomeDO>()
                .inIfPresent(ErpReceivableOtherIncomeDO::getId, reqVO.getIds())
                .likeIfPresent(ErpReceivableOtherIncomeDO::getNo, reqVO.getNo())
                .betweenIfPresent(ErpReceivableOtherIncomeDO::getBizTime, reqVO.getBizTime())
                .likeIfPresent(ErpReceivableOtherIncomeDO::getIncomeType, reqVO.getIncomeType())
                .eqIfPresent(ErpReceivableOtherIncomeDO::getAccountId, reqVO.getAccountId())
                .likeIfPresent(ErpReceivableOtherIncomeDO::getVoucherNo, reqVO.getVoucherNo())
                .eqIfPresent(ErpReceivableOtherIncomeDO::getDeptId, reqVO.getDeptId())
                .eqIfPresent(ErpReceivableOtherIncomeDO::getHandlerId, reqVO.getHandlerId())
                .likeIfPresent(ErpReceivableOtherIncomeDO::getParty, reqVO.getParty())
                .eqIfPresent(ErpReceivableOtherIncomeDO::getStatus, reqVO.getStatus())
                .likeIfPresent(ErpReceivableOtherIncomeDO::getRemark, reqVO.getRemark())
                .eqIfPresent(ErpReceivableOtherIncomeDO::getCreator, reqVO.getCreator());
        if (reqVO.getItemName() != null && !reqVO.getItemName().trim().isEmpty()) {
            wrapper.apply("EXISTS (SELECT 1 FROM erp_receivable_other_income_item i WHERE i.income_id = t.id "
                    + "AND i.tenant_id = t.tenant_id AND i.deleted = b'0' AND i.item_name LIKE {0})",
                    "%" + reqVO.getItemName().trim() + "%");
        }
        if (reqVO.getInvoiceNo() != null && !reqVO.getInvoiceNo().trim().isEmpty()) {
            wrapper.apply("EXISTS (SELECT 1 FROM erp_receivable_other_income_item i WHERE i.income_id = t.id "
                    + "AND i.tenant_id = t.tenant_id AND i.deleted = b'0' AND i.invoice_no LIKE {0})",
                    "%" + reqVO.getInvoiceNo().trim() + "%");
        }
        ErpKeywordQuery.appendWithDeptName(wrapper, reqVO.getKeyword(),
                ErpReceivableOtherIncomeDO::getNo,
                ErpReceivableOtherIncomeDO::getSettleMethod,
                ErpReceivableOtherIncomeDO::getVoucherNo,
                ErpReceivableOtherIncomeDO::getIncomeType,
                ErpReceivableOtherIncomeDO::getParty,
                ErpReceivableOtherIncomeDO::getRelatedBiz,
                ErpReceivableOtherIncomeDO::getDocType,
                ErpReceivableOtherIncomeDO::getRemark);
        return wrapper;
    }

    default int updateByIdAndStatus(Long id, Integer status, ErpReceivableOtherIncomeDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<ErpReceivableOtherIncomeDO>()
                .eq(ErpReceivableOtherIncomeDO::getId, id)
                .eq(ErpReceivableOtherIncomeDO::getStatus, status));
    }

    default ErpReceivableOtherIncomeDO selectByNo(String no) {
        return selectOne(ErpReceivableOtherIncomeDO::getNo, no);
    }

    default ErpReceivableOtherIncomeDO selectBySource(String sourceType, Long sourceId) {
        return selectOne(ErpReceivableOtherIncomeDO::getSourceType, sourceType,
                ErpReceivableOtherIncomeDO::getSourceId, sourceId);
    }

    default ErpReceivableOtherIncomeDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<ErpReceivableOtherIncomeDO>()
                .eq(ErpReceivableOtherIncomeDO::getId, id)
                .last("FOR UPDATE"));
    }
}
