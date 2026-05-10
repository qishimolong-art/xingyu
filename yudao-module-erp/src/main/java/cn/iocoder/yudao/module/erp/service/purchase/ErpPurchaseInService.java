package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseInFromOrderReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

/**
 * ERP 采购入库 Service 接口
 *
 * @author 芋道源码
 */
public interface ErpPurchaseInService {

    /**
     * 创建采购入库
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createPurchaseIn(@Valid ErpPurchaseInSaveReqVO createReqVO);

    /**
     * 更新采购入库
     *
     * @param updateReqVO 更新信息
     */
    void updatePurchaseIn(@Valid ErpPurchaseInSaveReqVO updateReqVO);

    /**
     * 更新采购入库的状态
     *
     * @param id 编号
     * @param status 状态
     */
    void updatePurchaseInStatus(Long id, Integer status);

    /**
     * 更新采购入库的付款金额
     *
     * @param id 编号
     * @param paymentPrice 付款金额
     */
    void updatePurchaseInPaymentPrice(Long id, BigDecimal paymentPrice);

    /**
     * 删除采购入库
     *
     * @param ids 编号数组
     */
    void deletePurchaseIn(List<Long> ids);

    /**
     * 获得采购入库
     *
     * @param id 编号
     * @return 采购入库
     */
    ErpPurchaseInDO getPurchaseIn(Long id);

    /**
     * 校验采购入库，已经审核通过
     *
     * @param id 编号
     * @return 采购入库
     */
    ErpPurchaseInDO validatePurchaseIn(Long id);

    /**
     * 获得采购入库分页
     *
     * @param pageReqVO 分页查询
     * @return 采购入库分页
     */
    PageResult<ErpPurchaseInDO> getPurchaseInPage(ErpPurchaseInPageReqVO pageReqVO);

    // ==================== 采购入库项 ====================

    /**
     * 获得采购入库项列表
     *
     * @param inId 采购入库编号
     * @return 采购入库项列表
     */
    List<ErpPurchaseInItemDO> getPurchaseInItemListByInId(Long inId);

    /**
     * 获得采购入库项 List
     *
     * @param inIds 采购入库编号数组
     * @return 采购入库项 List
     */
    List<ErpPurchaseInItemDO> getPurchaseInItemListByInIds(Collection<Long> inIds);

    /**
     * 查询某采购入库单的可退明细（按单退货模式使用）
     *
     * @param inId 采购入库单 ID（必须存在且已审批）
     * @return 可退明细列表；每项包含原入库数量、已退数量、可退数量、原入库单价等
     */
    java.util.List<cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnableItemRespVO> getReturnableItemsByInId(Long inId);

    /**
     * 从采购订单分批入库（自动审批生效）
     *
     * @param reqVO 分批入库请求
     * @return 入库单编号
     */
    Long createPurchaseInFromOrder(ErpPurchaseInFromOrderReqVO reqVO);

}