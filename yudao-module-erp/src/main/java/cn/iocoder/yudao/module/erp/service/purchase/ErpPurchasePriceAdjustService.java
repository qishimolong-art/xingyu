package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust.ErpPurchasePriceAdjustImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust.ErpPurchasePriceAdjustImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust.ErpPurchasePriceAdjustPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust.ErpPurchasePriceAdjustSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchasePriceAdjustItemDO;

import javax.validation.Valid;
import java.math.BigDecimal;
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
     *
     * @param reqVO 创建请求
     * @return 调价单 ID
     */
    Long createPurchasePriceAdjust(@Valid ErpPurchasePriceAdjustSaveReqVO reqVO);

    /**
     * 更新采购调价单（未审核状态下）
     *
     * @param reqVO 更新请求
     */
    void updatePurchasePriceAdjust(@Valid ErpPurchasePriceAdjustSaveReqVO reqVO);

    /**
     * 更新采购调价单状态（审核通过 / 反审核）
     *
     * <p>注意：本期审批通过后不支持反审核，status=PROCESS 将直接抛出异常。</p>
     *
     * @param id     调价单 ID
     * @param status 目标状态
     */
    void updatePurchasePriceAdjustStatus(Long id, Integer status);

    /**
     * 删除采购调价单（未审核状态下）
     *
     * @param ids 调价单 ID 集合
     */
    void deletePurchasePriceAdjust(List<Long> ids);

    /**
     * 获取采购调价单
     *
     * @param id 调价单 ID
     * @return 调价单 DO
     */
    ErpPurchasePriceAdjustDO getPurchasePriceAdjust(Long id);

    /**
     * 校验采购调价单存在
     *
     * @param id 调价单 ID
     * @return 调价单 DO
     */
    ErpPurchasePriceAdjustDO validatePurchasePriceAdjust(Long id);

    /**
     * 分页查询采购调价单
     *
     * @param pageReqVO 分页参数
     * @return 分页结果
     */
    PageResult<ErpPurchasePriceAdjustDO> getPurchasePriceAdjustPage(ErpPurchasePriceAdjustPageReqVO pageReqVO);

    /**
     * 查询某调价单的全部子项
     *
     * @param adjustId 调价单 ID
     * @return 子项列表
     */
    List<ErpPurchasePriceAdjustItemDO> getPurchasePriceAdjustItemListByAdjustId(Long adjustId);

    /**
     * 批量查询多个调价单的子项
     *
     * @param adjustIds 调价单 ID 集合
     * @return 子项列表
     */
    List<ErpPurchasePriceAdjustItemDO> getPurchasePriceAdjustItemListByAdjustIds(Collection<Long> adjustIds);

    ErpPurchasePriceAdjustImportRespVO importPurchasePriceAdjustItems(List<ErpPurchasePriceAdjustImportExcelVO> list);

    /**
     * 更新采购调价单已结算金额
     *
     * @param id           调价单 ID
     * @param paymentPrice 已结算金额
     */
    void updatePurchasePriceAdjustPaymentPrice(Long id, BigDecimal paymentPrice);

}
