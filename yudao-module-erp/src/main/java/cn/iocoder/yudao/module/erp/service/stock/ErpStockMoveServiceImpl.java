package cn.iocoder.yudao.module.erp.service.stock;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.number.MoneyUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.ErpStockUpdateRemarkReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockMovePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockMoveSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockTransferOutDraftCreateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockTransferOutDraftUpdateReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMoveItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMoveMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleBizSourceTypeEnum;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockTransferOutStatusEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockMoveApprovePermission;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockMoveOperationPermission;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockTransferOutPermissionScope;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserDeptId;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_STOCK_MOVE_TYPE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

// TODO 鑺嬭壙锛氳褰曟搷浣滄棩蹇?

/**
 * ERP 搴撳瓨璋冩嫧鍗?Service 瀹炵幇绫?
 *
 * @author 鑺嬮亾婧愮爜
 */
@Service
@Validated
public class ErpStockMoveServiceImpl implements ErpStockMoveService {

    private static final String FIELD_PERMISSION_MODULE = "erp_stock_move";
    private static final String TRANSFER_OUT_FIELD_PERMISSION_MODULE = "erp_stock_transfer_out";
    private static final String TRANSFER_OUT_DATA_PERMISSION_FORM = "erp_stock_transfer_out";
    private static final String TRANSFER_IN_DATA_PERMISSION_FORM = "erp_stock_transfer_in";
    private static final int TRANSFER_DIRECTION_OUT = 10;
    private static final int TRANSFER_DIRECTION_IN = 20;

    @Resource
    private ErpStockMoveMapper stockMoveMapper;
    @Resource
    private ErpStockMoveItemMapper stockMoveItemMapper;
    @Resource
    private ErpPurchaseInItemMapper purchaseInItemMapper;
    @Resource
    private ErpStockFieldPermissionMasker fieldPermissionMasker;

    @Resource
    private ErpNoRedisDAO noRedisDAO;

