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
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.pickdelivery.*;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleCartMapper;
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
    private ErpSalePickDeliveryOrderMapper orderMapper;
    @Resource
    private ErpSalePickDeliveryPickTaskMapper pickTaskMapper;
    @Resource
    private ErpSalePickDeliveryItemMapper itemMapper;
    @Resource
    private ErpSalePickDeliverySubmitMapper submitMapper;
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
                        .setCount(saleOutItem.getCount())
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
    public void generateForSaleCartTransferOuts(Long saleCartId) {
        if (saleCartId == null || orderMapper.selectBySource(
                ErpSaleBizSourceTypeEnum.CART.getType(), saleCartId) != null) {
            return;
        }
        ErpSaleCartDO cart = saleCartMapper.selectById(saleCartId);
        if (cart == null) {
            return;
        }
        List<ErpStockMoveDO> transferOuts = stockMoveService.getTransferOutListBySource(
                ErpSaleBizSourceTypeEnum.CART.getType(), saleCartId);
        if (CollUtil.isEmpty(transferOuts)) {
            return;
        }
        List<ErpStockMoveItemDO> transferItems = stockMoveService.getStockMoveItemListByMoveIds(
                convertSet(transferOuts, ErpStockMoveDO::getId));
        if (CollUtil.isEmpty(transferItems)) {
            return;
        }
        if (transferItems.stream().anyMatch(item -> item.getFromWarehouseId() == null)) {
            throw exception(SALE_PICK_DELIVERY_WAREHOUSE_REQUIRED);
        }

        Long tenantId = TenantContextHolder.getRequiredTenantId();
        ErpCustomerDO customer = cart.getCustomerId() == null ? null : customerService.getCustomer(cart.getCustomerId());
        String customerName = customer == null ? null : customer.getName();
        Map<Long, ErpWarehouseDO> warehouseMap = warehouseService.getWarehouseMap(
                convertSet(transferItems, ErpStockMoveItemDO::getFromWarehouseId));
        Map<Long, ErpProductRespVO> productMap = productService.getProductVOMap(
                convertSet(transferItems, ErpStockMoveItemDO::getProductId));

        ErpSalePickDeliveryOrderDO order = new ErpSalePickDeliveryOrderDO()
                .setSourceType(ErpSaleBizSourceTypeEnum.CART.getType())
                .setSourceId(cart.getId()).setSourceNo(cart.getNo())
                .setCustomerId(cart.getCustomerId()).setCustomerName(customerName).setDeptId(cart.getDeptId())
                .setPickStatus(ErpSalePickStatusEnum.WAITING.getStatus())
                .setDeliveryStatus(ErpSaleDeliveryStatusEnum.NOT_READY.getStatus())
                .setTotalItemCount(transferItems.size()).setPickedItemCount(0).setDeliveredItemCount(0);
        order.setTenantId(tenantId);
        orderMapper.insert(order);

        Map<Long, List<ErpStockMoveItemDO>> itemMap = transferItems.stream()
                .collect(Collectors.groupingBy(ErpStockMoveItemDO::getFromWarehouseId,
                        LinkedHashMap::new, Collectors.toList()));
        for (Map.Entry<Long, List<ErpStockMoveItemDO>> entry : itemMap.entrySet()) {
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
            for (ErpStockMoveItemDO transferItem : entry.getValue()) {
                ErpProductRespVO product = productMap.get(transferItem.getProductId());
                ErpSalePickDeliveryItemDO item = new ErpSalePickDeliveryItemDO()
                        .setOrderId(order.getId()).setPickTaskId(task.getId())
                        .setTransferOutId(transferItem.getMoveId()).setTransferOutItemId(transferItem.getId())
                        .setWarehouseId(warehouseId).setWarehouseName(task.getWarehouseName())
                        .setProductId(transferItem.getProductId())
                        .setProductCode(product == null ? null : product.getCode())
                        .setProductName(product == null ? null : product.getName())
                        .setStandard(product == null ? null : product.getStandard())
                        .setCount(transferItem.getCount())
                        .setWarehousePosition(transferItem.getFromShelf())
                        .setPackageQty(transferItem.getPackageQty())
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
    @Transactional(rollbackFor = Exception.class)
    public void submitPick(ErpSalePickSubmitReqVO reqVO) {
        validateSubmitPayload(reqVO.getItemIds(), reqVO.getFiles());
        ErpSalePickDeliveryPickTaskDO task = pickTaskMapper.selectByIdForUpdate(reqVO.getTaskId());
        if (task == null) {
            throw exception(SALE_PICK_TASK_NOT_EXISTS);
        }
        validatePickWarehousePermission(task.getWarehouseId());
        if (ErpSalePickStatusEnum.DONE.getStatus().equals(task.getStatus())) {
            throw exception(SALE_PICK_TASK_STATUS_INVALID);
        }
        ErpSalePickDeliveryOrderDO order = orderMapper.selectByIdForUpdate(task.getOrderId());
        if (order == null) {
            throw exception(SALE_PICK_DELIVERY_ORDER_NOT_EXISTS);
        }
        List<Long> itemIds = distinctIds(reqVO.getItemIds());
        List<ErpSalePickDeliveryItemDO> items = itemMapper.selectListByIdsForUpdate(itemIds);
        if (items.size() != itemIds.size() || items.stream().anyMatch(item ->
                !Objects.equals(item.getPickTaskId(), task.getId())
                        || ErpSalePickStatusEnum.DONE.getStatus().equals(item.getPickStatus()))) {
            throw exception(SALE_PICK_ITEM_INVALID);
        }
        LocalDateTime now = LocalDateTime.now();
        Long userId = getLoginUserId();
        insertSubmit(order.getId(), task.getId(), task.getSaleOutId(),
                ErpSalePickDeliverySubmitTypeEnum.PICK.getType(), userId, now, itemIds.size(), reqVO.getRemark(), reqVO.getFiles());
        items.forEach(item -> itemMapper.updateById(new ErpSalePickDeliveryItemDO()
                .setId(item.getId()).setPickStatus(ErpSalePickStatusEnum.DONE.getStatus())
                .setPickUserId(userId).setPickTime(now)
                .setDeliveryStatus(ErpSaleDeliveryStatusEnum.WAITING.getStatus())));
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
    @Transactional(rollbackFor = Exception.class)
    public void submitDelivery(ErpSaleDeliverySubmitReqVO reqVO) {
        validateSubmitPayload(reqVO.getItemIds(), reqVO.getFiles());
        ErpSalePickDeliveryOrderDO order = orderMapper.selectByIdForUpdate(reqVO.getOrderId());
        if (order == null) {
            throw exception(SALE_PICK_DELIVERY_ORDER_NOT_EXISTS);
        }
        if (!ErpSalePickStatusEnum.DONE.getStatus().equals(order.getPickStatus())) {
            throw exception(SALE_DELIVERY_NOT_READY);
        }
        if (ErpSaleDeliveryStatusEnum.DONE.getStatus().equals(order.getDeliveryStatus())
                || ErpSaleDeliveryStatusEnum.NOT_READY.getStatus().equals(order.getDeliveryStatus())) {
            throw exception(SALE_DELIVERY_STATUS_INVALID);
        }
        List<Long> itemIds = distinctIds(reqVO.getItemIds());
        List<ErpSalePickDeliveryItemDO> items = itemMapper.selectListByIdsForUpdate(itemIds);
        if (items.size() != itemIds.size() || items.stream().anyMatch(item ->
                !Objects.equals(item.getOrderId(), order.getId())
                        || !ErpSalePickStatusEnum.DONE.getStatus().equals(item.getPickStatus())
                        || ErpSaleDeliveryStatusEnum.DONE.getStatus().equals(item.getDeliveryStatus()))) {
            throw exception(SALE_DELIVERY_ITEM_INVALID);
        }
        LocalDateTime now = LocalDateTime.now();
        Long userId = getLoginUserId();
        insertSubmit(order.getId(), null, order.getSaleOutId(),
                ErpSalePickDeliverySubmitTypeEnum.DELIVERY.getType(), userId, now, itemIds.size(), reqVO.getRemark(), reqVO.getFiles());
        items.forEach(item -> itemMapper.updateById(new ErpSalePickDeliveryItemDO()
                .setId(item.getId()).setDeliveryStatus(ErpSaleDeliveryStatusEnum.DONE.getStatus())
                .setDeliveryUserId(userId).setDeliveryTime(now)));
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

    private void validateSubmitPayload(List<Long> itemIds, List<ErpSalePickSubmitReqVO.File> files) {
        if (CollUtil.isEmpty(itemIds)) {
            throw exception(SALE_PICK_DELIVERY_ITEM_REQUIRED);
        }
        if (CollUtil.isEmpty(files)) {
            throw exception(SALE_PICK_DELIVERY_FILE_REQUIRED);
        }
    }

    private List<Long> distinctIds(List<Long> ids) {
        return ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
    }

    private void insertSubmit(Long orderId, Long pickTaskId, Long saleOutId, Integer type, Long userId,
                              LocalDateTime now, Integer itemCount, String remark,
                              List<ErpSalePickSubmitReqVO.File> files) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        ErpSalePickDeliverySubmitDO submit = new ErpSalePickDeliverySubmitDO()
                .setOrderId(orderId).setPickTaskId(pickTaskId).setSaleOutId(saleOutId).setType(type)
                .setSubmitUserId(userId).setSubmitTime(now).setItemCount(itemCount).setRemark(remark);
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
    }

    private void refreshPickProgress(ErpSalePickDeliveryPickTaskDO task, ErpSalePickDeliveryOrderDO order, LocalDateTime now) {
        int pickedTaskCount = itemMapper.selectCountByPickTaskIdAndPickStatus(
                task.getId(), ErpSalePickStatusEnum.DONE.getStatus()).intValue();
        Integer taskStatus = buildStatus(pickedTaskCount, task.getTotalItemCount(),
                ErpSalePickStatusEnum.WAITING.getStatus(), ErpSalePickStatusEnum.PARTIAL.getStatus(),
                ErpSalePickStatusEnum.DONE.getStatus());
        pickTaskMapper.updateById(new ErpSalePickDeliveryPickTaskDO().setId(task.getId())
                .setPickedItemCount(pickedTaskCount).setStatus(taskStatus).setLatestPickTime(now)
                .setCompleteTime(ErpSalePickStatusEnum.DONE.getStatus().equals(taskStatus) ? now : null));

        int pickedOrderCount = itemMapper.selectCountByOrderIdAndPickStatus(
                order.getId(), ErpSalePickStatusEnum.DONE.getStatus()).intValue();
        Integer orderPickStatus = buildStatus(pickedOrderCount, order.getTotalItemCount(),
                ErpSalePickStatusEnum.WAITING.getStatus(), ErpSalePickStatusEnum.PARTIAL.getStatus(),
                ErpSalePickStatusEnum.DONE.getStatus());
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
        for (Long transferOutId : transferOutIds) {
            stockMoveService.approveSaleCartTransferOutAfterDelivery(transferOutId, userId);
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
