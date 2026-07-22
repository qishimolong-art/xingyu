package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.imports.ErpPurchaseImportResultRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInForAdjustRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInItemForAdjustRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInOrderImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInCreateTransferOutReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInCreateTransferOutRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInCreateSaleCartReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInCreateSaleCartRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInSaleCartableItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in.ErpPurchaseInTransferOutableItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseInFromOrderReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

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

    List<ErpPurchaseInDO> getPurchaseInList(Collection<Long> ids);

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

    Map<Long, BigDecimal> getApprovedReturnCountMapByInItemIds(Collection<Long> inItemIds);

    /**
     * 批量查询采购入库明细已生成的调拨出库数量。
     *
     * @param inItemIds 采购入库明细 ID 集合
     * @return Map&lt;采购入库明细 ID, 已调拨数量&gt;
     */
    Map<Long, BigDecimal> getTransferOutCountMapByInItemIds(Collection<Long> inItemIds);

    /**
     * 查询某采购入库单的可退明细（按单退货模式使用）
     *
     * @param inId 采购入库单 ID（必须存在且已审批）
     * @return 可退明细列表；每项包含原入库数量、已退数量、可退数量、原入库单价等
     */
    java.util.List<cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnableItemRespVO> getReturnableItemsByInId(Long inId);

    /**
     * 查询某采购入库单的可调拨出库明细。
     *
     * @param inId 采购入库单 ID（必须存在且已审批）
     * @return 可调拨明细列表
     */
    List<ErpPurchaseInTransferOutableItemRespVO> getTransferOutableItemsByInId(Long inId);

    /**
     * 查询某采购入库单的可转销售手推车明细。
     *
     * @param inId 采购入库单 ID
     * @return 可转销售手推车明细列表
     */
    List<ErpPurchaseInSaleCartableItemRespVO> getSaleCartableItemsByInId(Long inId);

    /**
     * 由采购入库单生成调拨出库单。
     *
     * @param reqVO 生成请求
     * @return 调拨出库单信息
     */
    ErpPurchaseInCreateTransferOutRespVO createTransferOutFromPurchaseIn(ErpPurchaseInCreateTransferOutReqVO reqVO);

    /**
     * 由采购入库单生成销售手推车。
     *
     * @param reqVO 生成请求
     * @return 销售手推车信息
     */
    ErpPurchaseInCreateSaleCartRespVO createSaleCartFromPurchaseIn(ErpPurchaseInCreateSaleCartReqVO reqVO);

    /**
     * 从采购订单分批入库（自动审批生效）
     *
     * @param reqVO 分批入库请求
     * @return 入库单编号
     */
    Long createPurchaseInFromOrder(ErpPurchaseInFromOrderReqVO reqVO);

    // ==================== 采购调价 专用查询 ====================

    /**
     * 查询指定供应商下所有已审批的入库单，用于采购调价「按入库单」方式
     *
     * @param supplierId 供应商编号（必填）
     * @return 已审批入库单列表
     */
    List<ErpPurchaseInForAdjustRespVO> getApprovedPurchaseInsBySupplier(Long supplierId);

    /**
     * 查询指定供应商下所有已审批入库单的明细行，用于采购调价「添加明细」方式
     *
     * @param supplierId       供应商编号（必填）
     * @param excludeAdjusted  是否过滤掉已调价过的行（true = 过滤）
     * @return 已审批入库单的明细行
     */
    List<ErpPurchaseInItemForAdjustRespVO> getApprovedPurchaseInItemsBySupplier(Long supplierId, Boolean excludeAdjusted);

    /**
     * 解析采购入库导入明细
     *
     * @param list Excel 行数据
     * @return 导入结果
     */
    ErpPurchaseInImportRespVO importPurchaseInItems(List<ErpPurchaseInImportExcelVO> list);

    /**
     * 导入采购入库整单。
     *
     * @param list Excel 行数据
     * @return 导入结果
     */
    ErpPurchaseImportResultRespVO importPurchaseInOrderList(List<ErpPurchaseInOrderImportExcelVO> list);

}
