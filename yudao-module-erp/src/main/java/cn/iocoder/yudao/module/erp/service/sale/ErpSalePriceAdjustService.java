package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust.ErpSaleOutItemForAdjustRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust.ErpSalePriceAdjustImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust.ErpSalePriceAdjustImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust.ErpSalePriceAdjustPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust.ErpSalePriceAdjustSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSalePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSalePriceAdjustItemDO;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;

/**
 * ERP 销售调价单 Service 接口
 *
 * @author 汽配ERP
 */
public interface ErpSalePriceAdjustService {

    Long createSalePriceAdjust(@Valid ErpSalePriceAdjustSaveReqVO createReqVO);

    void updateSalePriceAdjust(@Valid ErpSalePriceAdjustSaveReqVO updateReqVO);

    void updateSalePriceAdjustStatus(Long id, Integer status);

    void deleteSalePriceAdjust(List<Long> ids);

    ErpSalePriceAdjustDO getSalePriceAdjust(Long id);

    PageResult<ErpSalePriceAdjustDO> getSalePriceAdjustPage(ErpSalePriceAdjustPageReqVO pageReqVO);

    List<ErpSalePriceAdjustItemDO> getSalePriceAdjustItemListByAdjustId(Long adjustId);

    List<ErpSalePriceAdjustItemDO> getSalePriceAdjustItemListByAdjustIds(Collection<Long> adjustIds);

    List<ErpSaleOutItemForAdjustRespVO> getAdjustableItemsByCustomerId(Long customerId, Long saleOutId);

    ErpSalePriceAdjustImportRespVO importSalePriceAdjustItems(List<ErpSalePriceAdjustImportExcelVO> list);

}
