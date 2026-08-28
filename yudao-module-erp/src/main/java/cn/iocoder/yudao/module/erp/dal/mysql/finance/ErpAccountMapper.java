package cn.iocoder.yudao.module.erp.dal.mysql.finance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.account.ErpAccountPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpAccountDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import cn.iocoder.yudao.module.erp.enums.finance.ErpAccountDocumentStatusEnum;
import cn.iocoder.yudao.module.erp.service.finance.bo.ErpAccountBalanceBO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.Collection;
import java.util.List;

/**
 * ERP 结算账户 Mapper
 */
@Mapper
public interface ErpAccountMapper extends BaseMapperX<ErpAccountDO> {

    default PageResult<ErpAccountDO> selectPage(ErpAccountPageReqVO reqVO) {
        LambdaQueryWrapperX<ErpAccountDO> wrapper = new LambdaQueryWrapperX<ErpAccountDO>()
                .likeIfPresent(ErpAccountDO::getName, reqVO.getName())
                .likeIfPresent(ErpAccountDO::getNo, reqVO.getNo())
                .eqIfPresent(ErpAccountDO::getAccountType, reqVO.getAccountType())
                .eqIfPresent(ErpAccountDO::getDeptId, reqVO.getDeptId())
                .eqIfPresent(ErpAccountDO::getStatus, reqVO.getStatus())
                .eqIfPresent(ErpAccountDO::getDocumentStatus, reqVO.getDocumentStatus())
                .likeIfPresent(ErpAccountDO::getRemark, reqVO.getRemark());
        ErpKeywordQuery.appendWithDeptName(wrapper, reqVO.getKeyword(),
                ErpAccountDO::getName, ErpAccountDO::getNo, ErpAccountDO::getBankName,
                ErpAccountDO::getBankAccount, ErpAccountDO::getRemark);
        ErpFinanceSortUtils.apply(wrapper, reqVO.getOrderField(), reqVO.getOrderDirection(), "erp_account",
                "no", "name", "accountType", "bankName", "bankAccount", "deptId", "status",
                "documentStatus", "defaultStatus", "sort", "createTime", "updateTime");
        return selectPage(reqVO, wrapper);
    }

    default ErpAccountDO selectByDefaultStatus() {
        return selectOne(new LambdaQueryWrapperX<ErpAccountDO>()
                .eq(ErpAccountDO::getDefaultStatus, true)
                .eq(ErpAccountDO::getDocumentStatus,
                        ErpAccountDocumentStatusEnum.SUBMITTED.getStatus()));
    }

    default List<ErpAccountDO> selectListByStatus(Integer status) {
        return selectList(new LambdaQueryWrapperX<ErpAccountDO>()
                .eq(ErpAccountDO::getStatus, status)
                .eq(ErpAccountDO::getDocumentStatus,
                        ErpAccountDocumentStatusEnum.SUBMITTED.getStatus()));
    }

    default int updateByIdAndDocumentStatus(Long id, Integer documentStatus, ErpAccountDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<ErpAccountDO>()
                .eq(ErpAccountDO::getId, id)
                .eq(ErpAccountDO::getDocumentStatus, documentStatus));
    }

    /**
     * 按草稿页面快照更新全部可编辑字段。显式 set 允许用户清空草稿中的可选字段。
     */
    default int updateDraftByIdAndDocumentStatus(Long id, Integer documentStatus, ErpAccountDO updateObj) {
        return update(null, new LambdaUpdateWrapper<ErpAccountDO>()
                .set(ErpAccountDO::getName, updateObj.getName())
                .set(ErpAccountDO::getAccountType, updateObj.getAccountType())
                .set(ErpAccountDO::getBankName, updateObj.getBankName())
                .set(ErpAccountDO::getBankAccount, updateObj.getBankAccount())
                .set(ErpAccountDO::getNo, updateObj.getNo())
                .set(ErpAccountDO::getDeptId, updateObj.getDeptId())
                .set(ErpAccountDO::getRemark, updateObj.getRemark())
                .set(ErpAccountDO::getStatus, updateObj.getStatus())
                .set(ErpAccountDO::getSort, updateObj.getSort())
                .set(ErpAccountDO::getDefaultStatus, updateObj.getDefaultStatus())
                .eq(ErpAccountDO::getId, id)
                .eq(ErpAccountDO::getDocumentStatus, documentStatus));
    }

    @Select({
            "<script>",
            "SELECT t.account_id AS accountId, SUM(t.amount) AS currentBalance",
            "  FROM (",
            "        SELECT account_id, SUM(receipt_price) AS amount",
            "          FROM erp_finance_receipt",
            "         WHERE status = 20",
            "         GROUP BY account_id",
            "        UNION ALL",
            "        SELECT account_id, SUM(total_amount) AS amount",
            "          FROM erp_receivable_other_income",
            "         WHERE deleted = 0 AND status = 20",
            "         GROUP BY account_id",
            "        UNION ALL",
            "        SELECT account_id, -SUM(payment_price) AS amount",
            "          FROM erp_finance_payment",
            "         WHERE status = 20",
            "         GROUP BY account_id",
            "        UNION ALL",
            "        SELECT account_id, -SUM(total_amount) AS amount",
            "          FROM erp_payable_expense",
            "         WHERE deleted = 0 AND status = 20",
            "         GROUP BY account_id",
            "        UNION ALL",
            "        SELECT out_account_id AS account_id, -SUM(transfer_price + COALESCE(fee_price, 0)) AS amount",
            "          FROM erp_finance_transfer",
            "         WHERE status = 20",
            "         GROUP BY out_account_id",
            "        UNION ALL",
            "        SELECT in_account_id AS account_id, SUM(transfer_price) AS amount",
            "          FROM erp_finance_transfer",
            "         WHERE status = 20",
            "         GROUP BY in_account_id",
            "       ) t",
            " WHERE t.account_id IN",
            " <foreach collection='ids' item='id' open='(' separator=',' close=')'>",
            "   #{id}",
            " </foreach>",
            " GROUP BY t.account_id",
            "</script>"
    })
    List<ErpAccountBalanceBO> selectAccountBalanceList(@org.apache.ibatis.annotations.Param("ids") Collection<Long> ids);

}
