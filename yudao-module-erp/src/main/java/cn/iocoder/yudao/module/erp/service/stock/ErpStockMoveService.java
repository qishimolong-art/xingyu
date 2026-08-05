package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.ErpStockUpdateRemarkReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockMovePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockMoveSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockTransferOutDraftCreateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockTransferOutDraftUpdateReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveItemDO;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockMoveApprovePermission;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockMoveOperationPermission;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockTransferOutPermissionScope;

import javax.validation.Valid;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * ERP 库存调拨单 Service 接口
 *
 * @author 芋道源码
 */
public interface ErpStockMoveService {

    /**
     * 创建库存调拨单
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createStockMove(@Valid ErpStockMoveSaveReqVO createReqVO);

    /**
     * 创建用户尚未正式提交的调拨出库草稿。
     */
    Long createStockTransferOutDraft(ErpStockTransferOutDraftCreateReqVO createReqVO);

    /**
     * 严格校验并创建待审批调拨出库单。
     */
    Long createAndSubmitStockTransferOut(@Valid ErpStockMoveSaveReqVO createReqVO);

    /**
     * 创建 ERP 库存调拨草稿
     *
     * <p>用于销售手推车库存不足自动生成待完善草稿，允许调出仓库暂时为空。</p>
     *
     * @param createReqVO 创建信息
     * @return 编号
     */
    Long createStockMoveDraft(ErpStockMoveSaveReqVO createReqVO);

    /**
     * 按来源创建调拨出库草稿，或更新同来源已有的未审核调拨出库草稿。
     *
     * @param createReqVO 调拨出库草稿
     * @return 调拨出库单编号
     */
    Long createOrUpdateTransferOutDraftBySource(ErpStockMoveSaveReqVO createReqVO);

    /**
     * Synchronizes all unapproved transfer-out drafts of the same source.
     * Each request must represent one source department. Drafts no longer present are deleted.
     *
     * @param createReqVOs complete transfer-out draft list of the source
     * @return transfer-out document ids
     */
    List<Long> syncTransferOutDraftsBySource(List<ErpStockMoveSaveReqVO> createReqVOs);

    /**
     * 删除同来源的未审核调拨出库草稿。
     *
     * @param sourceType 来源类型
     * @param sourceId 来源编号
     */
    void deleteUnapprovedTransferOutBySource(Integer sourceType, Long sourceId);

    /**
     * 同来源是否存在未审核调拨出库单。
     *
     * @param sourceType 来源类型
     * @param sourceId 来源编号
     * @return 是否存在未审核调拨出库单
     */
    boolean hasUnapprovedTransferOutBySource(Integer sourceType, Long sourceId);

    /**
     * 同来源是否存在已审核调拨出库单。
     *
     * @param sourceType 来源类型
     * @param sourceId 来源编号
     * @return 是否存在已审核调拨出库单
     */
    boolean hasApprovedTransferOutBySource(Integer sourceType, Long sourceId);

    /**
     * Gets the unique approved transfer-out document of the source.
     * Returns {@code null} when none or more than one exists.
     *
     * @param sourceType source type
     * @param sourceId source id
     * @return unique approved transfer-out document
     */
    ErpStockMoveDO getApprovedTransferOutBySource(Integer sourceType, Long sourceId);

    /**
     * Gets all transfer-out documents of the source.
     *
     * @param sourceType source type
     * @param sourceId source id
     * @return transfer-out documents
     */
    List<ErpStockMoveDO> getTransferOutListBySource(Integer sourceType, Long sourceId);

    /**
     * Gets whether current login user can approve the stock move.
     *
     * @param stockMove stock move
     * @param items stock move items
     * @return approve permission state
     */
    ErpStockMoveApprovePermission getApprovePermission(ErpStockMoveDO stockMove, List<ErpStockMoveItemDO> items);

    /**
     * Batch gets whether current login user can approve stock moves.
     * The permission scope and related department context are loaded once for the whole list.
     *
     * @param stockMoves stock moves
     * @param itemMap stock move items grouped by move id
     * @return approve permission state keyed by move id
     */
    Map<Long, ErpStockMoveApprovePermission> getApprovePermissionMap(
            List<ErpStockMoveDO> stockMoves, Map<Long, List<ErpStockMoveItemDO>> itemMap);

    /**
     * Batch gets approve permission with a transfer-out scope already loaded for this request.
     *
     * @param stockMoves stock moves
     * @param itemMap stock move items grouped by move id
     * @param scope transfer-out permission scope; {@code null} keeps the legacy fallback behavior
     * @return approve permission state keyed by move id
     */
    Map<Long, ErpStockMoveApprovePermission> getApprovePermissionMap(
            List<ErpStockMoveDO> stockMoves, Map<Long, List<ErpStockMoveItemDO>> itemMap,
            ErpStockTransferOutPermissionScope scope);

    /**
     * Gets whether current login user can delete the stock move.
     *
     * @param stockMove stock move
     * @param items stock move items
     * @return delete permission state
     */
    ErpStockMoveOperationPermission getDeletePermission(ErpStockMoveDO stockMove, List<ErpStockMoveItemDO> items);

