package cn.iocoder.yudao.module.erp.dal.mysql.finance.payable;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.expense.ErpPayableExpensePageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableExpenseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpKeywordQuery;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.ErpFinanceSortUtils;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ErpPayableExpenseMapper extends BaseMapperX<ErpPayableExpenseDO> {

    default PageResult<ErpPayableExpenseDO> selectPage(ErpPayableExpensePageReqVO reqVO) {
        LambdaQueryWrapperX<ErpPayableExpenseDO> wrapper = new LambdaQueryWrapperX<ErpPayableExpenseDO>()
                .inIfPresent(ErpPayableExpenseDO::getId, reqVO.getIds())
                .likeIfPresent(ErpPayableExpenseDO::getNo, reqVO.getNo())
                .betweenIfPresent(ErpPayableExpenseDO::getBizTime, reqVO.getBizTime())
                .likeIfPresent(ErpPayableExpenseDO::getSettleMethod, reqVO.getSettleMethod())
                .eqIfPresent(ErpPayableExpenseDO::getAccountId, reqVO.getAccountId())
                .likeIfPresent(ErpPayableExpenseDO::getExpenseType, reqVO.getExpenseType())
                .eqIfPresent(ErpPayableExpenseDO::getDeptId, reqVO.getDeptId())
                .eqIfPresent(ErpPayableExpenseDO::getHandlerId, reqVO.getHandlerId())
                .likeIfPresent(ErpPayableExpenseDO::getParty, reqVO.getParty())
                .likeIfPresent(ErpPayableExpenseDO::getRemark, reqVO.getRemark())
                .eqIfPresent(ErpPayableExpenseDO::getStatus, reqVO.getStatus());
        ErpKeywordQuery.appendWithDeptName(wrapper, reqVO.getKeyword(),
                ErpPayableExpenseDO::getNo,
                ErpPayableExpenseDO::getSettleMethod,
                ErpPayableExpenseDO::getVoucherNo,
                ErpPayableExpenseDO::getExpenseType,
                ErpPayableExpenseDO::getParty,
                ErpPayableExpenseDO::getRelatedBiz,
                ErpPayableExpenseDO::getDocType,
                ErpPayableExpenseDO::getRemark);
        ErpFinanceSortUtils.apply(wrapper, reqVO.getOrderField(), reqVO.getOrderDirection(), "erp_payable_expense",
                "no", "bizTime", "settleMethod", "accountId", "expenseBizType", "expenseType", "deptId", "handlerId",
                "party", "totalAmount", "status", "remark", "creator", "relatedBiz", "updater",
                "createTime", "updateTime");
        return selectPage(reqVO, wrapper);
    }

    default int updateByIdAndStatus(Long id, Integer status, ErpPayableExpenseDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<ErpPayableExpenseDO>()
                .eq(ErpPayableExpenseDO::getId, id)
                .eq(ErpPayableExpenseDO::getStatus, status));
    }

    default ErpPayableExpenseDO selectByNo(String no) {
        return selectOne(ErpPayableExpenseDO::getNo, no);
    }

    default ErpPayableExpenseDO selectBySource(String sourceType, Long sourceId) {
        return selectOne(ErpPayableExpenseDO::getSourceType, sourceType,
                ErpPayableExpenseDO::getSourceId, sourceId);
    }

    default ErpPayableExpenseDO selectByIdForUpdate(Long id) {
        return selectOne(new LambdaQueryWrapperX<ErpPayableExpenseDO>()
                .eq(ErpPayableExpenseDO::getId, id)
                .last("FOR UPDATE"));
    }

}