    @Resource
    private ErpProductService productService;
    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private ErpStockService stockService;
    @Resource
    private ErpStockRecordService stockRecordService;
    @Resource
    private ErpStockLockService stockLockService;
    @Resource
    private ErpOperateLogService operateLogService;
    @Resource
    private DeptApi deptApi;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createStockMove(ErpStockMoveSaveReqVO createReqVO) {
        return doCreateStockMove(createReqVO, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createStockTransferOutDraft(ErpStockTransferOutDraftCreateReqVO createReqVO) {
        createReqVO.setTransferDirection(TRANSFER_DIRECTION_OUT);
        fieldPermissionMasker.clearHiddenFields(TRANSFER_OUT_FIELD_PERMISSION_MODULE, createReqVO);
        fieldPermissionMasker.clearHiddenItemFields(TRANSFER_OUT_FIELD_PERMISSION_MODULE, createReqVO.getItems());
        List<ErpStockMoveItemDO> stockMoveItems = buildStockTransferOutDraftItems(createReqVO);
        if (CollUtil.isEmpty(stockMoveItems)) {
            throw exception(STOCK_MOVE_DRAFT_ITEMS_REQUIRED);
        }
        String no = noRedisDAO.generate(ErpNoRedisDAO.STOCK_MOVE_NO_PREFIX);
        if (stockMoveMapper.selectByNo(no) != null) {
            throw exception(STOCK_MOVE_NO_EXISTS);
        }
        ErpStockMoveDO stockMove = BeanUtils.toBean(createReqVO, ErpStockMoveDO.class, target -> target
                .setNo(no)
                .setTransferDirection(TRANSFER_DIRECTION_OUT)
                .setRelatedMoveId(null)
                .setRelatedMoveNo(null)
                .setSourceType(null)
                .setSourceId(null)
                .setSourceNo(null)
                .setStatus(ErpStockTransferOutStatusEnum.DRAFT.getStatus())
                .setApproveUserId(null)
                .setApproveTime(null)
                .setTotalCount(getSumValue(stockMoveItems, ErpStockMoveItemDO::getCount,
                        BigDecimal::add, BigDecimal.ZERO))
                .setTotalPrice(getSumValue(stockMoveItems, ErpStockMoveItemDO::getTotalPrice,
                        BigDecimal::add, BigDecimal.ZERO)));
        fillDeptSnapshots(stockMove, stockMoveItems);
        stockMoveMapper.insert(stockMove);
        replaceStockMoveItems(stockMove.getId(), stockMoveItems);
        operateLogService.recordCreate(ERP_STOCK_MOVE_TYPE, stockMove.getId(), stockMove.getNo());
        return stockMove.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAndSubmitStockTransferOut(ErpStockMoveSaveReqVO createReqVO) {
        createReqVO.setTransferDirection(TRANSFER_DIRECTION_OUT);
        return doCreateStockMove(createReqVO, true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createStockMoveDraft(ErpStockMoveSaveReqVO createReqVO) {
        return doCreateStockMove(createReqVO, false);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createOrUpdateTransferOutDraftBySource(ErpStockMoveSaveReqVO createReqVO) {
        createReqVO.setTransferDirection(TRANSFER_DIRECTION_OUT);
        if (createReqVO.getSourceType() == null || createReqVO.getSourceId() == null) {
            return createStockMoveDraft(createReqVO);
        }
        ErpStockMoveDO existing = stockMoveMapper.selectListBySourceAndDirection(createReqVO.getSourceType(),
                        createReqVO.getSourceId(), TRANSFER_DIRECTION_OUT).stream()
                .filter(stockMove -> Objects.equals(stockMove.getFromDeptId(), createReqVO.getFromDeptId()))
                .findFirst().orElse(null);
        if (existing == null) {
            return createStockMoveDraft(createReqVO);
        }
        if (ErpAuditStatus.APPROVE.getStatus().equals(existing.getStatus())) {
            throw exception(SALE_WAREHOUSE_TRANSFER_APPROVED_EXISTS);
        }
        createReqVO.setId(existing.getId());
        doUpdateStockMove(createReqVO, existing, false);
        return existing.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public List<Long> syncTransferOutDraftsBySource(List<ErpStockMoveSaveReqVO> createReqVOs) {
        if (CollUtil.isEmpty(createReqVOs)) {
            return Collections.emptyList();
        }
        ErpStockMoveSaveReqVO firstReqVO = createReqVOs.get(0);
        Integer sourceType = firstReqVO.getSourceType();
        Long sourceId = firstReqVO.getSourceId();
        if (sourceType == null || sourceId == null) {
            return convertList(createReqVOs, this::createStockMoveDraft);
        }
        boolean invalidSource = createReqVOs.stream().anyMatch(reqVO ->
                !Objects.equals(sourceType, reqVO.getSourceType()) || !Objects.equals(sourceId, reqVO.getSourceId()));
        if (invalidSource) {
            throw new IllegalArgumentException("Transfer-out drafts must have the same source");
        }
        Map<Long, ErpStockMoveSaveReqVO> requestMap = new LinkedHashMap<>();
        createReqVOs.forEach(reqVO -> {
            reqVO.setTransferDirection(TRANSFER_DIRECTION_OUT);
            if (reqVO.getFromDeptId() == null || requestMap.putIfAbsent(reqVO.getFromDeptId(), reqVO) != null) {
                throw new IllegalArgumentException("Each transfer-out draft must have a unique source department");
            }
        });

        List<ErpStockMoveDO> existingList = stockMoveMapper.selectListBySourceAndDirectionForUpdate(
                sourceType, sourceId, TRANSFER_DIRECTION_OUT);
        if (existingList.stream().anyMatch(stockMove -> ErpAuditStatus.APPROVE.getStatus().equals(stockMove.getStatus()))) {
            throw exception(SALE_WAREHOUSE_TRANSFER_APPROVED_EXISTS);
        }
        Map<Long, ErpStockMoveDO> existingMap = new LinkedHashMap<>();
        List<ErpStockMoveDO> redundantList = new ArrayList<>();
        existingList.forEach(stockMove -> {
            if (stockMove.getFromDeptId() == null || existingMap.putIfAbsent(stockMove.getFromDeptId(), stockMove) != null
                    || !requestMap.containsKey(stockMove.getFromDeptId())) {
                redundantList.add(stockMove);
            }
        });
        redundantList.forEach(this::deleteUnapprovedTransferOutDraft);

        List<Long> result = new ArrayList<>(requestMap.size());
        requestMap.forEach((fromDeptId, reqVO) -> {
            ErpStockMoveDO existing = existingMap.get(fromDeptId);
            if (existing == null) {
                result.add(createStockMoveDraft(reqVO));
                return;
            }
            reqVO.setId(existing.getId());
            doUpdateStockMove(reqVO, existing, false);
            result.add(existing.getId());
        });
        return result;
    }

    private void deleteUnapprovedTransferOutDraft(ErpStockMoveDO stockMove) {
        deleteTransferInMirror(stockMove);
        stockMoveMapper.deleteById(stockMove.getId());
        stockMoveItemMapper.deleteByMoveId(stockMove.getId());
        operateLogService.recordDelete(ERP_STOCK_MOVE_TYPE, stockMove.getId(), stockMove.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUnapprovedTransferOutBySource(Integer sourceType, Long sourceId) {
        if (sourceType == null || sourceId == null) {
            return;
        }
        List<ErpStockMoveDO> stockMoves = stockMoveMapper.selectListBySourceAndDirectionForUpdate(
                sourceType, sourceId, TRANSFER_DIRECTION_OUT);
        if (CollUtil.isEmpty(stockMoves)) {
            return;
        }
        stockMoves.forEach(stockMove -> {
            if (ErpAuditStatus.APPROVE.getStatus().equals(stockMove.getStatus())) {
                throw exception(STOCK_MOVE_DELETE_FAIL_APPROVE, stockMove.getNo());
            }
        });
        stockMoves.forEach(stockMove -> {
            deleteUnapprovedTransferOutDraft(stockMove);
        });
    }

    @Override
    public boolean hasUnapprovedTransferOutBySource(Integer sourceType, Long sourceId) {
        if (sourceType == null || sourceId == null) {
            return false;
        }
        return stockMoveMapper.selectListBySourceAndDirection(sourceType, sourceId, TRANSFER_DIRECTION_OUT).stream()
                .anyMatch(stockMove -> !ErpAuditStatus.APPROVE.getStatus().equals(stockMove.getStatus()));
    }

    @Override
    public boolean hasApprovedTransferOutBySource(Integer sourceType, Long sourceId) {
        if (sourceType == null || sourceId == null) {
            return false;
        }
        return stockMoveMapper.selectListBySourceAndDirection(sourceType, sourceId, TRANSFER_DIRECTION_OUT).stream()
                .anyMatch(stockMove -> ErpAuditStatus.APPROVE.getStatus().equals(stockMove.getStatus()));
    }

    @Override
    public ErpStockMoveDO getApprovedTransferOutBySource(Integer sourceType, Long sourceId) {
        if (sourceType == null || sourceId == null) {
            return null;
        }
        List<ErpStockMoveDO> approvedTransfers = stockMoveMapper.selectListBySourceAndDirection(
                        sourceType, sourceId, TRANSFER_DIRECTION_OUT).stream()
                .filter(stockMove -> ErpAuditStatus.APPROVE.getStatus().equals(stockMove.getStatus()))
                .collect(Collectors.toList());
        return approvedTransfers.size() == 1 ? approvedTransfers.get(0) : null;
    }

    @Override
    public List<ErpStockMoveDO> getTransferOutListBySource(Integer sourceType, Long sourceId) {
        if (sourceType == null || sourceId == null) {
            return Collections.emptyList();
        }
        return stockMoveMapper.selectListBySourceAndDirection(sourceType, sourceId, TRANSFER_DIRECTION_OUT);
    }

    @Override
    public ErpStockMoveApprovePermission getApprovePermission(ErpStockMoveDO stockMove,
                                                              List<ErpStockMoveItemDO> items) {
        ErpStockTransferOutPermissionScope scope = getTransferOutPermissionScope();
        return getApprovePermission(stockMove, items, scope, null);
    }

    @Override
    public Map<Long, ErpStockMoveApprovePermission> getApprovePermissionMap(
            List<ErpStockMoveDO> stockMoves, Map<Long, List<ErpStockMoveItemDO>> itemMap) {
        return getApprovePermissionMap(stockMoves, itemMap, getTransferOutPermissionScope());
    }

    @Override
    public Map<Long, ErpStockMoveApprovePermission> getApprovePermissionMap(
            List<ErpStockMoveDO> stockMoves, Map<Long, List<ErpStockMoveItemDO>> itemMap,
            ErpStockTransferOutPermissionScope scope) {
        if (CollUtil.isEmpty(stockMoves)) {
            return Collections.emptyMap();
        }
        Map<Long, ErpStockMoveApprovePermission> permissionMap = new LinkedHashMap<>();
        if (scope != null && scope.isAll()) {
            stockMoves.forEach(stockMove -> permissionMap.put(stockMove.getId(),
                    ErpStockMoveApprovePermission.allowed()));
            return permissionMap;
        }

        Map<Long, List<ErpStockMoveItemDO>> safeItemMap = itemMap != null ? itemMap : Collections.emptyMap();
        if (scope != null) {
            stockMoves.forEach(stockMove -> permissionMap.put(stockMove.getId(), getApprovePermission(
                    stockMove, safeItemMap.get(stockMove.getId()), scope, null)));
            return permissionMap;
        }

        Set<Long> warehouseIds = new HashSet<>();
        stockMoves.forEach(stockMove -> {
            List<ErpStockMoveItemDO> items = safeItemMap.get(stockMove.getId());
            if (CollUtil.isEmpty(items)) {
                return;
            }
            boolean saleCartSource = isSaleCartSource(stockMove.getSourceType());
            items.forEach(item -> {
                if (item.getFromWarehouseId() != null && (saleCartSource || item.getFromDeptId() == null)) {
                    warehouseIds.add(item.getFromWarehouseId());
                }
            });
        });
        Map<Long, ErpWarehouseDO> warehouseMap = CollUtil.isEmpty(warehouseIds)
                ? Collections.emptyMap() : getWarehouseMap(warehouseIds, true);

        Long loginDeptId = getLoginUserDeptId();
        Set<Long> hierarchyDeptIds = new HashSet<>();
        if (loginDeptId != null) {
            stockMoves.forEach(stockMove -> {
                List<ErpStockMoveItemDO> items = safeItemMap.get(stockMove.getId());
                boolean ownerDeptCanApprove = scope == null || stockMove.getDeptId() != null
                        && scope.getDeptIds().contains(stockMove.getDeptId());
                if (ownerDeptCanApprove && isSaleCartCrossDeptMove(stockMove, items, warehouseMap)
                        && stockMove.getDeptId() != null) {
                    hierarchyDeptIds.add(stockMove.getDeptId());
                }
            });
        }
        Map<Long, DeptRespDTO> deptHierarchyMap = getDeptHierarchyMap(hierarchyDeptIds, loginDeptId);
        TransferOutApproveContext context = new TransferOutApproveContext(
                warehouseMap, deptHierarchyMap, loginDeptId);
        stockMoves.forEach(stockMove -> permissionMap.put(stockMove.getId(), getApprovePermission(
                stockMove, safeItemMap.get(stockMove.getId()), scope, context)));
        return permissionMap;
    }

    private ErpStockMoveApprovePermission getApprovePermission(ErpStockMoveDO stockMove,
                                                               List<ErpStockMoveItemDO> items,
                                                               ErpStockTransferOutPermissionScope scope,
                                                               TransferOutApproveContext context) {
        if (scope != null) {
            if (scope.isAll()) {
                return ErpStockMoveApprovePermission.allowed();
            }
            Set<Long> fromDeptIds = collectFromDeptSnapshotIds(stockMove, items);
            if (CollUtil.isNotEmpty(fromDeptIds) && scope.getDeptIds().containsAll(fromDeptIds)) {
                return ErpStockMoveApprovePermission.allowed();
            }
            return ErpStockMoveApprovePermission.denied(
                    "当前账号的数据权限不包含全部调出部门，不能审批该调拨出库单");
        }
        if (context == null) {
            return ErpStockMoveApprovePermission.of(getCrossDeptOperationPermission(stockMove, items,
                    "销售手推车跨部门调拨出库单只能由总公司审批"));
        }
        return ErpStockMoveApprovePermission.of(getCrossDeptOperationPermission(stockMove, items, context,
                "销售手推车跨部门调拨出库单只能由总公司审批"));
    }

    @Override
    public ErpStockMoveOperationPermission getDeletePermission(ErpStockMoveDO stockMove,
                                                               List<ErpStockMoveItemDO> items) {
        if (stockMove != null && isSaleCartSource(stockMove.getSourceType())) {
            return ErpStockMoveOperationPermission.denied(
                    "销售手推车来源调拨出库单请使用“解锁手推车”操作");
        }
        return getCrossDeptOperationPermission(stockMove, items,
                "销售手推车跨部门调拨出库单只能由总公司删除");
    }

    @Override
    public ErpStockMoveOperationPermission getUnlockCartPermission(ErpStockMoveDO stockMove,
                                                                   List<ErpStockMoveItemDO> items) {
        if (stockMove == null) {
            return ErpStockMoveOperationPermission.denied("调拨出库单不存在或已处理，请刷新后重试");
        }
        if (TRANSFER_DIRECTION_OUT != getTransferDirection(stockMove)) {
            return ErpStockMoveOperationPermission.denied("当前单据不是调拨出库单，不能解锁手推车");
        }
        if (!isSaleCartSource(stockMove.getSourceType())) {
            return ErpStockMoveOperationPermission.denied("当前调拨出库单不是由销售手推车生成，不能解锁");
        }
        if (stockMove.getSourceId() == null) {
            return ErpStockMoveOperationPermission.denied("调拨出库单缺少来源手推车信息，不能解锁");
        }
        if (!ErpAuditStatus.PROCESS.getStatus().equals(stockMove.getStatus())) {
            return ErpStockMoveOperationPermission.denied("调拨出库单已审核，不能解锁手推车");
        }
        return getCrossDeptOperationPermission(stockMove, items,
                "销售手推车跨部门调拨出库单只能由总公司解锁");
    }

    private ErpStockMoveOperationPermission getCrossDeptOperationPermission(ErpStockMoveDO stockMove,
                                                                           List<ErpStockMoveItemDO> items,
                                                                           String deniedReason) {
        if (!isSaleCartCrossDeptMove(stockMove, items)) {
            return ErpStockMoveOperationPermission.allowed();
        }
        Long loginDeptId = getLoginUserDeptId();
        if (loginDeptId != null && isAncestorDept(stockMove.getDeptId(), loginDeptId)) {
            return ErpStockMoveOperationPermission.allowed();
        }
        return ErpStockMoveOperationPermission.denied(deniedReason);
    }

    private ErpStockMoveOperationPermission getCrossDeptOperationPermission(ErpStockMoveDO stockMove,
                                                                           List<ErpStockMoveItemDO> items,
                                                                           TransferOutApproveContext context,
                                                                           String deniedReason) {
        if (!isSaleCartCrossDeptMove(stockMove, items, context.getWarehouseMap())) {
            return ErpStockMoveOperationPermission.allowed();
        }
        Long loginDeptId = context.getLoginDeptId();
        if (loginDeptId != null && isAncestorDept(stockMove.getDeptId(), loginDeptId,
                context.getDeptHierarchyMap())) {
            return ErpStockMoveOperationPermission.allowed();
        }
        return ErpStockMoveOperationPermission.denied(deniedReason);
    }

    private Long doCreateStockMove(ErpStockMoveSaveReqVO createReqVO, boolean requireWarehouses) {
        // 1.1 鏍￠獙鍑哄簱椤圭殑鏈夋晥鎬?
        List<ErpStockMoveItemDO> stockMoveItems = validateStockMoveItems(createReqVO.getItems(), requireWarehouses,
                isSaleCartSource(createReqVO.getSourceType()), createReqVO.getDeptId());
        validatePurchaseInSourceMoveCounts(stockMoveItems, null);
        if (requireWarehouses) {
            validateStockMoveItemsReadyForApprove(
                    stockMoveItems, isSaleCartSource(createReqVO.getSourceType()), createReqVO.getDeptId());
        }
        // 1.2 鐢熸垚璋冩嫧鍗曞彿锛屽苟鏍￠獙鍞竴鎬?
        String no = noRedisDAO.generate(ErpNoRedisDAO.STOCK_MOVE_NO_PREFIX);
        if (stockMoveMapper.selectByNo(no) != null) {
            throw exception(STOCK_MOVE_NO_EXISTS);
        }

        // 2.1 鎻掑叆鍑哄簱鍗?
        ErpStockMoveDO stockMove = BeanUtils.toBean(createReqVO, ErpStockMoveDO.class, in -> in
                .setNo(no).setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setTransferDirection(TRANSFER_DIRECTION_OUT)
                .setTotalCount(getSumValue(stockMoveItems, ErpStockMoveItemDO::getCount, BigDecimal::add))
                .setTotalPrice(getSumValue(stockMoveItems, ErpStockMoveItemDO::getTotalPrice, BigDecimal::add, BigDecimal.ZERO)));
        fillDeptSnapshots(stockMove, stockMoveItems);
        stockMoveMapper.insert(stockMove);
        // 2.2 鎻掑叆鍑哄簱鍗曢」
        stockMoveItems.forEach(o -> o.setMoveId(stockMove.getId()));
        stockMoveItemMapper.insertBatch(stockMoveItems);
        syncTransferInMirror(stockMove, stockMoveItems, null);
        operateLogService.recordCreate(ERP_STOCK_MOVE_TYPE, stockMove.getId(), stockMove.getNo());
        return stockMove.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStockMove(ErpStockMoveSaveReqVO updateReqVO) {
        updateStockMove(updateReqVO, FIELD_PERMISSION_MODULE);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStockMove(ErpStockMoveSaveReqVO updateReqVO, String fieldPermissionModule) {
        ErpStockTransferOutPermissionScope scope = getTransferOutPermissionScope();
        validateStockTransferOutVisible(updateReqVO.getId(), scope);
        if (scope == null) {
            doUpdateStockMove(updateReqVO, fieldPermissionModule);
            return;
        }
        DataPermissionUtils.executeIgnore(() -> doUpdateStockMove(updateReqVO, fieldPermissionModule));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStockTransferOutDraft(ErpStockTransferOutDraftUpdateReqVO updateReqVO) {
        ErpStockTransferOutPermissionScope scope = getTransferOutPermissionScope();
        validateStockTransferOutVisible(updateReqVO.getId(), scope);
        if (scope == null) {
            doUpdateStockTransferOutDraft(updateReqVO);
            return;
        }
        DataPermissionUtils.executeIgnore(() -> doUpdateStockTransferOutDraft(updateReqVO));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAndSubmitStockTransferOutDraft(ErpStockMoveSaveReqVO updateReqVO) {
        ErpStockTransferOutPermissionScope scope = getTransferOutPermissionScope();
        validateStockTransferOutVisible(updateReqVO.getId(), scope);
        Runnable action = () -> {
            doUpdateStockTransferOutDraft(updateReqVO);
            doSubmitStockTransferOutDraft(updateReqVO.getId());
        };
        if (scope == null) {
            action.run();
            return;
        }
        DataPermissionUtils.executeIgnore(action);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitStockTransferOutDraft(Long id) {
        ErpStockTransferOutPermissionScope scope = getTransferOutPermissionScope();
        validateStockTransferOutVisible(id, scope);
        if (scope == null) {
            doSubmitStockTransferOutDraft(id);
            return;
        }
        DataPermissionUtils.executeIgnore(() -> doSubmitStockTransferOutDraft(id));
    }

    @Override
    public void updateStockMoveRemark(ErpStockUpdateRemarkReqVO updateReqVO) {
        doUpdateStockMoveRemark(updateReqVO);
    }

    @Override
    public void updateStockTransferOutRemark(ErpStockUpdateRemarkReqVO updateReqVO) {
        ErpStockTransferOutPermissionScope scope = getTransferOutPermissionScope();
        validateStockTransferOutVisible(updateReqVO.getId(), scope);
        if (scope == null) {
            doUpdateStockMoveRemark(updateReqVO);
            return;
        }
        DataPermissionUtils.executeIgnore(() -> doUpdateStockMoveRemark(updateReqVO));
    }

    private void doUpdateStockMoveRemark(ErpStockUpdateRemarkReqVO updateReqVO) {
        ErpStockMoveDO stockMove = validateStockMoveExists(updateReqVO.getId());
        stockMoveMapper.updateById(new ErpStockMoveDO()
                .setId(updateReqVO.getId()).setRemark(updateReqVO.getRemark()));
        operateLogService.recordUpdate(ERP_STOCK_MOVE_TYPE, stockMove.getId(), stockMove.getNo());
    }

    private void doUpdateStockTransferOutDraft(ErpStockMoveSaveReqVO updateReqVO) {
        ErpStockMoveDO stockMove = validateStockMoveExists(updateReqVO.getId());
        validateTransferOut(stockMove);
        if (!ErpStockTransferOutStatusEnum.DRAFT.getStatus().equals(stockMove.getStatus())) {
            throw exception(STOCK_MOVE_UPDATE_FAIL_NOT_DRAFT, stockMove.getNo());
        }
        List<ErpStockMoveItemDO> oldItems = stockMoveItemMapper.selectListByMoveId(updateReqVO.getId());
        fieldPermissionMasker.preserveHiddenFields(TRANSFER_OUT_FIELD_PERMISSION_MODULE, updateReqVO, stockMove);
        fieldPermissionMasker.preserveHiddenItemFields(
                TRANSFER_OUT_FIELD_PERMISSION_MODULE, updateReqVO.getItems(), oldItems);
        List<ErpStockMoveItemDO> stockMoveItems = buildStockTransferOutDraftItems(updateReqVO);
        ErpStockMoveDO updateObj = BeanUtils.toBean(updateReqVO, ErpStockMoveDO.class, target -> target
                .setNo(stockMove.getNo())
                .setDeptId(updateReqVO.getDeptId() != null ? updateReqVO.getDeptId() : stockMove.getDeptId())
                .setTransferDirection(TRANSFER_DIRECTION_OUT)
                .setRelatedMoveId(stockMove.getRelatedMoveId())
                .setRelatedMoveNo(stockMove.getRelatedMoveNo())
                .setFromDeptId(stockMove.getFromDeptId())
                .setToDeptId(stockMove.getToDeptId())
                .setSourceType(stockMove.getSourceType())
                .setSourceId(stockMove.getSourceId())
                .setSourceNo(stockMove.getSourceNo())
                .setStatus(ErpStockTransferOutStatusEnum.DRAFT.getStatus())
                .setApproveUserId(stockMove.getApproveUserId())
                .setApproveTime(stockMove.getApproveTime())
                .setTotalCount(getSumValue(stockMoveItems, ErpStockMoveItemDO::getCount,
                        BigDecimal::add, BigDecimal.ZERO))
                .setTotalPrice(getSumValue(stockMoveItems, ErpStockMoveItemDO::getTotalPrice,
                        BigDecimal::add, BigDecimal.ZERO)));
        fillDeptSnapshots(updateObj, stockMoveItems);
        int updateCount = stockMoveMapper.updateByIdAndStatus(updateReqVO.getId(),
                ErpStockTransferOutStatusEnum.DRAFT.getStatus(), updateObj);
        if (updateCount == 0) {
            throw exception(STOCK_MOVE_UPDATE_FAIL_NOT_DRAFT, stockMove.getNo());
        }
        replaceStockMoveItems(updateReqVO.getId(), stockMoveItems);
        operateLogService.recordUpdate(ERP_STOCK_MOVE_TYPE, stockMove.getId(), stockMove.getNo());
    }

    private void doSubmitStockTransferOutDraft(Long id) {
        ErpStockMoveDO stockMove = validateStockMoveExists(id);
        validateTransferOut(stockMove);
        if (!ErpStockTransferOutStatusEnum.DRAFT.getStatus().equals(stockMove.getStatus())) {
            throw exception(STOCK_MOVE_SUBMIT_FAIL);
        }
        if (stockMove.getMoveTime() == null) {
            throw exception(STOCK_MOVE_SUBMIT_TIME_REQUIRED);
        }
        List<ErpStockMoveItemDO> persistedItems = stockMoveItemMapper.selectListByMoveId(id);
        if (CollUtil.isEmpty(persistedItems)) {
            throw exception(STOCK_MOVE_SUBMIT_ITEMS_REQUIRED);
        }
        ErpStockMoveSaveReqVO submitReqVO = BeanUtils.toBean(stockMove, ErpStockMoveSaveReqVO.class);
        submitReqVO.setItems(BeanUtils.toBean(persistedItems, ErpStockMoveSaveReqVO.Item.class));
        List<ErpStockMoveItemDO> normalizedItems = validateStockMoveItems(
                submitReqVO.getItems(), true, isSaleCartSource(stockMove.getSourceType()), stockMove.getDeptId());
        validatePurchaseInSourceMoveCounts(normalizedItems, stockMove.getId());
        validateStockMoveItemsReadyForApprove(
                normalizedItems, isSaleCartSource(stockMove.getSourceType()), stockMove.getDeptId());
        ErpStockMoveDO updateObj = new ErpStockMoveDO()
                .setStatus(ErpStockTransferOutStatusEnum.PROCESS.getStatus())
                .setTotalCount(getSumValue(normalizedItems, ErpStockMoveItemDO::getCount,
                        BigDecimal::add, BigDecimal.ZERO))
                .setTotalPrice(getSumValue(normalizedItems, ErpStockMoveItemDO::getTotalPrice,
                        BigDecimal::add, BigDecimal.ZERO))
                .setFromDeptId(stockMove.getFromDeptId())
                .setToDeptId(stockMove.getToDeptId());
        fillDeptSnapshots(updateObj, normalizedItems);
        int updateCount = stockMoveMapper.updateByIdAndStatus(id,
                ErpStockTransferOutStatusEnum.DRAFT.getStatus(), updateObj);
        if (updateCount == 0) {
            throw exception(STOCK_MOVE_SUBMIT_FAIL);
        }
        replaceStockMoveItems(id, normalizedItems);
        stockMove.setStatus(ErpStockTransferOutStatusEnum.PROCESS.getStatus())
                .setTotalCount(updateObj.getTotalCount())
                .setTotalPrice(updateObj.getTotalPrice())
                .setFromDeptId(updateObj.getFromDeptId())
                .setToDeptId(updateObj.getToDeptId());
        syncTransferInMirror(stockMove, stockMoveItemMapper.selectListByMoveId(id), null);
        operateLogService.recordUpdate(ERP_STOCK_MOVE_TYPE, stockMove.getId(), stockMove.getNo());
    }

    private void doUpdateStockMove(ErpStockMoveSaveReqVO updateReqVO, String fieldPermissionModule) {
        // 1.1 ??????
        ErpStockMoveDO stockMove = validateStockMoveExists(updateReqVO.getId());
        validateTransferOut(stockMove);
        if (ErpStockTransferOutStatusEnum.DRAFT.getStatus().equals(stockMove.getStatus())) {
            throw exception(STOCK_MOVE_FORMAL_UPDATE_FAIL_DRAFT, stockMove.getNo());
        }
        if (ErpAuditStatus.APPROVE.getStatus().equals(stockMove.getStatus())) {
            throw exception(STOCK_MOVE_UPDATE_FAIL_APPROVE, stockMove.getNo());
        }
        fieldPermissionMasker.preserveHiddenFields(fieldPermissionModule, updateReqVO, stockMove);
        fieldPermissionMasker.preserveHiddenItemFields(fieldPermissionModule, updateReqVO.getItems(),
                stockMoveItemMapper.selectListByMoveId(updateReqVO.getId()));
        doUpdateStockMove(updateReqVO, stockMove, true);
    }

    private void doUpdateStockMove(ErpStockMoveSaveReqVO updateReqVO, ErpStockMoveDO stockMove,
                                   boolean requireWarehouses) {
        List<ErpStockMoveItemDO> stockMoveItems = validateStockMoveItems(updateReqVO.getItems(), requireWarehouses,
                isSaleCartSource(stockMove.getSourceType()),
                updateReqVO.getDeptId() != null ? updateReqVO.getDeptId() : stockMove.getDeptId());
        validatePurchaseInSourceMoveCounts(stockMoveItems, stockMove.getId());
        ErpStockMoveDO updateObj = BeanUtils.toBean(updateReqVO, ErpStockMoveDO.class, in -> in
                .setTransferDirection(TRANSFER_DIRECTION_OUT)
                .setTotalCount(getSumValue(stockMoveItems, ErpStockMoveItemDO::getCount, BigDecimal::add))
                .setTotalPrice(getSumValue(stockMoveItems, ErpStockMoveItemDO::getTotalPrice, BigDecimal::add)));
        if (updateObj.getDeptId() == null) {
            updateObj.setDeptId(stockMove.getDeptId());
        }
        fillDeptSnapshots(updateObj, stockMoveItems);
        stockMoveMapper.updateById(updateObj);
        updateStockMoveItemList(updateReqVO.getId(), stockMoveItems);
        stockMove.setDeptId(updateObj.getDeptId())
                .setFromDeptId(updateObj.getFromDeptId())
                .setToDeptId(updateObj.getToDeptId())
                .setMoveTime(updateObj.getMoveTime())
                .setTotalCount(updateObj.getTotalCount())
                .setTotalPrice(updateObj.getTotalPrice())
                .setRemark(updateObj.getRemark())
                .setFileUrl(updateObj.getFileUrl());
        syncTransferInMirror(stockMove, stockMoveItemMapper.selectListByMoveId(stockMove.getId()), null);
        operateLogService.recordUpdate(ERP_STOCK_MOVE_TYPE, stockMove.getId(), stockMove.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStockTransferOutStatus(Long id, Integer status) {
        ErpStockTransferOutPermissionScope scope = getTransferOutPermissionScope();
        if (scope == null) {
            throw exception(STOCK_MOVE_APPROVE_DEPT_PERMISSION_DENIED);
        }
        if (getVisibleStockTransferOut(id, scope) == null) {
            throw exception(STOCK_MOVE_APPROVE_DEPT_PERMISSION_DENIED);
        }
        // 共享表的通用规则只识别 dept_id；此入口由调拨出库专用范围完成对象级审批校验。
        DataPermissionUtils.executeIgnore(() -> doUpdateStockMoveStatus(id, status, scope));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStockMoveStatus(Long id, Integer status) {
        doUpdateStockMoveStatus(id, status, getTransferOutPermissionScope());
    }

    private void doUpdateStockMoveStatus(Long id, Integer status, ErpStockTransferOutPermissionScope scope) {
        if (!ErpAuditStatus.APPROVE.getStatus().equals(status)) {
            throw exception(STOCK_MOVE_PROCESS_FAIL);
        }
        // 1.1 鏍￠獙瀛樺湪
        ErpStockMoveDO stockMove = validateStockMoveExists(id);
        validateTransferOut(stockMove);
        if (!ErpStockTransferOutStatusEnum.PROCESS.getStatus().equals(stockMove.getStatus())) {
            throw exception(STOCK_MOVE_APPROVE_FAIL);
        }
        boolean saleCartTransferOut = isSaleCartSource(stockMove.getSourceType());
        ErpStockMoveDO existingTransferIn = findTransferInMirror(stockMove);
        if (existingTransferIn != null
                && ErpAuditStatus.APPROVE.getStatus().equals(existingTransferIn.getStatus())) {
            throw exception(STOCK_MOVE_TRANSFER_IN_EXISTS);
        }

        // 2. 鏇存柊鐘舵€?
        List<ErpStockMoveItemDO> stockMoveItems = stockMoveItemMapper.selectListByMoveId(id);
        ErpStockMoveApprovePermission approvePermission = getApprovePermission(stockMove, stockMoveItems, scope, null);
        if (!Boolean.TRUE.equals(approvePermission.getApproveAllowed())) {
            throw exception(STOCK_MOVE_APPROVE_DEPT_PERMISSION_DENIED);
        }
        validateStockMoveItemsReadyForApprove(stockMoveItems, isSaleCartSource(stockMove.getSourceType()),
                stockMove.getDeptId());
        Set<Long> warehouseIds = new HashSet<>();
        stockMoveItems.forEach(item -> {
            warehouseIds.add(item.getFromWarehouseId());
            warehouseIds.add(item.getToWarehouseId());
        });
        Map<Long, ErpWarehouseDO> warehouseMap = getWarehouseMap(warehouseIds, true);
        validProductList(convertSet(stockMoveItems, ErpStockMoveItemDO::getProductId), true);
        TransferInMirror transferInMirror = syncTransferInMirror(stockMove, stockMoveItems, warehouseMap);
        if (transferInMirror == null) {
            throw exception(STOCK_MOVE_APPROVE_FAIL);
        }

        Long approveUserId = getLoginUserId();
        LocalDateTime approveTime = LocalDateTime.now();
        int updateCount = stockMoveMapper.updateByIdAndStatus(id, stockMove.getStatus(),
                new ErpStockMoveDO().setStatus(status)
                        .setApproveUserId(approveUserId)
                        .setApproveTime(approveTime));
        if (updateCount == 0) {
            throw exception(STOCK_MOVE_APPROVE_FAIL);
        }
        stockMove.setStatus(status).setApproveUserId(approveUserId).setApproveTime(approveTime);
        operateLogService.recordStatus(ERP_STOCK_MOVE_TYPE, stockMove.getId(), stockMove.getNo(), true);

        // 3. 鍙樻洿搴撳瓨
        Integer fromBizType = ErpStockRecordBizTypeEnum.MOVE_OUT.getType();
        stockMoveItems.forEach(stockMoveItem -> {
            BigDecimal fromCount = stockMoveItem.getCount().negate();
            stockRecordService.createStockRecord(new ErpStockRecordCreateReqBO(
                    stockMoveItem.getProductId(), stockMoveItem.getFromWarehouseId(), stockMoveItem.getBatchNo(), fromCount,
                    fromBizType, stockMoveItem.getMoveId(), stockMoveItem.getId(), stockMove.getNo(),
                    null, stockMove.getMoveTime()));
        });
        ErpStockMoveDO inMove = transferInMirror.getMove();
        int transferInUpdateCount = stockMoveMapper.updateByIdAndStatus(inMove.getId(),
                ErpAuditStatus.PROCESS.getStatus(), new ErpStockMoveDO()
                        .setStatus(ErpAuditStatus.APPROVE.getStatus())
                        .setApproveUserId(approveUserId)
                        .setApproveTime(approveTime));
        if (transferInUpdateCount == 0) {
            throw exception(STOCK_MOVE_APPROVE_FAIL);
        }
        inMove.setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setApproveUserId(approveUserId)
                .setApproveTime(approveTime);
        operateLogService.recordStatus(ERP_STOCK_MOVE_TYPE, inMove.getId(), inMove.getNo(), true);

        Map<String, ErpStockMoveItemDO> transferInItemMap = transferInMirror.getItems().stream()
                .collect(Collectors.toMap(this::buildTransferItemKey, item -> item, (first, second) -> first));
        Integer toBizType = ErpStockRecordBizTypeEnum.MOVE_IN.getType();
        stockMoveItems.forEach(stockMoveItem -> {
            ErpStockMoveItemDO transferInItem = transferInItemMap.get(buildTransferItemKey(stockMoveItem));
            if (transferInItem == null || transferInItem.getId() == null) {
                throw exception(STOCK_MOVE_APPROVE_FAIL);
            }
            BigDecimal toCount = stockMoveItem.getCount();
            // 调拨入库单价：优先取调出仓库当前成本均价；取不到则回退到明细单价。
            BigDecimal toUnitPrice = resolveStockMoveUnitPrice(stockMoveItem, warehouseMap);
            stockRecordService.createStockRecord(new ErpStockRecordCreateReqBO(
                    stockMoveItem.getProductId(), stockMoveItem.getToWarehouseId(), stockMoveItem.getBatchNo(), toCount,
                    toBizType, inMove.getId(), transferInItem.getId(), inMove.getNo(),
                    toUnitPrice, inMove.getMoveTime()));
        });
        if (saleCartTransferOut && stockMove.getSourceId() != null) {
            stockMoveItems.forEach(stockMoveItem -> stockLockService.transferStockLocks(
                    ErpSaleBizSourceTypeEnum.CART.getType(), stockMove.getSourceId(),
                    stockMoveItem.getProductId(), stockMoveItem.getFromWarehouseId(),
                    stockMoveItem.getToWarehouseId()));
            eventPublisher.publishEvent(new ErpSaleCartTransferOutApprovedEvent(
                    stockMove.getSourceId(), stockMove.getId(), approveUserId));
        }
    }

    private TransferInMirror syncTransferInMirror(ErpStockMoveDO outMove, List<ErpStockMoveItemDO> outItems,
                                                  Map<Long, ErpWarehouseDO> warehouseMap) {
        if (!isTransferInMirrorReady(outMove, outItems)) {
            deleteTransferInMirror(outMove);
            return null;
        }
        Map<Long, ErpWarehouseDO> effectiveWarehouseMap = warehouseMap != null
                ? warehouseMap : getWarehouseMap(collectWarehouseIds(outItems), true);
        Long fromDeptId = resolveFirstWarehouseDeptId(outItems, effectiveWarehouseMap, true);
        Long toDeptId = resolveFirstWarehouseDeptId(outItems, effectiveWarehouseMap, false);
        ErpStockMoveDO inMove = findTransferInMirror(outMove);
        boolean created = inMove == null;
        if (inMove != null && ErpAuditStatus.APPROVE.getStatus().equals(inMove.getStatus())) {
            throw exception(STOCK_MOVE_TRANSFER_IN_EXISTS);
        }
        if (created) {
            String inNo = noRedisDAO.generate(ErpNoRedisDAO.STOCK_TRANSFER_IN_NO_PREFIX);
            if (inNo == null || stockMoveMapper.selectByNo(inNo) != null) {
                throw exception(STOCK_MOVE_NO_EXISTS);
            }
            inMove = buildTransferInMirror(outMove, inNo, fromDeptId, toDeptId);
            stockMoveMapper.insert(inMove);
        } else {
            ErpStockMoveDO update = buildTransferInMirror(outMove, inMove.getNo(), fromDeptId, toDeptId)
                    .setId(inMove.getId());
            stockMoveMapper.updateById(update);
            inMove = update;
        }

        stockMoveItemMapper.deleteByMoveId(inMove.getId());
        ErpStockMoveDO finalInMove = inMove;
        List<ErpStockMoveItemDO> inItems = convertList(outItems,
                item -> BeanUtils.toBean(item, ErpStockMoveItemDO.class, in -> in
                        .setId(null)
                        .setMoveId(finalInMove.getId())
                        .setCreator(null)
                        .setCreateTime(null)
                        .setUpdater(null)
                        .setUpdateTime(null)));
        stockMoveItemMapper.insertBatch(inItems);
        outItems.forEach(item -> stockService.ensureStockExists(item.getProductId(), item.getToWarehouseId()));

        stockMoveMapper.updateById(new ErpStockMoveDO().setId(outMove.getId())
                .setRelatedMoveId(inMove.getId()).setRelatedMoveNo(inMove.getNo())
                .setFromDeptId(fromDeptId).setToDeptId(toDeptId));
        outMove.setRelatedMoveId(inMove.getId()).setRelatedMoveNo(inMove.getNo())
                .setFromDeptId(fromDeptId).setToDeptId(toDeptId);
        if (created) {
            operateLogService.recordCreate(ERP_STOCK_MOVE_TYPE, inMove.getId(), inMove.getNo());
        } else {
            operateLogService.recordUpdate(ERP_STOCK_MOVE_TYPE, inMove.getId(), inMove.getNo());
        }
        return new TransferInMirror(inMove, stockMoveItemMapper.selectListByMoveId(inMove.getId()));
    }

    private ErpStockMoveDO buildTransferInMirror(ErpStockMoveDO outMove, String inNo,
                                                  Long fromDeptId, Long toDeptId) {
        return BeanUtils.toBean(outMove, ErpStockMoveDO.class, in -> in
                .setId(null)
                .setNo(inNo)
                .setDeptId(toDeptId)
                .setTransferDirection(TRANSFER_DIRECTION_IN)
                .setRelatedMoveId(outMove.getId())
                .setRelatedMoveNo(outMove.getNo())
                .setFromDeptId(fromDeptId)
                .setToDeptId(toDeptId)
                .setMoveTime(outMove.getMoveTime())
                .setStatus(ErpAuditStatus.PROCESS.getStatus())
                .setApproveUserId(null)
                .setApproveTime(null)
                .setCreator(null)
                .setCreateTime(null)
                .setUpdater(null)
                .setUpdateTime(null));
    }

    private ErpStockMoveDO findTransferInMirror(ErpStockMoveDO outMove) {
        if (outMove == null || outMove.getId() == null) {
            return null;
        }
        if (outMove.getRelatedMoveId() != null) {
            ErpStockMoveDO relatedMove = stockMoveMapper.selectById(outMove.getRelatedMoveId());
            if (relatedMove != null && getTransferDirection(relatedMove) == TRANSFER_DIRECTION_IN
                    && Objects.equals(relatedMove.getRelatedMoveId(), outMove.getId())) {
                return relatedMove;
            }
        }
        return stockMoveMapper.selectByRelatedMoveIdAndDirection(outMove.getId(), TRANSFER_DIRECTION_IN);
    }

    private void deleteTransferInMirror(ErpStockMoveDO outMove) {
        ErpStockMoveDO inMove = findTransferInMirror(outMove);
        if (inMove == null) {
            return;
        }
        if (ErpAuditStatus.APPROVE.getStatus().equals(inMove.getStatus())) {
            throw exception(STOCK_MOVE_TRANSFER_IN_EXISTS);
        }
        stockMoveItemMapper.deleteByMoveId(inMove.getId());
        stockMoveMapper.deleteById(inMove.getId());
        stockMoveMapper.clearRelatedMove(outMove.getId());
        outMove.setRelatedMoveId(null).setRelatedMoveNo(null);
        operateLogService.recordDelete(ERP_STOCK_MOVE_TYPE, inMove.getId(), inMove.getNo());
    }

    private boolean isTransferInMirrorReady(ErpStockMoveDO outMove, List<ErpStockMoveItemDO> outItems) {
        return outMove != null && outMove.getId() != null && CollUtil.isNotEmpty(outItems)
                && outItems.stream().allMatch(item -> item.getProductId() != null
                && item.getFromWarehouseId() != null && item.getToWarehouseId() != null
                && item.getCount() != null);
    }

    private String buildTransferItemKey(ErpStockMoveItemDO item) {
        return item.getProductId() + "-" + item.getFromWarehouseId() + "-" + item.getToWarehouseId();
    }

    private Long resolveFirstWarehouseDeptId(List<ErpStockMoveItemDO> items, Map<Long, ErpWarehouseDO> warehouseMap,
                                             boolean fromWarehouse) {
        if (CollUtil.isEmpty(items) || CollUtil.isEmpty(warehouseMap)) {
            return null;
        }
        for (ErpStockMoveItemDO item : items) {
            Long warehouseId = fromWarehouse ? item.getFromWarehouseId() : item.getToWarehouseId();
            ErpWarehouseDO warehouse = warehouseMap.get(warehouseId);
            if (warehouse != null && warehouse.getDeptId() != null) {
                return warehouse.getDeptId();
            }
        }
        return null;
    }

    private void fillDeptSnapshots(ErpStockMoveDO stockMove, List<ErpStockMoveItemDO> items) {
        Map<Long, ErpWarehouseDO> warehouseMap = getWarehouseMap(collectWarehouseIds(items), true);
        Long resolvedFromDeptId = resolveFirstWarehouseDeptId(items, warehouseMap, true);
        Long resolvedToDeptId = resolveFirstWarehouseDeptId(items, warehouseMap, false);
        Long fromDeptId = resolvedFromDeptId != null ? resolvedFromDeptId : stockMove.getFromDeptId();
        Long toDeptId = resolvedToDeptId != null ? resolvedToDeptId : stockMove.getToDeptId();
        stockMove.setFromDeptId(fromDeptId);
        stockMove.setToDeptId(toDeptId);
        if (CollUtil.isEmpty(items)) {
            return;
        }
        items.forEach(item -> {
            ErpWarehouseDO fromWarehouse = item.getFromWarehouseId() == null ? null : warehouseMap.get(item.getFromWarehouseId());
            ErpWarehouseDO toWarehouse = item.getToWarehouseId() == null ? null : warehouseMap.get(item.getToWarehouseId());
            if (fromWarehouse != null) {
                item.setFromDeptId(fromWarehouse.getDeptId());
            }
            if (toWarehouse != null) {
                item.setToDeptId(toWarehouse.getDeptId());
            }
        });
    }

    private void validateTransferOut(ErpStockMoveDO stockMove) {
        if (TRANSFER_DIRECTION_IN == getTransferDirection(stockMove)) {
            throw exception(STOCK_MOVE_APPROVE_FAIL);
        }
    }

    private int getTransferDirection(ErpStockMoveDO stockMove) {
        return stockMove.getTransferDirection() != null ? stockMove.getTransferDirection() : TRANSFER_DIRECTION_OUT;
    }

    private List<ErpStockMoveItemDO> buildStockTransferOutDraftItems(ErpStockMoveSaveReqVO reqVO) {
        if (reqVO == null || CollUtil.isEmpty(reqVO.getItems())) {
            return Collections.emptyList();
        }
        List<ErpStockMoveSaveReqVO.Item> validItems = new ArrayList<>();
        Set<String> itemKeys = new HashSet<>();
        for (ErpStockMoveSaveReqVO.Item item : reqVO.getItems()) {
            if (item == null || item.getProductId() == null
                    || item.getFromWarehouseId() == null
                    || item.getCount() == null || item.getCount().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            String itemKey = item.getProductId() + "-" + item.getFromWarehouseId() + "-"
                    + (item.getToWarehouseId() != null ? item.getToWarehouseId() : "");
            if (!itemKeys.add(itemKey)) {
                continue;
            }
            ErpStockMoveSaveReqVO.Item validItem = BeanUtils.toBean(item, ErpStockMoveSaveReqVO.Item.class);
            if (validItem.getProductPrice() == null) {
                validItem.setProductPrice(BigDecimal.ZERO);
            }
            validItems.add(validItem);
        }
        if (CollUtil.isEmpty(validItems)) {
            return Collections.emptyList();
        }
        List<ErpProductDO> productList = validProductList(
                convertSet(validItems, ErpStockMoveSaveReqVO.Item::getProductId), true);
        Map<Long, ErpProductDO> productMap = convertMap(productList, ErpProductDO::getId);
        validateStockMoveItemWarehouses(validItems, false, false, reqVO.getDeptId());
        return convertList(validItems, itemReq -> BeanUtils.toBean(itemReq, ErpStockMoveItemDO.class, item -> item
                .setProductUnitId(productMap.get(item.getProductId()).getUnitId())
                .setTotalPrice(MoneyUtils.priceMultiply(item.getProductPrice(), item.getCount()))));
    }

    private List<ErpStockMoveItemDO> validateStockMoveItems(List<ErpStockMoveSaveReqVO.Item> list,
                                                            boolean requireWarehouses,
                                                            boolean saleCartSource,
                                                            Long saleDeptId) {
        validateDuplicateStockMoveItems(list);
        // 1.1 鏍￠獙浜у搧瀛樺湪
        List<ErpProductDO> productList = validProductList(
                convertSet(list, ErpStockMoveSaveReqVO.Item::getProductId), true);
        Map<Long, ErpProductDO> productMap = convertMap(productList, ErpProductDO::getId);
        // 1.2 鏍￠獙浠撳簱瀛樺湪
        validateStockMoveItemWarehouses(list, requireWarehouses, saleCartSource, saleDeptId);
        // 2. 杞寲涓?ErpStockMoveItemDO 鍒楄〃
        return convertList(list, o -> BeanUtils.toBean(o, ErpStockMoveItemDO.class, item -> item
                .setProductUnitId(productMap.get(item.getProductId()).getUnitId())
                .setTotalPrice(MoneyUtils.priceMultiply(item.getProductPrice(), item.getCount()))));
    }

    private void validatePurchaseInSourceMoveCounts(List<ErpStockMoveItemDO> stockMoveItems, Long excludeMoveId) {
        Set<Long> sourceInItemIds = convertSet(stockMoveItems, ErpStockMoveItemDO::getSourceInItemId);
        sourceInItemIds.remove(null);
        if (CollUtil.isEmpty(sourceInItemIds)) {
            return;
        }
        Map<Long, BigDecimal> currentCountMap = new java.util.LinkedHashMap<>();
        stockMoveItems.forEach(item -> {
            if (item.getSourceInItemId() != null) {
                currentCountMap.merge(item.getSourceInItemId(),
                        item.getCount() != null ? item.getCount() : BigDecimal.ZERO, BigDecimal::add);
            }
        });
        Map<Long, ErpPurchaseInItemDO> sourceItemMap = convertMap(
                purchaseInItemMapper.selectBatchIds(sourceInItemIds), ErpPurchaseInItemDO::getId);
        stockMoveItems.forEach(item -> {
            if (item.getSourceInItemId() == null) {
                return;
            }
            ErpPurchaseInItemDO sourceItem = sourceItemMap.get(item.getSourceInItemId());
            if (sourceItem == null
                    || (item.getSourceInId() != null && !Objects.equals(item.getSourceInId(), sourceItem.getInId()))
                    || !Objects.equals(item.getProductId(), sourceItem.getProductId())
                    || !Objects.equals(item.getFromWarehouseId(), sourceItem.getWarehouseId())) {
                throw exception(PURCHASE_IN_TRANSFER_OUT_SOURCE_ITEM_NOT_EXISTS);
            }
            item.setSourceInId(sourceItem.getInId());
            item.setSourceCount(sourceItem.getCount());
        });
        Map<Long, BigDecimal> movedCountMap = stockMoveItemMapper.selectMovedCountMapBySourceInItemIds(
                sourceInItemIds, excludeMoveId);
        for (Map.Entry<Long, BigDecimal> entry : currentCountMap.entrySet()) {
            ErpPurchaseInItemDO sourceItem = sourceItemMap.get(entry.getKey());
            if (sourceItem == null) {
                throw exception(PURCHASE_IN_TRANSFER_OUT_SOURCE_ITEM_NOT_EXISTS);
            }
            BigDecimal sourceCount = sourceItem.getCount() != null ? sourceItem.getCount() : BigDecimal.ZERO;
            BigDecimal movedCount = movedCountMap.getOrDefault(entry.getKey(), BigDecimal.ZERO);
            BigDecimal availableCount = sourceCount.subtract(movedCount);
            if (availableCount.compareTo(BigDecimal.ZERO) < 0) {
                availableCount = BigDecimal.ZERO;
            }
            if (entry.getValue().compareTo(availableCount) > 0) {
                throw exception(PURCHASE_IN_TRANSFER_OUT_EXCEED_AVAILABLE,
                        entry.getKey(), entry.getValue(), availableCount);
            }
        }
    }

    private List<ErpProductDO> validProductList(Collection<Long> productIds, boolean ignoreProductDataPermission) {
        if (ignoreProductDataPermission) {
            return DataPermissionUtils.executeIgnore(() -> productService.validProductList(productIds));
        }
        return productService.validProductList(productIds);
    }

    private Map<Long, ErpWarehouseDO> getWarehouseMap(Collection<Long> warehouseIds, boolean ignoreWarehouseDataPermission) {
        if (ignoreWarehouseDataPermission) {
            return DataPermissionUtils.executeIgnore(() -> warehouseService.getWarehouseMap(warehouseIds));
        }
        return warehouseService.getWarehouseMap(warehouseIds);
    }

    private boolean isSaleCartSource(Integer sourceType) {
        return ErpSaleBizSourceTypeEnum.CART.getType().equals(sourceType);
    }

    private BigDecimal resolveStockMoveUnitPrice(ErpStockMoveItemDO item,
                                                 Map<Long, ErpWarehouseDO> warehouseMap) {
        if (isCrossDeptMove(item.getFromWarehouseId(), item.getToWarehouseId(), warehouseMap)) {
            return item.getProductPrice();
        }
        ErpStockDO fromStock = DataPermissionUtils.executeIgnore(() ->
                stockService.getStock(item.getProductId(), item.getFromWarehouseId()));
        return (fromStock != null && fromStock.getCostPrice() != null)
                ? fromStock.getCostPrice() : item.getProductPrice();
    }

    private boolean isCrossDeptMove(Long fromWarehouseId, Long toWarehouseId, Map<Long, ErpWarehouseDO> warehouseMap) {
        ErpWarehouseDO fromWarehouse = warehouseMap.get(fromWarehouseId);
        ErpWarehouseDO toWarehouse = warehouseMap.get(toWarehouseId);
        return fromWarehouse != null && toWarehouse != null
                && fromWarehouse.getDeptId() != null && toWarehouse.getDeptId() != null
                && !fromWarehouse.getDeptId().equals(toWarehouse.getDeptId());
    }

    private void validateStockMoveItemWarehouses(List<ErpStockMoveSaveReqVO.Item> list, boolean requireWarehouses,
                                                 boolean saleCartSource, Long saleDeptId) {
        Set<Long> warehouseIds = new HashSet<>();
        Set<Long> fromWarehouseIds = new HashSet<>();
        Set<Long> toWarehouseIds = new HashSet<>();
        for (ErpStockMoveSaveReqVO.Item item : list) {
            if (requireWarehouses && (item.getFromWarehouseId() == null || item.getToWarehouseId() == null)) {
                throw exception(STOCK_MOVE_WAREHOUSE_REQUIRED);
            }
            if (item.getFromWarehouseId() != null && item.getToWarehouseId() != null
                    && item.getFromWarehouseId().equals(item.getToWarehouseId())) {
                throw exception(STOCK_MOVE_WAREHOUSE_SAME);
            }
            if (item.getFromWarehouseId() != null) {
                warehouseIds.add(item.getFromWarehouseId());
                fromWarehouseIds.add(item.getFromWarehouseId());
            }
            if (item.getToWarehouseId() != null) {
                warehouseIds.add(item.getToWarehouseId());
                toWarehouseIds.add(item.getToWarehouseId());
            }
        }
        if (saleCartSource) {
            warehouseService.validSaleWarehouseList(fromWarehouseIds);
            validWarehouseList(toWarehouseIds, true);
            if (!isSaleCartCrossDeptMove(saleDeptId, fromWarehouseIds)) {
                warehouseService.validateCurrentUserWarehousePermission(toWarehouseIds);
            }
            fromWarehouseIds.forEach(warehouseId ->
                    warehouseService.validateWarehouseSaleAllowedForDept(warehouseId, saleDeptId));
            return;
        }
        validWarehouseList(warehouseIds, true);
        warehouseService.validateCurrentUserStockMoveFromWarehousePermission(fromWarehouseIds);
        warehouseService.validateCurrentUserWarehousePermission(toWarehouseIds);
    }

    private void validateStockMoveItemsReadyForApprove(List<ErpStockMoveItemDO> list, boolean saleCartSource,
                                                       Long saleDeptId) {
        if (CollUtil.isEmpty(list)) {
            throw exception(STOCK_MOVE_ITEM_EMPTY);
        }
        Set<Long> warehouseIds = new HashSet<>();
        Set<Long> fromWarehouseIds = new HashSet<>();
        Set<Long> toWarehouseIds = new HashSet<>();
        for (ErpStockMoveItemDO item : list) {
            if (item.getCount() == null || item.getCount().compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(STOCK_MOVE_ITEM_COUNT_POSITIVE);
            }
            if (item.getProductPrice() == null || item.getProductPrice().compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(STOCK_MOVE_ITEM_PRICE_POSITIVE);
            }
            if (item.getFromWarehouseId() == null || item.getToWarehouseId() == null) {
                throw exception(STOCK_MOVE_WAREHOUSE_REQUIRED);
            }
            if (item.getFromWarehouseId().equals(item.getToWarehouseId())) {
                throw exception(STOCK_MOVE_WAREHOUSE_SAME);
            }
            warehouseIds.add(item.getFromWarehouseId());
            warehouseIds.add(item.getToWarehouseId());
            fromWarehouseIds.add(item.getFromWarehouseId());
            toWarehouseIds.add(item.getToWarehouseId());
        }
        if (saleCartSource) {
            // 审批权已经按调出部门校验；这里仅保留仓库启用、销售用途及部门分配等业务校验。
            warehouseService.validSaleWarehouseListForDept(fromWarehouseIds, saleDeptId);
            validWarehouseList(toWarehouseIds, true);
            return;
        }
        // 普通调拨同样只校验仓库有效性，不再要求审批人拥有调入仓库的数据权限。
        validWarehouseList(warehouseIds, true);
    }

    private List<ErpWarehouseDO> validWarehouseList(Collection<Long> warehouseIds, boolean ignoreWarehouseDataPermission) {
        if (ignoreWarehouseDataPermission) {
            return DataPermissionUtils.executeIgnore(() -> warehouseService.validWarehouseList(warehouseIds));
        }
        return warehouseService.validWarehouseList(warehouseIds);
    }

    private boolean isSaleCartCrossDeptMove(ErpStockMoveDO stockMove, List<ErpStockMoveItemDO> items) {
        return stockMove != null && isSaleCartSource(stockMove.getSourceType())
                && isSaleCartCrossDeptMove(stockMove.getDeptId(), collectFromWarehouseIds(items));
    }

    private boolean isSaleCartCrossDeptMove(ErpStockMoveDO stockMove, List<ErpStockMoveItemDO> items,
                                            Map<Long, ErpWarehouseDO> warehouseMap) {
        if (stockMove == null || !isSaleCartSource(stockMove.getSourceType()) || stockMove.getDeptId() == null) {
            return false;
        }
        for (Long fromWarehouseId : collectFromWarehouseIds(items)) {
            ErpWarehouseDO fromWarehouse = warehouseMap.get(fromWarehouseId);
            if (fromWarehouse != null && fromWarehouse.getDeptId() != null
                    && !Objects.equals(fromWarehouse.getDeptId(), stockMove.getDeptId())) {
                return true;
            }
        }
        return false;
    }

    private boolean isSaleCartCrossDeptMove(Long saleDeptId, Collection<Long> fromWarehouseIds) {
        if (saleDeptId == null || CollUtil.isEmpty(fromWarehouseIds)) {
            return false;
        }
        Map<Long, ErpWarehouseDO> warehouseMap = getWarehouseMap(fromWarehouseIds, true);
        for (Long fromWarehouseId : fromWarehouseIds) {
            ErpWarehouseDO fromWarehouse = warehouseMap.get(fromWarehouseId);
            if (fromWarehouse != null && fromWarehouse.getDeptId() != null
                    && !Objects.equals(fromWarehouse.getDeptId(), saleDeptId)) {
                return true;
            }
        }
        return false;
    }

    private Set<Long> collectFromWarehouseIds(List<ErpStockMoveItemDO> items) {
        Set<Long> fromWarehouseIds = new HashSet<>();
        if (CollUtil.isEmpty(items)) {
            return fromWarehouseIds;
        }
        items.forEach(item -> {
            if (item.getFromWarehouseId() != null) {
                fromWarehouseIds.add(item.getFromWarehouseId());
            }
        });
        return fromWarehouseIds;
    }

    private Set<Long> collectFromDeptIds(ErpStockMoveDO stockMove, List<ErpStockMoveItemDO> items) {
        return collectFromDeptIds(stockMove, items, null);
    }

    private Set<Long> collectFromDeptSnapshotIds(ErpStockMoveDO stockMove, List<ErpStockMoveItemDO> items) {
        Set<Long> fromDeptIds = new HashSet<>();
        if (CollUtil.isEmpty(items)) {
            if (stockMove != null && stockMove.getFromDeptId() != null) {
                fromDeptIds.add(stockMove.getFromDeptId());
            }
            return fromDeptIds;
        }
        for (ErpStockMoveItemDO item : items) {
            if (item.getFromDeptId() == null) {
                return Collections.emptySet();
            }
            fromDeptIds.add(item.getFromDeptId());
        }
        return fromDeptIds;
    }

    private Set<Long> collectFromDeptIds(ErpStockMoveDO stockMove, List<ErpStockMoveItemDO> items,
                                         Map<Long, ErpWarehouseDO> loadedWarehouseMap) {
        Set<Long> fromDeptIds = new HashSet<>();
        if (CollUtil.isEmpty(items)) {
            if (stockMove != null && stockMove.getFromDeptId() != null) {
                fromDeptIds.add(stockMove.getFromDeptId());
            }
            return fromDeptIds;
        }
        Set<Long> unresolvedWarehouseIds = new HashSet<>();
        items.forEach(item -> {
            if (item.getFromDeptId() != null) {
                fromDeptIds.add(item.getFromDeptId());
            } else if (item.getFromWarehouseId() != null) {
                unresolvedWarehouseIds.add(item.getFromWarehouseId());
            }
        });
        if (CollUtil.isNotEmpty(unresolvedWarehouseIds)) {
            Map<Long, ErpWarehouseDO> warehouseMap = loadedWarehouseMap != null ? loadedWarehouseMap
                    : getWarehouseMap(unresolvedWarehouseIds, true);
            for (Long warehouseId : unresolvedWarehouseIds) {
                ErpWarehouseDO warehouse = warehouseMap.get(warehouseId);
                if (warehouse == null || warehouse.getDeptId() == null) {
                    return Collections.emptySet();
                }
                fromDeptIds.add(warehouse.getDeptId());
            }
        }
        return fromDeptIds;
    }

    private Set<Long> collectWarehouseIds(List<ErpStockMoveItemDO> items) {
        Set<Long> warehouseIds = new HashSet<>();
        if (CollUtil.isEmpty(items)) {
            return warehouseIds;
        }
        items.forEach(item -> {
            if (item.getFromWarehouseId() != null) {
                warehouseIds.add(item.getFromWarehouseId());
            }
            if (item.getToWarehouseId() != null) {
                warehouseIds.add(item.getToWarehouseId());
            }
        });
        return warehouseIds;
    }

    private boolean isAncestorDept(Long childDeptId, Long possibleAncestorDeptId) {
        if (childDeptId == null || possibleAncestorDeptId == null || Objects.equals(childDeptId, possibleAncestorDeptId)) {
            return false;
        }
        Long currentId = childDeptId;
        Set<Long> visited = new HashSet<>();
        while (currentId != null && currentId > 0 && visited.add(currentId)) {
            DeptRespDTO current = deptApi.getDept(currentId);
            if (current == null || current.getParentId() == null || current.getParentId() <= 0) {
                return false;
            }
            Long parentId = current.getParentId();
            if (Objects.equals(parentId, possibleAncestorDeptId)) {
                return true;
            }
            currentId = parentId;
        }
        return false;
    }

    private boolean isAncestorDept(Long childDeptId, Long possibleAncestorDeptId,
                                   Map<Long, DeptRespDTO> deptHierarchyMap) {
        if (childDeptId == null || possibleAncestorDeptId == null
                || Objects.equals(childDeptId, possibleAncestorDeptId)) {
            return false;
        }
        Long currentId = childDeptId;
        Set<Long> visited = new HashSet<>();
        while (currentId != null && currentId > 0 && visited.add(currentId)) {
            DeptRespDTO current = deptHierarchyMap.get(currentId);
            if (current == null || current.getParentId() == null || current.getParentId() <= 0) {
                return false;
            }
            Long parentId = current.getParentId();
            if (Objects.equals(parentId, possibleAncestorDeptId)) {
                return true;
            }
            currentId = parentId;
        }
        return false;
    }

    private Map<Long, DeptRespDTO> getDeptHierarchyMap(Set<Long> childDeptIds, Long possibleAncestorDeptId) {
        if (CollUtil.isEmpty(childDeptIds) || possibleAncestorDeptId == null) {
            return Collections.emptyMap();
        }
        Map<Long, DeptRespDTO> hierarchyMap = new HashMap<>();
        Set<Long> pendingDeptIds = new HashSet<>(childDeptIds);
        while (CollUtil.isNotEmpty(pendingDeptIds)) {
            Map<Long, DeptRespDTO> currentLevelMap = deptApi.getDeptMap(pendingDeptIds);
            if (CollUtil.isEmpty(currentLevelMap)) {
                break;
            }
            hierarchyMap.putAll(currentLevelMap);
            Set<Long> parentDeptIds = new HashSet<>();
            currentLevelMap.values().forEach(dept -> {
                Long parentId = dept.getParentId();
                if (parentId != null && parentId > 0 && !Objects.equals(parentId, possibleAncestorDeptId)
                        && !hierarchyMap.containsKey(parentId)) {
                    parentDeptIds.add(parentId);
                }
            });
            pendingDeptIds = parentDeptIds;
        }
        return hierarchyMap;
    }

    private void validateDuplicateStockMoveItems(List<ErpStockMoveSaveReqVO.Item> list) {
        Set<String> keys = new HashSet<>();
        for (ErpStockMoveSaveReqVO.Item item : list) {
            String key = item.getProductId() + "-" + item.getFromWarehouseId() + "-" + item.getToWarehouseId();
            if (!keys.add(key)) {
                throw exception(STOCK_MOVE_ITEM_DUPLICATE, key);
            }
        }
    }

    private void updateStockMoveItemList(Long id, List<ErpStockMoveItemDO> newList) {
        // 绗竴姝ワ紝瀵规瘮鏂拌€佹暟鎹紝鑾峰緱娣诲姞銆佷慨鏀广€佸垹闄ょ殑鍒楄〃
        List<ErpStockMoveItemDO> oldList = stockMoveItemMapper.selectListByMoveId(id);
        List<List<ErpStockMoveItemDO>> diffList = diffList(oldList, newList, // id 涓嶅悓锛屽氨璁や负鏄笉鍚岀殑璁板綍
                (oldVal, newVal) -> oldVal.getId().equals(newVal.getId()));

        // 绗簩姝ワ紝鎵归噺娣诲姞銆佷慨鏀广€佸垹闄?
        if (CollUtil.isNotEmpty(diffList.get(0))) {
            diffList.get(0).forEach(o -> o.setMoveId(id));
            stockMoveItemMapper.insertBatch(diffList.get(0));
        }
        if (CollUtil.isNotEmpty(diffList.get(1))) {
            stockMoveItemMapper.updateBatch(diffList.get(1));
        }
        if (CollUtil.isNotEmpty(diffList.get(2))) {
            stockMoveItemMapper.deleteByIds(convertList(diffList.get(2), ErpStockMoveItemDO::getId));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteStockMove(List<Long> ids) {
        ErpStockTransferOutPermissionScope scope = getTransferOutPermissionScope();
        validateStockTransferOutVisible(ids, scope);
        if (scope == null) {
            doDeleteStockMove(ids);
            return;
        }
        DataPermissionUtils.executeIgnore(() -> doDeleteStockMove(ids));
    }

    private void doDeleteStockMove(List<Long> ids) {
        // 1. 鏍￠獙涓嶅浜庡凡瀹℃壒
        List<ErpStockMoveDO> stockMoves = stockMoveMapper.selectByIds(ids);
        if (CollUtil.isEmpty(stockMoves)) {
            return;
        }
        stockMoves.forEach(stockMove -> {
            validateTransferOut(stockMove);
            if (ErpAuditStatus.APPROVE.getStatus().equals(stockMove.getStatus())) {
                throw exception(STOCK_MOVE_DELETE_FAIL_APPROVE, stockMove.getNo());
            }
            if (isSaleCartSource(stockMove.getSourceType())) {
                throw exception(STOCK_MOVE_DELETE_CART_SOURCE_DENIED);
            }
            List<ErpStockMoveItemDO> stockMoveItems = stockMoveItemMapper.selectListByMoveId(stockMove.getId());
            ErpStockMoveOperationPermission deletePermission = getDeletePermission(stockMove, stockMoveItems);
            if (!Boolean.TRUE.equals(deletePermission.getAllowed())) {
                throw exception(STOCK_MOVE_DELETE_CROSS_DEPT_DENIED);
            }
        });

        // 2. 閬嶅巻鍒犻櫎锛屽苟璁板綍鎿嶄綔鏃ュ織
        stockMoves.forEach(stockMove -> {
            deleteTransferInMirror(stockMove);
            // 2.1 鍒犻櫎鍑哄簱鍗?
            stockMoveMapper.deleteById(stockMove.getId());
            // 2.2 鍒犻櫎鍑哄簱鍗曢」
            stockMoveItemMapper.deleteByMoveId(stockMove.getId());
            operateLogService.recordDelete(ERP_STOCK_MOVE_TYPE, stockMove.getId(), stockMove.getNo());
        });
    }

    private ErpStockMoveDO validateStockMoveExists(Long id) {
        ErpStockMoveDO stockMove = stockMoveMapper.selectById(id);
        if (stockMove == null) {
            throw exception(STOCK_MOVE_NOT_EXISTS);
        }
        return stockMove;
    }

    @Override
    public ErpStockMoveDO getStockMove(Long id) {
        return stockMoveMapper.selectById(id);
    }

    @Override
    public ErpStockMoveDO getVisibleStockTransferOut(Long id) {
        ErpStockTransferOutPermissionScope scope = getTransferOutPermissionScope();
        if (scope == null) {
            return stockMoveMapper.selectById(id);
        }
        return getVisibleStockTransferOut(id, scope);
    }

    private ErpStockMoveDO getVisibleStockTransferOut(Long id, ErpStockTransferOutPermissionScope scope) {
        return DataPermissionUtils.executeIgnore(() -> stockMoveMapper.selectVisibleTransferOutById(
                id, scope.getDeptIds(), scope.isAll()));
    }

    @Override
    public void validateStockTransferOutVisible(Long id) {
        validateStockTransferOutVisible(id, getTransferOutPermissionScope());
    }

    private void validateStockTransferOutVisible(Long id, ErpStockTransferOutPermissionScope scope) {
        if (scope == null) {
            return;
        }
        if (getVisibleStockTransferOut(id, scope) == null) {
            throw exception(STOCK_MOVE_NOT_EXISTS);
        }
    }

    private void validateStockTransferOutVisible(Collection<Long> ids, ErpStockTransferOutPermissionScope scope) {
        if (scope == null || CollUtil.isEmpty(ids)) {
            return;
        }
        Set<Long> expectedIds = ids.stream().filter(Objects::nonNull).collect(Collectors.toSet());
        if (expectedIds.isEmpty()) {
            return;
        }
        List<ErpStockMoveDO> visibleStockMoves = DataPermissionUtils.executeIgnore(() ->
                stockMoveMapper.selectVisibleTransferOutListByIds(expectedIds, scope.getDeptIds(),
                        scope.isAll()));
        Set<Long> visibleIds = convertSet(visibleStockMoves, ErpStockMoveDO::getId);
        if (!visibleIds.containsAll(expectedIds)) {
            throw exception(STOCK_MOVE_NOT_EXISTS);
        }
    }

    private void replaceStockMoveItems(Long id, List<ErpStockMoveItemDO> items) {
        stockMoveItemMapper.deleteByMoveId(id);
        if (CollUtil.isEmpty(items)) {
            return;
        }
        items.forEach(item -> item.setId(null).setMoveId(id));
        stockMoveItemMapper.insertBatch(items);
    }

    @Override
    public ErpStockMoveDO getVisibleStockTransferIn(Long id) {
        ErpStockTransferOutPermissionScope scope = getTransferInPermissionScope();
        if (scope == null) {
            return stockMoveMapper.selectById(id);
        }
        return DataPermissionUtils.executeIgnore(() -> stockMoveMapper.selectVisibleTransferInById(
                id, scope.getDeptIds(), scope.isAll()));
    }

    @Override
    public ErpStockMoveDO getStockMoveForUpdate(Long id) {
        return stockMoveMapper.selectByIdForUpdate(id);
    }

    @Override
    public PageResult<ErpStockMoveDO> getStockMovePage(ErpStockMovePageReqVO pageReqVO) {
        if (pageReqVO.getTransferDirection() == null) {
            pageReqVO.setTransferDirection(TRANSFER_DIRECTION_OUT);
        }
        return stockMoveMapper.selectPage(pageReqVO);
    }

    @Override
    public PageResult<ErpStockMoveDO> getVisibleStockTransferOutPage(ErpStockMovePageReqVO pageReqVO) {
        return getVisibleStockTransferOutPage(pageReqVO, getTransferOutPermissionScope());
    }

    @Override
    public PageResult<ErpStockMoveDO> getVisibleStockTransferOutPage(
            ErpStockMovePageReqVO pageReqVO, ErpStockTransferOutPermissionScope scope) {
        pageReqVO.setTransferDirection(TRANSFER_DIRECTION_OUT);
        if (scope == null) {
            return stockMoveMapper.selectPage(pageReqVO);
        }
        return DataPermissionUtils.executeIgnore(() -> stockMoveMapper.selectTransferOutPage(pageReqVO,
                scope.getDeptIds(), scope.isAll()));
    }

    @Override
    public PageResult<ErpStockMoveDO> getVisibleStockTransferInPage(ErpStockMovePageReqVO pageReqVO) {
        pageReqVO.setTransferDirection(TRANSFER_DIRECTION_IN);
        ErpStockTransferOutPermissionScope scope = getTransferInPermissionScope();
        if (scope == null) {
            return stockMoveMapper.selectPage(pageReqVO);
        }
        return DataPermissionUtils.executeIgnore(() -> stockMoveMapper.selectTransferInPage(pageReqVO,
                scope.getDeptIds(), scope.isAll()));
    }

    @Override
    public ErpStockTransferOutPermissionScope getTransferOutPermissionScope() {
        Long loginUserId = getLoginUserId();
        if (loginUserId == null || permissionApi == null) {
            return null;
        }
        DeptDataPermissionRespDTO permission = permissionApi.getDeptDataPermission(
                loginUserId, TRANSFER_OUT_DATA_PERMISSION_FORM);
        if (permission == null) {
            return new ErpStockTransferOutPermissionScope(false, Collections.emptySet());
        }
        return new ErpStockTransferOutPermissionScope(Boolean.TRUE.equals(permission.getAll()),
                permission.getDeptIds());
    }

    @Override
    public ErpStockTransferOutPermissionScope getTransferInPermissionScope() {
        Long loginUserId = getLoginUserId();
        if (loginUserId == null || permissionApi == null) {
            return null;
        }
        DeptDataPermissionRespDTO permission = permissionApi.getDeptDataPermission(
                loginUserId, TRANSFER_IN_DATA_PERMISSION_FORM);
        if (permission == null) {
            return new ErpStockTransferOutPermissionScope(false, Collections.emptySet());
        }
        return new ErpStockTransferOutPermissionScope(Boolean.TRUE.equals(permission.getAll()), permission.getDeptIds());
    }

    // ==================== 鍑哄簱椤?====================

    @Override
    public List<ErpStockMoveItemDO> getStockMoveItemListByMoveId(Long moveId) {
        return stockMoveItemMapper.selectListByMoveId(moveId);
    }

    @Override
    public List<ErpStockMoveItemDO> getStockMoveItemListByMoveIds(Collection<Long> moveIds) {
        if (CollUtil.isEmpty(moveIds)) {
            return Collections.emptyList();
        }
        return stockMoveItemMapper.selectListByMoveIds(moveIds);
    }

    private static final class TransferOutApproveContext {

        private final Map<Long, ErpWarehouseDO> warehouseMap;
        private final Map<Long, DeptRespDTO> deptHierarchyMap;
        private final Long loginDeptId;

        private TransferOutApproveContext(Map<Long, ErpWarehouseDO> warehouseMap,
                                          Map<Long, DeptRespDTO> deptHierarchyMap,
                                          Long loginDeptId) {
            this.warehouseMap = warehouseMap;
            this.deptHierarchyMap = deptHierarchyMap;
            this.loginDeptId = loginDeptId;
        }

        private Map<Long, ErpWarehouseDO> getWarehouseMap() {
            return warehouseMap;
        }

        private Map<Long, DeptRespDTO> getDeptHierarchyMap() {
            return deptHierarchyMap;
        }

        private Long getLoginDeptId() {
            return loginDeptId;
        }
    }

    private static final class TransferInMirror {

        private final ErpStockMoveDO move;
        private final List<ErpStockMoveItemDO> items;

        private TransferInMirror(ErpStockMoveDO move, List<ErpStockMoveItemDO> items) {
            this.move = move;
            this.items = items;
        }

        private ErpStockMoveDO getMove() {
            return move;
        }

        private List<ErpStockMoveItemDO> getItems() {
            return items;
        }
    }

}
