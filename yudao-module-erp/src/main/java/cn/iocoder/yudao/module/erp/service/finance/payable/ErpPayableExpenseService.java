package cn.iocoder.yudao.module.erp.service.finance.payable;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.expense.ErpPayableExpensePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.expense.ErpPayableExpenseSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableExpenseDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableExpenseItemDO;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;

public interface ErpPayableExpenseService {

    Long createPayableExpense(@Valid ErpPayableExpenseSaveReqVO createReqVO);

    void updatePayableExpense(@Valid ErpPayableExpenseSaveReqVO updateReqVO);

    void updatePayableExpenseStatus(Long id, Integer status);

    void deletePayableExpense(List<Long> ids);

    ErpPayableExpenseDO getPayableExpense(Long id);

    PageResult<ErpPayableExpenseDO> getPayableExpensePage(ErpPayableExpensePageReqVO pageReqVO);

    List<ErpPayableExpenseItemDO> getPayableExpenseItemListByExpenseId(Long expenseId);

    List<ErpPayableExpenseItemDO> getPayableExpenseItemListByExpenseIds(Collection<Long> expenseIds);

}
