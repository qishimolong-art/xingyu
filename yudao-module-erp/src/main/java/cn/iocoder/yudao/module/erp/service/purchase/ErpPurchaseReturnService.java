package cn.iocoder.yudao.module.erp.service.purchase;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.ErpPurchaseUpdateRemarkReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.imports.ErpPurchaseImportResultRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnDraftCreateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnDraftUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnItemBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnOrderImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns.ErpPurchaseReturnSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseReturnItemDO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

/**
 * ERP 采购退货 Service 接口
 *
 * @author 芋道源码
 */
public interface ErpPurchaseReturnService {

    /**
     * 创建采购退货
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createPurchaseReturn(@Valid ErpPurchaseReturnSaveReqVO createReqVO);

    /**
     * 创建采购退货草稿
     */
    Long createPurchaseReturnDraft(ErpPurchaseReturnDraftCreateReqVO createReqVO);

    /**
     * 更新采购退货
     *
     * @param updateReqVO 更新信息
     */
    void updatePurchaseReturn(@Valid ErpPurchaseReturnSaveReqVO updateReqVO);

    /**
     * 批量修改采购退货明细仓库/部门
     *
     * @param updateReqVO 批量修改信息
     */
    void batchUpdatePurchaseReturnItems(@Valid ErpPurchaseReturnItemBatchUpdateReqVO updateReqVO);

    /**
     * 保存采购退货草稿
     */
    void updatePurchaseReturnDraft(ErpPurchaseReturnDraftUpdateReqVO updateReqVO);

    /**
     * 更新并提交采购退货草稿
     */
    void updateAndSubmitPurchaseReturnDraft(@Valid ErpPurchaseReturnDraftUpdateReqVO updateReqVO);

    /**
     * 提交采购退货草稿
     */
    void submitPurchaseReturn(Long id);

    /**
     * 修改采购退货备注，不受审批状态限制
     *
     * @param updateReqVO 备注信息
     */
    void updatePurchaseReturnRemark(@Valid ErpPurchaseUpdateRemarkReqVO updateReqVO);

    /**
     * 更新采购退货的状态
     *
     * @param id 编号
     * @param status 状态
     */
    void updatePurchaseReturnStatus(Long id, Integer status);

    /**
     * 更新采购退货的退款金额
     *
     * @param id 编号
     * @param refundPrice 退款金额
     */
    void updatePurchaseReturnRefundPrice(Long id, BigDecimal refundPrice);

    /**
     * 删除采购退货
     *
     * @param ids 编号数组
     */
    void deletePurchaseReturn(List<Long> ids);

    /**
     * 获得采购退货
     *
     * @param id 编号
     * @return 采购退货
     */
    ErpPurchaseReturnDO getPurchaseReturn(Long id);

    /**
     * 校验采购退货，已经审核通过
     *
     * @param id 编号
     * @return 采购退货
     */
    ErpPurchaseReturnDO validatePurchaseReturn(Long id);

    /**
     * 获得采购退货分页
     *
     * @param pageReqVO 分页查询
     * @return 采购退货分页
     */
    PageResult<ErpPurchaseReturnDO> getPurchaseReturnPage(ErpPurchaseReturnPageReqVO pageReqVO);

    List<ErpPurchaseReturnDO> getPurchaseReturnList(Collection<Long> ids);

    // ==================== 采购退货项 ====================

    /**
     * 获得采购退货项列表
     *
     * @param returnId 采购退货编号
     * @return 采购退货项列表
     */
    List<ErpPurchaseReturnItemDO> getPurchaseReturnItemListByReturnId(Long returnId);

    PageResult<ErpPurchaseReturnItemDO> getPurchaseReturnItemPage(ErpPurchaseReturnItemPageReqVO pageReqVO);

    /**
     * 获得采购退货项 List
     *
     * @param returnIds 采购退货编号数组
     * @return 采购退货项 List
     */
    List<ErpPurchaseReturnItemDO> getPurchaseReturnItemListByReturnIds(Collection<Long> returnIds);

    /**
     * 获取采购退货目标仓库可用部门列表
     *
     * @param warehouseId 仓库编号
     * @return 部门列表
     */
    List<DeptSimpleRespVO> getWarehouseAvailableDeptSimpleList(Long warehouseId);

    /**
     * 获取采购退货目标仓库可用部门分页
     *
     * @param warehouseId 仓库编号
     * @param pageParam 分页参数
     * @return 部门分页
     */
    PageResult<DeptSimpleRespVO> getWarehouseAvailableDeptSimplePage(Long warehouseId, PageParam pageParam);

    /**
     * 解析采购退货导入明细
     *
     * @param list Excel 行数据
     * @return 导入结果
     */
    ErpPurchaseReturnImportRespVO importPurchaseReturnItems(List<ErpPurchaseReturnImportExcelVO> list);

    /**
     * 导入采购退货整单。
     *
     * @param list Excel 行数据
     * @return 导入结果
     */
    ErpPurchaseImportResultRespVO importPurchaseReturnOrderList(List<ErpPurchaseReturnOrderImportExcelVO> list);

}
