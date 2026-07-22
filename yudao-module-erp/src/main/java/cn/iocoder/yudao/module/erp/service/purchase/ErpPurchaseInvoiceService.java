package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.imports.ErpPurchaseImportResultRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoiceImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoicePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice.ErpPurchaseInvoiceSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceItemDO;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;

public interface ErpPurchaseInvoiceService {

    Long createPurchaseInvoice(@Valid ErpPurchaseInvoiceSaveReqVO createReqVO);

    void updatePurchaseInvoice(@Valid ErpPurchaseInvoiceSaveReqVO updateReqVO);

    void updatePurchaseInvoiceStatus(Long id, Integer status);

    void deletePurchaseInvoice(List<Long> ids);

    ErpPurchaseInvoiceDO getPurchaseInvoice(Long id);

    ErpPurchaseInvoiceDO validatePurchaseInvoice(Long id);

    PageResult<ErpPurchaseInvoiceDO> getPurchaseInvoicePage(ErpPurchaseInvoicePageReqVO pageReqVO);

    List<ErpPurchaseInvoiceDO> getPurchaseInvoiceList(Collection<Long> ids);

    List<ErpPurchaseInvoiceItemDO> getPurchaseInvoiceItemListByInvoiceId(Long invoiceId);

    List<ErpPurchaseInvoiceItemDO> getPurchaseInvoiceItemListByInvoiceIds(Collection<Long> invoiceIds);

    List<ErpPurchaseInvoiceItemDO> getPurchaseInvoiceItemListBySourceInIds(Collection<Long> sourceInIds);

    ErpPurchaseImportResultRespVO importPurchaseInvoiceList(List<ErpPurchaseInvoiceImportExcelVO> list);
}
