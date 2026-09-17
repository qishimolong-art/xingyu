package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.ErpSaleUpdateRemarkReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.imports.ErpSaleImportResultRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteConvertCartReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteDraftCreateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteDraftUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteItemBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteOrderImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuotePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteItemDO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;

/**
 * ERP 报价订单 Service 接口
 */
public interface ErpSaleQuoteService {

    Long createSaleQuote(@Valid ErpSaleQuoteSaveReqVO createReqVO);

    Long createSaleQuoteDraft(ErpSaleQuoteDraftCreateReqVO createReqVO);

    void updateSaleQuote(@Valid ErpSaleQuoteSaveReqVO updateReqVO);

    void updateSaleQuoteDraft(ErpSaleQuoteDraftUpdateReqVO updateReqVO);

    void batchUpdateSaleQuoteItems(@Valid ErpSaleQuoteItemBatchUpdateReqVO updateReqVO);

    void submitSaleQuote(Long id);

    void updateSaleQuoteRemark(@Valid ErpSaleUpdateRemarkReqVO updateReqVO);

    void deleteSaleQuote(List<Long> ids);

    Long approveSaleQuote(Long id);

    Long convertToCart(@Valid ErpSaleQuoteConvertCartReqVO reqVO);

    ErpSaleQuoteDO getSaleQuote(Long id);

    PageResult<ErpSaleQuoteDO> getSaleQuotePage(ErpSaleQuotePageReqVO pageReqVO);

    List<ErpSaleQuoteItemDO> getSaleQuoteItemListByQuoteId(Long quoteId);

    PageResult<ErpSaleQuoteItemDO> getSaleQuoteItemPage(ErpSaleQuoteItemPageReqVO pageReqVO);

    List<ErpSaleQuoteItemDO> getSaleQuoteItemListByQuoteIds(Collection<Long> quoteIds);

    List<DeptSimpleRespVO> getWarehouseAvailableDeptSimpleList(Long warehouseId);

    PageResult<DeptSimpleRespVO> getWarehouseAvailableDeptSimplePage(Long warehouseId, PageParam pageParam);

    ErpSaleQuoteImportRespVO parseImportData(List<ErpSaleQuoteImportExcelVO> list);

    ErpSaleImportResultRespVO importSaleQuoteOrderList(List<ErpSaleQuoteOrderImportExcelVO> list);

}
