package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.ErpSaleUpdateRemarkReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartConvertQuoteReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartFirstApproveConfigRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartFirstApproveConfigSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartSubmitRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart.ErpSaleCartUpdateBasicReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartItemDO;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * ERP 销售手推车 Service 接口
 */
public interface ErpSaleCartService {

    Long createSaleCart(ErpSaleCartSaveReqVO createReqVO);

    ErpSaleCartSubmitRespVO createAndSubmitSaleCart(@Valid ErpSaleCartSaveReqVO createReqVO);

    ErpSaleCartSubmitRespVO createAndSubmitSaleCartFromPurchaseIn(@Valid ErpSaleCartSaveReqVO createReqVO);

    void updateSaleCart(@Valid ErpSaleCartSaveReqVO updateReqVO);

    void updateSaleCartRemark(@Valid ErpSaleUpdateRemarkReqVO updateReqVO);

    void updateSaleCartDraft(ErpSaleCartSaveReqVO updateReqVO);

    void updateSaleCartBasic(@Valid ErpSaleCartUpdateBasicReqVO updateReqVO);

    ErpSaleCartSubmitRespVO submitSaleCart(Long id);

    void createCrossDeptTransferOutDraftByCartId(Long cartId);

    void firstApproveSaleCart(Long id);

    void cancelFirstApproveSaleCart(Long id);

    void unlockSaleCartByTransferOutId(Long transferOutId);

    List<Long> finalApproveSaleCart(Long id);

    void autoFinalApproveAfterTransferOut(Long id);

    void autoFinalApproveAfterTransferOut(Long id, Long finalApproveUserId);

    void rejectSaleCart(Long id);

    Long convertToQuote(ErpSaleCartConvertQuoteReqVO reqVO);

    void deleteSaleCart(List<Long> ids);

    ErpSaleCartDO getSaleCart(Long id);

    PageResult<ErpSaleCartDO> getSaleCartPage(ErpSaleCartPageReqVO pageReqVO);

    List<ErpSaleCartItemDO> getSaleCartItemListByCartId(Long cartId);

    List<ErpSaleCartItemDO> getSaleCartItemListByCartIds(Collection<Long> cartIds);

    ErpSaleCartImportRespVO parseImportData(List<ErpSaleCartImportExcelVO> list);

    ErpSaleCartFirstApproveConfigRespVO getFirstApproveConfig();

    boolean isFirstApproveRequiredForDept(Long deptId);

    Map<Long, Boolean> getFirstApproveRequiredMap(Collection<Long> deptIds);

    void updateFirstApproveConfig(@Valid ErpSaleCartFirstApproveConfigSaveReqVO reqVO);

}
