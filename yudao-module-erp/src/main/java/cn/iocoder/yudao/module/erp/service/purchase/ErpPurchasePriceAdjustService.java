package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust.ErpPurchasePriceAdjustPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust.ErpPurchasePriceAdjustSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustItemDO;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;

/**
 * ERP 采购调价单 Service 接口
 *
 * @author 汽配ERP
 */
public interface ErpPurchasePriceAdjustService {

    /**
     * 创建采购调价单
     */
    Long createPurchasePriceAdjust(@Valid ErpPurchasePriceAdjustSaveReqVO createReqVO);

    /**
     * 更新采购调价单
     */
    void updatePurchasePriceAdjust(@Valid ErpPurchasePriceAdjustSaveReqVO updateReqVO);

    /**
     * 更新采购调价单状态（审核/反审核）
     */
    void updatePurchasePriceAdjustStatus(Long id, Integer status);

    /**
     * 删除采购调价单
     */
    void deletePurchasePriceAdjust(List<Long> ids);

    /**
     * 获得采购调价单
     */
    ErpPurchasePriceAdjustDO getPurchasePriceAdjust(Long id);

    /**
     * 获得采购调价单分页
     */
    PageResult<ErpPurchasePriceAdjustDO> getPurchasePriceAdjustPage(ErpPurchasePriceAdjustPageReqVO pageReqVO);

    /**
     * 获得采购调价单项列表
     */
    List<ErpPurchasePriceAdjustItemDO> getPurchasePriceAdjustItemListByAdjustId(Long adjustId);

    /**
     * 获得采购调价单项列表（批量）
     */
    List<ErpPurchasePriceAdjustItemDO> getPurchasePriceAdjustItemListByAdjustIds(Collection<Long> adjustIds);

}
