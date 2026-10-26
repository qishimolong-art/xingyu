package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.ErpSaleUpdateRemarkReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnOrderImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.imports.ErpSaleImportResultRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnSummaryRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnDraftCreateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnDraftUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnCreatePurchaseReturnReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnCreateTargetDraftRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnCreateTransferOutReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnItemBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnPurchaseReturnableItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnPurchaseReturnableItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnTransferOutableItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnTransferOutableItemRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnItemDO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;

import javax.validation.Valid;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

/**
 * ERP 销售退货 Service 接口
 *
 * @author 芋道源码
 */
public interface ErpSaleReturnService {

    /**
     * 创建销售退货
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createSaleReturn(@Valid ErpSaleReturnSaveReqVO createReqVO);

    Long createSaleReturnDraft(ErpSaleReturnDraftCreateReqVO createReqVO);

    /**
     * 更新销售退货
     *
     * @param updateReqVO 更新信息
     */
    void updateSaleReturn(@Valid ErpSaleReturnSaveReqVO updateReqVO);

    void updateSaleReturnDraft(ErpSaleReturnDraftUpdateReqVO updateReqVO);

    void batchUpdateSaleReturnItems(@Valid ErpSaleReturnItemBatchUpdateReqVO updateReqVO);

    void submitSaleReturn(Long id);

    /** 下单确认并生成销售退货拣货任务。 */
    void confirmSaleReturn(Long id, String expectedCostBasisSignature);

    /** 撤销尚未开始拣货的下单确认。 */
    void cancelConfirmSaleReturn(Long id);

    /** 整单拣货完成后自动审批；仅供拣货完成事件调用。 */
    void autoApproveAfterPick(Long id, Long pickUserId);

    void updateSaleReturnRemark(@Valid ErpSaleUpdateRemarkReqVO updateReqVO);

    /**
     * 更新销售退货的状态
     *
     * @param id 编号
     * @param status 状态
     */
    void updateSaleReturnStatus(Long id, Integer status);

    void updateSaleReturnStatusWithCostBasis(Long id, Integer status, String expectedCostBasisSignature);

    /**
     * 更新销售退货的退款金额
     *
     * @param id 编号
     * @param refundPrice 退款金额
     */
    void updateSaleReturnRefundPrice(Long id, BigDecimal refundPrice);

    /**
     * 删除销售退货
     *
     * @param ids 编号数组
     */
    void deleteSaleReturn(List<Long> ids);

    /**
     * 获得销售退货
     *
     * @param id 编号
     * @return 销售退货
     */
    ErpSaleReturnDO getSaleReturn(Long id);

    /**
     * 校验销售退货，已经审核通过
     *
     * @param id 编号
     * @return 销售退货
     */
    ErpSaleReturnDO validateSaleReturn(Long id);

    /**
     * 获得销售退货分页
     *
     * @param pageReqVO 分页查询
     * @return 销售退货分页
     */
    PageResult<ErpSaleReturnDO> getSaleReturnPage(ErpSaleReturnPageReqVO pageReqVO);

    ErpSaleReturnSummaryRespVO getSaleReturnSummary(ErpSaleReturnPageReqVO pageReqVO);

    // ==================== 销售退货项 ====================

    /**
     * 获得销售退货项列表
     *
     * @param returnId 销售退货编号
     * @return 销售退货项列表
     */
    List<ErpSaleReturnItemDO> getSaleReturnItemListByReturnId(Long returnId);

    List<ErpSaleReturnItemDO> getSaleReturnItemListByReturnId(Long returnId, String orderField,
                                                              String orderDirection);

    PageResult<ErpSaleReturnItemDO> getSaleReturnItemPage(ErpSaleReturnItemPageReqVO pageReqVO);

    /**
     * 获得销售退货项 List
     *
     * @param returnIds 销售退货编号数组
     * @return 销售退货项 List
     */
    List<ErpSaleReturnItemDO> getSaleReturnItemListByReturnIds(Collection<Long> returnIds);

    List<DeptSimpleRespVO> getWarehouseAvailableDeptSimpleList(Long warehouseId);

    PageResult<DeptSimpleRespVO> getWarehouseAvailableDeptSimplePage(Long warehouseId, PageParam pageParam);

    List<ErpSaleReturnTransferOutableItemRespVO> getTransferOutableItemsByReturnId(Long returnId);

    PageResult<ErpSaleReturnTransferOutableItemRespVO> getTransferOutableItemPage(
            ErpSaleReturnTransferOutableItemPageReqVO pageReqVO);

    ErpSaleReturnCreateTargetDraftRespVO createTransferOutFromSaleReturn(
            @Valid ErpSaleReturnCreateTransferOutReqVO reqVO);

    List<ErpSaleReturnPurchaseReturnableItemRespVO> getPurchaseReturnableItemsByReturnId(Long returnId);

    PageResult<ErpSaleReturnPurchaseReturnableItemRespVO> getPurchaseReturnableItemPage(
            ErpSaleReturnPurchaseReturnableItemPageReqVO pageReqVO);

    ErpSaleReturnCreateTargetDraftRespVO createPurchaseReturnFromSaleReturn(
            @Valid ErpSaleReturnCreatePurchaseReturnReqVO reqVO);

    ErpSaleReturnImportRespVO parseImportData(List<ErpSaleReturnImportExcelVO> list);

    ErpSaleImportResultRespVO importSaleReturnOrderList(
            List<ErpSaleReturnOrderImportExcelVO> list);

}
