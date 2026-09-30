package cn.iocoder.yudao.module.erp.service.sale;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutPickDeliveryDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutPickDeliveryFileRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery.*;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.pickdelivery.*;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleCartMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleCartItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.pickdelivery.*;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleBizSourceTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleDeliveryStatusEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSalePickDeliverySubmitTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSalePickStatusEnum;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockMoveService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.infra.api.file.FileApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.springframework.stereotype.Service;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.*;

@Service
@Validated
public class ErpSalePickDeliveryServiceImpl implements ErpSalePickDeliveryService {

    private static final String VOUCHER_FILE_DIRECTORY = "erp/sale-pick-delivery/voucher";
    private static final long VOUCHER_FILE_MAX_SIZE = 5L * 1024 * 1024;
    private static final Set<String> VOUCHER_FILE_EXTENSIONS = new HashSet<>(Arrays.asList("jpg", "jpeg", "png"));
    private static final byte[] PNG_SIGNATURE = new byte[]{
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};

    @Resource
    private ErpSaleOutMapper saleOutMapper;
    @Resource
    private ErpSaleOutItemMapper saleOutItemMapper;
    @Resource
    private ErpSaleCartMapper saleCartMapper;
    @Resource
    private ErpSaleCartItemMapper saleCartItemMapper;
    @Resource
    private ErpSalePickDeliveryOrderMapper orderMapper;
    @Resource
    private ErpSalePickDeliveryPickTaskMapper pickTaskMapper;
    @Resource
    private ErpSalePickDeliveryItemMapper itemMapper;
    @Resource
    private ErpSalePickDeliverySubmitMapper submitMapper;
    @Resource
    private ErpSalePickDeliverySubmitItemMapper submitItemMapper;
    @Resource
    private ErpSalePickDeliverySubmitFileMapper submitFileMapper;
    @Resource
    private ErpCustomerService customerService;
    @Resource
    private ErpProductService productService;
    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private ErpStockMoveService stockMoveService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private FileApi fileApi;
    @Resource
    private ApplicationEventPublisher eventPublisher;

