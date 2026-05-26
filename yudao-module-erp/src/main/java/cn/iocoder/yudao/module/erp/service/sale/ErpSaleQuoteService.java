package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteConvertCartReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuotePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote.ErpSaleQuoteSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteItemDO;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;

/**
 * ERP 报价订单 Service 接口
 */
public interface ErpSaleQuoteService {

    Long createSaleQuote(@Valid ErpSaleQuoteSaveReqVO createReqVO);

    void updateSaleQuote(@Valid ErpSaleQuoteSaveReqVO updateReqVO);

    void deleteSaleQuote(List<Long> ids);

    Long approveSaleQuote(Long id);

    Long convertToCart(@Valid ErpSaleQuoteConvertCartReqVO reqVO);

    ErpSaleQuoteDO getSaleQuote(Long id);

    PageResult<ErpSaleQuoteDO> getSaleQuotePage(ErpSaleQuotePageReqVO pageReqVO);

    List<ErpSaleQuoteItemDO> getSaleQuoteItemListByQuoteId(Long quoteId);

    List<ErpSaleQuoteItemDO> getSaleQuoteItemListByQuoteIds(Collection<Long> quoteIds);

    ErpSaleQuoteImportRespVO parseImportData(List<ErpSaleQuoteImportExcelVO> list);

}
