package cn.iocoder.yudao.module.erp.service.finance.payable;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.ErpFinanceUpdateRemarkReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.other.ErpPayableOtherDraftSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.other.ErpPayableOtherPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.other.ErpPayableOtherSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableOtherDO;

import javax.validation.Valid;

public interface ErpPayableOtherService {

    Long createPayableOther(@Valid ErpPayableOtherSaveReqVO createReqVO);

    Long createPayableOtherDraft(ErpPayableOtherDraftSaveReqVO createReqVO);

    Long createAndSubmitPayableOther(@Valid ErpPayableOtherSaveReqVO createReqVO);

    void updatePayableOther(@Valid ErpPayableOtherSaveReqVO updateReqVO);

    void updatePayableOtherDraft(ErpPayableOtherDraftSaveReqVO updateReqVO);

    void updateAndSubmitPayableOther(@Valid ErpPayableOtherSaveReqVO updateReqVO);

    void submitPayableOther(Long id);

    void updatePayableOtherRemark(@Valid ErpFinanceUpdateRemarkReqVO updateReqVO);

    void updatePayableOtherStatus(Long id, Integer status);

    void deletePayableOther(Long id);

    ErpPayableOtherDO getPayableOther(Long id);

    PageResult<ErpPayableOtherDO> getPayableOtherPage(ErpPayableOtherPageReqVO pageReqVO);

}
