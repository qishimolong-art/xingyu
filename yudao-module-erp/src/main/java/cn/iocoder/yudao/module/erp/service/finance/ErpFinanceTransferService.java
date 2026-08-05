package cn.iocoder.yudao.module.erp.service.finance;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.ErpFinanceUpdateRemarkReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.transfer.ErpFinanceTransferDraftSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.transfer.ErpFinanceTransferPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.vo.transfer.ErpFinanceTransferSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.ErpFinanceTransferDO;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;

/**
 * ERP 银行转账单 Service 接口
 */
public interface ErpFinanceTransferService {

    Long createFinanceTransfer(@Valid ErpFinanceTransferSaveReqVO createReqVO);

    Long createFinanceTransferDraft(@Valid ErpFinanceTransferDraftSaveReqVO createReqVO);

    Long createFinanceTransferAndSubmit(@Valid ErpFinanceTransferSaveReqVO createReqVO);

    void updateFinanceTransfer(@Valid ErpFinanceTransferSaveReqVO updateReqVO);

    void updateFinanceTransferDraft(@Valid ErpFinanceTransferDraftSaveReqVO updateReqVO);

    void updateFinanceTransferDraftAndSubmit(@Valid ErpFinanceTransferDraftSaveReqVO updateReqVO);

    void submitFinanceTransfer(Long id);

    void updateFinanceTransferRemark(@Valid ErpFinanceUpdateRemarkReqVO updateReqVO);

    void updateFinanceTransferStatus(Long id, Integer status);

    void deleteFinanceTransfer(List<Long> ids);

    ErpFinanceTransferDO getFinanceTransfer(Long id);

    List<ErpFinanceTransferDO> getFinanceTransferList(Collection<Long> ids);

    PageResult<ErpFinanceTransferDO> getFinanceTransferPage(ErpFinanceTransferPageReqVO pageReqVO);

}
