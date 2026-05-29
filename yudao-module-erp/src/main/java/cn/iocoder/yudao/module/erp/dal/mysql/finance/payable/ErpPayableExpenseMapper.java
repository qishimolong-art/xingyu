package cn.iocoder.yudao.module.erp.dal.mysql.finance.payable;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.mybatis.core.mapper.BaseMapperX;
import cn.iocoder.yudao.framework.mybatis.core.query.LambdaQueryWrapperX;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.expense.ErpPayableExpensePageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableExpenseDO;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ErpPayableExpenseMapper extends BaseMapperX<ErpPayableExpenseDO> {

    default PageResult<ErpPayableExpenseDO> selectPage(ErpPayableExpensePageReqVO reqVO) {
        return selectPage(reqVO, new LambdaQueryWrapperX<ErpPayableExpenseDO>()
                .likeIfPresent(ErpPayableExpenseDO::getNo, reqVO.getNo())
                .betweenIfPresent(ErpPayableExpenseDO::getBizTime, reqVO.getBizTime())
                .likeIfPresent(ErpPayableExpenseDO::getSettleMethod, reqVO.getSettleMethod())
                .eqIfPresent(ErpPayableExpenseDO::getAccountId, reqVO.getAccountId())
                .likeIfPresent(ErpPayableExpenseDO::getExpenseType, reqVO.getExpenseType())
                .eqIfPresent(ErpPayableExpenseDO::getDeptId, reqVO.getDeptId())
                .eqIfPresent(ErpPayableExpenseDO::getHandlerId, reqVO.getHandlerId())
                .likeIfPresent(ErpPayableExpenseDO::getParty, reqVO.getParty())
                .eqIfPresent(ErpPayableExpenseDO::getStatus, reqVO.getStatus())
                .orderByDesc(ErpPayableExpenseDO::getId));
    }

    default int updateByIdAndStatus(Long id, Integer status, ErpPayableExpenseDO updateObj) {
        return update(updateObj, new LambdaUpdateWrapper<ErpPayableExpenseDO>()
                .eq(ErpPayableExpenseDO::getId, id)
                .eq(ErpPayableExpenseDO::getStatus, status));
    }

    default ErpPayableExpenseDO selectByNo(String no) {
        return selectOne(ErpPayableExpenseDO::getNo, no);
    }

}
