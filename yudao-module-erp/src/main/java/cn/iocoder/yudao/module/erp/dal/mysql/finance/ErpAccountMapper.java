package cn.iocoder.yudao.module.erp.dal.mysql.finance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.account.ErpAccountPageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpAccountDO;
import cn.iocoder.yudao.module.erp.service.finance.bo.ErpAccountBalanceBO;
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
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpAccountDO>()
                .likeIfPresent(ErpAccountDO::getName, reqVO.getName())
                .likeIfPresent(ErpAccountDO::getNo, reqVO.getNo())
                .eqIfPresent(ErpAccountDO::getAccountType, reqVO.getAccountType())
                .eqIfPresent(ErpAccountDO::getDeptId, reqVO.getDeptId())
                .eqIfPresent(ErpAccountDO::getStatus, reqVO.getStatus())
                .likeIfPresent(ErpAccountDO::getRemark, reqVO.getRemark())
                .orderByDesc(ErpAccountDO::getId));
    }

    default ErpAccountDO selectByDefaultStatus() {
        return selectOne(ErpAccountDO::getDefaultStatus, true);
    }

    default List<ErpAccountDO> selectListByStatus(Integer status) {
        return selectList(ErpAccountDO::getStatus, status);
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
            "        SELECT out_account_id AS account_id, -SUM(transfer_price) AS amount",
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
