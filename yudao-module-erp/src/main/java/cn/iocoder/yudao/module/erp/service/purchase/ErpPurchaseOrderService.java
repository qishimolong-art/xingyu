package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderDetailImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderImportResultRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderInableItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderInableItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderItemBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order.ErpPurchaseOrderUpdateRemarkReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseOrderItemDO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * ERP 采购订单 Service 接口
 *
 * @author 芋道源码
 */
public interface ErpPurchaseOrderService {

    /**
     * 创建采购订单
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createPurchaseOrder(@Valid ErpPurchaseOrderSaveReqVO createReqVO);

    Long createPurchaseOrderDraft(ErpPurchaseOrderSaveReqVO createReqVO);

    Long createAndSubmitPurchaseOrder(@Valid ErpPurchaseOrderSaveReqVO createReqVO);

    /**
     * 更新采购订单
     *
     * @param updateReqVO 更新信息
     */
    void updatePurchaseOrder(@Valid ErpPurchaseOrderSaveReqVO updateReqVO);

    void updatePurchaseOrderDraft(ErpPurchaseOrderSaveReqVO updateReqVO);

    void updateAndSubmitPurchaseOrder(@Valid ErpPurchaseOrderSaveReqVO updateReqVO);

    void submitPurchaseOrderDraft(Long id);

    /**
     * 修改采购订单备注，不受审批状态限制
     *
     * @param updateReqVO 备注信息
     */
    void updatePurchaseOrderRemark(@Valid ErpPurchaseOrderUpdateRemarkReqVO updateReqVO);

    /**
     * 批量修改采购订单明细仓库和部门，不受审批状态限制，但不允许修改已有下游业务的明细。
     *
     * @param updateReqVO 批量修改信息
     */
    void batchUpdatePurchaseOrderItems(@Valid ErpPurchaseOrderItemBatchUpdateReqVO updateReqVO);

    /**
     * 更新采购订单的状态
     *
     * @param id 编号
     * @param status 状态
     */
    void updatePurchaseOrderStatus(Long id, Integer status);

    /**
     * 更新采购订单的入库数量
     *
     * @param id 编号
     * @param inCountMap 入库数量 Map：key 采购订单项编号；value 入库数量
     */
    void updatePurchaseOrderInCount(Long id, Map<Long, BigDecimal> inCountMap);

    /**
     * 更新采购订单的退货数量
     *
     * @param orderId 编号
     * @param returnCountMap 退货数量 Map：key 采购订单项编号；value 退货数量
     */
    void updatePurchaseOrderReturnCount(Long orderId, Map<Long, BigDecimal> returnCountMap);

    /**
     * 删除采购订单
     *
     * @param ids 编号数组
     */
    void deletePurchaseOrder(List<Long> ids);

    /**
     * 获得采购订单
     *
     * @param id 编号
     * @return 采购订单
     */
    ErpPurchaseOrderDO getPurchaseOrder(Long id);

    /**
     * 校验采购订单，已经审核通过
     *
     * @param id 编号
     * @return 采购订单
     */
    ErpPurchaseOrderDO validatePurchaseOrder(Long id);

    /**
     * 获得当前用户对指定供应商可用的采购部门列表
     *
     * @param supplierId 供应商编号
     * @return 部门精简列表
     */
    List<DeptSimpleRespVO> getSupplierAvailableDeptSimpleList(Long supplierId);

    /**
     * 获得当前用户对指定供应商可用的采购部门分页
     *
     * @param supplierId 供应商编号
     * @param pageParam 分页参数
     * @return 部门分页
     */
    PageResult<DeptSimpleRespVO> getSupplierAvailableDeptSimplePage(Long supplierId, PageParam pageParam);

    /**
     * 获得当前用户对指定采购仓库可用的业务部门列表。
     *
     * @param warehouseId 仓库编号
     * @return 部门精简列表
     */
    List<DeptSimpleRespVO> getWarehouseAvailableDeptSimpleList(Long warehouseId);

    /**
     * 获得当前用户对指定采购仓库可用的业务部门分页。
     *
     * @param warehouseId 仓库编号
     * @param pageParam 分页参数
     * @return 部门分页
     */
    PageResult<DeptSimpleRespVO> getWarehouseAvailableDeptSimplePage(Long warehouseId, PageParam pageParam);

    /**
     * 获得采购订单分页
     *
     * @param pageReqVO 分页查询
     * @return 采购订单分页
     */
    PageResult<ErpPurchaseOrderDO> getPurchaseOrderPage(ErpPurchaseOrderPageReqVO pageReqVO);

    /**
     * 获得采购订单列表
     *
     * @param ids 编号数组
     * @return 采购订单列表
     */
    List<ErpPurchaseOrderDO> getPurchaseOrderList(Collection<Long> ids);

    // ==================== 采购订单项 ====================

    /**
     * 获得采购订单项列表
     *
     * @param orderId 采购订单编号
     * @return 采购订单项列表
     */
    List<ErpPurchaseOrderItemDO> getPurchaseOrderItemListByOrderId(Long orderId);

    /**
     * 获得采购订单项分页
     *
     * @param pageReqVO 明细分页查询
     * @return 采购订单项分页
     */
    PageResult<ErpPurchaseOrderItemDO> getPurchaseOrderItemPage(ErpPurchaseOrderItemPageReqVO pageReqVO);

    /**
     * 获得采购订单项 List
     *
     * @param orderIds 采购订单编号数组
     * @return 采购订单项 List
     */
    List<ErpPurchaseOrderItemDO> getPurchaseOrderItemListByOrderIds(Collection<Long> orderIds);

    /**
     * 解析导入的采购订单数据
     *
     * @param importVO 导入的 Excel 数据
     * @return 解析后的响应数据（名称已转换为ID）
     */
    ErpPurchaseOrderImportRespVO parseImportData(List<ErpPurchaseOrderDetailImportExcelVO> list);

    /**
     * 导入采购订单整单
     *
     * @param list 导入的 Excel 数据
     * @return 导入结果
     */
    ErpPurchaseOrderImportResultRespVO importPurchaseOrderList(List<ErpPurchaseOrderImportExcelVO> list);

    /**
     * 获取采购订单的可入库明细
     *
     * @param orderId 采购订单编号
     * @return 可入库明细列表
     */
    List<ErpPurchaseOrderInableItemRespVO> getInableItemsByOrderId(Long orderId);

    /**
     * 获取采购订单的可入库明细分页
     *
     * @param pageReqVO 分页查询
     * @return 可入库明细分页
     */
    PageResult<ErpPurchaseOrderInableItemRespVO> getInableItemPage(ErpPurchaseOrderInableItemPageReqVO pageReqVO);

}
