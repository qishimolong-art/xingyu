package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.ErpSaleUpdateRemarkReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust.ErpSaleOutItemForAdjustRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust.ErpSalePriceAdjustImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust.ErpSalePriceAdjustImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust.ErpSalePriceAdjustableItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust.ErpSalePriceAdjustDraftSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust.ErpSalePriceAdjustItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust.ErpSalePriceAdjustPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust.ErpSalePriceAdjustSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSalePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSalePriceAdjustItemDO;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

/**
 * ERP 销售调价单 Service 接口
 *
 * @author 汽配ERP
 */
public interface ErpSalePriceAdjustService {

    Long createSalePriceAdjust(@Valid ErpSalePriceAdjustSaveReqVO createReqVO);

    Long createSalePriceAdjustDraft(ErpSalePriceAdjustDraftSaveReqVO createReqVO);

    void updateSalePriceAdjust(@Valid ErpSalePriceAdjustSaveReqVO updateReqVO);

    void updateSalePriceAdjustDraft(ErpSalePriceAdjustDraftSaveReqVO updateReqVO);

    void submitSalePriceAdjust(Long id);

    void updateSalePriceAdjustRemark(@Valid ErpSaleUpdateRemarkReqVO updateReqVO);

    void updateSalePriceAdjustStatus(Long id, Integer status);

    void deleteSalePriceAdjust(List<Long> ids);

    ErpSalePriceAdjustDO getSalePriceAdjust(Long id);

    ErpSalePriceAdjustDO validateSalePriceAdjust(Long id);

    void updateSalePriceAdjustReceiptPrice(Long id, BigDecimal receiptPrice);

    PageResult<ErpSalePriceAdjustDO> getSalePriceAdjustPage(ErpSalePriceAdjustPageReqVO pageReqVO);

    List<ErpSalePriceAdjustItemDO> getSalePriceAdjustItemListByAdjustId(Long adjustId);

    PageResult<ErpSalePriceAdjustItemDO> getSalePriceAdjustItemPage(ErpSalePriceAdjustItemPageReqVO pageReqVO);

    List<ErpSalePriceAdjustItemDO> getSalePriceAdjustItemListByAdjustIds(Collection<Long> adjustIds);

    List<ErpSaleOutItemForAdjustRespVO> getAdjustableItemsByCustomerId(Long customerId, Long saleOutId,
                                                                        Boolean excludeAdjusted);

    PageResult<ErpSaleOutItemForAdjustRespVO> getAdjustableItemPage(
            ErpSalePriceAdjustableItemPageReqVO pageReqVO);

    ErpSalePriceAdjustImportRespVO importSalePriceAdjustItems(List<ErpSalePriceAdjustImportExcelVO> list);

}
