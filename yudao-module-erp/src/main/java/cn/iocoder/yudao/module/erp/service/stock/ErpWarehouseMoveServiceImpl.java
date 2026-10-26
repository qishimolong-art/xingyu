package cn.iocoder.yudao.module.erp.service.stock;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.ErpStockUpdateRemarkReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMoveDraftCreateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMoveDraftUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMoveItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMovePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMoveSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMoveSummaryRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery.ErpSalePickDeliverySummaryRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseMoveDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseMoveItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseMoveItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseMoveMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockRecordMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.common.ErpBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSalePickStatusEnum;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import cn.iocoder.yudao.module.erp.enums.stock.ErpWarehouseMoveStatusEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSalePickDeliveryService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpAssemblyWarehouseMoveCreateReqBO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_WAREHOUSE_MOVE_TYPE;

@Service
@Validated
public class ErpWarehouseMoveServiceImpl implements ErpWarehouseMoveService {

    private static final LocalDateTime EARLIEST_VALID_BUSINESS_TIME = LocalDateTime.of(2000, 1, 1, 0, 0);

    @Resource
    private ErpWarehouseMoveMapper warehouseMoveMapper;
    @Resource
    private ErpWarehouseMoveItemMapper warehouseMoveItemMapper;
    @Resource
    private ErpStockRecordMapper stockRecordMapper;
    @Resource
    private ErpStockMapper stockMapper;
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
    private ErpOperateLogService operateLogService;
    @Resource
    private ErpStockItemSnapshotSupport snapshotSupport;
    @Resource
    private ErpSalePickDeliveryService salePickDeliveryService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createWarehouseMove(ErpWarehouseMoveSaveReqVO createReqVO) {
        List<ErpWarehouseMoveItemDO> items = validateWarehouseMoveItems(createReqVO);
        String no = noRedisDAO.generate(ErpNoRedisDAO.WAREHOUSE_MOVE_NO_PREFIX);
        if (warehouseMoveMapper.selectByNo(no) != null) {
            throw exception(WAREHOUSE_MOVE_NO_EXISTS);
        }
        ErpWarehouseMoveDO warehouseMove = BeanUtils.toBean(createReqVO, ErpWarehouseMoveDO.class, in -> in
                .setNo(no).setStatus(ErpWarehouseMoveStatusEnum.PROCESS.getStatus())
                .setTotalCount(getSumValue(items, ErpWarehouseMoveItemDO::getCount, BigDecimal::add, BigDecimal.ZERO))
                .setTotalPrice(getSumValue(items, ErpWarehouseMoveItemDO::getTotalPrice, BigDecimal::add, BigDecimal.ZERO))
                .setTotalCostAmount(getSumValue(items, ErpWarehouseMoveItemDO::getCostAmount, BigDecimal::add, BigDecimal.ZERO)));
        warehouseMoveMapper.insert(warehouseMove);
        items.forEach(item -> item.setMoveId(warehouseMove.getId()));
        warehouseMoveItemMapper.insertBatch(items);
        operateLogService.recordCreate(ERP_WAREHOUSE_MOVE_TYPE, warehouseMove.getId(), warehouseMove.getNo());
        return warehouseMove.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createWarehouseMoveDraft(ErpWarehouseMoveDraftCreateReqVO createReqVO) {
        List<ErpWarehouseMoveItemDO> items = buildDraftItems(createReqVO);
        if (CollUtil.isEmpty(items)) {
            throw exception(WAREHOUSE_MOVE_DRAFT_ITEMS_REQUIRED);
        }
        String no = noRedisDAO.generate(ErpNoRedisDAO.WAREHOUSE_MOVE_NO_PREFIX);
        if (warehouseMoveMapper.selectByNo(no) != null) {
            throw exception(WAREHOUSE_MOVE_NO_EXISTS);
        }
        ErpWarehouseMoveDO warehouseMove = BeanUtils.toBean(createReqVO, ErpWarehouseMoveDO.class, target -> target
                .setNo(no)
                .setStatus(ErpWarehouseMoveStatusEnum.DRAFT.getStatus())
                .setTotalCount(getSumValue(items, ErpWarehouseMoveItemDO::getCount,
                        BigDecimal::add, BigDecimal.ZERO))
                .setTotalPrice(getSumValue(items, ErpWarehouseMoveItemDO::getTotalPrice,
                        BigDecimal::add, BigDecimal.ZERO))
                .setTotalCostAmount(getSumValue(items, ErpWarehouseMoveItemDO::getCostAmount,
                        BigDecimal::add, BigDecimal.ZERO)));
        warehouseMoveMapper.insert(warehouseMove);
        replaceWarehouseMoveItems(warehouseMove.getId(), items);
        operateLogService.recordCreate(ERP_WAREHOUSE_MOVE_TYPE, warehouseMove.getId(), warehouseMove.getNo());
        return warehouseMove.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWarehouseMove(ErpWarehouseMoveSaveReqVO updateReqVO) {
        ErpWarehouseMoveDO warehouseMove = validateWarehouseMoveExists(updateReqVO.getId());
        validateNotAssemblyManaged(warehouseMove);
        if (ErpWarehouseMoveStatusEnum.PICKING.getStatus().equals(warehouseMove.getStatus())) {
            throw exception(WAREHOUSE_MOVE_UPDATE_FAIL_CONFIRMED, warehouseMove.getNo());
        }
        if (ErpWarehouseMoveStatusEnum.APPROVE.getStatus().equals(warehouseMove.getStatus())) {
            throw exception(WAREHOUSE_MOVE_UPDATE_FAIL_APPROVE, warehouseMove.getNo());
        }
        List<ErpWarehouseMoveItemDO> oldItems = warehouseMoveItemMapper.selectListByMoveIdForUpdate(updateReqVO.getId());
        boolean incrementalItems = ErpStockItemOperationHelper.useIncrementalItems(updateReqVO.getItems(),
                ErpWarehouseMoveSaveReqVO.Item::getOperation, WAREHOUSE_MOVE_ITEM_OPERATION_INVALID);
        ErpStockItemOperationHelper.RequestChangeSet<ErpWarehouseMoveSaveReqVO.Item> itemChangeSet = null;
        List<ErpWarehouseMoveSaveReqVO.Item> itemReqs = updateReqVO.getItems();
        if (incrementalItems) {
            itemChangeSet = ErpStockItemOperationHelper.buildRequestChangeSet(updateReqVO.getItems(), oldItems,
                    ErpWarehouseMoveSaveReqVO.Item.class, ErpWarehouseMoveSaveReqVO.Item::getId,
                    ErpWarehouseMoveSaveReqVO.Item::setId, ErpWarehouseMoveSaveReqVO.Item::getOperation,
                    ErpWarehouseMoveItemDO::getId, WAREHOUSE_MOVE_ITEM_OPERATION_INVALID,
                    WAREHOUSE_MOVE_ITEM_UPDATE_NOT_EXISTS);
            itemReqs = itemChangeSet.getFinalItems();
        }
        ErpWarehouseMoveSaveReqVO finalReqVO = BeanUtils.toBean(updateReqVO, ErpWarehouseMoveSaveReqVO.class);
        finalReqVO.setItems(itemReqs);
        List<ErpWarehouseMoveItemDO> items = validateWarehouseMoveItems(finalReqVO);
        ErpWarehouseMoveDO updateObj = BeanUtils.toBean(updateReqVO, ErpWarehouseMoveDO.class, in -> in
                .setTotalCount(getSumValue(items, ErpWarehouseMoveItemDO::getCount, BigDecimal::add, BigDecimal.ZERO))
                .setTotalPrice(getSumValue(items, ErpWarehouseMoveItemDO::getTotalPrice, BigDecimal::add, BigDecimal.ZERO))
                .setTotalCostAmount(getSumValue(items, ErpWarehouseMoveItemDO::getCostAmount, BigDecimal::add, BigDecimal.ZERO)));
        if (updateObj.getDeptId() == null) {
            updateObj.setDeptId(warehouseMove.getDeptId());
        }
        warehouseMoveMapper.updateById(updateObj);
        if (incrementalItems) {
            applyWarehouseMoveItemChangeSet(updateReqVO.getId(), itemChangeSet, items);
        } else {
            updateWarehouseMoveItemList(updateReqVO.getId(), oldItems, items);
        }
        operateLogService.recordUpdate(ERP_WAREHOUSE_MOVE_TYPE, warehouseMove.getId(), warehouseMove.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWarehouseMoveDraft(ErpWarehouseMoveDraftUpdateReqVO updateReqVO) {
        ErpWarehouseMoveDO warehouseMove = validateWarehouseMoveExists(updateReqVO.getId());
        validateNotAssemblyManaged(warehouseMove);
        if (!ErpWarehouseMoveStatusEnum.DRAFT.getStatus().equals(warehouseMove.getStatus())) {
            throw exception(WAREHOUSE_MOVE_UPDATE_FAIL_NOT_DRAFT, warehouseMove.getNo());
        }
        List<ErpWarehouseMoveItemDO> oldItems = warehouseMoveItemMapper.selectListByMoveIdForUpdate(updateReqVO.getId());
        boolean incrementalItems = ErpStockItemOperationHelper.useIncrementalItems(updateReqVO.getItems(),
                ErpWarehouseMoveSaveReqVO.Item::getOperation, WAREHOUSE_MOVE_ITEM_OPERATION_INVALID);
        ErpStockItemOperationHelper.RequestChangeSet<ErpWarehouseMoveSaveReqVO.Item> itemChangeSet = null;
        List<ErpWarehouseMoveSaveReqVO.Item> itemReqs = updateReqVO.getItems();
        if (incrementalItems) {
            itemChangeSet = ErpStockItemOperationHelper.buildRequestChangeSet(updateReqVO.getItems(), oldItems,
                    ErpWarehouseMoveSaveReqVO.Item.class, ErpWarehouseMoveSaveReqVO.Item::getId,
                    ErpWarehouseMoveSaveReqVO.Item::setId, ErpWarehouseMoveSaveReqVO.Item::getOperation,
                    ErpWarehouseMoveItemDO::getId, WAREHOUSE_MOVE_ITEM_OPERATION_INVALID,
                    WAREHOUSE_MOVE_ITEM_UPDATE_NOT_EXISTS);
            itemReqs = itemChangeSet.getFinalItems();
        }
        ErpWarehouseMoveSaveReqVO finalReqVO = BeanUtils.toBean(updateReqVO, ErpWarehouseMoveSaveReqVO.class);
        finalReqVO.setItems(itemReqs);
        List<ErpWarehouseMoveItemDO> items = buildDraftItems(finalReqVO);
        ErpWarehouseMoveDO updateObj = BeanUtils.toBean(updateReqVO, ErpWarehouseMoveDO.class, target -> target
                .setDeptId(updateReqVO.getDeptId() != null ? updateReqVO.getDeptId() : warehouseMove.getDeptId())
                .setTotalCount(getSumValue(items, ErpWarehouseMoveItemDO::getCount,
                        BigDecimal::add, BigDecimal.ZERO))
                .setTotalPrice(getSumValue(items, ErpWarehouseMoveItemDO::getTotalPrice,
                        BigDecimal::add, BigDecimal.ZERO))
                .setTotalCostAmount(getSumValue(items, ErpWarehouseMoveItemDO::getCostAmount,
                        BigDecimal::add, BigDecimal.ZERO)));
        int updateCount = warehouseMoveMapper.updateDraftByIdAndStatus(updateReqVO.getId(),
                ErpWarehouseMoveStatusEnum.DRAFT.getStatus(), updateObj);
        if (updateCount == 0) {
            throw exception(WAREHOUSE_MOVE_UPDATE_FAIL_NOT_DRAFT, warehouseMove.getNo());
        }
        if (incrementalItems) {
            applyWarehouseMoveItemChangeSet(updateReqVO.getId(), itemChangeSet, items);
        } else {
            replaceWarehouseMoveItems(updateReqVO.getId(), items);
        }
        operateLogService.recordUpdate(ERP_WAREHOUSE_MOVE_TYPE, warehouseMove.getId(), warehouseMove.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAndSubmitWarehouseMoveDraft(ErpWarehouseMoveSaveReqVO updateReqVO) {
        ErpWarehouseMoveDO warehouseMove = validateWarehouseMoveExists(updateReqVO.getId());
        if (!ErpWarehouseMoveStatusEnum.DRAFT.getStatus().equals(warehouseMove.getStatus())) {
            throw exception(WAREHOUSE_MOVE_UPDATE_FAIL_NOT_DRAFT, warehouseMove.getNo());
        }
        updateWarehouseMove(updateReqVO);
        submitWarehouseMove(updateReqVO.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitWarehouseMove(Long id) {
        ErpWarehouseMoveDO warehouseMove = validateWarehouseMoveExists(id);
        if (!ErpWarehouseMoveStatusEnum.DRAFT.getStatus().equals(warehouseMove.getStatus())) {
            throw exception(WAREHOUSE_MOVE_SUBMIT_FAIL);
        }
        if (warehouseMove.getMoveTime() == null) {
            throw exception(WAREHOUSE_MOVE_SUBMIT_TIME_REQUIRED);
        }
        if (warehouseMove.getFromWarehouseId() == null || warehouseMove.getToWarehouseId() == null) {
            throw exception(WAREHOUSE_MOVE_WAREHOUSE_REQUIRED);
        }
        List<ErpWarehouseMoveItemDO> items = warehouseMoveItemMapper.selectListByMoveId(id);
        if (CollUtil.isEmpty(items)) {
            throw exception(WAREHOUSE_MOVE_SUBMIT_ITEMS_REQUIRED);
        }
        ErpWarehouseMoveSaveReqVO reqVO = BeanUtils.toBean(warehouseMove, ErpWarehouseMoveSaveReqVO.class);
        reqVO.setItems(BeanUtils.toBean(items, ErpWarehouseMoveSaveReqVO.Item.class));
        validateWarehouseMoveItems(reqVO);
        int updateCount = warehouseMoveMapper.updateByIdAndStatus(id,
                ErpWarehouseMoveStatusEnum.DRAFT.getStatus(),
                new ErpWarehouseMoveDO().setStatus(ErpWarehouseMoveStatusEnum.PROCESS.getStatus()));
        if (updateCount == 0) {
            throw exception(WAREHOUSE_MOVE_SUBMIT_FAIL);
        }
        operateLogService.recordUpdate(ERP_WAREHOUSE_MOVE_TYPE, warehouseMove.getId(), warehouseMove.getNo());
    }

    @Override
    public void updateWarehouseMoveRemark(ErpStockUpdateRemarkReqVO updateReqVO) {
        ErpWarehouseMoveDO warehouseMove = validateWarehouseMoveExists(updateReqVO.getId());
        validateNotAssemblyManaged(warehouseMove);
        warehouseMoveMapper.updateById(new ErpWarehouseMoveDO()
                .setId(updateReqVO.getId()).setRemark(updateReqVO.getRemark()));
        operateLogService.recordUpdate(ERP_WAREHOUSE_MOVE_TYPE, warehouseMove.getId(), warehouseMove.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateWarehouseMoveStatus(Long id, Integer status) {
        throw exception(WAREHOUSE_MOVE_MANUAL_APPROVE_FORBIDDEN);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmWarehouseMove(Long id) {
        ErpWarehouseMoveDO warehouseMove = lockWarehouseMove(id);
        if (!ErpWarehouseMoveStatusEnum.PROCESS.getStatus().equals(warehouseMove.getStatus())) {
            throw exception(WAREHOUSE_MOVE_CONFIRM_FAIL);
        }
        List<ErpWarehouseMoveItemDO> items = warehouseMoveItemMapper.selectListByMoveIdForUpdate(id);
        ErpWarehouseMoveSaveReqVO reqVO = BeanUtils.toBean(warehouseMove, ErpWarehouseMoveSaveReqVO.class);
        reqVO.setItems(BeanUtils.toBean(items, ErpWarehouseMoveSaveReqVO.Item.class));
        validateWarehouseMoveItems(reqVO);
        validateWarehouseMoveItemsReadyForApprove(warehouseMove, items, true);
        int changed = warehouseMoveMapper.updateByIdAndStatus(id, ErpWarehouseMoveStatusEnum.PROCESS.getStatus(),
                new ErpWarehouseMoveDO().setStatus(ErpWarehouseMoveStatusEnum.PICKING.getStatus())
                        .setConfirmUserId(getLoginUserId()).setConfirmTime(LocalDateTime.now()));
        if (changed != 1) {
            throw exception(WAREHOUSE_MOVE_CONFIRM_FAIL);
        }
        salePickDeliveryService.generateForWarehouseMove(id);
        operateLogService.recordUpdate(ERP_WAREHOUSE_MOVE_TYPE, id, warehouseMove.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelConfirmWarehouseMove(Long id) {
        ErpWarehouseMoveDO observed = warehouseMoveMapper.selectById(id);
        if (observed == null) {
            throw exception(WAREHOUSE_MOVE_NOT_EXISTS);
        }
        if (!ErpWarehouseMoveStatusEnum.PICKING.getStatus().equals(observed.getStatus())) {
            throw exception(WAREHOUSE_MOVE_CANCEL_CONFIRM_FAIL);
        }
        salePickDeliveryService.rollbackUnstartedWarehouseMoveFulfillment(id);
        ErpWarehouseMoveDO warehouseMove = lockWarehouseMove(id);
        int changed = warehouseMoveMapper.cancelConfirmByIdAndStatus(id,
                ErpWarehouseMoveStatusEnum.PICKING.getStatus(), ErpWarehouseMoveStatusEnum.PROCESS.getStatus());
        if (changed != 1) {
            throw exception(WAREHOUSE_MOVE_CANCEL_CONFIRM_FAIL);
        }
        operateLogService.recordUpdate(ERP_WAREHOUSE_MOVE_TYPE, id, warehouseMove.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void autoApproveAfterPick(Long id, Long pickUserId) {
        ErpWarehouseMoveDO warehouseMove = lockWarehouseMove(id);
        if (ErpWarehouseMoveStatusEnum.APPROVE.getStatus().equals(warehouseMove.getStatus())) {
            return;
        }
        if (!ErpWarehouseMoveStatusEnum.PICKING.getStatus().equals(warehouseMove.getStatus())) {
            throw exception(WAREHOUSE_MOVE_AUTO_APPROVE_FAIL);
        }
        ErpSalePickDeliverySummaryRespVO summary = salePickDeliveryService
                .getSummaryMapByWarehouseMoveIds(Collections.singleton(id)).get(id);
        if (summary == null || !ErpSalePickStatusEnum.DONE.getStatus().equals(summary.getPickStatus())) {
            throw exception(WAREHOUSE_MOVE_AUTO_APPROVE_FAIL);
        }
        List<ErpWarehouseMoveItemDO> items = warehouseMoveItemMapper.selectListByMoveIdForUpdate(id);
        validateWarehouseMoveItemsReadyForApprove(warehouseMove, items, false);
        approveWarehouseMove(warehouseMove, items, pickUserId != null ? pickUserId : getLoginUserId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ErpWarehouseMoveDO createAndApproveAssemblyMove(ErpAssemblyWarehouseMoveCreateReqBO reqBO) {
        if (reqBO == null || reqBO.getAssemblyOrderId() == null || reqBO.getAssemblyOrderNo() == null
                || reqBO.getProductId() == null || reqBO.getFromWarehouseId() == null
                || reqBO.getToWarehouseId() == null || reqBO.getCount() == null
                || reqBO.getCount().signum() <= 0) {
            throw exception(WAREHOUSE_MOVE_VOID_FAIL, "自动移货参数不完整");
        }
        if (reqBO.getFromWarehouseId().equals(reqBO.getToWarehouseId())) {
            throw exception(WAREHOUSE_MOVE_WAREHOUSE_SAME);
        }
        ErpWarehouseMoveDO existing = warehouseMoveMapper.selectBySource(
                ErpBizTypeEnum.ASSEMBLY_ORDER.getType(), reqBO.getAssemblyOrderId());
        if (existing != null) {
            if (ErpWarehouseMoveStatusEnum.APPROVE.getStatus().equals(existing.getStatus())) {
                return existing;
            }
            if (!ErpWarehouseMoveStatusEnum.VOIDED.getStatus().equals(existing.getStatus())) {
                throw exception(WAREHOUSE_MOVE_VOID_FAIL, "已存在状态异常的关联移货单");
            }
        }
        Set<Long> warehouseIds = new HashSet<>(Arrays.asList(
                reqBO.getFromWarehouseId(), reqBO.getToWarehouseId()));
        DataPermissionUtils.executeIgnore(() -> warehouseService.validWarehouseList(warehouseIds));
        ErpProductDO product = DataPermissionUtils.executeIgnore(() ->
                productService.validProductList(Collections.singleton(reqBO.getProductId())).get(0));
        stockService.ensureStockExists(reqBO.getProductId(), reqBO.getFromWarehouseId());
        ErpStockDO sourceStock = DataPermissionUtils.executeIgnore(() -> stockMapper
                .selectByProductIdAndWarehouseIdForUpdate(reqBO.getProductId(), reqBO.getFromWarehouseId()));
        BigDecimal available = sourceStock == null || sourceStock.getCount() == null
                ? BigDecimal.ZERO : sourceStock.getCount().subtract(
                        sourceStock.getLockCount() == null ? BigDecimal.ZERO : sourceStock.getLockCount());
        if (available.compareTo(reqBO.getCount()) < 0) {
            throw exception(WAREHOUSE_MOVE_STOCK_NOT_ENOUGH, reqBO.getProductId(), available, reqBO.getCount());
        }
        BigDecimal moveCostPrice = sourceStock.getCostPrice() == null
                ? BigDecimal.ZERO : sourceStock.getCostPrice().setScale(6, RoundingMode.HALF_UP);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime moveTime = reqBO.getMoveTime() == null
                || reqBO.getMoveTime().isBefore(EARLIEST_VALID_BUSINESS_TIME)
                ? now : reqBO.getMoveTime();
        Long operatorId = getLoginUserId();
        BigDecimal weight = product.getWeight();
        BigDecimal totalWeight = weight == null ? null : StockDecimalMath.multiply(weight, reqBO.getCount());
        BigDecimal amount = moveCostPrice.multiply(reqBO.getCount()).setScale(6, RoundingMode.HALF_UP);
        String no = noRedisDAO.generate(ErpNoRedisDAO.WAREHOUSE_MOVE_NO_PREFIX);
        if (warehouseMoveMapper.selectByNo(no) != null) {
            throw exception(WAREHOUSE_MOVE_NO_EXISTS);
        }
        ErpWarehouseMoveDO warehouseMove = new ErpWarehouseMoveDO()
                .setNo(no)
                .setDeptId(reqBO.getDeptId())
                .setMoveTime(moveTime)
                .setFromWarehouseId(reqBO.getFromWarehouseId())
                .setToWarehouseId(reqBO.getToWarehouseId())
                .setHandlerId(reqBO.getHandlerId())
                .setSourceType(ErpBizTypeEnum.ASSEMBLY_ORDER.getType())
                .setSourceId(reqBO.getAssemblyOrderId())
                .setSourceNo(reqBO.getAssemblyOrderNo())
                .setTotalCount(reqBO.getCount())
                .setTotalPrice(amount)
                .setTotalCostAmount(amount)
                .setStatus(ErpWarehouseMoveStatusEnum.APPROVE.getStatus())
                .setConfirmUserId(operatorId)
                .setConfirmTime(now)
                .setApproveUserId(operatorId)
                .setApproveTime(now)
                .setRemark(reqBO.getRemark());
        warehouseMoveMapper.insert(warehouseMove);

        ErpWarehouseMoveItemDO item = new ErpWarehouseMoveItemDO()
                .setMoveId(warehouseMove.getId())
                .setFromWarehouseId(reqBO.getFromWarehouseId())
                .setToWarehouseId(reqBO.getToWarehouseId())
                .setProductId(reqBO.getProductId())
                .setProductUnitId(reqBO.getProductUnitId() != null
                        ? reqBO.getProductUnitId() : product.getUnitId())
                .setPackageQty(product.getPackageQty())
                .setProductPrice(moveCostPrice)
                .setCount(reqBO.getCount())
                .setTotalPrice(amount)
                .setFromShelf(reqBO.getFromShelf())
                .setToShelf(reqBO.getToShelf())
                .setCostPrice(moveCostPrice)
                .setCostAmount(amount)
                .setWeight(weight)
                .setTotalWeight(totalWeight)
                .setBatchNo(reqBO.getBatchNo())
                .setRemark("套件组装完成自动移货");
        warehouseMoveItemMapper.insert(item);
        createAssemblyMoveStockRecord(warehouseMove, item, reqBO.getFromWarehouseId(),
                reqBO.getCount().negate(), ErpStockRecordBizTypeEnum.WAREHOUSE_MOVE_OUT,
                "OUT", null, null);
        createAssemblyMoveStockRecord(warehouseMove, item, reqBO.getToWarehouseId(),
                reqBO.getCount(), ErpStockRecordBizTypeEnum.WAREHOUSE_MOVE_IN,
                "IN", null, null);
        salePickDeliveryService.generateCompletedForWarehouseMove(warehouseMove.getId(), operatorId);
        operateLogService.recordCreate(ERP_WAREHOUSE_MOVE_TYPE, warehouseMove.getId(), warehouseMove.getNo());
        operateLogService.recordStatus(ERP_WAREHOUSE_MOVE_TYPE, warehouseMove.getId(), warehouseMove.getNo(), true);
        return warehouseMove;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void voidAssemblyMove(Long id, String reason) {
        ErpWarehouseMoveDO move = lockWarehouseMove(id);
        if (!ErpBizTypeEnum.ASSEMBLY_ORDER.getType().equals(move.getSourceType())) {
            throw exception(WAREHOUSE_MOVE_VOID_FAIL, "移货单不是由套件组装生成");
        }
        if (ErpWarehouseMoveStatusEnum.VOIDED.getStatus().equals(move.getStatus())) {
            return;
        }
        if (!ErpWarehouseMoveStatusEnum.APPROVE.getStatus().equals(move.getStatus())) {
            throw exception(WAREHOUSE_MOVE_VOID_FAIL, "关联移货单不是已完成状态");
        }
        List<ErpWarehouseMoveItemDO> items = warehouseMoveItemMapper.selectListByMoveIdForUpdate(id);
        if (CollUtil.isEmpty(items)) {
            throw exception(WAREHOUSE_MOVE_VOID_FAIL, "关联移货单缺少明细");
        }
        List<cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockRecordDO> originalRecords =
                stockRecordMapper.selectListByBizTypesAndBizId(Arrays.asList(
                        ErpStockRecordBizTypeEnum.WAREHOUSE_MOVE_OUT.getType(),
                        ErpStockRecordBizTypeEnum.WAREHOUSE_MOVE_IN.getType()), id);
        Map<String, cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockRecordDO> originalMap =
                originalRecords.stream().collect(Collectors.toMap(
                        record -> record.getBizItemId() + ":" + record.getBizType(),
                        record -> record, (first, second) -> first));
        for (ErpWarehouseMoveItemDO item : items) {
            stockService.ensureStockExists(item.getProductId(), item.getToWarehouseId());
            ErpStockDO targetStock = DataPermissionUtils.executeIgnore(() -> stockMapper
                    .selectByProductIdAndWarehouseIdForUpdate(item.getProductId(), item.getToWarehouseId()));
            BigDecimal available = targetStock == null || targetStock.getCount() == null
                    ? BigDecimal.ZERO : targetStock.getCount().subtract(
                            targetStock.getLockCount() == null ? BigDecimal.ZERO : targetStock.getLockCount());
            if (available.compareTo(item.getCount()) < 0) {
                throw exception(WAREHOUSE_MOVE_VOID_FAIL,
                        "移入仓成品可用库存不足，产品 " + item.getProductId());
            }
            if (item.getBatchNo() != null) {
                BigDecimal batchAvailable = stockRecordMapper.selectBatchBalance(
                        item.getProductId(), item.getToWarehouseId(), item.getBatchNo());
                if (batchAvailable.compareTo(item.getCount()) < 0) {
                    throw exception(WAREHOUSE_MOVE_VOID_FAIL,
                            "移入仓成品批次库存不足，批次 " + item.getBatchNo());
                }
            }
            cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockRecordDO inRecord = originalMap.get(
                    item.getId() + ":" + ErpStockRecordBizTypeEnum.WAREHOUSE_MOVE_IN.getType());
            cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockRecordDO outRecord = originalMap.get(
                    item.getId() + ":" + ErpStockRecordBizTypeEnum.WAREHOUSE_MOVE_OUT.getType());
            if (inRecord == null || outRecord == null) {
                throw exception(WAREHOUSE_MOVE_VOID_FAIL, "找不到关联移货库存流水");
            }
            createAssemblyMoveStockRecord(move, item, item.getToWarehouseId(), item.getCount().negate(),
                    ErpStockRecordBizTypeEnum.WAREHOUSE_MOVE_IN_CANCEL, "IN_CANCEL",
                    inRecord.getId(), ErpStockRecordBizTypeEnum.WAREHOUSE_MOVE_IN.getType());
            createAssemblyMoveStockRecord(move, item, item.getFromWarehouseId(), item.getCount(),
                    ErpStockRecordBizTypeEnum.WAREHOUSE_MOVE_OUT_CANCEL, "OUT_CANCEL",
                    outRecord.getId(), ErpStockRecordBizTypeEnum.WAREHOUSE_MOVE_OUT.getType());
        }
        int changed = warehouseMoveMapper.updateByIdAndStatus(id,
                ErpWarehouseMoveStatusEnum.APPROVE.getStatus(), new ErpWarehouseMoveDO()
                        .setStatus(ErpWarehouseMoveStatusEnum.VOIDED.getStatus())
                        .setVoidUserId(getLoginUserId()).setVoidTime(LocalDateTime.now())
                        .setVoidReason(reason));
        if (changed != 1) {
            throw exception(WAREHOUSE_MOVE_VOID_FAIL, "移货单状态已变化");
        }
        operateLogService.recordUpdate(ERP_WAREHOUSE_MOVE_TYPE, id, move.getNo());
    }

    private void createAssemblyMoveStockRecord(ErpWarehouseMoveDO move, ErpWarehouseMoveItemDO item,
                                               Long warehouseId, BigDecimal count,
                                               ErpStockRecordBizTypeEnum bizType, String action,
                                               Long sourceRecordId, Integer sourceBizType) {
        ErpStockRecordCreateReqBO request = new ErpStockRecordCreateReqBO(
                item.getProductId(), warehouseId, item.getBatchNo(), item.getProductUnitId(),
                item.getPackageQty(), item.getWeight(),
                item.getTotalWeight() == null ? null
                        : (count.signum() < 0 ? item.getTotalWeight().negate() : item.getTotalWeight()),
                count, bizType.getType(), move.getId(), item.getId(), move.getNo(),
                item.getCostPrice(), move.getMoveTime());
        request.setAccountingDeptId(move.getDeptId());
        request.setPostingActionKey("assembly-move:" + move.getId() + ":" + action + ":" + item.getId());
        request.setFinancialUnitCost(item.getCostPrice());
        request.setSettlementUnitCost(item.getCostPrice());
        request.setCostBasisConfirmed(true);
        request.setSourcePriceBasis("WAREHOUSE_MOVING_AVERAGE");
        request.setSourceRecordId(sourceRecordId);
        if (sourceBizType != null) {
            request.setSourceBizType(sourceBizType);
            request.setSourceBizId(move.getId());
            request.setSourceBizItemId(item.getId());
        }
        stockRecordService.createStockRecord(request);
    }

    private void approveWarehouseMove(ErpWarehouseMoveDO warehouseMove, List<ErpWarehouseMoveItemDO> items,
                                      Long approveUserId) {
        int updateCount = warehouseMoveMapper.updateByIdAndStatus(warehouseMove.getId(),
                ErpWarehouseMoveStatusEnum.PICKING.getStatus(),
                new ErpWarehouseMoveDO().setStatus(ErpWarehouseMoveStatusEnum.APPROVE.getStatus())
                        .setApproveUserId(approveUserId).setApproveTime(LocalDateTime.now()));
        if (updateCount == 0) {
            throw exception(WAREHOUSE_MOVE_AUTO_APPROVE_FAIL);
        }
        operateLogService.recordStatus(ERP_WAREHOUSE_MOVE_TYPE, warehouseMove.getId(), warehouseMove.getNo(), true);
        items.forEach(item -> {
            stockRecordService.createStockRecord(new ErpStockRecordCreateReqBO(
                    item.getProductId(), item.getFromWarehouseId(), item.getBatchNo(), item.getProductUnitId(),
                    item.getPackageQty(), item.getWeight(),
                    item.getTotalWeight() == null ? null : item.getTotalWeight().negate(), item.getCount().negate(),
                    ErpStockRecordBizTypeEnum.WAREHOUSE_MOVE_OUT.getType(), item.getMoveId(), item.getId(),
                    warehouseMove.getNo(), null, warehouseMove.getMoveTime()));
            stockRecordService.createStockRecord(new ErpStockRecordCreateReqBO(
                    item.getProductId(), item.getToWarehouseId(), item.getBatchNo(), item.getProductUnitId(),
                    item.getPackageQty(), item.getWeight(), item.getTotalWeight(), item.getCount(),
                    ErpStockRecordBizTypeEnum.WAREHOUSE_MOVE_IN.getType(), item.getMoveId(), item.getId(),
                    warehouseMove.getNo(), resolveMoveInUnitPrice(item), warehouseMove.getMoveTime()));
        });
    }

    private List<ErpWarehouseMoveItemDO> validateWarehouseMoveItems(ErpWarehouseMoveSaveReqVO reqVO) {
        if (reqVO.getFromWarehouseId() == null || reqVO.getToWarehouseId() == null) {
            throw exception(WAREHOUSE_MOVE_WAREHOUSE_REQUIRED);
        }
        if (reqVO.getFromWarehouseId().equals(reqVO.getToWarehouseId())) {
            throw exception(WAREHOUSE_MOVE_WAREHOUSE_SAME);
        }
        if (CollUtil.isEmpty(reqVO.getItems())) {
            throw exception(WAREHOUSE_MOVE_SUBMIT_ITEMS_REQUIRED);
        }
        validateWarehouseMoveItemRequiredFields(reqVO.getItems());
        validateDuplicateWarehouseMoveItems(reqVO);
        Set<Long> warehouseIds = new HashSet<>();
        warehouseIds.add(reqVO.getFromWarehouseId());
        warehouseIds.add(reqVO.getToWarehouseId());
        DataPermissionUtils.executeIgnore(() -> warehouseService.validWarehouseList(warehouseIds));
        warehouseService.validateCurrentUserStockMoveFromWarehousePermission(java.util.Collections.singleton(reqVO.getFromWarehouseId()));
        warehouseService.validateCurrentUserWarehousePermission(java.util.Collections.singleton(reqVO.getToWarehouseId()));
        List<ErpProductDO> productList = DataPermissionUtils.executeIgnore(() -> productService.validProductList(
                convertSet(reqVO.getItems(), ErpWarehouseMoveSaveReqVO.Item::getProductId)));
        Map<Long, ErpProductDO> productMap = convertMap(productList, ErpProductDO::getId);
        return convertList(reqVO.getItems(), itemReq -> {
            if (itemReq.getCount() == null || itemReq.getCount().compareTo(BigDecimal.ZERO) <= 0) {
                throw exception(WAREHOUSE_MOVE_ITEM_COUNT_POSITIVE);
            }
            ErpProductDO product = productMap.get(itemReq.getProductId());
            BigDecimal costPrice = itemReq.getCostPrice();
            if (costPrice == null) {
                ErpStockDO stock = DataPermissionUtils.executeIgnore(() ->
                        stockService.getStock(itemReq.getProductId(), reqVO.getFromWarehouseId()));
                costPrice = stock == null || stock.getCostPrice() == null ? BigDecimal.ZERO : stock.getCostPrice();
            }
            BigDecimal finalCostPrice = costPrice;
            BigDecimal productPrice = itemReq.getProductPrice() == null ? finalCostPrice : itemReq.getProductPrice();
            BigDecimal weight = itemReq.getWeight() == null && product != null ? product.getWeight() : itemReq.getWeight();
            return BeanUtils.toBean(itemReq, ErpWarehouseMoveItemDO.class, item -> item
                    .setFromWarehouseId(reqVO.getFromWarehouseId())
                    .setToWarehouseId(reqVO.getToWarehouseId())
                    .setProductUnitId(snapshotSupport.resolveProductUnitId(item.getProductUnitId(), product))
                    .setPackageQty(snapshotSupport.resolvePackageQty(itemReq.getPackageQty(), product))
                    .setProductPrice(productPrice)
                    .setTotalPrice(StockDecimalMath.multiply(productPrice, itemReq.getCount()))
                    .setCostPrice(finalCostPrice)
                    .setCostAmount(StockDecimalMath.multiply(finalCostPrice, itemReq.getCount()))
                    .setWeight(weight)
                    .setTotalWeight(StockDecimalMath.multiply(weight, itemReq.getCount())));
        });
    }

    private void validateWarehouseMoveItemRequiredFields(List<ErpWarehouseMoveSaveReqVO.Item> items) {
        for (ErpWarehouseMoveSaveReqVO.Item item : items) {
            if (item == null || item.getProductId() == null) {
                throw exception(WAREHOUSE_MOVE_SUBMIT_ITEMS_REQUIRED);
            }
        }
    }

    private List<ErpWarehouseMoveItemDO> buildDraftItems(ErpWarehouseMoveSaveReqVO reqVO) {
        if (reqVO.getFromWarehouseId() == null || reqVO.getToWarehouseId() == null
                || reqVO.getFromWarehouseId().equals(reqVO.getToWarehouseId())
                || CollUtil.isEmpty(reqVO.getItems())) {
            return Collections.emptyList();
        }
        List<ErpWarehouseMoveSaveReqVO.Item> validItems = convertList(reqVO.getItems(), item -> {
            if (item == null || item.getProductId() == null || item.getCount() == null
                    || item.getCount().compareTo(BigDecimal.ZERO) <= 0) {
                return null;
            }
            return item;
        });
        validItems.removeIf(java.util.Objects::isNull);
        if (CollUtil.isEmpty(validItems)) {
            return Collections.emptyList();
        }
        ErpWarehouseMoveSaveReqVO validReqVO = BeanUtils.toBean(reqVO, ErpWarehouseMoveSaveReqVO.class);
        validReqVO.setItems(validItems);
        return validateWarehouseMoveItems(validReqVO);
    }

    private void validateWarehouseMoveItemsReadyForApprove(ErpWarehouseMoveDO warehouseMove,
                                                           List<ErpWarehouseMoveItemDO> items,
                                                           boolean validateCurrentUserPermission) {
        if (CollUtil.isEmpty(items)) {
            throw exception(WAREHOUSE_MOVE_NOT_EXISTS);
        }
        validateDuplicateWarehouseMoveItems(warehouseMove, items);
        Set<Long> warehouseIds = new HashSet<>();
        warehouseIds.add(warehouseMove.getFromWarehouseId());
        warehouseIds.add(warehouseMove.getToWarehouseId());
        DataPermissionUtils.executeIgnore(() -> warehouseService.validWarehouseList(warehouseIds));
        if (validateCurrentUserPermission) {
            warehouseService.validateCurrentUserStockMoveFromWarehousePermission(
                    java.util.Collections.singleton(warehouseMove.getFromWarehouseId()));
            warehouseService.validateCurrentUserWarehousePermission(
                    java.util.Collections.singleton(warehouseMove.getToWarehouseId()));
        }
        for (ErpWarehouseMoveItemDO item : items) {
            ErpStockDO stock = DataPermissionUtils.executeIgnore(() ->
                    stockService.getStock(item.getProductId(), item.getFromWarehouseId()));
            BigDecimal available = stock == null || stock.getCount() == null ? BigDecimal.ZERO : stock.getCount();
            if (available.compareTo(item.getCount()) < 0) {
                throw exception(WAREHOUSE_MOVE_STOCK_NOT_ENOUGH, item.getProductId(), available, item.getCount());
            }
        }
    }

    private BigDecimal resolveMoveInUnitPrice(ErpWarehouseMoveItemDO item) {
        if (item.getCostPrice() != null) {
            return item.getCostPrice();
        }
        ErpStockDO stock = DataPermissionUtils.executeIgnore(() ->
                stockService.getStock(item.getProductId(), item.getFromWarehouseId()));
        if (stock != null && stock.getCostPrice() != null) {
            return stock.getCostPrice();
        }
        return item.getProductPrice();
    }

    private void validateDuplicateWarehouseMoveItems(ErpWarehouseMoveSaveReqVO reqVO) {
        Set<String> keys = new HashSet<>();
        for (ErpWarehouseMoveSaveReqVO.Item item : reqVO.getItems()) {
            String key = item.getProductId() + "-" + reqVO.getFromWarehouseId() + "-"
                    + reqVO.getToWarehouseId() + "-" + (item.getFromShelf() == null ? "" : item.getFromShelf());
            if (!keys.add(key)) {
                throw exception(WAREHOUSE_MOVE_ITEM_DUPLICATE, key);
            }
        }
    }

    private void validateDuplicateWarehouseMoveItems(ErpWarehouseMoveDO warehouseMove,
                                                     List<ErpWarehouseMoveItemDO> items) {
        Set<String> keys = new HashSet<>();
        for (ErpWarehouseMoveItemDO item : items) {
            String key = item.getProductId() + "-" + warehouseMove.getFromWarehouseId() + "-"
                    + warehouseMove.getToWarehouseId() + "-" + (item.getFromShelf() == null ? "" : item.getFromShelf());
            if (!keys.add(key)) {
                throw exception(WAREHOUSE_MOVE_ITEM_DUPLICATE, key);
            }
        }
    }

    private void updateWarehouseMoveItemList(Long id, List<ErpWarehouseMoveItemDO> oldList,
                                             List<ErpWarehouseMoveItemDO> newList) {
        // 复用主表更新前锁定的明细，避免批量写入后提交校验命中当前会话中的旧查询缓存。
        List<List<ErpWarehouseMoveItemDO>> diffList = diffList(oldList, newList,
                (oldVal, newVal) -> oldVal.getId().equals(newVal.getId()));
        if (CollUtil.isNotEmpty(diffList.get(0))) {
            diffList.get(0).forEach(o -> o.setMoveId(id));
            warehouseMoveItemMapper.insertBatch(diffList.get(0));
        }
        if (CollUtil.isNotEmpty(diffList.get(1))) {
            warehouseMoveItemMapper.updateBatch(diffList.get(1));
        }
        if (CollUtil.isNotEmpty(diffList.get(2))) {
            warehouseMoveItemMapper.deleteByIds(convertList(diffList.get(2), ErpWarehouseMoveItemDO::getId));
        }
    }

    private void applyWarehouseMoveItemChangeSet(Long moveId,
            ErpStockItemOperationHelper.RequestChangeSet<ErpWarehouseMoveSaveReqVO.Item> changeSet,
            List<ErpWarehouseMoveItemDO> finalItems) {
        if (CollUtil.isNotEmpty(changeSet.getDeleteIds())) {
            warehouseMoveItemMapper.deleteByIds(changeSet.getDeleteIds());
        }
        List<ErpWarehouseMoveItemDO> insertList = finalItems.stream()
                .filter(item -> item.getId() == null)
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(insertList)) {
            insertList.forEach(item -> item.setMoveId(moveId));
            warehouseMoveItemMapper.insertBatch(insertList);
        }
        List<ErpWarehouseMoveItemDO> updateList = finalItems.stream()
                .filter(item -> item.getId() != null && changeSet.getUpdateIds().contains(item.getId()))
                .collect(Collectors.toList());
        if (CollUtil.isNotEmpty(updateList)) {
            updateList.forEach(item -> item.setMoveId(moveId));
            warehouseMoveItemMapper.updateBatch(updateList);
        }
    }

    private void replaceWarehouseMoveItems(Long id, List<ErpWarehouseMoveItemDO> items) {
        warehouseMoveItemMapper.deleteByMoveId(id);
        if (CollUtil.isEmpty(items)) {
            return;
        }
        items.forEach(item -> item.setId(null).setMoveId(id));
        warehouseMoveItemMapper.insertBatch(items);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteWarehouseMove(List<Long> ids) {
        List<ErpWarehouseMoveDO> moves = warehouseMoveMapper.selectByIds(ids);
        if (CollUtil.isEmpty(moves)) {
            return;
        }
        moves.forEach(move -> {
            validateNotAssemblyManaged(move);
            if (ErpWarehouseMoveStatusEnum.PICKING.getStatus().equals(move.getStatus())) {
                throw exception(WAREHOUSE_MOVE_PICK_ALREADY_STARTED);
            }
            if (ErpWarehouseMoveStatusEnum.APPROVE.getStatus().equals(move.getStatus())) {
                throw exception(WAREHOUSE_MOVE_DELETE_FAIL_APPROVE, move.getNo());
            }
        });
        moves.forEach(move -> {
            warehouseMoveMapper.deleteById(move.getId());
            warehouseMoveItemMapper.deleteByMoveId(move.getId());
            operateLogService.recordDelete(ERP_WAREHOUSE_MOVE_TYPE, move.getId(), move.getNo());
        });
    }

    private ErpWarehouseMoveDO validateWarehouseMoveExists(Long id) {
        ErpWarehouseMoveDO warehouseMove = warehouseMoveMapper.selectById(id);
        if (warehouseMove == null) {
            throw exception(WAREHOUSE_MOVE_NOT_EXISTS);
        }
        return warehouseMove;
    }

    private void validateNotAssemblyManaged(ErpWarehouseMoveDO move) {
        if (ErpBizTypeEnum.ASSEMBLY_ORDER.getType().equals(move.getSourceType())) {
            throw exception(WAREHOUSE_MOVE_SYSTEM_MANAGED);
        }
    }

    private ErpWarehouseMoveDO lockWarehouseMove(Long id) {
        ErpWarehouseMoveDO warehouseMove = warehouseMoveMapper.selectByIdForUpdate(id);
        if (warehouseMove == null) {
            throw exception(WAREHOUSE_MOVE_NOT_EXISTS);
        }
        return warehouseMove;
    }

    @Override
    public ErpWarehouseMoveDO getWarehouseMove(Long id) {
        return warehouseMoveMapper.selectById(id);
    }

    @Override
    public List<ErpWarehouseMoveDO> getWarehouseMoveList(Collection<Long> ids) {
        return CollUtil.isEmpty(ids) ? Collections.emptyList() : warehouseMoveMapper.selectByIds(ids);
    }

    @Override
    public PageResult<ErpWarehouseMoveDO> getWarehouseMovePage(ErpWarehouseMovePageReqVO pageReqVO) {
        return warehouseMoveMapper.selectPage(pageReqVO);
    }

    @Override
    public ErpWarehouseMoveSummaryRespVO getWarehouseMoveSummary(ErpWarehouseMovePageReqVO pageReqVO) {
        return warehouseMoveMapper.selectSummary(pageReqVO);
    }

    @Override
    public List<ErpWarehouseMoveItemDO> getWarehouseMoveItemListByMoveId(Long moveId) {
        return warehouseMoveItemMapper.selectListByMoveId(moveId);
    }

    @Override
    public List<ErpWarehouseMoveItemDO> getWarehouseMoveItemListByMoveId(Long moveId, String orderField,
                                                                         String orderDirection) {
        return warehouseMoveItemMapper.selectListByMoveId(moveId, orderField, orderDirection);
    }

    @Override
    public PageResult<ErpWarehouseMoveItemDO> getWarehouseMoveItemPage(ErpWarehouseMoveItemPageReqVO pageReqVO) {
        validateWarehouseMoveExists(pageReqVO.getMoveId());
        return warehouseMoveItemMapper.selectPageByMoveId(pageReqVO);
    }

    @Override
    public List<ErpWarehouseMoveItemDO> getWarehouseMoveItemListByMoveIds(Collection<Long> moveIds) {
        return warehouseMoveItemMapper.selectListByMoveIds(moveIds);
    }

}