    @Override
    public String getSaleOutDeliveryUserNames(Long saleOutId) {
        if (saleOutId == null) {
            return "";
        }
        // 只读取本单去重人员，不加载作业明细、附件；提交记录保留分批送货的全部操作人。
        Set<Long> userIds = new LinkedHashSet<>();
        submitMapper.selectDeliveryUsersBySaleOutId(saleOutId)
                .forEach(submit -> userIds.add(submit.getSubmitUserId()));
        itemMapper.selectDeliveryUsersBySaleOutId(saleOutId)
                .forEach(item -> userIds.add(item.getDeliveryUserId()));
        userIds.remove(null);
        if (userIds.isEmpty()) {
            return "";
        }
        Map<Long, AdminUserRespDTO> users = adminUserApi.getUserMap(userIds);
        Set<String> names = new LinkedHashSet<>();
        for (Long userId : userIds) {
            AdminUserRespDTO user = users.get(userId);
            if (user != null && StringUtils.hasText(user.getNickname())) {
                names.add(user.getNickname().trim());
            }
        }
        return String.join("、", names);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateForSaleOut(Long saleOutId) {
        if (saleOutId == null || orderMapper.selectBySaleOutId(saleOutId) != null) {
            return;
        }
        ErpSaleOutDO saleOut = saleOutMapper.selectByIdForUpdate(saleOutId);
        if (saleOut == null || !ErpAuditStatus.APPROVE.getStatus().equals(saleOut.getStatus())) {
            return;
        }
        if (bindSourceOrderToSaleOutIfPresent(saleOut)) {
            return;
        }
        List<ErpSaleOutItemDO> saleOutItems = saleOutItemMapper.selectListByOutIdForUpdate(saleOutId);
        if (CollUtil.isEmpty(saleOutItems)) {
            return;
        }
        if (saleOutItems.stream().anyMatch(item -> item.getWarehouseId() == null)) {
            throw exception(SALE_PICK_DELIVERY_WAREHOUSE_REQUIRED);
        }

        Long tenantId = TenantContextHolder.getRequiredTenantId();
        ErpCustomerDO customer = saleOut.getCustomerId() == null ? null : customerService.getCustomer(saleOut.getCustomerId());
        Map<Long, ErpWarehouseDO> warehouseMap = warehouseService.getWarehouseMap(convertSet(saleOutItems, ErpSaleOutItemDO::getWarehouseId));
        Map<Long, ErpProductRespVO> productMap = productService.getProductVOMap(convertSet(saleOutItems, ErpSaleOutItemDO::getProductId));
        String customerName = customer == null ? null : customer.getName();

        ErpSalePickDeliveryOrderDO order = new ErpSalePickDeliveryOrderDO()
                .setSaleOutId(saleOut.getId()).setSaleOutNo(saleOut.getNo())
                .setCustomerId(saleOut.getCustomerId()).setCustomerName(customerName).setDeptId(saleOut.getDeptId())
                .setPickStatus(ErpSalePickStatusEnum.WAITING.getStatus())
                .setDeliveryStatus(ErpSaleDeliveryStatusEnum.NOT_READY.getStatus())
                .setTotalItemCount(saleOutItems.size()).setPickedItemCount(0).setDeliveredItemCount(0);
        order.setTenantId(tenantId);
        orderMapper.insert(order);

        Map<Long, List<ErpSaleOutItemDO>> itemMap = saleOutItems.stream()
                .collect(Collectors.groupingBy(ErpSaleOutItemDO::getWarehouseId, LinkedHashMap::new, Collectors.toList()));
        for (Map.Entry<Long, List<ErpSaleOutItemDO>> entry : itemMap.entrySet()) {
            Long warehouseId = entry.getKey();
            ErpWarehouseDO warehouse = warehouseMap.get(warehouseId);
            ErpSalePickDeliveryPickTaskDO task = new ErpSalePickDeliveryPickTaskDO()
                    .setOrderId(order.getId()).setSaleOutId(saleOut.getId()).setSaleOutNo(saleOut.getNo())
                    .setCustomerId(saleOut.getCustomerId()).setCustomerName(customerName)
                    .setWarehouseId(warehouseId).setWarehouseName(warehouse == null ? null : warehouse.getName())
                    .setStatus(ErpSalePickStatusEnum.WAITING.getStatus())
                    .setTotalItemCount(entry.getValue().size()).setPickedItemCount(0);
            task.setTenantId(tenantId);
            pickTaskMapper.insert(task);
            for (ErpSaleOutItemDO saleOutItem : entry.getValue()) {
                ErpProductRespVO product = productMap.get(saleOutItem.getProductId());
                ErpSalePickDeliveryItemDO item = new ErpSalePickDeliveryItemDO()
                        .setOrderId(order.getId()).setPickTaskId(task.getId())
                        .setSaleOutId(saleOut.getId()).setSaleOutItemId(saleOutItem.getId())
                        .setWarehouseId(warehouseId).setWarehouseName(task.getWarehouseName())
                        .setProductId(saleOutItem.getProductId())
                        .setProductCode(product == null ? null : product.getCode())
                        .setProductName(product == null ? null : product.getName())
                        .setStandard(product == null ? saleOutItem.getStandard() : product.getStandard())
                        .setCount(saleOutItem.getCount()).setPickedCount(BigDecimal.ZERO).setDeliveredCount(BigDecimal.ZERO)
                        .setWarehousePosition(saleOutItem.getWarehousePosition())
                        .setPackageQty(saleOutItem.getPackageQty())
                        .setPickStatus(ErpSalePickStatusEnum.WAITING.getStatus())
                        .setDeliveryStatus(ErpSaleDeliveryStatusEnum.NOT_READY.getStatus());
                item.setTenantId(tenantId);
                itemMapper.insert(item);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateForSaleCart(Long saleCartId) {
        if (saleCartId == null || orderMapper.selectBySource(
                ErpSaleBizSourceTypeEnum.CART.getType(), saleCartId) != null) {
            return;
        }
        ErpSaleCartDO cart = saleCartMapper.selectById(saleCartId);
        if (cart == null) {
            return;
        }
        List<ErpSaleCartItemDO> cartItems = saleCartItemMapper.selectListByCartIdForUpdate(saleCartId);
        if (CollUtil.isEmpty(cartItems)) {
            return;
        }
        if (cartItems.stream().anyMatch(item -> item.getWarehouseId() == null)) {
            throw exception(SALE_PICK_DELIVERY_WAREHOUSE_REQUIRED);
        }
        Map<Long, ErpWarehouseDO> sourceWarehouseMap = warehouseService.getWarehouseMap(
                convertSet(cartItems, ErpSaleCartItemDO::getWarehouseId));
        Set<Long> crossWarehouseIds = cartItems.stream()
                .map(ErpSaleCartItemDO::getWarehouseId)
                .filter(warehouseId -> isCrossDeptWarehouse(cart.getDeptId(), sourceWarehouseMap.get(warehouseId)))
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<ErpStockMoveDO> transferOuts = stockMoveService.getTransferOutListBySource(
                ErpSaleBizSourceTypeEnum.CART.getType(), saleCartId);
        List<ErpStockMoveItemDO> transferItems = CollUtil.isEmpty(transferOuts)
                ? Collections.emptyList() : stockMoveService.getStockMoveItemListByMoveIds(
                convertSet(transferOuts, ErpStockMoveDO::getId));
        if (transferItems.stream().anyMatch(item -> item.getFromWarehouseId() == null)) {
            throw exception(SALE_PICK_DELIVERY_WAREHOUSE_REQUIRED);
        }

        Map<String, BigDecimal> expectedCrossCounts = new LinkedHashMap<>();
        cartItems.stream().filter(item -> crossWarehouseIds.contains(item.getWarehouseId())).forEach(item ->
                expectedCrossCounts.merge(buildProductWarehouseKey(item.getProductId(), item.getWarehouseId()),
                        item.getCount(), BigDecimal::add));
        Map<String, BigDecimal> actualCrossCounts = new LinkedHashMap<>();
        transferItems.forEach(item -> actualCrossCounts.merge(
                buildProductWarehouseKey(item.getProductId(), item.getFromWarehouseId()),
                item.getCount(), BigDecimal::add));
        if (!countMapEquals(expectedCrossCounts, actualCrossCounts)) {
            throw exception(SALE_PICK_DELIVERY_CART_SOURCE_INVALID);
        }

        List<SaleCartFulfillmentItem> fulfillmentItems = new ArrayList<>();
        cartItems.stream().filter(item -> !crossWarehouseIds.contains(item.getWarehouseId()))
                .forEach(item -> fulfillmentItems.add(SaleCartFulfillmentItem.fromCart(item)));
        transferItems.forEach(item -> fulfillmentItems.add(SaleCartFulfillmentItem.fromTransfer(item)));
        if (CollUtil.isEmpty(fulfillmentItems)) {
            return;
        }

        Long tenantId = TenantContextHolder.getRequiredTenantId();
        ErpCustomerDO customer = cart.getCustomerId() == null ? null : customerService.getCustomer(cart.getCustomerId());
        String customerName = customer == null ? null : customer.getName();
        Map<Long, ErpWarehouseDO> warehouseMap = warehouseService.getWarehouseMap(
                convertSet(fulfillmentItems, item -> item.warehouseId));
        Map<Long, ErpProductRespVO> productMap = productService.getProductVOMap(
                convertSet(fulfillmentItems, item -> item.productId));

        ErpSalePickDeliveryOrderDO order = new ErpSalePickDeliveryOrderDO()
                .setSourceType(ErpSaleBizSourceTypeEnum.CART.getType())
                .setSourceId(cart.getId()).setSourceNo(cart.getNo())
                .setCustomerId(cart.getCustomerId()).setCustomerName(customerName).setDeptId(cart.getDeptId())
                .setPickStatus(ErpSalePickStatusEnum.WAITING.getStatus())
                .setDeliveryStatus(ErpSaleDeliveryStatusEnum.NOT_READY.getStatus())
                .setTotalItemCount(fulfillmentItems.size()).setPickedItemCount(0).setDeliveredItemCount(0);
        order.setTenantId(tenantId);
        orderMapper.insert(order);

        Map<Long, List<SaleCartFulfillmentItem>> itemMap = fulfillmentItems.stream()
                .collect(Collectors.groupingBy(item -> item.warehouseId,
                        LinkedHashMap::new, Collectors.toList()));
        for (Map.Entry<Long, List<SaleCartFulfillmentItem>> entry : itemMap.entrySet()) {
            Long warehouseId = entry.getKey();
            ErpWarehouseDO warehouse = warehouseMap.get(warehouseId);
            ErpSalePickDeliveryPickTaskDO task = new ErpSalePickDeliveryPickTaskDO()
                    .setOrderId(order.getId())
                    .setSourceType(order.getSourceType()).setSourceId(order.getSourceId()).setSourceNo(order.getSourceNo())
                    .setCustomerId(cart.getCustomerId()).setCustomerName(customerName)
                    .setWarehouseId(warehouseId).setWarehouseName(warehouse == null ? null : warehouse.getName())
                    .setStatus(ErpSalePickStatusEnum.WAITING.getStatus())
                    .setTotalItemCount(entry.getValue().size()).setPickedItemCount(0);
            task.setTenantId(tenantId);
            pickTaskMapper.insert(task);
            for (SaleCartFulfillmentItem sourceItem : entry.getValue()) {
                ErpProductRespVO product = productMap.get(sourceItem.productId);
                ErpSalePickDeliveryItemDO item = new ErpSalePickDeliveryItemDO()
                        .setOrderId(order.getId()).setPickTaskId(task.getId())
                        .setTransferOutId(sourceItem.transferOutId).setTransferOutItemId(sourceItem.transferOutItemId)
                        .setWarehouseId(warehouseId).setWarehouseName(task.getWarehouseName())
                        .setProductId(sourceItem.productId)
                        .setProductCode(product == null ? null : product.getCode())
                        .setProductName(product == null ? null : product.getName())
                        .setStandard(product == null ? sourceItem.standard : product.getStandard())
                        .setCount(sourceItem.count).setPickedCount(BigDecimal.ZERO).setDeliveredCount(BigDecimal.ZERO)
                        .setWarehousePosition(sourceItem.warehousePosition)
                        .setPackageQty(sourceItem.packageQty)
                        .setPickStatus(ErpSalePickStatusEnum.WAITING.getStatus())
                        .setDeliveryStatus(ErpSaleDeliveryStatusEnum.NOT_READY.getStatus());
                item.setTenantId(tenantId);
                itemMapper.insert(item);
            }
        }
    }

    @Override
    public PageResult<ErpSalePickTaskRespVO> getPickPage(ErpSalePickPageReqVO reqVO, boolean mobile) {
        if (mobile) {
            fillCurrentUserWarehouseIds(reqVO);
        }
        PageResult<ErpSalePickDeliveryPickTaskDO> pageResult = pickTaskMapper.selectPage(reqVO);
        return new PageResult<>(convertList(pageResult.getList(), this::buildPickResp), pageResult.getTotal());
    }

    @Override
    public ErpSalePickTaskRespVO getPick(Long id, boolean mobile, boolean includeDetail) {
        ErpSalePickDeliveryPickTaskDO task = pickTaskMapper.selectById(id);
        if (task == null) {
            throw exception(SALE_PICK_TASK_NOT_EXISTS);
        }
        if (mobile) {
            validatePickWarehousePermission(task.getWarehouseId());
        }
        return buildPickResp(task, includeDetail);
    }

    @Override
    public PageResult<ErpSalePickDeliveryItemRespVO> getPickItemPage(Long taskId, PageParam pageParam, boolean mobile) {
        ErpSalePickDeliveryPickTaskDO task = pickTaskMapper.selectById(taskId);
        if (task == null) {
            throw exception(SALE_PICK_TASK_NOT_EXISTS);
        }
        if (mobile) {
            validatePickWarehousePermission(task.getWarehouseId());
        }
        PageResult<ErpSalePickDeliveryItemDO> pageResult = itemMapper.selectPageByPickTaskId(pageParam, taskId);
        return new PageResult<>(buildItemRespList(pageResult.getList()), pageResult.getTotal());
    }

    @Override
    public PageResult<ErpSalePickDeliverySubmitRespVO> getPickSubmitPage(Long taskId, PageParam pageParam, boolean mobile) {
        ErpSalePickDeliveryPickTaskDO task = pickTaskMapper.selectById(taskId);
        if (task == null) {
            throw exception(SALE_PICK_TASK_NOT_EXISTS);
        }
        if (mobile) {
            validatePickWarehousePermission(task.getWarehouseId());
        }
        PageResult<ErpSalePickDeliverySubmitDO> pageResult = submitMapper.selectPageByPickTaskIdAndType(
                pageParam, taskId, ErpSalePickDeliverySubmitTypeEnum.PICK.getType());
        return new PageResult<>(buildSubmitRespList(pageResult.getList()), pageResult.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void submitPick(ErpSalePickSubmitReqVO reqVO) {
        List<Long> itemIds = validateSubmitPayload(reqVO.getItemIds(), reqVO.getItems(), reqVO.getRequestId(), reqVO.getFiles());
        // 所有作业统一先锁主单，避免拣货任务与送货/来源回绑出现反向锁序。
        ErpSalePickDeliveryPickTaskDO snapshot = pickTaskMapper.selectById(reqVO.getTaskId());
        if (snapshot == null) {
            throw exception(SALE_PICK_TASK_NOT_EXISTS);
        }
        ErpSalePickDeliveryOrderDO order = orderMapper.selectByIdForUpdate(snapshot.getOrderId());
        if (order == null) {
            throw exception(SALE_PICK_DELIVERY_ORDER_NOT_EXISTS);
        }
        ErpSalePickDeliveryPickTaskDO task = pickTaskMapper.selectByIdForUpdate(reqVO.getTaskId());
        if (task == null || !Objects.equals(task.getOrderId(), order.getId())) {
            throw exception(SALE_PICK_TASK_NOT_EXISTS);
        }
        validatePickWarehousePermission(task.getWarehouseId());
        String hash = requestHash(task.getId(), reqVO.getItems(), reqVO.getRemark(), reqVO.getFiles());
        if (isCompletedRequest(order.getId(), 10, reqVO.getRequestId(), hash)) {
            return;
        }
        if (ErpSalePickStatusEnum.DONE.getStatus().equals(task.getStatus())) {
            throw exception(SALE_PICK_TASK_STATUS_INVALID);
        }
        List<ErpSalePickDeliveryItemDO> items = itemMapper.selectListByIdsForUpdate(itemIds);
        if (items.size() != itemIds.size() || items.stream().anyMatch(item ->
                !Objects.equals(item.getPickTaskId(), task.getId())
                        || !Objects.equals(item.getOrderId(), order.getId())
                        || ErpSalePickStatusEnum.DONE.getStatus().equals(item.getPickStatus()))) {
            throw exception(SALE_PICK_ITEM_INVALID);
        }
        Map<Long, BigDecimal> quantities = resolveQuantities(items, reqVO.getItems(), true);
        LocalDateTime now = LocalDateTime.now();
        Long userId = getLoginUserId();
        Long submitId = insertSubmit(order.getId(), task.getId(), task.getSaleOutId(),
                ErpSalePickDeliverySubmitTypeEnum.PICK.getType(), userId, now, itemIds.size(), reqVO.getRemark(),
                reqVO.getFiles(), reqVO.getRequestId(), hash);
        for (ErpSalePickDeliveryItemDO item : items) {
            BigDecimal picked = completedCount(item.getPickedCount(), item.getPickStatus(), item.getCount())
                    .add(quantities.get(item.getId()));
            itemMapper.updateById(new ErpSalePickDeliveryItemDO().setId(item.getId()).setPickedCount(picked)
                    .setPickStatus(quantityStatus(picked, item.getCount()))
                    .setPickUserId(userId).setPickTime(now)
                    .setDeliveryStatus(picked.compareTo(item.getCount()) == 0
                            ? ErpSaleDeliveryStatusEnum.WAITING.getStatus() : ErpSaleDeliveryStatusEnum.NOT_READY.getStatus()));
            insertSubmitItem(submitId, item, quantities.get(item.getId()));
        }
        refreshPickProgress(task, order, now);
    }

    @Override
    public PageResult<ErpSaleDeliveryOrderRespVO> getDeliveryPage(ErpSaleDeliveryPageReqVO reqVO, boolean mobile) {
        PageResult<ErpSalePickDeliveryOrderDO> pageResult = orderMapper.selectPage(reqVO);
        return new PageResult<>(convertList(pageResult.getList(), this::buildDeliveryResp), pageResult.getTotal());
    }

    @Override
    public ErpSaleDeliveryOrderRespVO getDelivery(Long id, boolean mobile, boolean includeDetail) {
        ErpSalePickDeliveryOrderDO order = orderMapper.selectById(id);
        if (order == null) {
            throw exception(SALE_PICK_DELIVERY_ORDER_NOT_EXISTS);
        }
        return buildDeliveryResp(order, includeDetail);
    }

    @Override
    public PageResult<ErpSalePickDeliveryItemRespVO> getDeliveryItemPage(Long orderId, PageParam pageParam, boolean mobile) {
        ErpSalePickDeliveryOrderDO order = orderMapper.selectById(orderId);
        if (order == null) {
            throw exception(SALE_PICK_DELIVERY_ORDER_NOT_EXISTS);
        }
        PageResult<ErpSalePickDeliveryItemDO> pageResult = itemMapper.selectPageByOrderId(pageParam, orderId);
        return new PageResult<>(buildItemRespList(pageResult.getList()), pageResult.getTotal());
    }

    @Override
    public PageResult<ErpSalePickDeliverySubmitRespVO> getDeliverySubmitPage(Long orderId, PageParam pageParam, boolean mobile) {
        ErpSalePickDeliveryOrderDO order = orderMapper.selectById(orderId);
        if (order == null) {
            throw exception(SALE_PICK_DELIVERY_ORDER_NOT_EXISTS);
        }
        PageResult<ErpSalePickDeliverySubmitDO> pageResult = submitMapper.selectPageByOrderIdAndType(
                pageParam, orderId, ErpSalePickDeliverySubmitTypeEnum.DELIVERY.getType());
        return new PageResult<>(buildSubmitRespList(pageResult.getList()), pageResult.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void submitDelivery(ErpSaleDeliverySubmitReqVO reqVO) {
        List<Long> itemIds = validateSubmitPayload(reqVO.getItemIds(), reqVO.getItems(), reqVO.getRequestId(), reqVO.getFiles());
        ErpSalePickDeliveryOrderDO order = orderMapper.selectByIdForUpdate(reqVO.getOrderId());
        if (order == null) {
            throw exception(SALE_PICK_DELIVERY_ORDER_NOT_EXISTS);
        }
        String hash = requestHash(order.getId(), reqVO.getItems(), reqVO.getRemark(), reqVO.getFiles());
        if (isCompletedRequest(order.getId(), 20, reqVO.getRequestId(), hash)) {
            return;
        }
        if (!ErpSalePickStatusEnum.DONE.getStatus().equals(order.getPickStatus())) {
            throw exception(SALE_DELIVERY_NOT_READY);
        }
        if (ErpSaleDeliveryStatusEnum.DONE.getStatus().equals(order.getDeliveryStatus())
                || ErpSaleDeliveryStatusEnum.NOT_READY.getStatus().equals(order.getDeliveryStatus())) {
            throw exception(SALE_DELIVERY_STATUS_INVALID);
        }
        List<ErpSalePickDeliveryItemDO> items = itemMapper.selectListByIdsForUpdate(itemIds);
        if (items.size() != itemIds.size() || items.stream().anyMatch(item ->
                !Objects.equals(item.getOrderId(), order.getId())
                        || !ErpSalePickStatusEnum.DONE.getStatus().equals(item.getPickStatus())
                        || ErpSaleDeliveryStatusEnum.DONE.getStatus().equals(item.getDeliveryStatus()))) {
            throw exception(SALE_DELIVERY_ITEM_INVALID);
        }
        Map<Long, BigDecimal> quantities = resolveQuantities(items, reqVO.getItems(), false);
        LocalDateTime now = LocalDateTime.now();
        Long userId = getLoginUserId();
        Long submitId = insertSubmit(order.getId(), null, order.getSaleOutId(),
                ErpSalePickDeliverySubmitTypeEnum.DELIVERY.getType(), userId, now, itemIds.size(), reqVO.getRemark(),
                reqVO.getFiles(), reqVO.getRequestId(), hash);
        for (ErpSalePickDeliveryItemDO item : items) {
            BigDecimal delivered = completedCount(item.getDeliveredCount(), item.getDeliveryStatus(), item.getCount())
                    .add(quantities.get(item.getId()));
            itemMapper.updateById(new ErpSalePickDeliveryItemDO().setId(item.getId()).setDeliveredCount(delivered)
                    .setDeliveryStatus(quantityStatus(delivered, item.getCount()))
                    .setDeliveryUserId(userId).setDeliveryTime(now));
            insertSubmitItem(submitId, item, quantities.get(item.getId()));
        }
        Integer deliveryStatus = refreshDeliveryProgress(order, now);
        if (ErpSaleDeliveryStatusEnum.DONE.getStatus().equals(deliveryStatus)) {
            approveLinkedCartTransferOutsAfterDelivery(order, userId);
        }
    }

    @Override
    public String uploadVoucher(byte[] content, String fileName) {
        String extension = validateVoucherFile(content, fileName);
        String contentType = "png".equals(extension) ? "image/png" : "image/jpeg";
        return fileApi.createFile(content, fileName, VOUCHER_FILE_DIRECTORY, contentType);
    }

    @Override
    public Map<Long, ErpSalePickDeliverySummaryRespVO> getSummaryMapBySaleOutIds(Collection<Long> saleOutIds) {
        if (CollUtil.isEmpty(saleOutIds)) {
            return Collections.emptyMap();
        }
        List<ErpSalePickDeliveryOrderDO> orders = orderMapper.selectListBySaleOutIds(saleOutIds);
        if (CollUtil.isEmpty(orders)) {
            return Collections.emptyMap();
        }
        return orders.stream().collect(Collectors.toMap(ErpSalePickDeliveryOrderDO::getSaleOutId,
                this::buildSummaryResp, (first, second) -> first));
    }

    @Override
    public Map<Long, ErpSalePickDeliverySummaryRespVO> getSummaryMapBySaleCartIds(Collection<Long> saleCartIds) {
        if (CollUtil.isEmpty(saleCartIds)) {
            return Collections.emptyMap();
        }
        List<ErpSalePickDeliveryOrderDO> orders = orderMapper.selectListBySourceIds(
                ErpSaleBizSourceTypeEnum.CART.getType(), saleCartIds);
        if (CollUtil.isEmpty(orders)) {
            return Collections.emptyMap();
        }
        return orders.stream().collect(Collectors.toMap(ErpSalePickDeliveryOrderDO::getSourceId,
                this::buildSummaryResp, (first, second) -> first));
    }

    @Override
    public ErpSaleOutPickDeliveryDetailRespVO getSaleOutPickDeliveryDetail(Long saleOutId) {
        ErpSaleOutPickDeliveryDetailRespVO resp = new ErpSaleOutPickDeliveryDetailRespVO();
        ErpSalePickDeliveryOrderDO order = orderMapper.selectBySaleOutId(saleOutId);
        if (order == null) {
            resp.setPickTasks(Collections.emptyList());
            resp.setSubmits(Collections.emptyList());
            return resp;
        }
        resp.setSummary(buildSummaryResp(order));
        resp.setDeliveryOrder(buildDeliveryResp(order, false));
        resp.setPickTasks(convertList(pickTaskMapper.selectListByOrderId(order.getId()), this::buildPickResp));
        resp.setSubmits(buildSubmitRespList(submitMapper.selectListBySaleOutId(saleOutId)));
        return resp;
    }

    @Override
    public PageResult<ErpSalePickDeliveryItemRespVO> getSaleOutPickDeliveryItemPage(Long saleOutId,
                                                                                    PageParam pageParam) {
        ErpSalePickDeliveryOrderDO order = orderMapper.selectBySaleOutId(saleOutId);
        if (order == null) {
            return PageResult.empty();
        }
        PageResult<ErpSalePickDeliveryItemDO> pageResult = itemMapper.selectPageBySaleOutId(pageParam, saleOutId);
        return new PageResult<>(buildItemRespList(pageResult.getList()), pageResult.getTotal());
    }

    @Override
    public PageResult<ErpSaleOutPickDeliveryFileRespVO> getSaleOutPickDeliveryFilePage(Long saleOutId, Integer type,
                                                                                       PageParam pageParam) {
        ErpSalePickDeliveryOrderDO order = orderMapper.selectBySaleOutId(saleOutId);
        if (order == null) {
            return PageResult.empty();
        }
        List<ErpSalePickDeliverySubmitRespVO> submits = buildSubmitRespList(
                submitMapper.selectListBySaleOutIdAndType(saleOutId, type));
        if (CollUtil.isEmpty(submits)) {
            return PageResult.empty();
        }
        List<ErpSaleOutPickDeliveryFileRespVO> files = new ArrayList<>();
        for (ErpSalePickDeliverySubmitRespVO submit : submits) {
            if (CollUtil.isEmpty(submit.getFiles())) {
                continue;
            }
            for (ErpSalePickDeliveryFileRespVO file : submit.getFiles()) {
                files.add(buildSaleOutFileResp(submit, file));
            }
        }
        if (CollUtil.isEmpty(files)) {
            return PageResult.empty();
        }
        int pageNo = pageParam.getPageNo() == null ? 1 : pageParam.getPageNo();
        int pageSize = pageParam.getPageSize() == null ? 10 : pageParam.getPageSize();
        int from = Math.max(0, (pageNo - 1) * pageSize);
        if (from >= files.size()) {
            return new PageResult<>(Collections.emptyList(), (long) files.size());
        }
        int to = Math.min(files.size(), from + pageSize);
        return new PageResult<>(files.subList(from, to), (long) files.size());
    }

    private void fillCurrentUserWarehouseIds(ErpSalePickPageReqVO reqVO) {
        List<Long> warehouseIds = warehouseService.getUserPickWarehouseIds(getLoginUserId());
        reqVO.setWarehouseIds(CollUtil.isEmpty(warehouseIds) ? Collections.singletonList(-1L) : warehouseIds);
    }

    private void validatePickWarehousePermission(Long warehouseId) {
        List<Long> warehouseIds = warehouseService.getUserPickWarehouseIds(getLoginUserId());
        if (CollUtil.isEmpty(warehouseIds) || !warehouseIds.contains(warehouseId)) {
            throw exception(SALE_PICK_TASK_WAREHOUSE_PERMISSION_DENIED);
        }
    }

    private List<Long> validateSubmitPayload(List<Long> legacyIds, List<ErpSalePickDeliverySubmitItemReqVO> items,
                                           String requestId, List<ErpSalePickSubmitReqVO.File> files) {
        if ((legacyIds != null && items != null) || (items != null && (requestId == null
                || !requestId.matches("[A-Za-z0-9_-]{16,64}"))) || (items == null && requestId != null)) {
            throw exception(SALE_PICK_DELIVERY_PAYLOAD_INVALID);
        }
        List<Long> ids = items == null ? legacyIds : items.stream().map(item -> item == null ? null : item.getItemId())
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(ids)) {
            throw exception(SALE_PICK_DELIVERY_ITEM_REQUIRED);
        }
        if (ids.contains(null) || new HashSet<>(ids).size() != ids.size()) {
            throw exception(SALE_PICK_DELIVERY_PAYLOAD_INVALID);
        }
        if (CollUtil.isEmpty(files)) {
            throw exception(SALE_PICK_DELIVERY_FILE_REQUIRED);
        }
        if (items != null && items.stream().anyMatch(item -> !validQuantity(item.getQuantity()))) {
            throw exception(SALE_PICK_DELIVERY_QUANTITY_INVALID);
        }
        return ids;
    }

    private boolean validQuantity(BigDecimal quantity) {
        return quantity != null && quantity.signum() > 0 && quantity.stripTrailingZeros().scale() <= 0
                && quantity.precision() - quantity.scale() <= 18;
    }

    private BigDecimal completedCount(BigDecimal value, Integer status, BigDecimal total) {
        return value != null ? value : (Integer.valueOf(30).equals(status) ? total : BigDecimal.ZERO);
    }

    private int quantityStatus(BigDecimal completed, BigDecimal total) {
        return completed.signum() == 0 ? 10 : (completed.compareTo(total) >= 0 ? 30 : 20);
    }

    private Map<Long, BigDecimal> resolveQuantities(List<ErpSalePickDeliveryItemDO> items,
                                                   List<ErpSalePickDeliverySubmitItemReqVO> requested, boolean pick) {
        Map<Long, BigDecimal> requestedMap = requested == null ? Collections.emptyMap() : requested.stream()
                .collect(Collectors.toMap(ErpSalePickDeliverySubmitItemReqVO::getItemId,
                        ErpSalePickDeliverySubmitItemReqVO::getQuantity));
        Map<Long, BigDecimal> quantities = new LinkedHashMap<>();
        for (ErpSalePickDeliveryItemDO item : items) {
            BigDecimal done = completedCount(pick ? item.getPickedCount() : item.getDeliveredCount(),
                    pick ? item.getPickStatus() : item.getDeliveryStatus(), item.getCount());
            BigDecimal remaining = item.getCount().subtract(done);
            BigDecimal quantity = requested == null ? remaining : requestedMap.get(item.getId());
            if (!validQuantity(quantity) || quantity.compareTo(remaining) > 0) {
                throw exception(SALE_PICK_DELIVERY_QUANTITY_INVALID);
            }
            quantities.put(item.getId(), quantity);
        }
        return quantities;
    }

    private String requestHash(Long parentId, List<ErpSalePickDeliverySubmitItemReqVO> items, String remark,
                               List<ErpSalePickSubmitReqVO.File> files) {
        if (items == null) {
            return null;
        }
        List<String> canonicalItems = items.stream().sorted(Comparator.comparing(ErpSalePickDeliverySubmitItemReqVO::getItemId))
                .map(item -> item.getItemId() + ":" + item.getQuantity().stripTrailingZeros().toPlainString())
                .collect(Collectors.toList());
        String material = cn.hutool.json.JSONUtil.toJsonStr(Arrays.asList(parentId, getLoginUserId(), canonicalItems,
                remark, files));
        return cn.hutool.crypto.digest.DigestUtil.sha256Hex(material);
    }

    private boolean isCompletedRequest(Long orderId, Integer type, String requestId, String hash) {
        if (requestId == null) {
            return false;
        }
        // 主单行锁内查询，包含完成状态的重试也直接返回，避免最后一批重复触发联动。
        ErpSalePickDeliverySubmitDO previous = submitMapper.selectByRequestId(orderId, type, requestId);
        if (previous == null) {
            return false;
        }
        if (!Objects.equals(previous.getRequestHash(), hash) || !Objects.equals(previous.getSubmitUserId(), getLoginUserId())) {
            throw exception(SALE_PICK_DELIVERY_REQUEST_CONFLICT);
        }
        return true;
    }

    private void insertSubmitItem(Long submitId, ErpSalePickDeliveryItemDO item, BigDecimal quantity) {
        ErpSalePickDeliverySubmitItemDO row = new ErpSalePickDeliverySubmitItemDO().setSubmitId(submitId)
                .setItemId(item.getId()).setProductCode(item.getProductCode()).setProductName(item.getProductName())
                .setQuantity(quantity);
        row.setTenantId(TenantContextHolder.getRequiredTenantId());
        submitItemMapper.insert(row);
    }

    @Override
    public PageResult<ErpSalePickDeliverySubmitItemRespVO> getSubmitItemPage(Long parentId, Long submitId,
                                                                           boolean pick, PageParam pageParam) {
        // 与既有分页接口使用相同父单检查，避免构建详情时加载整单明细。
        if (pick) {
            if (pickTaskMapper.selectById(parentId) == null) {
                throw exception(SALE_PICK_TASK_NOT_EXISTS);
            }
        } else if (orderMapper.selectById(parentId) == null) {
            throw exception(SALE_PICK_DELIVERY_ORDER_NOT_EXISTS);
        }
        ErpSalePickDeliverySubmitDO submit = submitMapper.selectById(submitId);
        if (submit == null || !Objects.equals(submit.getType(), pick ? 10 : 20)
                || !Objects.equals(pick ? submit.getPickTaskId() : submit.getOrderId(), parentId)) {
            throw exception(SALE_PICK_DELIVERY_PAYLOAD_INVALID);
        }
        PageResult<ErpSalePickDeliverySubmitItemDO> page = submitItemMapper.selectPageBySubmitId(pageParam, submitId);
        return new PageResult<>(BeanUtils.toBean(page.getList(), ErpSalePickDeliverySubmitItemRespVO.class), page.getTotal());
    }

    private Long insertSubmit(Long orderId, Long pickTaskId, Long saleOutId, Integer type, Long userId,
                              LocalDateTime now, Integer itemCount, String remark,
                              List<ErpSalePickSubmitReqVO.File> files, String requestId, String requestHash) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        ErpSalePickDeliverySubmitDO submit = new ErpSalePickDeliverySubmitDO()
                .setOrderId(orderId).setPickTaskId(pickTaskId).setSaleOutId(saleOutId).setType(type)
                .setSubmitUserId(userId).setSubmitTime(now).setItemCount(itemCount).setRemark(remark)
                .setRequestId(requestId).setRequestHash(requestHash).setQuantityDetails(true);
        submit.setTenantId(tenantId);
        submitMapper.insert(submit);
        for (int i = 0; i < files.size(); i++) {
            ErpSalePickSubmitReqVO.File file = files.get(i);
            ErpSalePickDeliverySubmitFileDO submitFile = new ErpSalePickDeliverySubmitFileDO()
                    .setSubmitId(submit.getId()).setFileUrl(file.getFileUrl())
                    .setFileName(file.getFileName()).setFileType(file.getFileType()).setSort(i + 1);
            submitFile.setTenantId(tenantId);
            submitFileMapper.insert(submitFile);
        }
        return submit.getId();
    }

    private void refreshPickProgress(ErpSalePickDeliveryPickTaskDO task, ErpSalePickDeliveryOrderDO order, LocalDateTime now) {
        int pickedTaskCount = itemMapper.selectCountByPickTaskIdAndPickStatus(
                task.getId(), ErpSalePickStatusEnum.DONE.getStatus()).intValue();
        Integer taskStatus = buildStatus(pickedTaskCount, task.getTotalItemCount(),
                ErpSalePickStatusEnum.WAITING.getStatus(), ErpSalePickStatusEnum.PARTIAL.getStatus(),
                ErpSalePickStatusEnum.DONE.getStatus());
        if (taskStatus == 10 && itemMapper.hasPickProgress(task.getId(), null)) { taskStatus = 20; }
        pickTaskMapper.updateById(new ErpSalePickDeliveryPickTaskDO().setId(task.getId())
                .setPickedItemCount(pickedTaskCount).setStatus(taskStatus).setLatestPickTime(now)
                .setCompleteTime(ErpSalePickStatusEnum.DONE.getStatus().equals(taskStatus) ? now : null));

        int pickedOrderCount = itemMapper.selectCountByOrderIdAndPickStatus(
                order.getId(), ErpSalePickStatusEnum.DONE.getStatus()).intValue();
        Integer orderPickStatus = buildStatus(pickedOrderCount, order.getTotalItemCount(),
                ErpSalePickStatusEnum.WAITING.getStatus(), ErpSalePickStatusEnum.PARTIAL.getStatus(),
                ErpSalePickStatusEnum.DONE.getStatus());
        if (orderPickStatus == 10 && itemMapper.hasPickProgress(null, order.getId())) { orderPickStatus = 20; }
        ErpSalePickDeliveryOrderDO update = new ErpSalePickDeliveryOrderDO().setId(order.getId())
                .setPickedItemCount(pickedOrderCount).setPickStatus(orderPickStatus).setLatestPickTime(now);
        if (ErpSalePickStatusEnum.DONE.getStatus().equals(orderPickStatus)) {
            update.setPickCompleteTime(now).setDeliveryStatus(ErpSaleDeliveryStatusEnum.WAITING.getStatus());
        }
        orderMapper.updateById(update);
    }

    private Integer refreshDeliveryProgress(ErpSalePickDeliveryOrderDO order, LocalDateTime now) {
        int deliveredCount = itemMapper.selectCountByOrderIdAndDeliveryStatus(
                order.getId(), ErpSaleDeliveryStatusEnum.DONE.getStatus()).intValue();
        Integer deliveryStatus = buildStatus(deliveredCount, order.getTotalItemCount(),
                ErpSaleDeliveryStatusEnum.WAITING.getStatus(), ErpSaleDeliveryStatusEnum.PARTIAL.getStatus(),
                ErpSaleDeliveryStatusEnum.DONE.getStatus());
        if (deliveryStatus == 10 && itemMapper.hasDeliveryProgress(order.getId())) { deliveryStatus = 20; }
        ErpSalePickDeliveryOrderDO update = new ErpSalePickDeliveryOrderDO().setId(order.getId())
                .setDeliveredItemCount(deliveredCount).setDeliveryStatus(deliveryStatus).setLatestDeliveryTime(now);
        if (ErpSaleDeliveryStatusEnum.DONE.getStatus().equals(deliveryStatus)) {
            update.setDeliveryCompleteTime(now);
        }
        orderMapper.updateById(update);
        return deliveryStatus;
    }

    private void approveLinkedCartTransferOutsAfterDelivery(ErpSalePickDeliveryOrderDO order, Long userId) {
        if (!ErpSaleBizSourceTypeEnum.CART.getType().equals(order.getSourceType())) {
            return;
        }
        List<ErpSalePickDeliveryItemDO> deliveredItems = itemMapper.selectListByOrderId(order.getId());
        Set<Long> transferOutIds = deliveredItems.stream()
                .map(ErpSalePickDeliveryItemDO::getTransferOutId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (transferOutIds.isEmpty()) {
            eventPublisher.publishEvent(new ErpSaleCartDeliveryCompletedEvent(order.getSourceId(), userId));
            return;
        }
        for (Long transferOutId : transferOutIds) {
            stockMoveService.approveSaleCartTransferOutAfterDelivery(transferOutId, userId);
        }
    }

    private boolean isCrossDeptWarehouse(Long saleDeptId, ErpWarehouseDO warehouse) {
        return saleDeptId != null && warehouse != null && warehouse.getDeptId() != null
                && !Objects.equals(saleDeptId, warehouse.getDeptId());
    }

    private String buildProductWarehouseKey(Long productId, Long warehouseId) {
        return productId + "-" + warehouseId;
    }

    private boolean countMapEquals(Map<String, BigDecimal> expected, Map<String, BigDecimal> actual) {
        return expected.keySet().equals(actual.keySet()) && expected.entrySet().stream().allMatch(entry ->
                entry.getValue() != null && actual.get(entry.getKey()) != null
                        && entry.getValue().compareTo(actual.get(entry.getKey())) == 0);
    }

    private static final class SaleCartFulfillmentItem {
        private Long transferOutId;
        private Long transferOutItemId;
        private Long warehouseId;
        private Long productId;
        private BigDecimal count;
        private String warehousePosition;
        private Integer packageQty;
        private String standard;

        private static SaleCartFulfillmentItem fromCart(ErpSaleCartItemDO item) {
            SaleCartFulfillmentItem result = new SaleCartFulfillmentItem();
            result.warehouseId = item.getWarehouseId();
            result.productId = item.getProductId();
            result.count = item.getCount();
            result.warehousePosition = item.getWarehousePosition();
            result.packageQty = item.getPackageQty();
            result.standard = item.getStandard();
            return result;
        }

        private static SaleCartFulfillmentItem fromTransfer(ErpStockMoveItemDO item) {
            SaleCartFulfillmentItem result = new SaleCartFulfillmentItem();
            result.transferOutId = item.getMoveId();
            result.transferOutItemId = item.getId();
            result.warehouseId = item.getFromWarehouseId();
            result.productId = item.getProductId();
            result.count = item.getCount();
            result.warehousePosition = item.getFromShelf();
            result.packageQty = item.getPackageQty();
            return result;
        }
    }

    private boolean bindSourceOrderToSaleOutIfPresent(ErpSaleOutDO saleOut) {
        if (!ErpSaleBizSourceTypeEnum.CART.getType().equals(saleOut.getSourceType())
                || saleOut.getSourceId() == null) {
            return false;
        }
        ErpSalePickDeliveryOrderDO order = orderMapper.selectBySourceForUpdate(
                saleOut.getSourceType(), saleOut.getSourceId());
        if (order == null) {
            return false;
        }
        orderMapper.updateById(new ErpSalePickDeliveryOrderDO()
                .setId(order.getId()).setSaleOutId(saleOut.getId()).setSaleOutNo(saleOut.getNo()));
        List<ErpSalePickDeliveryPickTaskDO> tasks = pickTaskMapper.selectListByOrderId(order.getId());
        tasks.forEach(task -> pickTaskMapper.updateById(new ErpSalePickDeliveryPickTaskDO()
                .setId(task.getId()).setSaleOutId(saleOut.getId()).setSaleOutNo(saleOut.getNo())));
        itemMapper.selectListByOrderId(order.getId()).forEach(item -> itemMapper.updateById(
                new ErpSalePickDeliveryItemDO().setId(item.getId()).setSaleOutId(saleOut.getId())));
        submitMapper.selectListByOrderId(order.getId()).forEach(submit -> submitMapper.updateById(
                new ErpSalePickDeliverySubmitDO().setId(submit.getId()).setSaleOutId(saleOut.getId())));
        return true;
    }

    private Integer buildStatus(Integer doneCount, Integer totalCount, Integer waiting, Integer partial, Integer done) {
        if (doneCount == null || doneCount <= 0) {
            return waiting;
        }
        return doneCount >= totalCount ? done : partial;
    }

    private ErpSalePickTaskRespVO buildPickResp(ErpSalePickDeliveryPickTaskDO task) {
        ErpSalePickTaskRespVO resp = BeanUtils.toBean(task, ErpSalePickTaskRespVO.class);
        resp.setDisplayNo(buildDisplayNo(task.getSaleOutNo(), task.getSourceNo()));
        resp.setPickProgress(buildProgress(task.getPickedItemCount(), task.getTotalItemCount()));
        return resp;
    }

    private ErpSalePickTaskRespVO buildPickResp(ErpSalePickDeliveryPickTaskDO task, boolean includeDetail) {
        ErpSalePickTaskRespVO resp = BeanUtils.toBean(task, ErpSalePickTaskRespVO.class);
        resp.setDisplayNo(buildDisplayNo(task.getSaleOutNo(), task.getSourceNo()));
        resp.setPickProgress(buildProgress(task.getPickedItemCount(), task.getTotalItemCount()));
        List<Long> warehouses = warehouseService.getUserPickWarehouseIds(getLoginUserId());
        resp.setWarehouseOperable(warehouses != null && warehouses.contains(task.getWarehouseId()));
        resp.setTotalPieceCount(calculateTotalPieceCount(itemMapper.selectListByPickTaskId(task.getId())));
        if (includeDetail) {
            resp.setItems(buildItemRespList(itemMapper.selectListByPickTaskId(task.getId())));
            resp.setSubmits(buildSubmitRespList(submitMapper.selectListByPickTaskIdAndType(
                    task.getId(), ErpSalePickDeliverySubmitTypeEnum.PICK.getType())));
        }
        return resp;
    }

    private ErpSaleDeliveryOrderRespVO buildDeliveryResp(ErpSalePickDeliveryOrderDO order) {
        ErpSaleDeliveryOrderRespVO resp = BeanUtils.toBean(order, ErpSaleDeliveryOrderRespVO.class);
        resp.setDisplayNo(buildDisplayNo(order.getSaleOutNo(), order.getSourceNo()));
        resp.setPickProgress(buildProgress(order.getPickedItemCount(), order.getTotalItemCount()));
        resp.setDeliveryProgress(buildProgress(order.getDeliveredItemCount(), order.getTotalItemCount()));
        return resp;
    }

    private ErpSaleDeliveryOrderRespVO buildDeliveryResp(ErpSalePickDeliveryOrderDO order, boolean includeDetail) {
        ErpSaleDeliveryOrderRespVO resp = BeanUtils.toBean(order, ErpSaleDeliveryOrderRespVO.class);
        resp.setDisplayNo(buildDisplayNo(order.getSaleOutNo(), order.getSourceNo()));
        resp.setPickProgress(buildProgress(order.getPickedItemCount(), order.getTotalItemCount()));
        resp.setDeliveryProgress(buildProgress(order.getDeliveredItemCount(), order.getTotalItemCount()));
        resp.setTotalPieceCount(calculateTotalPieceCount(itemMapper.selectListByOrderId(order.getId())));
        if (includeDetail) {
            resp.setItems(buildItemRespList(itemMapper.selectListByOrderId(order.getId())));
            resp.setSubmits(buildSubmitRespList(submitMapper.selectListByOrderIdAndType(
                    order.getId(), ErpSalePickDeliverySubmitTypeEnum.DELIVERY.getType())));
        }
        return resp;
    }

    private ErpSalePickDeliverySummaryRespVO buildSummaryResp(ErpSalePickDeliveryOrderDO order) {
        ErpSalePickDeliverySummaryRespVO resp = BeanUtils.toBean(order, ErpSalePickDeliverySummaryRespVO.class);
        resp.setOrderId(order.getId());
        resp.setPickProgress(buildProgress(order.getPickedItemCount(), order.getTotalItemCount()));
        resp.setDeliveryProgress(buildProgress(order.getDeliveredItemCount(), order.getTotalItemCount()));
        return resp;
    }

    private List<ErpSalePickDeliveryItemRespVO> buildItemRespList(List<ErpSalePickDeliveryItemDO> items) {
        List<ErpSalePickDeliveryItemRespVO> respList = BeanUtils.toBean(items, ErpSalePickDeliveryItemRespVO.class);
        Set<Long> userIds = new HashSet<>();
        respList.forEach(item -> {
            if (item.getPickUserId() != null) {
                userIds.add(item.getPickUserId());
            }
            if (item.getDeliveryUserId() != null) {
                userIds.add(item.getDeliveryUserId());
            }
        });
        Map<Long, AdminUserRespDTO> userMap = CollUtil.isEmpty(userIds) ? Collections.emptyMap() : adminUserApi.getUserMap(userIds);
        respList.forEach(item -> {
            AdminUserRespDTO pickUser = userMap.get(item.getPickUserId());
            item.setPickUserName(pickUser == null ? null : pickUser.getNickname());
            AdminUserRespDTO deliveryUser = userMap.get(item.getDeliveryUserId());
            item.setDeliveryUserName(deliveryUser == null ? null : deliveryUser.getNickname());
            item.setPieceCount(calculatePieceCount(item.getCount(), item.getPackageQty()));
            item.setPickedCount(completedCount(item.getPickedCount(), item.getPickStatus(), item.getCount()));
            item.setDeliveredCount(completedCount(item.getDeliveredCount(), item.getDeliveryStatus(), item.getCount()));
            item.setRemainingPickCount(item.getCount().subtract(item.getPickedCount()).max(BigDecimal.ZERO));
            item.setRemainingDeliveryCount(item.getCount().subtract(item.getDeliveredCount()).max(BigDecimal.ZERO));
        });
        return respList;
    }

    private BigDecimal calculateTotalPieceCount(List<ErpSalePickDeliveryItemDO> items) {
        if (CollUtil.isEmpty(items)) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (ErpSalePickDeliveryItemDO item : items) {
            BigDecimal pieceCount = calculatePieceCount(item.getCount(), item.getPackageQty());
            if (pieceCount != null) {
                total = total.add(pieceCount);
            }
        }
        return total.stripTrailingZeros();
    }

    private BigDecimal calculatePieceCount(BigDecimal count, Integer packageQty) {
        if (count == null || packageQty == null || packageQty <= 0) {
            return null;
        }
        return count.divide(BigDecimal.valueOf(packageQty), 3, RoundingMode.HALF_UP).stripTrailingZeros();
    }

    private List<ErpSalePickDeliverySubmitRespVO> buildSubmitRespList(List<ErpSalePickDeliverySubmitDO> submits) {
        List<ErpSalePickDeliverySubmitRespVO> respList = BeanUtils.toBean(submits, ErpSalePickDeliverySubmitRespVO.class);
        if (CollUtil.isEmpty(respList)) {
            return respList;
        }
        Map<Long, List<ErpSalePickDeliverySubmitFileDO>> fileMap = convertMultiMap(
                submitFileMapper.selectListBySubmitIds(convertSet(respList, ErpSalePickDeliverySubmitRespVO::getId)),
                ErpSalePickDeliverySubmitFileDO::getSubmitId);
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(convertSet(respList, ErpSalePickDeliverySubmitRespVO::getSubmitUserId));
        respList.forEach(submit -> {
            AdminUserRespDTO user = userMap.get(submit.getSubmitUserId());
            submit.setSubmitUserName(user == null ? null : user.getNickname());
            submit.setFiles(BeanUtils.toBean(fileMap.getOrDefault(submit.getId(), Collections.emptyList()),
                    ErpSalePickDeliveryFileRespVO.class));
        });
        return respList;
    }

    private ErpSaleOutPickDeliveryFileRespVO buildSaleOutFileResp(ErpSalePickDeliverySubmitRespVO submit,
                                                                  ErpSalePickDeliveryFileRespVO file) {
        ErpSaleOutPickDeliveryFileRespVO resp = BeanUtils.toBean(file, ErpSaleOutPickDeliveryFileRespVO.class);
        resp.setType(submit.getType());
        resp.setTypeName(getSubmitTypeName(submit.getType()));
        resp.setSubmitUserId(submit.getSubmitUserId());
        resp.setSubmitUserName(submit.getSubmitUserName());
        resp.setSubmitTime(submit.getSubmitTime());
        resp.setItemCount(submit.getItemCount());
        resp.setRemark(submit.getRemark());
        return resp;
    }

    private String getSubmitTypeName(Integer type) {
        if (ErpSalePickDeliverySubmitTypeEnum.PICK.getType().equals(type)) {
            return ErpSalePickDeliverySubmitTypeEnum.PICK.getName();
        }
        if (ErpSalePickDeliverySubmitTypeEnum.DELIVERY.getType().equals(type)) {
            return ErpSalePickDeliverySubmitTypeEnum.DELIVERY.getName();
        }
        return null;
    }

    private String buildProgress(Integer doneCount, Integer totalCount) {
        int done = doneCount == null ? 0 : doneCount;
        int total = totalCount == null ? 0 : totalCount;
        return done + "/" + total;
    }

    private String buildDisplayNo(String saleOutNo, String sourceNo) {
        return saleOutNo != null ? saleOutNo : sourceNo;
    }

    private String validateVoucherFile(byte[] content, String fileName) {
        if (content == null || content.length == 0) {
            throw exception(SALE_PICK_DELIVERY_VOUCHER_FILE_EMPTY);
        }
        if (content.length > VOUCHER_FILE_MAX_SIZE) {
            throw exception(SALE_PICK_DELIVERY_VOUCHER_FILE_SIZE_EXCEEDED);
        }
        String extension = FileUtil.extName(fileName);
        extension = extension != null ? extension.toLowerCase(Locale.ROOT) : "";
        if (!VOUCHER_FILE_EXTENSIONS.contains(extension) || !isVoucherFileSignatureValid(content, extension)) {
            throw exception(SALE_PICK_DELIVERY_VOUCHER_FILE_TYPE_INVALID);
        }
        return extension;
    }

    private boolean isVoucherFileSignatureValid(byte[] content, String extension) {
        if ("png".equals(extension)) {
            if (content.length < PNG_SIGNATURE.length) {
                return false;
            }
            for (int i = 0; i < PNG_SIGNATURE.length; i++) {
                if (content[i] != PNG_SIGNATURE[i]) {
                    return false;
                }
            }
            return true;
        }
        return content.length >= 3
                && (content[0] & 0xFF) == 0xFF
                && (content[1] & 0xFF) == 0xD8
                && (content[2] & 0xFF) == 0xFF;
    }

}
