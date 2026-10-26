package cn.iocoder.yudao.module.erp.service.assembly;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.module.erp.controller.admin.assembly.vo.order.*;
import cn.iocoder.yudao.module.erp.dal.dataobject.assembly.*;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.product.ErpProductUnitDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockRecordDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseMoveDO;
import cn.iocoder.yudao.module.erp.dal.mysql.assembly.*;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockRecordMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.enums.assembly.ErpAssemblyOrderStatusEnum;
import cn.iocoder.yudao.module.erp.enums.assembly.ErpAssemblyRecipeStatusEnum;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockRecordBizTypeEnum;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductUnitService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockRecordService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseMoveService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpAssemblyWarehouseMoveCreateReqBO;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockRecordCreateReqBO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.erp.enums.LogRecordConstants.ERP_ASSEMBLY_ORDER_TYPE;

@Service
@Validated
public class ErpAssemblyOrderServiceImpl implements ErpAssemblyOrderService {

    private static final LocalDateTime EARLIEST_VALID_BUSINESS_TIME = LocalDateTime.of(2000, 1, 1, 0, 0);

    private static final int COST_SCALE = 6;

    @Resource
    private ErpAssemblyOrderMapper orderMapper;
    @Resource
    private ErpAssemblyOrderItemMapper orderItemMapper;
    @Resource
    private ErpAssemblyRecipeMapper recipeMapper;
    @Resource
    private ErpAssemblyRecipeItemMapper recipeItemMapper;
    @Resource
    private ErpProductService productService;
    @Resource
    private ErpProductUnitService productUnitService;
    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private ErpWarehouseMoveService warehouseMoveService;
    @Resource
    private ErpStockService stockService;
    @Resource
    private ErpStockRecordService stockRecordService;
    @Resource
    private ErpStockMapper stockMapper;
    @Resource
    private ErpStockRecordMapper stockRecordMapper;
    @Resource
    private ErpNoRedisDAO noRedisDAO;
    @Resource
    private ErpOperateLogService operateLogService;
    @Resource
    private ErpCustomerService customerService;
    @Resource
    private AdminUserApi adminUserApi;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDraft(ErpAssemblyOrderSaveReqVO reqVO) {
        validateSetCount(reqVO.getSetCount());
        ErpAssemblyRecipeDO recipe = validateRecipeForNewOrder(reqVO.getRecipeId());
        ErpWarehouseDO warehouse = validateWarehouse(reqVO.getWarehouseId());
        Long outWarehouseId = resolveOutWarehouseId(reqVO);
        if (outWarehouseId != null) {
            validateWarehouse(outWarehouseId);
        }
        validateBusinessRelations(reqVO);
        List<ErpAssemblyRecipeItemDO> recipeItems = recipeItemMapper.selectListByRecipeId(recipe.getId());
        if (CollUtil.isEmpty(recipeItems)) {
            throw exception(ASSEMBLY_ORDER_ITEMS_REQUIRED);
        }
        Set<Long> productIds = recipeItems.stream().map(ErpAssemblyRecipeItemDO::getProductId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        productIds.add(recipe.getProductId());
        Map<Long, ErpProductDO> productMap = getValidProductMap(productIds);
        ErpProductDO finishedProduct = productMap.get(recipe.getProductId());
        Map<Long, ErpProductUnitDO> unitMap = getProductUnitMap(productMap.values());
        List<ErpAssemblyOrderItemDO> orderItems = buildItemsFromRecipe(
                recipeItems, reqVO.getItems(), reqVO.getSetCount(), productMap, unitMap);

        String no = noRedisDAO.generate(ErpNoRedisDAO.ASSEMBLY_ORDER_NO_PREFIX);
        if (orderMapper.selectByNo(no) != null) {
            throw exception(ASSEMBLY_ORDER_NO_EXISTS);
        }
        ErpAssemblyOrderDO order = BeanUtils.toBean(reqVO, ErpAssemblyOrderDO.class)
                .setId(null)
                .setNo(no)
                .setRecipeVersion(recipe.getVersion())
                .setRecipeModuleCode(StringUtils.hasText(recipe.getModuleCode())
                        ? recipe.getModuleCode() : finishedProduct.getCode())
                .setRecipeModuleName(StringUtils.hasText(recipe.getModuleName())
                        ? recipe.getModuleName() : finishedProduct.getName())
                .setProductId(recipe.getProductId())
                .setOutWarehouseId(outWarehouseId)
                .setHandlerId(reqVO.getHandlerId() != null ? reqVO.getHandlerId() : getLoginUserId())
                .setDeptId(warehouse.getDeptId())
                .setAssemblyTime(normalizeBusinessTime(reqVO.getAssemblyTime(), LocalDateTime.now()))
                .setFinishedBatchNo(normalizeBatch(reqVO.getFinishedBatchNo()))
                .setLaborCostPerSet(defaultZero(recipe.getLaborCostPerSet()).setScale(COST_SCALE, RoundingMode.HALF_UP))
                .setOtherCost(defaultZero(reqVO.getOtherCost()).setScale(COST_SCALE, RoundingMode.HALF_UP))
                .setMaterialCost(BigDecimal.ZERO)
                .setLaborCost(BigDecimal.ZERO)
                .setTotalCost(BigDecimal.ZERO)
                .setUnitCost(BigDecimal.ZERO)
                .setExecutionRevision(0)
                .setStatus(ErpAssemblyOrderStatusEnum.DRAFT.getStatus());
        orderMapper.insert(order);
        insertOrderItems(order.getId(), orderItems);
        operateLogService.recordCreate(ERP_ASSEMBLY_ORDER_TYPE, order.getId(), order.getNo());
        return order.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateDraft(ErpAssemblyOrderSaveReqVO reqVO) {
        validateSetCount(reqVO.getSetCount());
        ErpAssemblyOrderDO order = validateDraftForUpdate(reqVO.getId());
        if (!Objects.equals(order.getRecipeId(), reqVO.getRecipeId())) {
            throw exception(ASSEMBLY_ORDER_ITEM_INVALID, "草稿创建后不能更换组装配方");
        }
        ErpWarehouseDO warehouse = validateWarehouse(reqVO.getWarehouseId());
        Long outWarehouseId = resolveOutWarehouseId(reqVO);
        if (outWarehouseId != null) {
            validateWarehouse(outWarehouseId);
        }
        validateBusinessRelations(reqVO);
        List<ErpAssemblyOrderItemDO> existingItems = orderItemMapper.selectListByOrderId(order.getId());
        if (CollUtil.isEmpty(existingItems)) {
            throw exception(ASSEMBLY_ORDER_ITEMS_REQUIRED);
        }
        List<ErpAssemblyOrderItemDO> newItems = buildItemsFromSnapshot(
                existingItems, reqVO.getItems(), reqVO.getSetCount());
        ErpAssemblyOrderDO updateObj = BeanUtils.toBean(reqVO, ErpAssemblyOrderDO.class)
                .setId(order.getId())
                .setNo(null)
                .setRecipeId(null)
                .setRecipeVersion(null)
                .setRecipeModuleCode(null)
                .setRecipeModuleName(null)
                .setProductId(null)
                .setHandlerId(reqVO.getHandlerId() != null ? reqVO.getHandlerId() : getLoginUserId())
                .setOutWarehouseId(outWarehouseId)
                .setDeptId(warehouse.getDeptId())
                .setAssemblyTime(normalizeBusinessTime(reqVO.getAssemblyTime(),
                        normalizeBusinessTime(order.getAssemblyTime(), LocalDateTime.now())))
                .setFinishedBatchNo(normalizeBatch(reqVO.getFinishedBatchNo()))
                .setStatus(null)
                .setLaborCostPerSet(null)
                .setOtherCost(defaultZero(reqVO.getOtherCost()).setScale(COST_SCALE, RoundingMode.HALF_UP))
                .setMaterialCost(null)
                .setLaborCost(null)
                .setTotalCost(null)
                .setUnitCost(null);
        orderMapper.updateById(updateObj);
        orderItemMapper.deleteByOrderId(order.getId());
        insertOrderItems(order.getId(), newItems);
        operateLogService.recordUpdate(ERP_ASSEMBLY_ORDER_TYPE, order.getId(), order.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirm(Long id) {
        ErpAssemblyOrderDO order = validateOrderExistsForUpdate(id);
        if (ErpAssemblyOrderStatusEnum.CONFIRMED.getStatus().equals(order.getStatus())) {
            return;
        }
        validateStatus(order, ErpAssemblyOrderStatusEnum.DRAFT);
        validateOrderReadyForAssembly(order);
        ErpAssemblyOrderDO confirmed = new ErpAssemblyOrderDO()
                .setStatus(ErpAssemblyOrderStatusEnum.CONFIRMED.getStatus())
                .setConfirmUserId(getLoginUserId())
                .setConfirmTime(LocalDateTime.now());
        updateStatus(order, ErpAssemblyOrderStatusEnum.DRAFT, confirmed);
        operateLogService.recordStatus(ERP_ASSEMBLY_ORDER_TYPE, "确认装配", order.getId(), order.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reverseConfirm(Long id) {
        ErpAssemblyOrderDO order = validateOrderExistsForUpdate(id);
        if (ErpAssemblyOrderStatusEnum.DRAFT.getStatus().equals(order.getStatus())) {
            return;
        }
        validateStatus(order, ErpAssemblyOrderStatusEnum.CONFIRMED);
        ErpAssemblyOrderDO draft = new ErpAssemblyOrderDO()
                .setStatus(ErpAssemblyOrderStatusEnum.DRAFT.getStatus())
                .setReverseConfirmUserId(getLoginUserId())
                .setReverseConfirmTime(LocalDateTime.now());
        updateStatus(order, ErpAssemblyOrderStatusEnum.CONFIRMED, draft);
        operateLogService.recordStatus(ERP_ASSEMBLY_ORDER_TYPE, "反确认装配", order.getId(), order.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void start(Long id) {
        ErpAssemblyOrderDO order = validateOrderExistsForUpdate(id);
        if (ErpAssemblyOrderStatusEnum.ASSEMBLING.getStatus().equals(order.getStatus())) {
            return;
        }
        validateStatus(order, ErpAssemblyOrderStatusEnum.CONFIRMED);
        validateOrderReadyForAssembly(order);
        int nextRevision = defaultRevision(order) + 1;
        ErpAssemblyOrderDO assembling = new ErpAssemblyOrderDO()
                .setStatus(ErpAssemblyOrderStatusEnum.ASSEMBLING.getStatus())
                .setExecutionRevision(nextRevision)
                .setStartUserId(getLoginUserId())
                .setStartTime(LocalDateTime.now());
        updateStatus(order, ErpAssemblyOrderStatusEnum.CONFIRMED, assembling);
        operateLogService.recordStatus(ERP_ASSEMBLY_ORDER_TYPE, "开始装配", order.getId(), order.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelStart(Long id) {
        ErpAssemblyOrderDO order = validateOrderExistsForUpdate(id);
        if (ErpAssemblyOrderStatusEnum.CONFIRMED.getStatus().equals(order.getStatus())) {
            return;
        }
        validateStatus(order, ErpAssemblyOrderStatusEnum.ASSEMBLING);
        ErpAssemblyOrderDO confirmed = new ErpAssemblyOrderDO()
                .setStatus(ErpAssemblyOrderStatusEnum.CONFIRMED.getStatus())
                .setCancelStartUserId(getLoginUserId())
                .setCancelStartTime(LocalDateTime.now());
        updateStatus(order, ErpAssemblyOrderStatusEnum.ASSEMBLING, confirmed);
        operateLogService.recordStatus(ERP_ASSEMBLY_ORDER_TYPE, "取消开始装配", order.getId(), order.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void complete(Long id) {
        ErpAssemblyOrderDO order = validateOrderExistsForUpdate(id);
        if (ErpAssemblyOrderStatusEnum.COMPLETED.getStatus().equals(order.getStatus())) {
            return;
        }
        if (!ErpAssemblyOrderStatusEnum.ASSEMBLING.getStatus().equals(order.getStatus())) {
            throw exception(ASSEMBLY_ORDER_STATUS_CHANGED);
        }
        validateOrderWarehousePermissions(order);
        LocalDateTime completedAt = LocalDateTime.now();
        Long sourceWarehouseId = resolveSourceWarehouseId(order);
        List<ErpAssemblyOrderItemDO> items = orderItemMapper.selectListByOrderId(order.getId());
        if (CollUtil.isEmpty(items)) {
            throw exception(ASSEMBLY_ORDER_ITEMS_REQUIRED);
        }
        Set<Long> productIds = items.stream().map(ErpAssemblyOrderItemDO::getProductId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        productIds.add(order.getProductId());
        Map<Long, ErpProductDO> productMap = getValidProductMap(productIds);
        ErpProductDO finishedProduct = productMap.get(order.getProductId());
        validateCompletionBatches(order, items, productMap);

        Set<Long> componentProductIds = items.stream().map(ErpAssemblyOrderItemDO::getProductId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, ErpStockDO> stockMap = lockStockRows(componentProductIds, sourceWarehouseId);
        validateComponentStock(order, items, productMap, stockMap);

        BigDecimal materialCost = BigDecimal.ZERO;
        items.sort(Comparator.comparing(ErpAssemblyOrderItemDO::getProductId)
                .thenComparing(ErpAssemblyOrderItemDO::getId));
        for (ErpAssemblyOrderItemDO item : items) {
            ErpStockDO stock = stockMap.get(item.getProductId());
            BigDecimal costPrice = defaultZero(stock.getCostPrice());
            BigDecimal costAmount = item.getConsumeQuantity().multiply(costPrice)
                    .setScale(COST_SCALE, RoundingMode.HALF_UP);
            item.setCostPrice(costPrice).setCostAmount(costAmount);
            materialCost = materialCost.add(costAmount);
            ErpStockRecordCreateReqBO recordReq = buildStockRecordReq(order, sourceWarehouseId,
                    item.getProductId(), item.getId(),
                    item.getBatchNo(), item.getConsumeQuantity().negate(),
                    ErpStockRecordBizTypeEnum.ASSEMBLY_COMPONENT_OUT.getType(), costPrice,
                    "COMPONENT_OUT:" + item.getId(), null);
            stockRecordService.createStockRecord(recordReq);
        }
        orderItemMapper.updateBatch(items);

        BigDecimal finishedCount = BigDecimal.valueOf(order.getSetCount());
        materialCost = materialCost.setScale(COST_SCALE, RoundingMode.HALF_UP);
        BigDecimal laborCost = defaultZero(order.getLaborCostPerSet()).multiply(finishedCount)
                .setScale(COST_SCALE, RoundingMode.HALF_UP);
        BigDecimal totalCost = materialCost.add(laborCost).add(defaultZero(order.getOtherCost()))
                .setScale(COST_SCALE, RoundingMode.HALF_UP);
        BigDecimal unitCost = totalCost.divide(finishedCount, COST_SCALE, RoundingMode.HALF_UP);
        ErpStockRecordCreateReqBO finishedReq = buildStockRecordReq(order, sourceWarehouseId,
                order.getProductId(), order.getId(),
                order.getFinishedBatchNo(), finishedCount,
                ErpStockRecordBizTypeEnum.ASSEMBLY_FINISHED_IN.getType(), unitCost,
                "FINISHED_IN", null);
        finishedReq.setProductUnitId(finishedProduct.getUnitId());
        stockRecordService.createStockRecord(finishedReq);

        Long warehouseMoveId = null;
        if (!Objects.equals(sourceWarehouseId, order.getWarehouseId())) {
            ErpStockDO sourceFinishedStock = DataPermissionUtils.executeIgnore(() ->
                    stockService.getStock(order.getProductId(), sourceWarehouseId));
            BigDecimal moveCostPrice = sourceFinishedStock == null
                    ? BigDecimal.ZERO : defaultZero(sourceFinishedStock.getCostPrice());
            ErpWarehouseMoveDO warehouseMove = warehouseMoveService.createAndApproveAssemblyMove(
                    new ErpAssemblyWarehouseMoveCreateReqBO()
                            .setAssemblyOrderId(order.getId())
                            .setAssemblyOrderNo(order.getNo())
                            .setProductId(order.getProductId())
                            .setProductUnitId(finishedProduct.getUnitId())
                            .setFromWarehouseId(sourceWarehouseId)
                            .setToWarehouseId(order.getWarehouseId())
                            .setFromShelf(sourceFinishedStock == null ? null : sourceFinishedStock.getShelf())
                            .setToShelf(order.getShelf())
                            .setBatchNo(order.getFinishedBatchNo())
                            .setCount(finishedCount)
                            .setCostPrice(moveCostPrice)
                            .setDeptId(order.getDeptId())
                            .setHandlerId(order.getHandlerId())
                            .setMoveTime(completedAt)
                            .setRemark("套件组装单 " + order.getNo() + " 完成后自动移货"));
            warehouseMoveId = warehouseMove.getId();
        }

        ErpAssemblyOrderDO completed = new ErpAssemblyOrderDO()
                .setStatus(ErpAssemblyOrderStatusEnum.COMPLETED.getStatus())
                .setMaterialCost(materialCost)
                .setLaborCost(laborCost)
                .setTotalCost(totalCost)
                .setUnitCost(unitCost)
                .setWarehouseMoveId(warehouseMoveId)
                .setCompleteUserId(getLoginUserId())
                .setCompleteTime(completedAt);
        if (orderMapper.updateByIdAndStatus(order.getId(), ErpAssemblyOrderStatusEnum.ASSEMBLING.getStatus(), completed) == 0) {
            throw exception(ASSEMBLY_ORDER_STATUS_CHANGED);
        }
        operateLogService.recordStatus(ERP_ASSEMBLY_ORDER_TYPE, "完成组装", order.getId(), order.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reverseAssembly(Long id) {
        ErpAssemblyOrderDO order = validateOrderExistsForUpdate(id);
        if (ErpAssemblyOrderStatusEnum.CONFIRMED.getStatus().equals(order.getStatus())) {
            return;
        }
        validateStatus(order, ErpAssemblyOrderStatusEnum.COMPLETED);
        validateOrderWarehousePermissions(order);
        Long sourceWarehouseId = resolveSourceWarehouseId(order);
        if (order.getWarehouseMoveId() != null) {
            warehouseMoveService.voidAssemblyMove(order.getWarehouseMoveId(), "套件组装反装配");
        }
        List<ErpAssemblyOrderItemDO> items = orderItemMapper.selectListByOrderId(order.getId());
        if (CollUtil.isEmpty(items)) {
            throw exception(ASSEMBLY_ORDER_ORIGINAL_RECORD_MISSING);
        }
        Set<Long> productIds = items.stream().map(ErpAssemblyOrderItemDO::getProductId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        productIds.add(order.getProductId());
        Map<Long, ErpProductDO> productMap = getValidProductMap(productIds);
        Map<Long, ErpStockDO> stockMap = lockStockRows(Collections.singleton(order.getProductId()), sourceWarehouseId);
        validateFinishedStockForVoid(order, productMap.get(order.getProductId()), stockMap.get(order.getProductId()));

        List<ErpStockRecordDO> historyRecords = stockRecordMapper.selectListByBizTypesAndBizId(
                Arrays.asList(ErpStockRecordBizTypeEnum.ASSEMBLY_COMPONENT_OUT.getType(),
                        ErpStockRecordBizTypeEnum.ASSEMBLY_FINISHED_IN.getType(),
                        ErpStockRecordBizTypeEnum.ASSEMBLY_COMPONENT_OUT_CANCEL.getType(),
                        ErpStockRecordBizTypeEnum.ASSEMBLY_FINISHED_IN_CANCEL.getType()), order.getId());
        Set<Long> reversedRecordIds = historyRecords.stream()
                .filter(record -> record.getSourceRecordId() != null)
                .map(ErpStockRecordDO::getSourceRecordId)
                .collect(Collectors.toSet());
        List<ErpStockRecordDO> originalRecords = historyRecords.stream()
                .filter(record -> (ErpStockRecordBizTypeEnum.ASSEMBLY_COMPONENT_OUT.getType().equals(record.getBizType())
                        || ErpStockRecordBizTypeEnum.ASSEMBLY_FINISHED_IN.getType().equals(record.getBizType()))
                        && !reversedRecordIds.contains(record.getId()))
                .collect(Collectors.toList());
        OriginalAssemblyRecords originals = validateOriginalRecords(order, items, originalRecords);
        ErpStockRecordDO finishedOriginal = originals.finished;
        Map<Long, ErpStockRecordDO> componentRecordMap = originals.components;

        BigDecimal finishedCount = BigDecimal.valueOf(order.getSetCount());
        ErpStockRecordCreateReqBO finishedCancelReq = buildStockRecordReq(order, sourceWarehouseId,
                order.getProductId(), order.getId(),
                order.getFinishedBatchNo(), finishedCount.negate(),
                ErpStockRecordBizTypeEnum.ASSEMBLY_FINISHED_IN_CANCEL.getType(), order.getUnitCost(),
                "FINISHED_IN_CANCEL", finishedOriginal.getId());
        finishedCancelReq.setSourceBizType(ErpStockRecordBizTypeEnum.ASSEMBLY_FINISHED_IN.getType());
        finishedCancelReq.setSourceBizId(order.getId());
        finishedCancelReq.setSourceBizItemId(order.getId());
        stockRecordService.createStockRecord(finishedCancelReq);

        items.sort(Comparator.comparing(ErpAssemblyOrderItemDO::getProductId)
                .thenComparing(ErpAssemblyOrderItemDO::getId));
        for (ErpAssemblyOrderItemDO item : items) {
            ErpStockRecordDO original = componentRecordMap.get(item.getId());
            if (original == null || !Objects.equals(original.getProductId(), item.getProductId())
                    || !Objects.equals(normalizeBatch(original.getBatchNo()), normalizeBatch(item.getBatchNo()))) {
                throw exception(ASSEMBLY_ORDER_ORIGINAL_RECORD_MISSING);
            }
            ErpStockRecordCreateReqBO cancelReq = buildStockRecordReq(order, sourceWarehouseId,
                    item.getProductId(), item.getId(),
                    item.getBatchNo(), item.getConsumeQuantity(),
                    ErpStockRecordBizTypeEnum.ASSEMBLY_COMPONENT_OUT_CANCEL.getType(), item.getCostPrice(),
                    "COMPONENT_OUT_CANCEL:" + item.getId(), original.getId());
            cancelReq.setSourceBizType(ErpStockRecordBizTypeEnum.ASSEMBLY_COMPONENT_OUT.getType());
            cancelReq.setSourceBizId(order.getId());
            cancelReq.setSourceBizItemId(item.getId());
            stockRecordService.createStockRecord(cancelReq);
        }

        ErpAssemblyOrderDO reversed = new ErpAssemblyOrderDO()
                .setStatus(ErpAssemblyOrderStatusEnum.CONFIRMED.getStatus())
                .setReverseAssemblyUserId(getLoginUserId())
                .setReverseAssemblyTime(LocalDateTime.now());
        if (orderMapper.updateByIdAndStatus(order.getId(), ErpAssemblyOrderStatusEnum.COMPLETED.getStatus(), reversed) == 0) {
            throw exception(ASSEMBLY_ORDER_STATUS_CHANGED);
        }
        operateLogService.recordStatus(ERP_ASSEMBLY_ORDER_TYPE, "反装配", order.getId(), order.getNo());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDraft(Long id) {
        ErpAssemblyOrderDO order = validateDraftForUpdate(id);
        orderItemMapper.deleteByOrderId(id);
        orderMapper.deleteById(id);
        operateLogService.recordDelete(ERP_ASSEMBLY_ORDER_TYPE, order.getId(), order.getNo());
    }

    @Override
    public ErpAssemblyOrderDO getOrder(Long id) {
        return orderMapper.selectById(id);
    }

    @Override
    public PageResult<ErpAssemblyOrderDO> getOrderPage(ErpAssemblyOrderPageReqVO reqVO) {
        return orderMapper.selectPage(reqVO);
    }

    @Override
    public List<ErpAssemblyOrderItemDO> getOrderItemList(Long orderId) {
        return orderItemMapper.selectListByOrderId(orderId);
    }

    @Override
    public List<ErpAssemblyOrderItemDO> getOrderItemList(Collection<Long> orderIds) {
        return orderItemMapper.selectListByOrderIds(orderIds);
    }

    @Override
    public PageResult<ErpAssemblyOrderItemDO> getOrderItemPage(ErpAssemblyOrderItemPageReqVO reqVO) {
        if (orderMapper.selectById(reqVO.getOrderId()) == null) {
            throw exception(ASSEMBLY_ORDER_NOT_EXISTS);
        }
        return orderItemMapper.selectPage(reqVO);
    }

    private ErpAssemblyRecipeDO validateRecipeForNewOrder(Long recipeId) {
        ErpAssemblyRecipeDO recipe = recipeMapper.selectById(recipeId);
        if (recipe == null) {
            throw exception(ASSEMBLY_RECIPE_NOT_EXISTS);
        }
        if (!ErpAssemblyRecipeStatusEnum.ENABLED.getStatus().equals(recipe.getStatus())) {
            throw exception(ASSEMBLY_ORDER_RECIPE_DISABLED);
        }
        return recipe;
    }

    private ErpAssemblyOrderDO validateDraftForUpdate(Long id) {
        ErpAssemblyOrderDO order = validateOrderExistsForUpdate(id);
        if (!ErpAssemblyOrderStatusEnum.DRAFT.getStatus().equals(order.getStatus())) {
            throw exception(ASSEMBLY_ORDER_NOT_DRAFT, order.getNo());
        }
        return order;
    }

    private void validateStatus(ErpAssemblyOrderDO order, ErpAssemblyOrderStatusEnum expected) {
        if (!expected.getStatus().equals(order.getStatus())) {
            throw exception(ASSEMBLY_ORDER_STATUS_CHANGED);
        }
    }

    private void updateStatus(ErpAssemblyOrderDO order, ErpAssemblyOrderStatusEnum expected,
                              ErpAssemblyOrderDO updateObj) {
        if (orderMapper.updateByIdAndStatus(order.getId(), expected.getStatus(), updateObj) == 0) {
            throw exception(ASSEMBLY_ORDER_STATUS_CHANGED);
        }
    }

    private int defaultRevision(ErpAssemblyOrderDO order) {
        return order.getExecutionRevision() == null ? 0 : order.getExecutionRevision();
    }

    /**
     * 确认、开始阶段只做实时可用量检查，不创建库存维度、不写库存流水，也不形成业务占用。
     */
    private void validateOrderReadyForAssembly(ErpAssemblyOrderDO order) {
        validateOrderWarehousePermissions(order);
        Long sourceWarehouseId = resolveSourceWarehouseId(order);
        List<ErpAssemblyOrderItemDO> items = orderItemMapper.selectListByOrderId(order.getId());
        if (CollUtil.isEmpty(items)) {
            throw exception(ASSEMBLY_ORDER_ITEMS_REQUIRED);
        }
        Set<Long> productIds = items.stream().map(ErpAssemblyOrderItemDO::getProductId)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        productIds.add(order.getProductId());
        Map<Long, ErpProductDO> productMap = getValidProductMap(productIds);
        validateCompletionBatches(order, items, productMap);
        Map<String, ErpStockDO> rawStockMap = DataPermissionUtils.executeIgnore(() ->
                stockService.getStockMap(productIds, Collections.singleton(sourceWarehouseId)));
        Map<Long, ErpStockDO> stockMap = new HashMap<>();
        for (Long productId : productIds) {
            ErpStockDO stock = rawStockMap.get(productId + "_" + sourceWarehouseId);
            if (stock == null) {
                stock = new ErpStockDO().setProductId(productId).setWarehouseId(sourceWarehouseId)
                        .setCount(BigDecimal.ZERO).setLockCount(BigDecimal.ZERO);
            }
            stockMap.put(productId, stock);
        }
        validateComponentStock(order, items, productMap, stockMap);
    }

    private ErpAssemblyOrderDO validateOrderExistsForUpdate(Long id) {
        if (id == null) {
            throw exception(ASSEMBLY_ORDER_NOT_EXISTS);
        }
        ErpAssemblyOrderDO order = orderMapper.selectByIdForUpdate(id);
        if (order == null) {
            throw exception(ASSEMBLY_ORDER_NOT_EXISTS);
        }
        return order;
    }

    private ErpWarehouseDO validateWarehouse(Long warehouseId) {
        if (warehouseId == null) {
            throw exception(ASSEMBLY_ORDER_ITEM_INVALID, "组装仓库不能为空");
        }
        warehouseService.validateCurrentUserWarehousePermission(Collections.singleton(warehouseId));
        ErpWarehouseDO warehouse = warehouseService.getWarehouse(warehouseId);
        if (warehouse == null) {
            throw exception(ASSEMBLY_ORDER_ITEM_INVALID, "组装仓库不存在");
        }
        return warehouse;
    }

    private void validateBusinessRelations(ErpAssemblyOrderSaveReqVO reqVO) {
        if (reqVO.getCustomerId() != null) {
            customerService.validateCustomer(reqVO.getCustomerId());
        }
        Long handlerId = reqVO.getHandlerId() != null ? reqVO.getHandlerId() : getLoginUserId();
        if (handlerId != null && adminUserApi.getUser(handlerId) == null) {
            throw exception(ASSEMBLY_ORDER_ITEM_INVALID, "经办人不存在");
        }
        if (reqVO.getOtherCost() != null && reqVO.getOtherCost().signum() < 0) {
            throw exception(ASSEMBLY_ORDER_ITEM_INVALID, "其他成本不能小于 0");
        }
    }

    private Long resolveOutWarehouseId(ErpAssemblyOrderSaveReqVO reqVO) {
        return reqVO.getOutWarehouseId();
    }

    private void validateOrderWarehousePermissions(ErpAssemblyOrderDO order) {
        Set<Long> warehouseIds = new HashSet<>();
        warehouseIds.add(order.getWarehouseId());
        if (order.getOutWarehouseId() != null) {
            warehouseIds.add(order.getOutWarehouseId());
        }
        warehouseService.validateCurrentUserWarehousePermission(warehouseIds);
    }

    private Long resolveSourceWarehouseId(ErpAssemblyOrderDO order) {
        return order.getOutWarehouseId() != null ? order.getOutWarehouseId() : order.getWarehouseId();
    }

    private Map<Long, ErpProductDO> getValidProductMap(Collection<Long> productIds) {
        List<ErpProductDO> products = productService.validProductList(productIds);
        Map<Long, ErpProductDO> map = products.stream()
                .collect(Collectors.toMap(ErpProductDO::getId, Function.identity()));
        if (map.size() != new HashSet<>(productIds).size()) {
            throw exception(ASSEMBLY_ORDER_ITEM_INVALID, "成品或组成配件不存在、已停用或无权访问");
        }
        return map;
    }

    private Map<Long, ErpProductUnitDO> getProductUnitMap(Collection<ErpProductDO> products) {
        Set<Long> unitIds = products.stream().map(ErpProductDO::getUnitId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        return unitIds.isEmpty() ? Collections.emptyMap() : productUnitService.getProductUnitMap(unitIds);
    }

    private List<ErpAssemblyOrderItemDO> buildItemsFromRecipe(
            List<ErpAssemblyRecipeItemDO> recipeItems,
            List<ErpAssemblyOrderSaveReqVO.Item> requestedItems,
            Integer setCount,
            Map<Long, ErpProductDO> productMap,
            Map<Long, ErpProductUnitDO> unitMap) {
        Map<Long, ErpAssemblyRecipeItemDO> recipeItemMap = recipeItems.stream()
                .collect(Collectors.toMap(ErpAssemblyRecipeItemDO::getId, Function.identity()));
        Map<Long, List<ErpAssemblyOrderSaveReqVO.Item>> allocationMap = groupAllocations(requestedItems);
        validateAllocationKeys(allocationMap.keySet(), recipeItemMap.keySet());
        List<ErpAssemblyOrderItemDO> result = new ArrayList<>();
        for (ErpAssemblyRecipeItemDO recipeItem : recipeItems) {
            ErpProductDO product = productMap.get(recipeItem.getProductId());
            BigDecimal required = recipeItem.getQuantityPerSet().multiply(BigDecimal.valueOf(setCount));
            List<ErpAssemblyOrderSaveReqVO.Item> allocations = allocationMap.get(recipeItem.getId());
            if (CollUtil.isEmpty(allocations)) {
                allocations = Collections.singletonList(defaultAllocation(recipeItem.getId(), required));
            }
            validateAllocations(product, required, allocations, false);
            for (ErpAssemblyOrderSaveReqVO.Item allocation : allocations) {
                ErpProductUnitDO unit = product.getUnitId() != null ? unitMap.get(product.getUnitId()) : null;
                result.add(new ErpAssemblyOrderItemDO()
                        .setRecipeItemId(recipeItem.getId())
                        .setProductId(recipeItem.getProductId())
                        .setQuantityPerSet(recipeItem.getQuantityPerSet())
                        .setRequiredQuantity(required)
                        .setBatchNo(normalizeBatch(allocation.getBatchNo()))
                        .setConsumeQuantity(allocation.getConsumeQuantity())
                        .setProductCode(product.getCode())
                        .setProductName(product.getName())
                        .setProductStandard(product.getStandard())
                        .setProductUnitName(unit != null ? unit.getName() : null)
                        .setProductFactoryCode(product.getFactoryCode())
                        .setBatchNoEnabled(Boolean.TRUE.equals(product.getBatchNoEnabled()))
                        .setSort(recipeItem.getSort())
                        .setRemark(allocation.getRemark() != null ? allocation.getRemark() : recipeItem.getRemark()));
            }
        }
        return result;
    }

    private List<ErpAssemblyOrderItemDO> buildItemsFromSnapshot(
            List<ErpAssemblyOrderItemDO> existingItems,
            List<ErpAssemblyOrderSaveReqVO.Item> requestedItems,
            Integer setCount) {
        Map<Long, ErpAssemblyOrderItemDO> templateMap = existingItems.stream()
                .collect(Collectors.toMap(ErpAssemblyOrderItemDO::getRecipeItemId, Function.identity(),
                        (first, second) -> first, LinkedHashMap::new));
        Map<Long, List<ErpAssemblyOrderSaveReqVO.Item>> allocationMap = groupAllocations(requestedItems);
        validateAllocationKeys(allocationMap.keySet(), templateMap.keySet());
        List<ErpAssemblyOrderItemDO> result = new ArrayList<>();
        for (ErpAssemblyOrderItemDO template : templateMap.values()) {
            BigDecimal required = template.getQuantityPerSet().multiply(BigDecimal.valueOf(setCount));
            List<ErpAssemblyOrderSaveReqVO.Item> allocations = allocationMap.get(template.getRecipeItemId());
            if (CollUtil.isEmpty(allocations)) {
                allocations = Collections.singletonList(defaultAllocation(template.getRecipeItemId(), required));
            }
            validateSnapshotAllocations(template, required, allocations, false);
            for (ErpAssemblyOrderSaveReqVO.Item allocation : allocations) {
                result.add(new ErpAssemblyOrderItemDO()
                        .setRecipeItemId(template.getRecipeItemId())
                        .setProductId(template.getProductId())
                        .setQuantityPerSet(template.getQuantityPerSet())
                        .setRequiredQuantity(required)
                        .setBatchNo(normalizeBatch(allocation.getBatchNo()))
                        .setConsumeQuantity(allocation.getConsumeQuantity())
                        .setProductCode(template.getProductCode())
                        .setProductName(template.getProductName())
                        .setProductStandard(template.getProductStandard())
                        .setProductUnitName(template.getProductUnitName())
                        .setProductFactoryCode(template.getProductFactoryCode())
                        .setBatchNoEnabled(template.getBatchNoEnabled())
                        .setSort(template.getSort())
                        .setRemark(allocation.getRemark() != null ? allocation.getRemark() : template.getRemark()));
            }
        }
        return result;
    }

    private Map<Long, List<ErpAssemblyOrderSaveReqVO.Item>> groupAllocations(
            List<ErpAssemblyOrderSaveReqVO.Item> items) {
        if (CollUtil.isEmpty(items)) {
            return Collections.emptyMap();
        }
        return items.stream().collect(Collectors.groupingBy(ErpAssemblyOrderSaveReqVO.Item::getRecipeItemId,
                LinkedHashMap::new, Collectors.toList()));
    }

    private void validateAllocationKeys(Set<Long> actual, Set<Long> allowed) {
        if (!allowed.containsAll(actual)) {
            throw exception(ASSEMBLY_ORDER_ITEM_INVALID, "存在不属于配方的组成配件");
        }
    }

    private void validateAllocations(ErpProductDO product, BigDecimal required,
                                     List<ErpAssemblyOrderSaveReqVO.Item> allocations, boolean completing) {
        validateAllocationValues(product.getName(), Boolean.TRUE.equals(product.getBatchNoEnabled()),
                required, allocations, completing);
    }

    private void validateSnapshotAllocations(ErpAssemblyOrderItemDO template, BigDecimal required,
                                             List<ErpAssemblyOrderSaveReqVO.Item> allocations, boolean completing) {
        validateAllocationValues(template.getProductName(), Boolean.TRUE.equals(template.getBatchNoEnabled()),
                required, allocations, completing);
    }

    private void validateAllocationValues(String productName, boolean batchEnabled, BigDecimal required,
                                          List<ErpAssemblyOrderSaveReqVO.Item> allocations, boolean completing) {
        BigDecimal total = BigDecimal.ZERO;
        Set<String> batches = new HashSet<>();
        boolean hasBlankBatch = false;
        for (ErpAssemblyOrderSaveReqVO.Item allocation : allocations) {
            if (allocation.getConsumeQuantity() == null || allocation.getConsumeQuantity().signum() <= 0) {
                throw exception(ASSEMBLY_ORDER_ITEM_INVALID, productName + "消耗数量必须大于 0");
            }
            total = total.add(allocation.getConsumeQuantity());
            String batch = normalizeBatch(allocation.getBatchNo());
            if (batch == null) {
                hasBlankBatch = true;
            } else if (!batches.add(batch)) {
                throw exception(ASSEMBLY_ORDER_ITEM_INVALID, productName + "批次 " + batch + " 重复");
            }
        }
        if (total.compareTo(required) != 0) {
            throw exception(ASSEMBLY_ORDER_BATCH_TOTAL_INVALID, productName,
                    format(required), format(total));
        }
        if (!batchEnabled && (allocations.size() != 1 || !batches.isEmpty())) {
            throw exception(ASSEMBLY_ORDER_ITEM_INVALID, productName + "未启用批次管理，不能拆分批次");
        }
        if (batchEnabled && hasBlankBatch && (completing || allocations.size() > 1)) {
            throw exception(ASSEMBLY_ORDER_BATCH_REQUIRED, productName);
        }
    }

    private ErpAssemblyOrderSaveReqVO.Item defaultAllocation(Long recipeItemId, BigDecimal required) {
        ErpAssemblyOrderSaveReqVO.Item item = new ErpAssemblyOrderSaveReqVO.Item();
        item.setRecipeItemId(recipeItemId);
        item.setConsumeQuantity(required);
        return item;
    }

    private void validateCompletionBatches(ErpAssemblyOrderDO order, List<ErpAssemblyOrderItemDO> items,
                                           Map<Long, ErpProductDO> productMap) {
        ErpProductDO finishedProduct = productMap.get(order.getProductId());
        if (Boolean.TRUE.equals(finishedProduct.getBatchNoEnabled())
                && !StringUtils.hasText(order.getFinishedBatchNo())) {
            throw exception(ASSEMBLY_ORDER_FINISHED_BATCH_REQUIRED, finishedProduct.getName());
        }
        Map<Long, List<ErpAssemblyOrderItemDO>> groups = items.stream()
                .collect(Collectors.groupingBy(ErpAssemblyOrderItemDO::getRecipeItemId));
        for (List<ErpAssemblyOrderItemDO> group : groups.values()) {
            ErpAssemblyOrderItemDO template = group.get(0);
            List<ErpAssemblyOrderSaveReqVO.Item> allocations = group.stream().map(item -> {
                ErpAssemblyOrderSaveReqVO.Item req = new ErpAssemblyOrderSaveReqVO.Item();
                req.setRecipeItemId(item.getRecipeItemId());
                req.setBatchNo(item.getBatchNo());
                req.setConsumeQuantity(item.getConsumeQuantity());
                return req;
            }).collect(Collectors.toList());
            validateSnapshotAllocations(template, template.getRequiredQuantity(), allocations, true);
        }
    }

    private Map<Long, ErpStockDO> lockStockRows(Collection<Long> productIds, Long warehouseId) {
        List<Long> sortedProductIds = productIds.stream().distinct().sorted().collect(Collectors.toList());
        Map<Long, ErpStockDO> result = new LinkedHashMap<>();
        for (Long productId : sortedProductIds) {
            stockService.ensureStockExists(productId, warehouseId);
            ErpStockDO stock = DataPermissionUtils.executeIgnore(
                    () -> stockMapper.selectByProductIdAndWarehouseIdForUpdate(productId, warehouseId));
            if (stock == null) {
                throw exception(ASSEMBLY_ORDER_ITEM_INVALID, "无法建立库存维度：" + productId);
            }
            result.put(productId, stock);
        }
        return result;
    }

    private void validateComponentStock(ErpAssemblyOrderDO order, List<ErpAssemblyOrderItemDO> items,
                                        Map<Long, ErpProductDO> productMap, Map<Long, ErpStockDO> stockMap) {
        Map<Long, BigDecimal> requiredByProduct = items.stream().collect(Collectors.groupingBy(
                ErpAssemblyOrderItemDO::getProductId,
                Collectors.reducing(BigDecimal.ZERO, ErpAssemblyOrderItemDO::getConsumeQuantity, BigDecimal::add)));
        Long sourceWarehouseId = resolveSourceWarehouseId(order);
        ErpWarehouseDO warehouse = warehouseService.getWarehouse(sourceWarehouseId);
        String warehouseName = warehouse != null ? warehouse.getName() : String.valueOf(sourceWarehouseId);
        for (Map.Entry<Long, BigDecimal> entry : requiredByProduct.entrySet()) {
            ErpStockDO stock = stockMap.get(entry.getKey());
            BigDecimal available = defaultZero(stock.getCount()).subtract(defaultZero(stock.getLockCount()));
            if (available.compareTo(entry.getValue()) < 0) {
                throw exception(ASSEMBLY_ORDER_STOCK_INSUFFICIENT,
                        productMap.get(entry.getKey()).getName(), warehouseName,
                        format(entry.getValue()), format(available));
            }
        }
        for (ErpAssemblyOrderItemDO item : items) {
            if (!Boolean.TRUE.equals(item.getBatchNoEnabled())) {
                continue;
            }
            BigDecimal batchAvailable = stockRecordMapper.selectBatchBalance(
                    item.getProductId(), sourceWarehouseId, item.getBatchNo());
            if (batchAvailable.compareTo(item.getConsumeQuantity()) < 0) {
                throw exception(ASSEMBLY_ORDER_BATCH_STOCK_INSUFFICIENT,
                        item.getProductName(), item.getBatchNo(),
                        format(item.getConsumeQuantity()), format(batchAvailable));
            }
        }
    }

    private void validateFinishedStockForVoid(ErpAssemblyOrderDO order, ErpProductDO finishedProduct,
                                               ErpStockDO stock) {
        Long sourceWarehouseId = resolveSourceWarehouseId(order);
        BigDecimal required = BigDecimal.valueOf(order.getSetCount());
        BigDecimal available = defaultZero(stock.getCount()).subtract(defaultZero(stock.getLockCount()));
        if (available.compareTo(required) < 0) {
            throw exception(ASSEMBLY_ORDER_VOID_FINISHED_STOCK_INSUFFICIENT,
                    finishedProduct.getName(), format(required), format(available));
        }
        if (Boolean.TRUE.equals(finishedProduct.getBatchNoEnabled())) {
            BigDecimal batchAvailable = stockRecordMapper.selectBatchBalance(
                    order.getProductId(), sourceWarehouseId, order.getFinishedBatchNo());
            if (batchAvailable.compareTo(required) < 0) {
                throw exception(ASSEMBLY_ORDER_BATCH_STOCK_INSUFFICIENT,
                        finishedProduct.getName(), order.getFinishedBatchNo(),
                        format(required), format(batchAvailable));
            }
        }
    }

    private OriginalAssemblyRecords validateOriginalRecords(
            ErpAssemblyOrderDO order, List<ErpAssemblyOrderItemDO> items,
            List<ErpStockRecordDO> originalRecords) {
        if (order.getUnitCost() == null || order.getTotalCost() == null
                || originalRecords == null || originalRecords.size() != items.size() + 1) {
            throw exception(ASSEMBLY_ORDER_ORIGINAL_RECORD_MISSING);
        }
        List<ErpStockRecordDO> finishedRecords = originalRecords.stream()
                .filter(record -> ErpStockRecordBizTypeEnum.ASSEMBLY_FINISHED_IN.getType()
                        .equals(record.getBizType()))
                .collect(Collectors.toList());
        if (finishedRecords.size() != 1) {
            throw exception(ASSEMBLY_ORDER_ORIGINAL_RECORD_MISSING);
        }
        ErpStockRecordDO finished = finishedRecords.get(0);
        Long sourceWarehouseId = resolveSourceWarehouseId(order);
        BigDecimal finishedCount = BigDecimal.valueOf(order.getSetCount());
        if (!Objects.equals(finished.getProductId(), order.getProductId())
                || !Objects.equals(finished.getWarehouseId(), sourceWarehouseId)
                || !sameNumber(finished.getCount(), finishedCount)
                || !sameNumber(finished.getUnitPrice(), order.getUnitCost())
                || !Objects.equals(normalizeBatch(finished.getBatchNo()), normalizeBatch(order.getFinishedBatchNo()))) {
            throw exception(ASSEMBLY_ORDER_ORIGINAL_RECORD_MISSING);
        }
        Map<Long, ErpStockRecordDO> components = originalRecords.stream()
                .filter(record -> ErpStockRecordBizTypeEnum.ASSEMBLY_COMPONENT_OUT.getType()
                        .equals(record.getBizType()))
                .collect(Collectors.toMap(ErpStockRecordDO::getBizItemId, Function.identity(),
                        (first, second) -> first));
        if (components.size() != items.size()) {
            throw exception(ASSEMBLY_ORDER_ORIGINAL_RECORD_MISSING);
        }
        for (ErpAssemblyOrderItemDO item : items) {
            ErpStockRecordDO original = components.get(item.getId());
            if (original == null || item.getCostPrice() == null || item.getCostAmount() == null
                    || !Objects.equals(original.getProductId(), item.getProductId())
                    || !Objects.equals(original.getWarehouseId(), sourceWarehouseId)
                    || !sameNumber(original.getCount(), item.getConsumeQuantity().negate())
                    || !sameNumber(original.getUnitPrice(), item.getCostPrice())
                    || !Objects.equals(normalizeBatch(original.getBatchNo()), normalizeBatch(item.getBatchNo()))) {
                throw exception(ASSEMBLY_ORDER_ORIGINAL_RECORD_MISSING);
            }
        }
        return new OriginalAssemblyRecords(finished, components);
    }

    private boolean sameNumber(BigDecimal first, BigDecimal second) {
        return first != null && second != null && first.compareTo(second) == 0;
    }

    private static final class OriginalAssemblyRecords {
        private final ErpStockRecordDO finished;
        private final Map<Long, ErpStockRecordDO> components;

        private OriginalAssemblyRecords(ErpStockRecordDO finished,
                                        Map<Long, ErpStockRecordDO> components) {
            this.finished = finished;
            this.components = components;
        }
    }

    private ErpStockRecordCreateReqBO buildStockRecordReq(
            ErpAssemblyOrderDO order, Long warehouseId, Long productId, Long bizItemId, String batchNo,
            BigDecimal count, Integer bizType, BigDecimal unitCost, String actionSuffix,
            Long sourceRecordId) {
        ErpStockRecordCreateReqBO req = new ErpStockRecordCreateReqBO(
                productId, warehouseId, normalizeBatch(batchNo), count,
                bizType, order.getId(), bizItemId, order.getNo(), unitCost, order.getAssemblyTime());
        req.setAccountingDeptId(order.getDeptId());
        req.setPostingActionKey("assembly:" + order.getId() + ":rev:" + defaultRevision(order)
                + ":" + actionSuffix);
        req.setFinancialUnitCost(unitCost);
        req.setSettlementUnitCost(unitCost);
        req.setCostBasisConfirmed(true);
        req.setSourcePriceBasis("ASSEMBLY_ACTUAL_COST");
        req.setSourceRecordId(sourceRecordId);
        return req;
    }

    private void insertOrderItems(Long orderId, List<ErpAssemblyOrderItemDO> items) {
        items.forEach(item -> item.setId(null).setOrderId(orderId)
                .setBatchNo(normalizeBatch(item.getBatchNo()))
                .setCostPrice(BigDecimal.ZERO)
                .setCostAmount(BigDecimal.ZERO));
        orderItemMapper.insertBatch(items);
    }

    private void validateSetCount(Integer setCount) {
        if (setCount == null || setCount <= 0) {
            throw exception(ASSEMBLY_ORDER_ITEM_INVALID, "组装套数必须是正整数");
        }
    }

    private String normalizeBatch(String batchNo) {
        return StringUtils.hasText(batchNo) ? batchNo.trim() : null;
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    /**
     * 空日期兼容场景可能把 Unix 时间戳 0 反序列化为 1970 年时间。
     * 业务单据不接受该占位值：新增使用当前时间，编辑草稿优先保留已有有效时间。
     */
    private LocalDateTime normalizeBusinessTime(LocalDateTime value, LocalDateTime fallback) {
        return value == null || value.isBefore(EARLIEST_VALID_BUSINESS_TIME) ? fallback : value;
    }

    private String format(BigDecimal value) {
        return defaultZero(value).stripTrailingZeros().toPlainString();
    }

}
