package cn.iocoder.yudao.module.erp.service.finance.payable;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.ErpFinanceUpdateRemarkReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.expense.ErpPayableExpenseDraftSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.expense.ErpPayableExpenseItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.expense.ErpPayableExpensePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.expense.ErpPayableExpenseSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableExpenseDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableExpenseItemDO;
import cn.iocoder.yudao.module.erp.service.finance.bo.ErpSaleCartFreightDraftCreateReqBO;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;

public interface ErpPayableExpenseService {

    Long createPayableExpense(@Valid ErpPayableExpenseSaveReqVO createReqVO);

    Long createPayableExpenseDraft(ErpPayableExpenseDraftSaveReqVO createReqVO);

    Long createAndSubmitPayableExpense(@Valid ErpPayableExpenseSaveReqVO createReqVO);

    Long createFromSaleCartFreight(ErpSaleCartFreightDraftCreateReqBO createReqBO);

    void updatePayableExpense(@Valid ErpPayableExpenseSaveReqVO updateReqVO);

    void updatePayableExpenseDraft(ErpPayableExpenseDraftSaveReqVO updateReqVO);

    void updateAndSubmitPayableExpense(@Valid ErpPayableExpenseSaveReqVO updateReqVO);

    void submitPayableExpense(Long id);

    void updatePayableExpenseRemark(@Valid ErpFinanceUpdateRemarkReqVO updateReqVO);

    void updatePayableExpenseStatus(Long id, Integer status);

    void deletePayableExpense(List<Long> ids);

    ErpPayableExpenseDO getPayableExpense(Long id);

    PageResult<ErpPayableExpenseDO> getPayableExpensePage(ErpPayableExpensePageReqVO pageReqVO);

    List<ErpPayableExpenseItemDO> getPayableExpenseItemListByExpenseId(Long expenseId);

    PageResult<ErpPayableExpenseItemDO> getPayableExpenseItemPage(ErpPayableExpenseItemPageReqVO pageReqVO);

    List<ErpPayableExpenseItemDO> getPayableExpenseItemListByExpenseIds(Collection<Long> expenseIds);

}
