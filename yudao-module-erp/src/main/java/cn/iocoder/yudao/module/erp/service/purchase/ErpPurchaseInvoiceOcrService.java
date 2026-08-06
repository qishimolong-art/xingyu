package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoiceocr.ErpPurchaseInvoiceOcrBatchPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoiceocr.ErpPurchaseInvoiceOcrUploadReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoiceocr.ErpPurchaseInvoiceOcrUpdateFactoryOrderNoReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceOcrBatchDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceOcrItemDO;

import javax.validation.Valid;
import java.util.List;

public interface ErpPurchaseInvoiceOcrService {

    Long uploadPurchaseInvoiceOcrBatch(@Valid ErpPurchaseInvoiceOcrUploadReqVO uploadReqVO);

    ErpPurchaseInvoiceOcrBatchDO getPurchaseInvoiceOcrBatch(Long id);

    PageResult<ErpPurchaseInvoiceOcrBatchDO> getPurchaseInvoiceOcrBatchPage(
            @Valid ErpPurchaseInvoiceOcrBatchPageReqVO pageReqVO);

    ErpPurchaseInvoiceOcrBatchDO validatePurchaseInvoiceOcrBatch(Long id);

    List<ErpPurchaseInvoiceOcrItemDO> getPurchaseInvoiceOcrItemListByBatchId(Long batchId);

    Boolean recognizePurchaseInvoiceOcrBatch(Long batchId);

    Boolean matchPurchaseInvoiceOcrBatch(Long batchId);

    Boolean confirmPurchaseInvoiceOcrBatch(Long batchId);

    Boolean updatePurchaseInvoiceOcrItemFactoryOrderNo(Long itemId,
                                                       @Valid ErpPurchaseInvoiceOcrUpdateFactoryOrderNoReqVO updateReqVO);

    List<ErpPurchaseInvoiceOcrItemDO> getPurchaseInvoiceOcrErrorItemList(Long batchId);

}
