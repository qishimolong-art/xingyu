package cn.iocoder.yudao.module.erp.service.finance.receivable;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherincome.ErpReceivableOtherIncomePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherincome.ErpReceivableOtherIncomeSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableOtherIncomeDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableOtherIncomeItemDO;

import java.util.Collection;
import java.util.List;

public interface ErpReceivableOtherIncomeService {

    Long createOtherIncome(ErpReceivableOtherIncomeSaveReqVO createReqVO);

    void updateOtherIncome(ErpReceivableOtherIncomeSaveReqVO updateReqVO);

    void updateOtherIncomeStatus(Long id, Integer status);

    void deleteOtherIncome(List<Long> ids);

    ErpReceivableOtherIncomeDO getOtherIncome(Long id);

    PageResult<ErpReceivableOtherIncomeDO> getOtherIncomePage(ErpReceivableOtherIncomePageReqVO pageReqVO);

    List<ErpReceivableOtherIncomeItemDO> getOtherIncomeItemListByIncomeId(Long incomeId);

    List<ErpReceivableOtherIncomeItemDO> getOtherIncomeItemListByIncomeIds(Collection<Long> incomeIds);
}
