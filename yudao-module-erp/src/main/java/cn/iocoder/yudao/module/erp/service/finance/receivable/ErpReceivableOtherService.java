package cn.iocoder.yudao.module.erp.service.finance.receivable;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.ErpFinanceUpdateRemarkReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherreceivable.ErpReceivableOtherDraftSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherreceivable.ErpReceivableOtherPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherreceivable.ErpReceivableOtherSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceReceiptDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable.ErpReceivableOtherDO;
import cn.iocoder.yudao.module.erp.service.finance.bo.ErpSaleCartFreightDraftCreateReqBO;

import javax.validation.Valid;

public interface ErpReceivableOtherService {

    String RECEIPT_DISCOUNT_SOURCE_TYPE = "收款单优惠";

    Long createReceivableOther(@Valid ErpReceivableOtherSaveReqVO createReqVO);

    Long createReceivableOtherDraft(ErpReceivableOtherDraftSaveReqVO createReqVO);

    Long createAndSubmitReceivableOther(@Valid ErpReceivableOtherSaveReqVO createReqVO);

    Long createFromSaleCartFreight(ErpSaleCartFreightDraftCreateReqBO createReqBO);

    Long createFromFinanceReceiptDiscount(ErpFinanceReceiptDO receipt);

    void updateReceivableOther(@Valid ErpReceivableOtherSaveReqVO updateReqVO);

    void updateReceivableOtherDraft(ErpReceivableOtherDraftSaveReqVO updateReqVO);

    void updateAndSubmitReceivableOther(@Valid ErpReceivableOtherSaveReqVO updateReqVO);

    void submitReceivableOther(Long id);

    void updateReceivableOtherRemark(@Valid ErpFinanceUpdateRemarkReqVO updateReqVO);

    void updateReceivableOtherStatus(Long id, Integer status);

    void deleteReceivableOther(Long id);

    ErpReceivableOtherDO getReceivableOther(Long id);

    PageResult<ErpReceivableOtherDO> getReceivableOtherPage(ErpReceivableOtherPageReqVO pageReqVO);
}