    /**
     * Gets whether current login user can unlock the sale cart related to a stock transfer-out.
     *
     * @param stockMove stock move
     * @param items stock move items
     * @return unlock permission state
     */
    ErpStockMoveOperationPermission getUnlockCartPermission(ErpStockMoveDO stockMove,
                                                            List<ErpStockMoveItemDO> items);

    /**
     * 更新库存调拨单
     *
     * @param updateReqVO 更新信息
     */
    void updateStockMove(@Valid ErpStockMoveSaveReqVO updateReqVO);

    /**
     * 保存已有调拨出库草稿。
     */
    void updateStockTransferOutDraft(ErpStockTransferOutDraftUpdateReqVO updateReqVO);

    /**
     * 保存已有草稿的当前编辑内容并正式提交。
     */
    void updateAndSubmitStockTransferOutDraft(@Valid ErpStockMoveSaveReqVO updateReqVO);

    /**
     * 将持久化的只读草稿正式提交为待审批状态。
     */
    void submitStockTransferOutDraft(Long id);

    void updateStockMoveRemark(@Valid ErpStockUpdateRemarkReqVO updateReqVO);

    void updateStockTransferOutRemark(@Valid ErpStockUpdateRemarkReqVO updateReqVO);

    /**
     * 更新库存调拨单
     *
     * @param updateReqVO 更新信息
     * @param fieldPermissionModule 字段权限模块
     */
    void updateStockMove(@Valid ErpStockMoveSaveReqVO updateReqVO, String fieldPermissionModule);

    /**
     * 更新库存调拨单的状态
     *
     * @param id 编号
     * @param status 状态
     */
    void updateStockMoveStatus(Long id, Integer status);

    /**
     * 按调拨出库单专用数据权限更新审核状态。
     *
     * @param id 编号
     * @param status 状态
     */
    void updateStockTransferOutStatus(Long id, Integer status);

    /**
     * 删除库存调拨单
     *
     * @param ids 编号数组
     */
    void deleteStockMove(List<Long> ids);

    /**
     * 获得库存调拨单
     *
     * @param id 编号
     * @return 库存调拨单
     */
    ErpStockMoveDO getStockMove(Long id);

    /**
     * 获得当前登录用户可见的调拨出库单。
     *
     * @param id 编号
     * @return 调拨出库单
     */
    ErpStockMoveDO getVisibleStockTransferOut(Long id);

    /**
     * 校验当前登录用户可查看指定调拨出库单。
     *
     * @param id 编号
     */
    void validateStockTransferOutVisible(Long id);

    /**
     * 获得当前登录用户可见的调拨入库单。
     *
     * @param id 编号
     * @return 调拨入库单
     */
    ErpStockMoveDO getVisibleStockTransferIn(Long id);

    /**
     * Gets and locks a stock move for a subsequent status-changing operation.
     *
     * @param id stock move id
     * @return stock move
     */
    ErpStockMoveDO getStockMoveForUpdate(Long id);

    /**
     * 获得库存调拨单分页
     *
     * @param pageReqVO 分页查询
     * @return 库存调拨单分页
     */
    PageResult<ErpStockMoveDO> getStockMovePage(ErpStockMovePageReqVO pageReqVO);

    /**
     * 获得当前登录用户可见的调拨出库单分页。
     *
     * @param pageReqVO 分页查询
     * @return 调拨出库单分页
     */
    PageResult<ErpStockMoveDO> getVisibleStockTransferOutPage(ErpStockMovePageReqVO pageReqVO);

    /**
     * Gets the transfer-out permission scope for reuse in one query request.
     *
     * @return transfer-out permission scope; {@code null} only for an internal call without a login user
     */
    ErpStockTransferOutPermissionScope getTransferOutPermissionScope();

    /**
     * Gets the transfer-in permission scope for reuse in one query request.
     */
    ErpStockTransferOutPermissionScope getTransferInPermissionScope();

    /**
     * Gets a visible transfer-out page with a scope already loaded for this request.
     *
     * @param pageReqVO page query
     * @param scope transfer-out permission scope; {@code null} is reserved for internal calls
     * @return transfer-out page
     */
    PageResult<ErpStockMoveDO> getVisibleStockTransferOutPage(
            ErpStockMovePageReqVO pageReqVO, ErpStockTransferOutPermissionScope scope);

    /**
     * 获得当前登录用户可见的调拨入库单分页。
     *
     * @param pageReqVO 分页查询
     * @return 调拨入库单分页
     */
    PageResult<ErpStockMoveDO> getVisibleStockTransferInPage(ErpStockMovePageReqVO pageReqVO);

    // ==================== 调拨项 ====================

    /**
     * 获得库存调拨单项列表
     *
     * @param moveId 调拨编号
     * @return 库存调拨单项列表
     */
    List<ErpStockMoveItemDO> getStockMoveItemListByMoveId(Long moveId);

    /**
     * 获得库存调拨单项 List
     *
     * @param moveIds 调拨编号数组
     * @return 库存调拨单项 List
     */
    List<ErpStockMoveItemDO> getStockMoveItemListByMoveIds(Collection<Long> moveIds);

}
