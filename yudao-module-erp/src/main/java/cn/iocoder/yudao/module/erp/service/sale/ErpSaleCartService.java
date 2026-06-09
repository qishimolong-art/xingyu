package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartConvertQuoteReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartUpdateBasicReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartUpdateFileReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartItemDO;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;

/**
 * ERP 销售手推车 Service 接口
 */
public interface ErpSaleCartService {

    Long createSaleCart(@Valid ErpSaleCartSaveReqVO createReqVO);

    void updateSaleCart(@Valid ErpSaleCartSaveReqVO updateReqVO);

    void updateSaleCartBasic(@Valid ErpSaleCartUpdateBasicReqVO updateReqVO);

    void updateSaleCartFile(@Valid ErpSaleCartUpdateFileReqVO updateReqVO);

    void submitSaleCart(Long id);

    void firstApproveSaleCart(Long id);

    List<Long> finalApproveSaleCart(Long id);

    void rejectSaleCart(Long id);

    Long convertToQuote(ErpSaleCartConvertQuoteReqVO reqVO);

    void deleteSaleCart(List<Long> ids);

    ErpSaleCartDO getSaleCart(Long id);

    PageResult<ErpSaleCartDO> getSaleCartPage(ErpSaleCartPageReqVO pageReqVO);

    List<ErpSaleCartItemDO> getSaleCartItemListByCartId(Long cartId);

    List<ErpSaleCartItemDO> getSaleCartItemListByCartIds(Collection<Long> cartIds);

    ErpSaleCartImportRespVO parseImportData(List<ErpSaleCartImportExcelVO> list);

}
