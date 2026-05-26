package cn.iocoder.yudao.module.erp.service.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.prereceipt.ErpPreReceiptPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.prereceipt.ErpPreReceiptSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPreReceiptDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpPreReceiptItemDO;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;

/**
 * ERP 预收款单 Service 接口
 */
public interface ErpPreReceiptService {

    Long createPreReceipt(@Valid ErpPreReceiptSaveReqVO createReqVO);

    void updatePreReceipt(@Valid ErpPreReceiptSaveReqVO updateReqVO);

    void updatePreReceiptStatus(Long id, Integer status);

    void deletePreReceipt(List<Long> ids);

    ErpPreReceiptDO getPreReceipt(Long id);

    PageResult<ErpPreReceiptDO> getPreReceiptPage(ErpPreReceiptPageReqVO pageReqVO);

    List<ErpPreReceiptItemDO> getPreReceiptItemListByPreReceiptId(Long preReceiptId);

    List<ErpPreReceiptItemDO> getPreReceiptItemListByPreReceiptIds(Collection<Long> preReceiptIds);

}
