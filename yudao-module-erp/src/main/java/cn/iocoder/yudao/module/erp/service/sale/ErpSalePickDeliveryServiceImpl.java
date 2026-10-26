package cn.iocoder.yudao.module.erp.service.sale;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.io.FileUtil;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutPickDeliveryDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutExpressFileRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutPickDeliveryFileRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutPickDeliveryOperationItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery.*;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleCartItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.pickdelivery.*;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseMoveDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseMoveItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleCartMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleCartItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleReturnItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleReturnMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.pickdelivery.*;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockInItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockInMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockOutItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockOutMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseMoveItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseMoveMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleBizSourceTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleDeliveryStatusEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSalePickDeliverySubmitTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSalePickStatusEnum;
import cn.iocoder.yudao.module.erp.enums.stock.ErpWarehouseMoveStatusEnum;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockInStatusEnum;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockOutStatusEnum;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import cn.iocoder.yudao.module.erp.service.sale.notification.ErpSalePickTaskCreatedEvent;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockMoveService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockInPickCompletedEvent;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockOutPickCompletedEvent;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseMovePickCompletedEvent;
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
    private ErpSaleReturnMapper saleReturnMapper;
    @Resource
    private ErpSaleReturnItemMapper saleReturnItemMapper;
    @Resource
    private ErpWarehouseMoveMapper warehouseMoveMapper;
    @Resource
    private ErpWarehouseMoveItemMapper warehouseMoveItemMapper;
    @Resource
    private ErpStockInMapper stockInMapper;
    @Resource
    private ErpStockInItemMapper stockInItemMapper;
    @Resource
    private ErpStockOutMapper stockOutMapper;
    @Resource
    private ErpStockOutItemMapper stockOutItemMapper;
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
    private ErpSupplierService supplierService;
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
            insertPickTask(task, tenantId);
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
            insertPickTask(task, tenantId);
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
    @Transactional(rollbackFor = Exception.class)
    public void generateForSaleReturn(Long saleReturnId) {
        if (saleReturnId == null || orderMapper.selectBySource(
                ErpSaleBizSourceTypeEnum.SALE_RETURN.getType(), saleReturnId) != null) {
            return;
        }
        ErpSaleReturnDO saleReturn = saleReturnMapper.selectByIdForUpdate(saleReturnId);
        if (saleReturn == null || !cn.iocoder.yudao.module.erp.enums.sale.ErpSaleReturnStatusEnum.PICKING
                .getStatus().equals(saleReturn.getStatus())) {
            throw exception(SALE_RETURN_CONFIRM_FAIL);
        }
        List<ErpSaleReturnItemDO> returnItems = saleReturnItemMapper.selectListByReturnIdForUpdate(saleReturnId);
        if (CollUtil.isEmpty(returnItems) || returnItems.stream().anyMatch(item -> item.getWarehouseId() == null)) {
            throw exception(SALE_PICK_DELIVERY_WAREHOUSE_REQUIRED);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        ErpCustomerDO customer = saleReturn.getCustomerId() == null ? null
                : customerService.getCustomer(saleReturn.getCustomerId());
        String customerName = customer == null ? null : customer.getName();
        Map<Long, ErpWarehouseDO> warehouses = warehouseService.getWarehouseMap(
                convertSet(returnItems, ErpSaleReturnItemDO::getWarehouseId));
        Map<Long, ErpProductRespVO> products = productService.getProductVOMap(
                convertSet(returnItems, ErpSaleReturnItemDO::getProductId));

        ErpSalePickDeliveryOrderDO order = new ErpSalePickDeliveryOrderDO()
                .setSourceType(ErpSaleBizSourceTypeEnum.SALE_RETURN.getType())
                .setSourceId(saleReturn.getId()).setSourceNo(saleReturn.getNo())
                .setCustomerId(saleReturn.getCustomerId()).setCustomerName(customerName)
                .setDeptId(saleReturn.getDeptId())
                .setPickStatus(ErpSalePickStatusEnum.WAITING.getStatus())
                .setDeliveryStatus(ErpSaleDeliveryStatusEnum.NOT_READY.getStatus())
                .setTotalItemCount(returnItems.size()).setPickedItemCount(0).setDeliveredItemCount(0);
        order.setTenantId(tenantId);
        orderMapper.insert(order);

        Map<Long, List<ErpSaleReturnItemDO>> grouped = returnItems.stream().collect(Collectors.groupingBy(
                ErpSaleReturnItemDO::getWarehouseId, LinkedHashMap::new, Collectors.toList()));
        for (Map.Entry<Long, List<ErpSaleReturnItemDO>> entry : grouped.entrySet()) {
            Long warehouseId = entry.getKey();
            ErpWarehouseDO warehouse = warehouses.get(warehouseId);
            ErpSalePickDeliveryPickTaskDO task = new ErpSalePickDeliveryPickTaskDO()
                    .setOrderId(order.getId()).setSourceType(order.getSourceType())
                    .setSourceId(order.getSourceId()).setSourceNo(order.getSourceNo())
                    .setCustomerId(saleReturn.getCustomerId()).setCustomerName(customerName)
                    .setWarehouseId(warehouseId).setWarehouseName(warehouse == null ? null : warehouse.getName())
                    .setStatus(ErpSalePickStatusEnum.WAITING.getStatus())
                    .setTotalItemCount(entry.getValue().size()).setPickedItemCount(0);
            task.setTenantId(tenantId);
            insertPickTask(task, tenantId);
            for (ErpSaleReturnItemDO returnItem : entry.getValue()) {
                ErpProductRespVO product = products.get(returnItem.getProductId());
                ErpSalePickDeliveryItemDO item = new ErpSalePickDeliveryItemDO()
                        .setOrderId(order.getId()).setPickTaskId(task.getId())
                        .setSaleReturnItemId(returnItem.getId())
                        .setWarehouseId(warehouseId).setWarehouseName(task.getWarehouseName())
                        .setProductId(returnItem.getProductId())
                        .setProductCode(product == null ? null : product.getCode())
                        .setProductName(product == null ? null : product.getName())
                        .setStandard(product == null ? null : product.getStandard())
                        .setCount(returnItem.getCount()).setPickedCount(BigDecimal.ZERO)
                        .setDeliveredCount(BigDecimal.ZERO).setWarehousePosition(returnItem.getWarehousePosition())
                        .setPackageQty(returnItem.getPackageQty())
                        .setPickStatus(ErpSalePickStatusEnum.WAITING.getStatus())
                        .setDeliveryStatus(ErpSaleDeliveryStatusEnum.NOT_READY.getStatus());
                item.setTenantId(tenantId);
                itemMapper.insert(item);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateForWarehouseMove(Long warehouseMoveId) {
        generateWarehouseMovePick(warehouseMoveId, false, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateCompletedForWarehouseMove(Long warehouseMoveId, Long pickUserId) {
        generateWarehouseMovePick(warehouseMoveId, true,
                pickUserId != null ? pickUserId : getLoginUserId());
    }

    private void generateWarehouseMovePick(Long warehouseMoveId, boolean completed, Long pickUserId) {
        if (warehouseMoveId == null || orderMapper.selectBySource(
                ErpSaleBizSourceTypeEnum.WAREHOUSE_MOVE.getType(), warehouseMoveId) != null) {
            return;
        }
        ErpWarehouseMoveDO warehouseMove = warehouseMoveMapper.selectByIdForUpdate(warehouseMoveId);
        Integer expectedStatus = completed ? ErpWarehouseMoveStatusEnum.APPROVE.getStatus()
                : ErpWarehouseMoveStatusEnum.PICKING.getStatus();
        if (warehouseMove == null || !expectedStatus.equals(warehouseMove.getStatus())) {
            throw exception(WAREHOUSE_MOVE_CONFIRM_FAIL);
        }
        List<ErpWarehouseMoveItemDO> moveItems = warehouseMoveItemMapper
                .selectListByMoveIdForUpdate(warehouseMoveId);
        if (CollUtil.isEmpty(moveItems) || warehouseMove.getToWarehouseId() == null
                || moveItems.stream().anyMatch(item -> item.getToWarehouseId() == null)) {
            throw exception(SALE_PICK_DELIVERY_WAREHOUSE_REQUIRED);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        ErpWarehouseDO warehouse = warehouseService.getWarehouse(warehouseMove.getToWarehouseId());
        Map<Long, ErpProductRespVO> products = productService.getProductVOMap(
                convertSet(moveItems, ErpWarehouseMoveItemDO::getProductId));
        LocalDateTime completedTime = completed ? LocalDateTime.now() : null;
        Integer pickStatus = completed ? ErpSalePickStatusEnum.DONE.getStatus()
                : ErpSalePickStatusEnum.WAITING.getStatus();
        int pickedItemCount = completed ? moveItems.size() : 0;

        ErpSalePickDeliveryOrderDO order = new ErpSalePickDeliveryOrderDO()
                .setSourceType(ErpSaleBizSourceTypeEnum.WAREHOUSE_MOVE.getType())
                .setSourceId(warehouseMove.getId()).setSourceNo(warehouseMove.getNo())
                .setDeptId(warehouseMove.getDeptId())
                .setPickStatus(pickStatus)
                .setDeliveryStatus(ErpSaleDeliveryStatusEnum.NOT_READY.getStatus())
                .setTotalItemCount(moveItems.size()).setPickedItemCount(pickedItemCount).setDeliveredItemCount(0)
                .setLatestPickTime(completedTime).setPickCompleteTime(completedTime);
        order.setTenantId(tenantId);
        orderMapper.insert(order);

        ErpSalePickDeliveryPickTaskDO task = new ErpSalePickDeliveryPickTaskDO()
                .setOrderId(order.getId()).setSourceType(order.getSourceType())
                .setSourceId(order.getSourceId()).setSourceNo(order.getSourceNo())
                .setWarehouseId(warehouseMove.getToWarehouseId())
                .setWarehouseName(warehouse == null ? null : warehouse.getName())
                .setStatus(pickStatus)
                .setTotalItemCount(moveItems.size()).setPickedItemCount(pickedItemCount)
                .setLatestPickTime(completedTime).setCompleteTime(completedTime);
        task.setTenantId(tenantId);
        if (completed) {
            // 系统自动完成的移货无需发送“待拣货”通知，但必须保留完整任务与提交记录。
            pickTaskMapper.insert(task);
        } else {
            insertPickTask(task, tenantId);
        }
        List<ErpSalePickDeliveryItemDO> pickItems = new ArrayList<>(moveItems.size());
        for (ErpWarehouseMoveItemDO moveItem : moveItems) {
            ErpProductRespVO product = products.get(moveItem.getProductId());
            ErpSalePickDeliveryItemDO item = new ErpSalePickDeliveryItemDO()
                    .setOrderId(order.getId()).setPickTaskId(task.getId())
                    .setWarehouseMoveItemId(moveItem.getId())
                    .setWarehouseId(warehouseMove.getToWarehouseId()).setWarehouseName(task.getWarehouseName())
                    .setProductId(moveItem.getProductId())
                    .setProductCode(product == null ? null : product.getCode())
                    .setProductName(product == null ? null : product.getName())
                    .setStandard(product == null ? null : product.getStandard())
                    .setCount(moveItem.getCount())
                    .setPickedCount(completed ? moveItem.getCount() : BigDecimal.ZERO)
                    .setDeliveredCount(BigDecimal.ZERO).setWarehousePosition(moveItem.getToShelf())
                    .setPackageQty(moveItem.getPackageQty())
                    .setPickStatus(pickStatus).setPickUserId(completed ? pickUserId : null)
                    .setPickTime(completedTime)
                    .setDeliveryStatus(ErpSaleDeliveryStatusEnum.NOT_READY.getStatus());
            item.setTenantId(tenantId);
            itemMapper.insert(item);
            pickItems.add(item);
        }
        if (completed) {
            Long submitId = insertSubmit(order.getId(), task.getId(), null,
                    ErpSalePickDeliverySubmitTypeEnum.PICK.getType(), pickUserId, completedTime,
                    pickItems.size(), "套件组装自动移货同步完成", Collections.emptyList(), null, null);
            for (ErpSalePickDeliveryItemDO item : pickItems) {
                insertSubmitItem(submitId, item, item.getCount());
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateForStockIn(Long stockInId) {
        if (stockInId == null || orderMapper.selectBySource(
                ErpSaleBizSourceTypeEnum.STOCK_IN.getType(), stockInId) != null) {
            return;
        }
        ErpStockInDO stockIn = stockInMapper.selectByIdForUpdate(stockInId);
        if (stockIn == null || !ErpStockInStatusEnum.PICKING.getStatus().equals(stockIn.getStatus())) {
            throw exception(STOCK_IN_CONFIRM_FAIL);
        }
        List<ErpStockInItemDO> stockInItems = stockInItemMapper.selectListByInIdForUpdate(stockInId);
        if (CollUtil.isEmpty(stockInItems)
                || stockInItems.stream().anyMatch(item -> item.getWarehouseId() == null)) {
            throw exception(SALE_PICK_DELIVERY_WAREHOUSE_REQUIRED);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        ErpSupplierDO supplier = stockIn.getSupplierId() == null ? null
                : supplierService.getSupplier(stockIn.getSupplierId());
        String supplierName = supplier == null ? null : supplier.getName();
        Map<Long, ErpWarehouseDO> warehouses = warehouseService.getWarehouseMap(
                convertSet(stockInItems, ErpStockInItemDO::getWarehouseId));
        Map<Long, ErpProductRespVO> products = productService.getProductVOMap(
                convertSet(stockInItems, ErpStockInItemDO::getProductId));

        ErpSalePickDeliveryOrderDO order = new ErpSalePickDeliveryOrderDO()
                .setSourceType(ErpSaleBizSourceTypeEnum.STOCK_IN.getType())
                .setSourceId(stockIn.getId()).setSourceNo(stockIn.getNo())
                .setCustomerName(supplierName).setDeptId(stockIn.getDeptId())
                .setPickStatus(ErpSalePickStatusEnum.WAITING.getStatus())
                .setDeliveryStatus(ErpSaleDeliveryStatusEnum.NOT_READY.getStatus())
                .setTotalItemCount(stockInItems.size()).setPickedItemCount(0).setDeliveredItemCount(0);
        order.setTenantId(tenantId);
        orderMapper.insert(order);

        Map<Long, List<ErpStockInItemDO>> grouped = stockInItems.stream().collect(Collectors.groupingBy(
                ErpStockInItemDO::getWarehouseId, LinkedHashMap::new, Collectors.toList()));
        for (Map.Entry<Long, List<ErpStockInItemDO>> entry : grouped.entrySet()) {
            Long warehouseId = entry.getKey();
            ErpWarehouseDO warehouse = warehouses.get(warehouseId);
            ErpSalePickDeliveryPickTaskDO task = new ErpSalePickDeliveryPickTaskDO()
                    .setOrderId(order.getId()).setSourceType(order.getSourceType())
                    .setSourceId(order.getSourceId()).setSourceNo(order.getSourceNo())
                    .setCustomerName(supplierName)
                    .setWarehouseId(warehouseId).setWarehouseName(warehouse == null ? null : warehouse.getName())
                    .setStatus(ErpSalePickStatusEnum.WAITING.getStatus())
                    .setTotalItemCount(entry.getValue().size()).setPickedItemCount(0);
            task.setTenantId(tenantId);
            insertPickTask(task, tenantId);
            for (ErpStockInItemDO stockInItem : entry.getValue()) {
                ErpProductRespVO product = products.get(stockInItem.getProductId());
                ErpSalePickDeliveryItemDO item = new ErpSalePickDeliveryItemDO()
                        .setOrderId(order.getId()).setPickTaskId(task.getId())
                        .setStockInItemId(stockInItem.getId())
                        .setWarehouseId(warehouseId).setWarehouseName(task.getWarehouseName())
                        .setProductId(stockInItem.getProductId())
                        .setProductCode(product == null ? null : product.getCode())
                        .setProductName(product == null ? null : product.getName())
                        .setStandard(product == null ? null : product.getStandard())
                        .setCount(stockInItem.getCount()).setPickedCount(BigDecimal.ZERO)
                        .setDeliveredCount(BigDecimal.ZERO).setPackageQty(stockInItem.getPackageQty())
                        .setPickStatus(ErpSalePickStatusEnum.WAITING.getStatus())
                        .setDeliveryStatus(ErpSaleDeliveryStatusEnum.NOT_READY.getStatus());
                item.setTenantId(tenantId);
                itemMapper.insert(item);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateForStockOut(Long stockOutId) {
        if (stockOutId == null || orderMapper.selectBySource(
                ErpSaleBizSourceTypeEnum.STOCK_OUT.getType(), stockOutId) != null) {
            return;
        }
        ErpStockOutDO stockOut = stockOutMapper.selectByIdForUpdate(stockOutId);
        if (stockOut == null || !ErpStockOutStatusEnum.PICKING.getStatus().equals(stockOut.getStatus())) {
            throw exception(STOCK_OUT_CONFIRM_FAIL);
        }
        List<ErpStockOutItemDO> stockOutItems = stockOutItemMapper.selectListByOutIdForUpdate(stockOutId);
        if (CollUtil.isEmpty(stockOutItems)
                || stockOutItems.stream().anyMatch(item -> item.getWarehouseId() == null)) {
            throw exception(SALE_PICK_DELIVERY_WAREHOUSE_REQUIRED);
        }
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        ErpCustomerDO customer = stockOut.getCustomerId() == null ? null
                : customerService.getCustomer(stockOut.getCustomerId());
        String customerName = customer == null ? null : customer.getName();
        Map<Long, ErpWarehouseDO> warehouses = warehouseService.getWarehouseMap(
                convertSet(stockOutItems, ErpStockOutItemDO::getWarehouseId));
        Map<Long, ErpProductRespVO> products = productService.getProductVOMap(
                convertSet(stockOutItems, ErpStockOutItemDO::getProductId));

        ErpSalePickDeliveryOrderDO order = new ErpSalePickDeliveryOrderDO()
                .setSourceType(ErpSaleBizSourceTypeEnum.STOCK_OUT.getType())
                .setSourceId(stockOut.getId()).setSourceNo(stockOut.getNo())
                .setCustomerId(stockOut.getCustomerId()).setCustomerName(customerName)
                .setDeptId(stockOut.getDeptId())
                .setPickStatus(ErpSalePickStatusEnum.WAITING.getStatus())
                .setDeliveryStatus(ErpSaleDeliveryStatusEnum.NOT_READY.getStatus())
                .setTotalItemCount(stockOutItems.size()).setPickedItemCount(0).setDeliveredItemCount(0);
        order.setTenantId(tenantId);
        orderMapper.insert(order);

        Map<Long, List<ErpStockOutItemDO>> grouped = stockOutItems.stream().collect(Collectors.groupingBy(
                ErpStockOutItemDO::getWarehouseId, LinkedHashMap::new, Collectors.toList()));
        for (Map.Entry<Long, List<ErpStockOutItemDO>> entry : grouped.entrySet()) {
            Long warehouseId = entry.getKey();
            ErpWarehouseDO warehouse = warehouses.get(warehouseId);
            ErpSalePickDeliveryPickTaskDO task = new ErpSalePickDeliveryPickTaskDO()
                    .setOrderId(order.getId()).setSourceType(order.getSourceType())
                    .setSourceId(order.getSourceId()).setSourceNo(order.getSourceNo())
                    .setCustomerId(stockOut.getCustomerId()).setCustomerName(customerName)
                    .setWarehouseId(warehouseId).setWarehouseName(warehouse == null ? null : warehouse.getName())
                    .setStatus(ErpSalePickStatusEnum.WAITING.getStatus())
                    .setTotalItemCount(entry.getValue().size()).setPickedItemCount(0);
            task.setTenantId(tenantId);
            insertPickTask(task, tenantId);
            for (ErpStockOutItemDO stockOutItem : entry.getValue()) {
                ErpProductRespVO product = products.get(stockOutItem.getProductId());
                ErpSalePickDeliveryItemDO item = new ErpSalePickDeliveryItemDO()
                        .setOrderId(order.getId()).setPickTaskId(task.getId())
                        .setStockOutItemId(stockOutItem.getId())
                        .setWarehouseId(warehouseId).setWarehouseName(task.getWarehouseName())
                        .setProductId(stockOutItem.getProductId())
                        .setProductCode(product == null ? null : product.getCode())
                        .setProductName(product == null ? null : product.getName())
                        .setStandard(product == null ? null : product.getStandard())
                        .setCount(stockOutItem.getCount()).setPickedCount(BigDecimal.ZERO)
                        .setDeliveredCount(BigDecimal.ZERO).setPackageQty(stockOutItem.getPackageQty())
                        .setPickStatus(ErpSalePickStatusEnum.WAITING.getStatus())
                        .setDeliveryStatus(ErpSaleDeliveryStatusEnum.NOT_READY.getStatus());
                item.setTenantId(tenantId);
                itemMapper.insert(item);
            }
        }
    }

    /**
     * 新增拣货任务并发布事务后通知事件。事件监听器仅在事务提交成功后执行，
     * 因此后续任一明细写入失败导致回滚时不会误发通知。
     */
    private void insertPickTask(ErpSalePickDeliveryPickTaskDO task, Long tenantId) {
        pickTaskMapper.insert(task);
        eventPublisher.publishEvent(new ErpSalePickTaskCreatedEvent(tenantId, task.getId()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rollbackUnstartedSaleReturnFulfillment(Long saleReturnId) {
        ErpSalePickDeliveryOrderDO order = orderMapper.selectBySourceForUpdate(
                ErpSaleBizSourceTypeEnum.SALE_RETURN.getType(), saleReturnId);
        if (order == null) {
            return;
        }
        List<ErpSalePickDeliverySubmitDO> submits = submitMapper.selectListByOrderId(order.getId());
        List<ErpSalePickDeliveryPickTaskDO> tasks = pickTaskMapper.selectListByOrderId(order.getId());
        List<ErpSalePickDeliveryItemDO> items = itemMapper.selectListByOrderId(order.getId());
        if (!isUnstartedFulfillment(order, tasks, items, submits)) {
            throw exception(SALE_RETURN_PICK_ALREADY_STARTED);
        }
        itemMapper.deleteByOrderId(order.getId());
        pickTaskMapper.deleteByOrderId(order.getId());
        orderMapper.deleteById(order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rollbackUnstartedWarehouseMoveFulfillment(Long warehouseMoveId) {
        ErpSalePickDeliveryOrderDO order = orderMapper.selectBySourceForUpdate(
                ErpSaleBizSourceTypeEnum.WAREHOUSE_MOVE.getType(), warehouseMoveId);
        if (order == null) {
            return;
        }
        List<ErpSalePickDeliverySubmitDO> submits = submitMapper.selectListByOrderId(order.getId());
        List<ErpSalePickDeliveryPickTaskDO> tasks = pickTaskMapper.selectListByOrderId(order.getId());
        List<ErpSalePickDeliveryItemDO> items = itemMapper.selectListByOrderId(order.getId());
        if (!isUnstartedFulfillment(order, tasks, items, submits)) {
            throw exception(WAREHOUSE_MOVE_PICK_ALREADY_STARTED);
        }
        itemMapper.deleteByOrderId(order.getId());
        pickTaskMapper.deleteByOrderId(order.getId());
        orderMapper.deleteById(order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rollbackUnstartedStockInFulfillment(Long stockInId) {
        rollbackUnstartedPickOnlyFulfillment(ErpSaleBizSourceTypeEnum.STOCK_IN.getType(), stockInId,
                STOCK_IN_PICK_ALREADY_STARTED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rollbackUnstartedStockOutFulfillment(Long stockOutId) {
        rollbackUnstartedPickOnlyFulfillment(ErpSaleBizSourceTypeEnum.STOCK_OUT.getType(), stockOutId,
                STOCK_OUT_PICK_ALREADY_STARTED);
    }

    private void rollbackUnstartedPickOnlyFulfillment(Integer sourceType, Long sourceId,
                                                       cn.iocoder.yudao.framework.common.exception.ErrorCode errorCode) {
        ErpSalePickDeliveryOrderDO order = orderMapper.selectBySourceForUpdate(sourceType, sourceId);
        if (order == null) {
            return;
        }
        List<ErpSalePickDeliverySubmitDO> submits = submitMapper.selectListByOrderId(order.getId());
        List<ErpSalePickDeliveryPickTaskDO> tasks = pickTaskMapper.selectListByOrderId(order.getId());
        List<ErpSalePickDeliveryItemDO> items = itemMapper.selectListByOrderId(order.getId());
        if (!isUnstartedFulfillment(order, tasks, items, submits)) {
            throw exception(errorCode);
        }
        itemMapper.deleteByOrderId(order.getId());
        pickTaskMapper.deleteByOrderId(order.getId());
        orderMapper.deleteById(order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rollbackUnstartedSaleCartFulfillment(Long saleCartId) {
        if (saleCartId == null) {
            return;
        }
        ErpSalePickDeliveryOrderDO order = orderMapper.selectBySourceForUpdate(
                ErpSaleBizSourceTypeEnum.CART.getType(), saleCartId);
        if (order == null) {
            return;
        }
        List<ErpSalePickDeliverySubmitDO> submits = submitMapper.selectListByOrderId(order.getId());
        List<ErpSalePickDeliveryPickTaskDO> tasks = pickTaskMapper.selectListByOrderId(order.getId());
        List<ErpSalePickDeliveryItemDO> items = itemMapper.selectListByOrderId(order.getId());
        if (!isUnstartedFulfillment(order, tasks, items, submits)) {
            throw exception(SALE_CART_FULFILLMENT_TASK_STARTED);
        }
        // 子表先于主表逻辑删除；后续解锁任一步骤失败时由外层事务整体回滚。
        itemMapper.deleteByOrderId(order.getId());
        pickTaskMapper.deleteByOrderId(order.getId());
        orderMapper.deleteById(order.getId());
    }

    private boolean isUnstartedFulfillment(ErpSalePickDeliveryOrderDO order,
                                           List<ErpSalePickDeliveryPickTaskDO> tasks,
                                           List<ErpSalePickDeliveryItemDO> items,
                                           List<ErpSalePickDeliverySubmitDO> submits) {
        if (order.getSaleOutId() != null || StringUtils.hasText(order.getSaleOutNo())
                || !ErpSalePickStatusEnum.WAITING.getStatus().equals(order.getPickStatus())
                || !ErpSaleDeliveryStatusEnum.NOT_READY.getStatus().equals(order.getDeliveryStatus())
                || !isZeroOrNull(order.getPickedItemCount()) || !isZeroOrNull(order.getDeliveredItemCount())
                || order.getLatestPickTime() != null || order.getLatestDeliveryTime() != null
                || order.getPickCompleteTime() != null || order.getDeliveryCompleteTime() != null
                || CollUtil.isNotEmpty(submits)) {
            return false;
        }
        boolean taskStarted = tasks.stream().anyMatch(task -> task.getSaleOutId() != null
                || StringUtils.hasText(task.getSaleOutNo())
                || !ErpSalePickStatusEnum.WAITING.getStatus().equals(task.getStatus())
                || !isZeroOrNull(task.getPickedItemCount())
                || task.getLatestPickTime() != null || task.getCompleteTime() != null);
        if (taskStarted) {
            return false;
        }
        return items.stream().noneMatch(item -> item.getSaleOutId() != null || item.getSaleOutItemId() != null
                || !ErpSalePickStatusEnum.WAITING.getStatus().equals(item.getPickStatus())
                || !ErpSaleDeliveryStatusEnum.NOT_READY.getStatus().equals(item.getDeliveryStatus())
                || !isZeroOrNull(item.getPickedCount()) || !isZeroOrNull(item.getDeliveredCount())
                || item.getPickUserId() != null || item.getPickTime() != null
                || item.getDeliveryUserId() != null || item.getDeliveryTime() != null);
    }

    private boolean isZeroOrNull(Integer value) {
        return value == null || value == 0;
    }

    private boolean isZeroOrNull(BigDecimal value) {
        return value == null || value.compareTo(BigDecimal.ZERO) == 0;
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
            BigDecimal delivered = completedCount(item.getDeliveredCount(), item.getDeliveryStatus(), item.getCount());
            boolean pickOnly = ErpSaleBizSourceTypeEnum.isPickOnly(order.getSourceType());
            itemMapper.updateById(new ErpSalePickDeliveryItemDO().setId(item.getId()).setPickedCount(picked)
                    .setPickStatus(quantityStatus(picked, item.getCount()))
                    .setPickUserId(userId).setPickTime(now)
                    .setDeliveryStatus(pickOnly ? ErpSaleDeliveryStatusEnum.NOT_READY.getStatus()
                            : deliveryQuantityStatus(picked, delivered, item.getCount())));
            insertSubmitItem(submitId, item, quantities.get(item.getId()));
        }
        refreshPickProgress(task, order, now);
    }

    @Override
    public PageResult<ErpSaleDeliveryOrderRespVO> getDeliveryPage(ErpSaleDeliveryPageReqVO reqVO, boolean mobile) {
        PageResult<ErpSalePickDeliveryPickTaskDO> pageResult = pickTaskMapper.selectDeliveryPage(reqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return new PageResult<>(Collections.emptyList(), pageResult.getTotal());
        }
        Map<Long, ErpSalePickDeliveryOrderDO> orderMap = convertMap(
                orderMapper.selectBatchIds(convertSet(pageResult.getList(), ErpSalePickDeliveryPickTaskDO::getOrderId)),
                ErpSalePickDeliveryOrderDO::getId);
        Map<Long, List<ErpSalePickDeliveryItemDO>> itemMap = convertMultiMap(
                itemMapper.selectListByPickTaskIds(convertSet(pageResult.getList(), ErpSalePickDeliveryPickTaskDO::getId)),
                ErpSalePickDeliveryItemDO::getPickTaskId);
        return new PageResult<>(convertList(pageResult.getList(), task -> buildDeliveryTaskResp(task,
                orderMap.get(task.getOrderId()), itemMap.getOrDefault(task.getId(), Collections.emptyList()), false)),
                pageResult.getTotal());
    }

    @Override
    public ErpSaleDeliveryOrderRespVO getDelivery(Long id, boolean mobile, boolean includeDetail) {
        ErpSalePickDeliveryPickTaskDO task = pickTaskMapper.selectById(id);
        if (task == null) {
            throw exception(SALE_PICK_TASK_NOT_EXISTS);
        }
        ErpSalePickDeliveryOrderDO order = orderMapper.selectById(task.getOrderId());
        if (order == null) {
            throw exception(SALE_PICK_DELIVERY_ORDER_NOT_EXISTS);
        }
        validateDeliveryRequired(order);
        ErpSaleDeliveryOrderRespVO resp = buildDeliveryTaskResp(
                task, order, itemMapper.selectListByPickTaskId(task.getId()), includeDetail);
        resp.setLegacyHistoryWarning(submitMapper.selectUnassignedLegacyDeliveryCount(task.getOrderId()) > 0);
        return resp;
    }

    @Override
    public PageResult<ErpSalePickDeliveryItemRespVO> getDeliveryItemPage(Long taskId, PageParam pageParam, boolean mobile) {
        ErpSalePickDeliveryPickTaskDO task = pickTaskMapper.selectById(taskId);
        if (task == null) {
            throw exception(SALE_PICK_TASK_NOT_EXISTS);
        }
        validateDeliveryRequired(orderMapper.selectById(task.getOrderId()));
        PageResult<ErpSalePickDeliveryItemDO> pageResult = itemMapper.selectPageByPickTaskId(pageParam, taskId);
        return new PageResult<>(buildItemRespList(pageResult.getList()), pageResult.getTotal());
    }

    @Override
    public PageResult<ErpSalePickDeliverySubmitRespVO> getDeliverySubmitPage(Long taskId, PageParam pageParam, boolean mobile) {
        ErpSalePickDeliveryPickTaskDO task = pickTaskMapper.selectById(taskId);
        if (task == null) {
            throw exception(SALE_PICK_TASK_NOT_EXISTS);
        }
        validateDeliveryRequired(orderMapper.selectById(task.getOrderId()));
        PageResult<ErpSalePickDeliverySubmitDO> pageResult = submitMapper.selectDeliveryPageByTaskId(
                pageParam, taskId, task.getOrderId());
        return new PageResult<>(buildDeliverySubmitRespList(pageResult.getList(), taskId), pageResult.getTotal());
    }

    @Override
    @Transactional(rollbackFor = Exception.class, isolation = org.springframework.transaction.annotation.Isolation.READ_COMMITTED)
    public void submitDelivery(ErpSaleDeliverySubmitReqVO reqVO) {
        List<Long> itemIds = validateSubmitPayload(reqVO.getItemIds(), reqVO.getItems(), reqVO.getRequestId(), reqVO.getFiles());
        ErpSalePickDeliveryPickTaskDO snapshot = pickTaskMapper.selectById(reqVO.getTaskId());
        if (snapshot == null) {
            throw exception(SALE_PICK_TASK_NOT_EXISTS);
        }
        ErpSalePickDeliveryOrderDO order = orderMapper.selectByIdForUpdate(snapshot.getOrderId());
        if (order == null) {
            throw exception(SALE_PICK_DELIVERY_ORDER_NOT_EXISTS);
        }
        validateDeliveryRequired(order);
        ErpSalePickDeliveryPickTaskDO task = pickTaskMapper.selectByIdForUpdate(reqVO.getTaskId());
        if (task == null || !Objects.equals(task.getOrderId(), order.getId())) {
            throw exception(SALE_PICK_TASK_NOT_EXISTS);
        }
        String hash = requestHash(task.getId(), reqVO.getItems(), reqVO.getRemark(), reqVO.getFiles());
        if (isCompletedRequest(order.getId(), 20, reqVO.getRequestId(), hash)) {
            return;
        }
        if (ErpSaleDeliveryStatusEnum.DONE.getStatus().equals(order.getDeliveryStatus())) {
            throw exception(SALE_DELIVERY_STATUS_INVALID);
        }
        List<ErpSalePickDeliveryItemDO> items = itemMapper.selectListByIdsForUpdate(itemIds);
        if (items.size() != itemIds.size() || items.stream().anyMatch(item ->
                !Objects.equals(item.getOrderId(), order.getId())
                        || !Objects.equals(item.getPickTaskId(), task.getId())
                        || ErpSaleDeliveryStatusEnum.DONE.getStatus().equals(item.getDeliveryStatus()))) {
            throw exception(SALE_DELIVERY_ITEM_INVALID);
        }
        Map<Long, BigDecimal> quantities = resolveQuantities(items, reqVO.getItems(), false);
        LocalDateTime now = LocalDateTime.now();
        Long userId = getLoginUserId();
        Long submitId = insertSubmit(order.getId(), task.getId(), order.getSaleOutId(),
                ErpSalePickDeliverySubmitTypeEnum.DELIVERY.getType(), userId, now, itemIds.size(), reqVO.getRemark(),
                reqVO.getFiles(), reqVO.getRequestId(), hash);
        for (ErpSalePickDeliveryItemDO item : items) {
            BigDecimal picked = completedCount(item.getPickedCount(), item.getPickStatus(), item.getCount());
            BigDecimal delivered = completedCount(item.getDeliveredCount(), item.getDeliveryStatus(), item.getCount())
                    .add(quantities.get(item.getId()));
            itemMapper.updateById(new ErpSalePickDeliveryItemDO().setId(item.getId()).setDeliveredCount(delivered)
                    .setDeliveryStatus(deliveryQuantityStatus(picked, delivered, item.getCount()))
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
    public Set<Long> getSaleOutIdsWithDeliveryFiles(Collection<Long> saleOutIds) {
        if (CollUtil.isEmpty(saleOutIds)) {
            return Collections.emptySet();
        }
        List<ErpSalePickDeliverySubmitDO> submits = submitMapper.selectListBySaleOutIdsAndType(
                saleOutIds, ErpSalePickDeliverySubmitTypeEnum.DELIVERY.getType());
        if (CollUtil.isEmpty(submits)) {
            return Collections.emptySet();
        }
        Set<Long> submitIdsWithFiles = new HashSet<>(submitFileMapper.selectSubmitIdsWithFiles(
                convertSet(submits, ErpSalePickDeliverySubmitDO::getId)));
        if (submitIdsWithFiles.isEmpty()) {
            return Collections.emptySet();
        }
        return submits.stream()
                .filter(submit -> submitIdsWithFiles.contains(submit.getId()))
                .map(ErpSalePickDeliverySubmitDO::getSaleOutId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    @Override
    public PageResult<ErpSaleOutExpressFileRespVO> getSaleOutExpressFilePage(Long saleOutId,
                                                                             PageParam pageParam) {
        ErpSaleOutDO saleOut = saleOutMapper.selectById(saleOutId);
        if (saleOut == null) {
            return PageResult.empty();
        }
        LinkedHashMap<String, ErpSaleOutExpressFileRespVO> deliveryFiles = new LinkedHashMap<>();
        List<ErpSalePickDeliverySubmitDO> deliverySubmits = new ArrayList<>(
                submitMapper.selectListBySaleOutIdAndType(
                        saleOutId, ErpSalePickDeliverySubmitTypeEnum.DELIVERY.getType()));
        deliverySubmits.sort(Comparator
                .comparing(ErpSalePickDeliverySubmitDO::getSubmitTime,
                        Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(ErpSalePickDeliverySubmitDO::getId,
                        Comparator.nullsLast(Comparator.reverseOrder())));
        List<ErpSalePickDeliverySubmitRespVO> submits = buildSubmitRespList(deliverySubmits);
        for (ErpSalePickDeliverySubmitRespVO submit : submits) {
            if (CollUtil.isEmpty(submit.getFiles())) {
                continue;
            }
            for (ErpSalePickDeliveryFileRespVO file : submit.getFiles()) {
                if (!StringUtils.hasText(file.getFileUrl())) {
                    continue;
                }
                String fileUrl = file.getFileUrl().trim();
                deliveryFiles.putIfAbsent(fileUrl, new ErpSaleOutExpressFileRespVO()
                        .setId(file.getId())
                        .setSubmitId(submit.getId())
                        .setFileUrl(fileUrl)
                        .setFileName(file.getFileName())
                        .setFileType(file.getFileType())
                        .setSourceType("DELIVERY")
                        .setSourceName("送货快递单")
                        .setSubmitUserId(submit.getSubmitUserId())
                        .setSubmitUserName(submit.getSubmitUserName())
                        .setSubmitTime(submit.getSubmitTime())
                        .setSort(file.getSort()));
            }
        }
        LinkedHashMap<String, ErpSaleOutExpressFileRespVO> uniqueFiles = new LinkedHashMap<>();
        if (StringUtils.hasText(saleOut.getExpressFileUrl())) {
            String fileUrl = saleOut.getExpressFileUrl().trim();
            // 自动同步的兼容主图仍按送货快递单展示；仅真正独立的历史/人工图片标记为补传。
            if (!deliveryFiles.containsKey(fileUrl)) {
                uniqueFiles.put(fileUrl, new ErpSaleOutExpressFileRespVO()
                        .setFileUrl(fileUrl)
                        .setFileName("销售单补传快递单")
                        .setFileType(FileUtil.extName(fileUrl))
                        .setSourceType("MANUAL")
                        .setSourceName("销售单补传")
                        .setSubmitTime(saleOut.getUpdateTime() != null
                                ? saleOut.getUpdateTime() : saleOut.getCreateTime()));
            }
        }
        uniqueFiles.putAll(deliveryFiles);
        return pageList(new ArrayList<>(uniqueFiles.values()), pageParam);
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
    public Map<Long, ErpSalePickDeliverySummaryRespVO> getSummaryMapBySaleReturnIds(Collection<Long> saleReturnIds) {
        if (CollUtil.isEmpty(saleReturnIds)) {
            return Collections.emptyMap();
        }
        List<ErpSalePickDeliveryOrderDO> orders = orderMapper.selectListBySourceIds(
                ErpSaleBizSourceTypeEnum.SALE_RETURN.getType(), saleReturnIds);
        return orders.stream().collect(Collectors.toMap(ErpSalePickDeliveryOrderDO::getSourceId,
                this::buildSummaryResp, (first, second) -> first));
    }

    @Override
    public Map<Long, ErpSalePickDeliverySummaryRespVO> getSummaryMapByWarehouseMoveIds(
            Collection<Long> warehouseMoveIds) {
        if (CollUtil.isEmpty(warehouseMoveIds)) {
            return Collections.emptyMap();
        }
        List<ErpSalePickDeliveryOrderDO> orders = orderMapper.selectListBySourceIds(
                ErpSaleBizSourceTypeEnum.WAREHOUSE_MOVE.getType(), warehouseMoveIds);
        return orders.stream().collect(Collectors.toMap(ErpSalePickDeliveryOrderDO::getSourceId,
                this::buildSummaryResp, (first, second) -> first));
    }

    @Override
    public Map<Long, ErpSalePickDeliverySummaryRespVO> getSummaryMapByStockInIds(Collection<Long> stockInIds) {
        if (CollUtil.isEmpty(stockInIds)) {
            return Collections.emptyMap();
        }
        List<ErpSalePickDeliveryOrderDO> orders = orderMapper.selectListBySourceIds(
                ErpSaleBizSourceTypeEnum.STOCK_IN.getType(), stockInIds);
        return orders.stream().collect(Collectors.toMap(ErpSalePickDeliveryOrderDO::getSourceId,
                this::buildSummaryResp, (first, second) -> first));
    }

    @Override
    public Map<Long, ErpSalePickDeliverySummaryRespVO> getSummaryMapByStockOutIds(Collection<Long> stockOutIds) {
        if (CollUtil.isEmpty(stockOutIds)) {
            return Collections.emptyMap();
        }
        List<ErpSalePickDeliveryOrderDO> orders = orderMapper.selectListBySourceIds(
                ErpSaleBizSourceTypeEnum.STOCK_OUT.getType(), stockOutIds);
        return orders.stream().collect(Collectors.toMap(ErpSalePickDeliveryOrderDO::getSourceId,
                this::buildSummaryResp, (first, second) -> first));
    }

    @Override
    public ErpSalePickSourceDetailRespVO getSourcePickDetail(Integer sourceType, Long sourceId) {
        ErpSalePickSourceDetailRespVO resp = new ErpSalePickSourceDetailRespVO();
        resp.setPickTasks(Collections.emptyList());
        ErpSalePickDeliveryOrderDO order = orderMapper.selectBySource(sourceType, sourceId);
        if (order == null) {
            return resp;
        }

        List<ErpSalePickQuantityAggregateDO> quantityAggregates =
                itemMapper.selectPickQuantityAggregatesByOrderId(order.getId());
        Map<Long, ErpSalePickQuantityAggregateDO> quantityAggregateMap = quantityAggregates.stream()
                .filter(item -> item.getPickTaskId() != null)
                .collect(Collectors.toMap(ErpSalePickQuantityAggregateDO::getPickTaskId,
                        item -> item, (first, second) -> first));
        BigDecimal totalQuantity = quantityAggregates.stream()
                .map(ErpSalePickQuantityAggregateDO::getTotalQuantity)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pickedQuantity = quantityAggregates.stream()
                .map(ErpSalePickQuantityAggregateDO::getPickedQuantity)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        ErpSalePickDeliverySummaryRespVO summary = buildSummaryResp(order);
        fillPickQuantity(summary, totalQuantity, pickedQuantity);
        resp.setSummary(summary);

        List<ErpSalePickDeliverySubmitDO> pickSubmits = submitMapper.selectListByOrderIdAndType(
                order.getId(), ErpSalePickDeliverySubmitTypeEnum.PICK.getType());
        Set<Long> pickerIds = convertSet(pickSubmits, ErpSalePickDeliverySubmitDO::getSubmitUserId);
        pickerIds.remove(null);
        Map<Long, AdminUserRespDTO> userMap = pickerIds.isEmpty()
                ? Collections.emptyMap() : Optional.ofNullable(adminUserApi.getUserMap(pickerIds))
                .orElse(Collections.emptyMap());
        Map<Long, LocalDateTime> taskStartTimeMap = new HashMap<>();
        Map<Long, LinkedHashSet<String>> taskPickerNameMap = new HashMap<>();
        pickSubmits.stream().filter(submit -> submit.getPickTaskId() != null)
                .sorted(Comparator.comparing(ErpSalePickDeliverySubmitDO::getSubmitTime,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .forEach(submit -> {
                    if (submit.getSubmitTime() != null) {
                        taskStartTimeMap.merge(submit.getPickTaskId(), submit.getSubmitTime(),
                                (first, second) -> first.isBefore(second) ? first : second);
                    }
                    AdminUserRespDTO user = userMap.get(submit.getSubmitUserId());
                    if (user != null && StringUtils.hasText(user.getNickname())) {
                        taskPickerNameMap.computeIfAbsent(submit.getPickTaskId(), key -> new LinkedHashSet<>())
                                .add(user.getNickname().trim());
                    }
                });

        resp.setPickTasks(convertList(pickTaskMapper.selectListByOrderId(order.getId()), task -> {
            ErpSalePickTaskRespVO taskResp = buildPickResp(task);
            ErpSalePickQuantityAggregateDO aggregate = quantityAggregateMap.get(task.getId());
            if (aggregate != null) {
                fillPickQuantity(taskResp, aggregate.getTotalQuantity(), aggregate.getPickedQuantity());
            }
            taskResp.setStartTime(taskStartTimeMap.get(task.getId()));
            taskResp.setPickerNames(new ArrayList<>(taskPickerNameMap.getOrDefault(
                    task.getId(), new LinkedHashSet<>())));
            return taskResp;
        }));
        return resp;
    }

    @Override
    public PageResult<ErpSalePickDeliveryItemRespVO> getSourcePickItemPage(
            Integer sourceType, Long sourceId, PageParam pageParam) {
        ErpSalePickDeliveryOrderDO order = orderMapper.selectBySource(sourceType, sourceId);
        if (order == null) {
            return PageResult.empty();
        }
        PageResult<ErpSalePickDeliveryItemDO> page = itemMapper.selectPageByOrderId(pageParam, order.getId());
        return new PageResult<>(buildItemRespList(page.getList()), page.getTotal());
    }

    @Override
    public PageResult<ErpSaleOutPickDeliveryOperationItemRespVO> getSourcePickOperationItemPage(
            Integer sourceType, Long sourceId, PageParam pageParam) {
        ErpSalePickDeliveryOrderDO order = orderMapper.selectBySource(sourceType, sourceId);
        if (order == null) {
            return PageResult.empty();
        }
        PageResult<ErpSalePickDeliverySubmitItemDO> page = submitItemMapper.selectPageByOrderIdAndType(
                pageParam, order.getId(), ErpSalePickDeliverySubmitTypeEnum.PICK.getType());
        if (CollUtil.isEmpty(page.getList())) {
            return new PageResult<>(Collections.emptyList(), page.getTotal());
        }
        Set<Long> submitIds = convertSet(page.getList(), ErpSalePickDeliverySubmitItemDO::getSubmitId);
        Map<Long, ErpSalePickDeliverySubmitDO> submitMap = convertMap(
                submitMapper.selectBatchIds(submitIds), ErpSalePickDeliverySubmitDO::getId);
        Map<Long, ErpSalePickDeliveryItemDO> itemMap = convertMap(
                itemMapper.selectBatchIds(convertSet(page.getList(), ErpSalePickDeliverySubmitItemDO::getItemId)),
                ErpSalePickDeliveryItemDO::getId);
        Set<Long> userIds = convertSet(submitMap.values(), ErpSalePickDeliverySubmitDO::getSubmitUserId);
        userIds.remove(null);
        Map<Long, AdminUserRespDTO> userMap = userIds.isEmpty()
                ? Collections.emptyMap() : Optional.ofNullable(adminUserApi.getUserMap(userIds))
                .orElse(Collections.emptyMap());
        List<ErpSaleOutPickDeliveryOperationItemRespVO> rows = convertList(page.getList(), submitItem -> {
            ErpSalePickDeliverySubmitDO submit = submitMap.get(submitItem.getSubmitId());
            ErpSalePickDeliveryItemDO item = itemMap.get(submitItem.getItemId());
            AdminUserRespDTO user = submit == null ? null : userMap.get(submit.getSubmitUserId());
            return buildOperationItemResp(submitItem, submit, item, user,
                    ErpSalePickDeliverySubmitTypeEnum.PICK.getType());
        });
        return new PageResult<>(rows, page.getTotal());
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
    public PageResult<ErpSaleOutPickDeliveryOperationItemRespVO> getSaleOutPickDeliveryOperationItemPage(
            Long saleOutId, Integer type, PageParam pageParam) {
        if (!ErpSalePickDeliverySubmitTypeEnum.PICK.getType().equals(type)
                && !ErpSalePickDeliverySubmitTypeEnum.DELIVERY.getType().equals(type)) {
            return PageResult.empty();
        }
        ErpSalePickDeliveryOrderDO order = orderMapper.selectBySaleOutId(saleOutId);
        if (order == null) {
            return PageResult.empty();
        }
        List<ErpSalePickDeliverySubmitDO> submits = submitMapper.selectListBySaleOutIdAndType(saleOutId, type)
                .stream().filter(submit -> Boolean.TRUE.equals(submit.getQuantityDetails()))
                .collect(Collectors.toList());
        if (CollUtil.isEmpty(submits)) {
            return PageResult.empty();
        }
        Map<Long, ErpSalePickDeliverySubmitDO> submitMap = convertMap(submits, ErpSalePickDeliverySubmitDO::getId);
        PageResult<ErpSalePickDeliverySubmitItemDO> page = submitItemMapper.selectPageBySubmitIds(
                pageParam, submitMap.keySet());
        if (CollUtil.isEmpty(page.getList())) {
            return new PageResult<>(Collections.emptyList(), page.getTotal());
        }
        Map<Long, ErpSalePickDeliveryItemDO> itemMap = convertMap(
                itemMapper.selectBatchIds(convertSet(page.getList(), ErpSalePickDeliverySubmitItemDO::getItemId)),
                ErpSalePickDeliveryItemDO::getId);
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(
                convertSet(submits, ErpSalePickDeliverySubmitDO::getSubmitUserId));
        List<ErpSaleOutPickDeliveryOperationItemRespVO> rows = convertList(page.getList(), submitItem -> {
            ErpSalePickDeliverySubmitDO submit = submitMap.get(submitItem.getSubmitId());
            ErpSalePickDeliveryItemDO item = itemMap.get(submitItem.getItemId());
            AdminUserRespDTO user = submit == null ? null : userMap.get(submit.getSubmitUserId());
            return buildOperationItemResp(submitItem, submit, item, user, type);
        });
        return new PageResult<>(rows, page.getTotal());
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
        return quantity != null && quantity.signum() > 0 && quantity.stripTrailingZeros().scale() <= 6
                && quantity.precision() - quantity.scale() <= 18;
    }

    private BigDecimal completedCount(BigDecimal value, Integer status, BigDecimal total) {
        return value != null ? value : (Integer.valueOf(30).equals(status) ? total : BigDecimal.ZERO);
    }

    private int quantityStatus(BigDecimal completed, BigDecimal total) {
        return completed.signum() == 0 ? 10 : (completed.compareTo(total) >= 0 ? 30 : 20);
    }

    private int deliveryQuantityStatus(BigDecimal picked, BigDecimal delivered, BigDecimal total) {
        if (delivered.compareTo(total) >= 0) {
            return ErpSaleDeliveryStatusEnum.DONE.getStatus();
        }
        if (delivered.signum() > 0) {
            return ErpSaleDeliveryStatusEnum.PARTIAL.getStatus();
        }
        return picked.compareTo(delivered) > 0
                ? ErpSaleDeliveryStatusEnum.WAITING.getStatus()
                : ErpSaleDeliveryStatusEnum.NOT_READY.getStatus();
    }

    private Map<Long, BigDecimal> resolveQuantities(List<ErpSalePickDeliveryItemDO> items,
                                                   List<ErpSalePickDeliverySubmitItemReqVO> requested, boolean pick) {
        Map<Long, BigDecimal> requestedMap = requested == null ? Collections.emptyMap() : requested.stream()
                .collect(Collectors.toMap(ErpSalePickDeliverySubmitItemReqVO::getItemId,
                        ErpSalePickDeliverySubmitItemReqVO::getQuantity));
        Map<Long, BigDecimal> quantities = new LinkedHashMap<>();
        for (ErpSalePickDeliveryItemDO item : items) {
            BigDecimal done = completedCount(item.getPickedCount(), item.getPickStatus(), item.getCount());
            BigDecimal remaining = pick
                    ? item.getCount().subtract(done)
                    : done.subtract(completedCount(item.getDeliveredCount(), item.getDeliveryStatus(), item.getCount()));
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
        ErpSalePickDeliveryPickTaskDO deliveryTask = null;
        if (pick) {
            if (pickTaskMapper.selectById(parentId) == null) {
                throw exception(SALE_PICK_TASK_NOT_EXISTS);
            }
        } else {
            deliveryTask = pickTaskMapper.selectById(parentId);
            if (deliveryTask == null) {
                throw exception(SALE_PICK_TASK_NOT_EXISTS);
            }
        }
        ErpSalePickDeliverySubmitDO submit = submitMapper.selectById(submitId);
        if (submit == null || !Objects.equals(submit.getType(), pick ? 10 : 20)) {
            throw exception(SALE_PICK_DELIVERY_PAYLOAD_INVALID);
        }
        PageResult<ErpSalePickDeliverySubmitItemDO> page;
        if (pick) {
            if (!Objects.equals(submit.getPickTaskId(), parentId)) {
                throw exception(SALE_PICK_DELIVERY_PAYLOAD_INVALID);
            }
            page = submitItemMapper.selectPageBySubmitId(pageParam, submitId);
        } else {
            if (!Objects.equals(submit.getOrderId(), deliveryTask.getOrderId())
                    || (submit.getPickTaskId() != null && !Objects.equals(submit.getPickTaskId(), parentId))) {
                throw exception(SALE_PICK_DELIVERY_PAYLOAD_INVALID);
            }
            page = submitItemMapper.selectPageBySubmitIdAndPickTaskId(pageParam, submitId, parentId);
            if (submit.getPickTaskId() == null && page.getTotal() == 0) {
                throw exception(SALE_PICK_DELIVERY_PAYLOAD_INVALID);
            }
        }
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
            update.setPickCompleteTime(now);
        }
        orderMapper.updateById(update);
        if (ErpSaleBizSourceTypeEnum.isPickOnly(order.getSourceType())) {
            if (ErpSalePickStatusEnum.DONE.getStatus().equals(orderPickStatus)) {
                if (ErpSaleBizSourceTypeEnum.SALE_RETURN.getType().equals(order.getSourceType())) {
                    eventPublisher.publishEvent(new ErpSaleReturnPickCompletedEvent(
                            order.getSourceId(), getLoginUserId()));
                } else if (ErpSaleBizSourceTypeEnum.WAREHOUSE_MOVE.getType().equals(order.getSourceType())) {
                    eventPublisher.publishEvent(new ErpWarehouseMovePickCompletedEvent(
                            order.getSourceId(), getLoginUserId()));
                } else if (ErpSaleBizSourceTypeEnum.STOCK_IN.getType().equals(order.getSourceType())) {
                    eventPublisher.publishEvent(new ErpStockInPickCompletedEvent(
                            order.getSourceId(), getLoginUserId()));
                } else if (ErpSaleBizSourceTypeEnum.STOCK_OUT.getType().equals(order.getSourceType())) {
                    eventPublisher.publishEvent(new ErpStockOutPickCompletedEvent(
                            order.getSourceId(), getLoginUserId()));
                }
            }
            return;
        }
        // 补拣只刷新可送状态，不得改写最近送货时间或送货完成时间。
        refreshDeliveryProgress(order, null);
    }

    private Integer refreshDeliveryProgress(ErpSalePickDeliveryOrderDO order, LocalDateTime now) {
        int deliveredCount = itemMapper.selectCountByOrderIdAndDeliveryStatus(
                order.getId(), ErpSaleDeliveryStatusEnum.DONE.getStatus()).intValue();
        Integer deliveryStatus = buildStatus(deliveredCount, order.getTotalItemCount(),
                ErpSaleDeliveryStatusEnum.WAITING.getStatus(), ErpSaleDeliveryStatusEnum.PARTIAL.getStatus(),
                ErpSaleDeliveryStatusEnum.DONE.getStatus());
        if (!ErpSaleDeliveryStatusEnum.DONE.getStatus().equals(deliveryStatus)) {
            if (itemMapper.hasDeliveryProgress(order.getId())) {
                deliveryStatus = ErpSaleDeliveryStatusEnum.PARTIAL.getStatus();
            } else if (!itemMapper.hasAvailableDelivery(order.getId())) {
                deliveryStatus = ErpSaleDeliveryStatusEnum.NOT_READY.getStatus();
            }
        }
        ErpSalePickDeliveryOrderDO update = new ErpSalePickDeliveryOrderDO().setId(order.getId())
                .setDeliveredItemCount(deliveredCount).setDeliveryStatus(deliveryStatus);
        if (now != null) {
            update.setLatestDeliveryTime(now);
        }
        if (now != null && ErpSaleDeliveryStatusEnum.DONE.getStatus().equals(deliveryStatus)) {
            update.setDeliveryCompleteTime(now);
        }
        orderMapper.updateById(update);
        return deliveryStatus;
    }

    private void validateDeliveryRequired(ErpSalePickDeliveryOrderDO order) {
        if (order == null) {
            throw exception(SALE_PICK_DELIVERY_ORDER_NOT_EXISTS);
        }
        if (ErpSaleBizSourceTypeEnum.SALE_RETURN.getType().equals(order.getSourceType())) {
            throw exception(SALE_RETURN_DELIVERY_NOT_REQUIRED);
        }
        if (ErpSaleBizSourceTypeEnum.WAREHOUSE_MOVE.getType().equals(order.getSourceType())) {
            throw exception(WAREHOUSE_MOVE_DELIVERY_NOT_REQUIRED);
        }
        if (ErpSaleBizSourceTypeEnum.STOCK_IN.getType().equals(order.getSourceType())) {
            throw exception(STOCK_IN_DELIVERY_NOT_REQUIRED);
        }
        if (ErpSaleBizSourceTypeEnum.STOCK_OUT.getType().equals(order.getSourceType())) {
            throw exception(STOCK_OUT_DELIVERY_NOT_REQUIRED);
        }
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
        List<ErpSalePickDeliverySubmitDO> submits = submitMapper.selectListByOrderId(order.getId());
        submits.forEach(submit -> submitMapper.updateById(
                new ErpSalePickDeliverySubmitDO().setId(submit.getId()).setSaleOutId(saleOut.getId())));
        syncPrimaryExpressFileIfBlank(saleOut, submits);
        return true;
    }

    /**
     * 销售手推车履约完成后生成销售单时，将最新一次送货提交的第一张快递单同步为兼容主图。
     * 全部附件仍保留在送货附件表中；这里仅填充旧接口依赖的单图字段，绝不覆盖已有值。
     */
    private void syncPrimaryExpressFileIfBlank(ErpSaleOutDO saleOut,
                                               List<ErpSalePickDeliverySubmitDO> submits) {
        if (StringUtils.hasText(saleOut.getExpressFileUrl()) || CollUtil.isEmpty(submits)) {
            return;
        }
        List<ErpSalePickDeliverySubmitDO> deliverySubmits = submits.stream()
                .filter(submit -> ErpSalePickDeliverySubmitTypeEnum.DELIVERY.getType().equals(submit.getType()))
                .sorted(Comparator
                        .comparing(ErpSalePickDeliverySubmitDO::getSubmitTime,
                                Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ErpSalePickDeliverySubmitDO::getId,
                                Comparator.nullsLast(Comparator.reverseOrder())))
                .collect(Collectors.toList());
        if (deliverySubmits.isEmpty()) {
            return;
        }
        Map<Long, List<ErpSalePickDeliverySubmitFileDO>> fileMap = convertMultiMap(
                submitFileMapper.selectListBySubmitIds(convertSet(deliverySubmits,
                        ErpSalePickDeliverySubmitDO::getId)),
                ErpSalePickDeliverySubmitFileDO::getSubmitId);
        Comparator<ErpSalePickDeliverySubmitFileDO> fileComparator = Comparator
                .comparing(ErpSalePickDeliverySubmitFileDO::getSort,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(ErpSalePickDeliverySubmitFileDO::getId,
                        Comparator.nullsLast(Comparator.naturalOrder()));
        for (ErpSalePickDeliverySubmitDO submit : deliverySubmits) {
            ErpSalePickDeliverySubmitFileDO primaryFile = fileMap
                    .getOrDefault(submit.getId(), Collections.emptyList()).stream()
                    .filter(file -> StringUtils.hasText(file.getFileUrl()))
                    .min(fileComparator)
                    .orElse(null);
            if (primaryFile == null) {
                continue;
            }
            String fileUrl = primaryFile.getFileUrl().trim();
            saleOutMapper.updateById(new ErpSaleOutDO().setId(saleOut.getId()).setExpressFileUrl(fileUrl));
            saleOut.setExpressFileUrl(fileUrl);
            return;
        }
    }

    private <T> PageResult<T> pageList(List<T> rows, PageParam pageParam) {
        if (CollUtil.isEmpty(rows)) {
            return PageResult.empty();
        }
        int pageNo = pageParam.getPageNo() == null ? 1 : pageParam.getPageNo();
        int pageSize = pageParam.getPageSize() == null ? 10 : pageParam.getPageSize();
        int from = Math.max(0, (pageNo - 1) * pageSize);
        if (from >= rows.size()) {
            return new PageResult<>(Collections.emptyList(), (long) rows.size());
        }
        int to = Math.min(rows.size(), from + pageSize);
        return new PageResult<>(rows.subList(from, to), (long) rows.size());
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
        resp.setSourceTypeName(ErpSaleBizSourceTypeEnum.getPickSourceName(task.getSourceType()));
        resp.setPickProgress(buildProgress(task.getPickedItemCount(), task.getTotalItemCount()));
        return resp;
    }

    private void fillPickQuantity(ErpSalePickDeliverySummaryRespVO resp,
                                  BigDecimal totalQuantity, BigDecimal pickedQuantity) {
        BigDecimal normalizedTotal = totalQuantity == null ? BigDecimal.ZERO : totalQuantity;
        BigDecimal normalizedPicked = pickedQuantity == null ? BigDecimal.ZERO : pickedQuantity;
        resp.setTotalQuantity(normalizedTotal);
        resp.setPickedQuantity(normalizedPicked);
        resp.setRemainingPickQuantity(normalizedTotal.subtract(normalizedPicked).max(BigDecimal.ZERO));
    }

    private void fillPickQuantity(ErpSalePickTaskRespVO resp,
                                  BigDecimal totalQuantity, BigDecimal pickedQuantity) {
        BigDecimal normalizedTotal = totalQuantity == null ? BigDecimal.ZERO : totalQuantity;
        BigDecimal normalizedPicked = pickedQuantity == null ? BigDecimal.ZERO : pickedQuantity;
        resp.setTotalQuantity(normalizedTotal);
        resp.setPickedQuantity(normalizedPicked);
        resp.setRemainingPickQuantity(normalizedTotal.subtract(normalizedPicked).max(BigDecimal.ZERO));
    }

    private ErpSaleOutPickDeliveryOperationItemRespVO buildOperationItemResp(
            ErpSalePickDeliverySubmitItemDO submitItem, ErpSalePickDeliverySubmitDO submit,
            ErpSalePickDeliveryItemDO item, AdminUserRespDTO user, Integer defaultType) {
        return new ErpSaleOutPickDeliveryOperationItemRespVO()
                .setId(submitItem.getId())
                .setSubmitId(submitItem.getSubmitId())
                .setType(submit == null ? defaultType : submit.getType())
                .setSubmitUserId(submit == null ? null : submit.getSubmitUserId())
                .setSubmitUserName(user == null ? null : user.getNickname())
                .setSubmitTime(submit == null ? null : submit.getSubmitTime())
                .setItemId(submitItem.getItemId())
                .setProductCode(StringUtils.hasText(submitItem.getProductCode())
                        ? submitItem.getProductCode() : item == null ? null : item.getProductCode())
                .setProductName(StringUtils.hasText(submitItem.getProductName())
                        ? submitItem.getProductName() : item == null ? null : item.getProductName())
                .setStandard(item == null ? null : item.getStandard())
                .setWarehouseId(item == null ? null : item.getWarehouseId())
                .setWarehouseName(item == null ? null : item.getWarehouseName())
                .setQuantity(submitItem.getQuantity());
    }

    private ErpSalePickTaskRespVO buildPickResp(ErpSalePickDeliveryPickTaskDO task, boolean includeDetail) {
        ErpSalePickTaskRespVO resp = BeanUtils.toBean(task, ErpSalePickTaskRespVO.class);
        resp.setDisplayNo(buildDisplayNo(task.getSaleOutNo(), task.getSourceNo()));
        resp.setSourceTypeName(ErpSaleBizSourceTypeEnum.getPickSourceName(task.getSourceType()));
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
        resp.setDeliverableItemCount(itemMapper.selectCountByOrderIdWithAvailableDelivery(order.getId()).intValue());
        if (includeDetail) {
            resp.setItems(buildItemRespList(itemMapper.selectListByOrderId(order.getId())));
            resp.setSubmits(buildSubmitRespList(submitMapper.selectListByOrderIdAndType(
                    order.getId(), ErpSalePickDeliverySubmitTypeEnum.DELIVERY.getType())));
        }
        return resp;
    }

    private ErpSaleDeliveryOrderRespVO buildDeliveryTaskResp(ErpSalePickDeliveryPickTaskDO task,
                                                              ErpSalePickDeliveryOrderDO order,
                                                              List<ErpSalePickDeliveryItemDO> items,
                                                              boolean includeDetail) {
        ErpSaleDeliveryOrderRespVO resp = BeanUtils.toBean(task, ErpSaleDeliveryOrderRespVO.class);
        resp.setId(task.getId());
        resp.setOrderId(task.getOrderId());
        resp.setDisplayNo(buildDisplayNo(task.getSaleOutNo(), task.getSourceNo()));
        resp.setDeptId(order == null ? null : order.getDeptId());
        resp.setPickStatus(task.getStatus());
        resp.setLatestPickTime(task.getLatestPickTime());
        resp.setPickCompleteTime(task.getCompleteTime());
        resp.setCreateTime(task.getCreateTime());

        int totalItemCount = task.getTotalItemCount() == null ? items.size() : task.getTotalItemCount();
        int pickedItemCount = 0;
        int deliveredItemCount = 0;
        int deliverableItemCount = 0;
        boolean hasDeliveryProgress = false;
        LocalDateTime latestDeliveryTime = null;
        for (ErpSalePickDeliveryItemDO item : items) {
            BigDecimal picked = completedCount(item.getPickedCount(), item.getPickStatus(), item.getCount());
            BigDecimal delivered = completedCount(item.getDeliveredCount(), item.getDeliveryStatus(), item.getCount());
            if (picked.compareTo(item.getCount()) >= 0) {
                pickedItemCount++;
            }
            if (delivered.compareTo(item.getCount()) >= 0) {
                deliveredItemCount++;
            }
            if (picked.compareTo(delivered) > 0) {
                deliverableItemCount++;
            }
            if (delivered.signum() > 0) {
                hasDeliveryProgress = true;
            }
            if (item.getDeliveryTime() != null
                    && (latestDeliveryTime == null || item.getDeliveryTime().isAfter(latestDeliveryTime))) {
                latestDeliveryTime = item.getDeliveryTime();
            }
        }
        Integer deliveryStatus;
        if (totalItemCount > 0 && deliveredItemCount >= totalItemCount) {
            deliveryStatus = ErpSaleDeliveryStatusEnum.DONE.getStatus();
        } else if (hasDeliveryProgress) {
            deliveryStatus = ErpSaleDeliveryStatusEnum.PARTIAL.getStatus();
        } else if (deliverableItemCount > 0) {
            deliveryStatus = ErpSaleDeliveryStatusEnum.WAITING.getStatus();
        } else {
            deliveryStatus = ErpSaleDeliveryStatusEnum.NOT_READY.getStatus();
        }
        resp.setTotalItemCount(totalItemCount);
        resp.setPickedItemCount(pickedItemCount);
        resp.setDeliveredItemCount(deliveredItemCount);
        resp.setDeliverableItemCount(deliverableItemCount);
        resp.setPickProgress(buildProgress(pickedItemCount, totalItemCount));
        resp.setDeliveryStatus(deliveryStatus);
        resp.setDeliveryProgress(buildProgress(deliveredItemCount, totalItemCount));
        resp.setLatestDeliveryTime(latestDeliveryTime);
        resp.setDeliveryCompleteTime(ErpSaleDeliveryStatusEnum.DONE.getStatus().equals(deliveryStatus)
                ? latestDeliveryTime : null);
        resp.setTotalPieceCount(calculateTotalPieceCount(items));
        if (includeDetail) {
            PageParam allSubmits = new PageParam();
            allSubmits.setPageSize(PageParam.PAGE_SIZE_NONE);
            resp.setItems(buildItemRespList(items));
            resp.setSubmits(buildDeliverySubmitRespList(
                    submitMapper.selectDeliveryPageByTaskId(allSubmits,
                            task.getId(), task.getOrderId()).getList(), task.getId()));
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
            item.setAvailableDeliveryCount(item.getPickedCount().subtract(item.getDeliveredCount()).max(BigDecimal.ZERO));
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

    private List<ErpSalePickDeliverySubmitRespVO> buildDeliverySubmitRespList(
            List<ErpSalePickDeliverySubmitDO> submits, Long taskId) {
        List<ErpSalePickDeliverySubmitRespVO> respList = buildSubmitRespList(submits);
        if (CollUtil.isEmpty(submits)) {
            return respList;
        }
        Map<Long, ErpSalePickDeliverySubmitDO> submitMap = convertMap(submits, ErpSalePickDeliverySubmitDO::getId);
        respList.forEach(resp -> {
            ErpSalePickDeliverySubmitDO submit = submitMap.get(resp.getId());
            if (submit != null && submit.getPickTaskId() == null) {
                resp.setItemCount(Math.toIntExact(
                        submitItemMapper.selectCountBySubmitIdAndPickTaskId(submit.getId(), taskId)));
            }
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
