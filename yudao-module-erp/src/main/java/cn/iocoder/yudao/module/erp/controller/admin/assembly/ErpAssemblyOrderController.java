package cn.iocoder.yudao.module.erp.controller.admin.assembly;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.assembly.vo.order.*;
import cn.iocoder.yudao.module.erp.controller.admin.assembly.vo.recipe.*;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockBatchNoRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.assembly.ErpAssemblyOrderDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.assembly.ErpAssemblyOrderItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.assembly.ErpAssemblyRecipeItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.common.ErpPrintRecordDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseMoveDO;
import cn.iocoder.yudao.module.erp.dal.mysql.assembly.ErpAssemblyOrderItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.assembly.ErpAssemblyRecipeItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.common.ErpPrintRecordMapper;
import cn.iocoder.yudao.module.erp.service.assembly.ErpAssemblyOrderService;
import cn.iocoder.yudao.module.erp.service.common.ErpPrintService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseMoveService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserSimpleRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.enums.CommonStatusEnum.ENABLE;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserId;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ASSEMBLY_ORDER_NOT_EXISTS;

@Tag(name = "管理后台 - 套件组装单")
@RestController
@RequestMapping("/erp/assembly-order")
@Validated
public class ErpAssemblyOrderController {

    private static final String PRINT_MODULE_KEY = "assembly_order";

    @Resource
    private ErpAssemblyOrderService orderService;
    @Resource
    private ErpAssemblyRecipeController recipeReader;
    @Resource
    private cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseMapper warehouseMapper;
    @Resource
    private cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseInItemMapper purchaseInItemMapper;
    @Resource
    private ErpAssemblyOrderItemMapper orderItemMapper;
    @Resource
    private ErpAssemblyRecipeItemMapper recipeItemMapper;
    @Resource
    private ErpProductService productService;
    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private ErpWarehouseMoveService warehouseMoveService;
    @Resource
    private ErpStockService stockService;
    @Resource
    private ErpCustomerService customerService;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private ErpPrintService printService;
    @Resource
    private ErpPrintRecordMapper printRecordMapper;

    @PostMapping("/create-draft")
    @Operation(summary = "创建套件组装单草稿")
    @PreAuthorize("@ss.hasPermission('erp:assembly-order:create')")
    public CommonResult<Long> createDraft(@Valid @RequestBody ErpAssemblyOrderSaveReqVO reqVO) {
        return success(orderService.createDraft(reqVO));
    }

    @PutMapping("/update-draft")
    @Operation(summary = "修改套件组装单草稿")
    @PreAuthorize("@ss.hasPermission('erp:assembly-order:update')")
    public CommonResult<Boolean> updateDraft(@Valid @RequestBody ErpAssemblyOrderSaveReqVO reqVO) {
        orderService.updateDraft(reqVO);
        return success(true);
    }

    @PutMapping("/confirm")
    @Operation(summary = "确认装配")
    @PreAuthorize("@ss.hasPermission('erp:assembly-order:confirm')")
    public CommonResult<Boolean> confirm(@RequestParam("id") Long id) {
        orderService.confirm(id);
        return success(true);
    }

    @PutMapping("/reverse-confirm")
    @Operation(summary = "反确认装配")
    @PreAuthorize("@ss.hasPermission('erp:assembly-order:reverse-confirm')")
    public CommonResult<Boolean> reverseConfirm(@RequestParam("id") Long id) {
        orderService.reverseConfirm(id);
        return success(true);
    }

    @PutMapping("/start")
    @Operation(summary = "开始装配")
    @PreAuthorize("@ss.hasPermission('erp:assembly-order:start')")
    public CommonResult<Boolean> start(@RequestParam("id") Long id) {
        orderService.start(id);
        return success(true);
    }

    @PutMapping("/cancel-start")
    @Operation(summary = "取消开始装配")
    @PreAuthorize("@ss.hasPermission('erp:assembly-order:cancel-start')")
    public CommonResult<Boolean> cancelStart(@RequestParam("id") Long id) {
        orderService.cancelStart(id);
        return success(true);
    }

    @PutMapping("/complete")
    @Operation(summary = "完成套件组装单")
    @PreAuthorize("@ss.hasPermission('erp:assembly-order:complete')")
    public CommonResult<Boolean> complete(@RequestParam("id") Long id) {
        orderService.complete(id);
        return success(true);
    }

    @PutMapping("/reverse-assembly")
    @Operation(summary = "反装配已完成套件组装单")
    @PreAuthorize("@ss.hasPermission('erp:assembly-order:reverse-assembly')")
    public CommonResult<Boolean> reverseAssembly(@RequestParam("id") Long id) {
        orderService.reverseAssembly(id);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除套件组装单草稿")
    @PreAuthorize("@ss.hasPermission('erp:assembly-order:delete')")
    public CommonResult<Boolean> deleteDraft(@RequestParam("id") Long id) {
        orderService.deleteDraft(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获取套件组装单")
    @PreAuthorize("@ss.hasPermission('erp:assembly-order:query')")
    public CommonResult<ErpAssemblyOrderRespVO> getOrder(
            @RequestParam("id") Long id,
            @RequestParam(value = "includeItems", defaultValue = "true") Boolean includeItems) {
        ErpAssemblyOrderDO order = orderService.getOrder(id);
        if (order == null) {
            return success(null);
        }
        ErpAssemblyOrderRespVO respVO = BeanUtils.toBean(order, ErpAssemblyOrderRespVO.class);
        fillRelations(Collections.singletonList(respVO));
        if (Boolean.TRUE.equals(includeItems)) {
            List<ErpAssemblyOrderItemDO> items = orderService.getOrderItemList(id);
            respVO.setItems(buildItemVOList(order, items));
            respVO.setItemCount(items.size());
        } else {
            respVO.setItemCount(toCountMap(orderItemMapper.selectCountRowsByOrderIds(Collections.singleton(id)))
                    .getOrDefault(id, 0));
        }
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "获取套件组装单分页")
    @PreAuthorize("@ss.hasPermission('erp:assembly-order:query')")
    public CommonResult<PageResult<ErpAssemblyOrderRespVO>> getOrderPage(@Valid ErpAssemblyOrderPageReqVO reqVO) {
        PageResult<ErpAssemblyOrderDO> page = orderService.getOrderPage(reqVO);
        List<ErpAssemblyOrderRespVO> list = BeanUtils.toBean(page.getList(), ErpAssemblyOrderRespVO.class);
        List<Long> orderIds = list.stream().map(ErpAssemblyOrderRespVO::getId).collect(Collectors.toList());
        Map<Long, Integer> countMap = toCountMap(orderItemMapper.selectCountRowsByOrderIds(orderIds));
        list.forEach(vo -> vo.setItemCount(countMap.getOrDefault(vo.getId(), 0)));
        fillRelations(list);
        return success(new PageResult<>(list, page.getTotal()));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出单张套件组装单")
    @PreAuthorize("@ss.hasPermission('erp:assembly-order:query')")
    public void exportOrder(@RequestParam("id") Long id, HttpServletResponse response) throws IOException {
        ErpAssemblyOrderDO order = orderService.getOrder(id);
        if (order == null) {
            throw exception(ASSEMBLY_ORDER_NOT_EXISTS);
        }
        ErpAssemblyOrderRespVO vo = BeanUtils.toBean(order, ErpAssemblyOrderRespVO.class);
        fillRelations(Collections.singletonList(vo));
        ErpAssemblyOrderExportRespVO exportVO = new ErpAssemblyOrderExportRespVO();
        exportVO.setNo(vo.getNo());
        exportVO.setAssemblyTime(vo.getAssemblyTime());
        exportVO.setStatusName(statusName(vo.getStatus()));
        exportVO.setObjectName(vo.getCustomerName());
        exportVO.setDrawingNo(vo.getProductDrawingNo());
        exportVO.setProductCode(vo.getProductCode());
        exportVO.setProductName(vo.getProductName());
        exportVO.setOriginPlace(vo.getProductOriginPlace());
        exportVO.setStandard(vo.getProductStandard());
        exportVO.setFeatureCode(vo.getProductFeatureCode());
        exportVO.setBrand(vo.getProductBrand());
        exportVO.setVehicleModel(vo.getProductVehicleModel());
        exportVO.setWarehouseName(vo.getWarehouseName());
        exportVO.setShelf(vo.getShelf());
        exportVO.setSetCount(vo.getSetCount());
        exportVO.setTotalCost(vo.getTotalCost());
        exportVO.setUnitCost(vo.getUnitCost());
        exportVO.setRetailPrice(vo.getProductRetailPrice());
        exportVO.setCreatorName(vo.getCreatorName());
        exportVO.setUpdaterName(vo.getUpdaterName());
        exportVO.setPrintCount(vo.getPrintCount());
        exportVO.setPrintTime(vo.getPrintTime());
        writeExport(response, exportVO);
    }

    static void writeExport(HttpServletResponse response, ErpAssemblyOrderExportRespVO exportVO) throws IOException {
        ExcelUtils.write(new javax.servlet.http.HttpServletResponseWrapper(response) {
            @Override public void setContentType(String ignored) {
                super.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            }
        }, "套件组装单.xlsx", "套件组装单", ErpAssemblyOrderExportRespVO.class, Collections.singletonList(exportVO));
    }

    @PostMapping("/record-print")
    @Operation(summary = "记录套件组装单打印")
    @PreAuthorize("@ss.hasPermission('erp:assembly-order:query')")
    public CommonResult<Boolean> recordPrint(@RequestParam("id") Long id) {
        ErpAssemblyOrderDO order = orderService.getOrder(id);
        if (order == null) {
            throw exception(ASSEMBLY_ORDER_NOT_EXISTS);
        }
        Long loginUserId = getLoginUserId();
        AdminUserRespDTO user = loginUserId == null ? null : adminUserApi.getUser(loginUserId);
        printRecordMapper.insert(ErpPrintRecordDO.builder()
                .moduleKey(PRINT_MODULE_KEY)
                .businessId(order.getId())
                .businessNo(order.getNo())
                .printerId(loginUserId)
                .printerName(user == null ? null : user.getNickname())
                .printTime(LocalDateTime.now())
                .build());
        return success(true);
    }

    @GetMapping("/item-page")
    @Operation(summary = "获取套件组装单明细分页")
    @PreAuthorize("@ss.hasPermission('erp:assembly-order:query')")
    public CommonResult<PageResult<ErpAssemblyOrderRespVO.Item>> getOrderItemPage(
            @Valid ErpAssemblyOrderItemPageReqVO reqVO) {
        ErpAssemblyOrderDO order = orderService.getOrder(reqVO.getOrderId());
        if (order == null) {
            throw exception(ASSEMBLY_ORDER_NOT_EXISTS);
        }
        PageResult<ErpAssemblyOrderItemDO> page = orderService.getOrderItemPage(reqVO);
        return success(new PageResult<>(buildItemVOList(order, page.getList()),
                page.getTotal()));
    }

    @PostMapping("/item-page")
    @Operation(summary = "获取套件组装单明细分页（编辑模式）")
    @PreAuthorize("@ss.hasPermission('erp:assembly-order:query')")
    public CommonResult<PageResult<ErpAssemblyOrderRespVO.Item>> getOrderEditItemPage(
            @Valid @RequestBody ErpAssemblyOrderItemPageReqVO reqVO) {
        return getOrderItemPage(reqVO);
    }

    @GetMapping("/component-batch-nos")
    @Operation(summary = "获取组装配件在指定仓库的可用批次")
    @PreAuthorize("@ss.hasAnyPermissions('erp:assembly-order:query', 'erp:assembly-order:create', "
            + "'erp:assembly-order:update', 'erp:assembly-order:complete')")
    public CommonResult<List<ErpStockBatchNoRespVO>> getComponentBatchNoList(
            @RequestParam("productId") Long productId,
            @RequestParam("warehouseId") Long warehouseId) {
        warehouseService.validateCurrentUserWarehousePermission(Collections.singleton(warehouseId));
        productService.validProductList(Collections.singleton(productId));
        return success(DataPermissionUtils.executeIgnore(() ->
                stockService.getAvailableBatchNoList(productId, warehouseId)));
    }

    @GetMapping("/warehouse-option-page")
    @PreAuthorize("@ss.hasAnyPermissions('erp:assembly-order:query', 'erp:assembly-order:create', 'erp:assembly-order:update')")
    public CommonResult<PageResult<Map<String, Object>>> warehouseOptionPage(
            @Valid cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehousePageReqVO req) {
        validatePage(req); req.setStatus(0);
        PageResult<ErpWarehouseDO> page = DataPermissionUtils.executeIgnore(() ->
                warehouseService.hasCurrentUserAllWarehousePermission() ? warehouseMapper.selectPage(req)
                        : warehouseMapper.selectPageByIds(req, warehouseService.getCurrentUserAuthorizedWarehouseIds()));
        List<Map<String, Object>> rows = page.getList().stream().map(row -> {
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("id", row.getId()); result.put("displayName", row.getName()); return result;
        }).collect(Collectors.toList());
        return success(new PageResult<>(rows, page.getTotal()));
    }

    @GetMapping("/recipe-option-page")
    @PreAuthorize("@ss.hasAnyPermissions('erp:assembly-order:create', 'erp:assembly-order:update')")
    public CommonResult<PageResult<ErpAssemblyRecipeRespVO>> recipeOptionPage(@Valid ErpAssemblyRecipePageReqVO req) {
        validatePage(req);
        req.setStatus(1);
        return success(recipeReader.readRecipePage(req));
    }

    @GetMapping("/recipe-header")
    @PreAuthorize("@ss.hasAnyPermissions('erp:assembly-order:create', 'erp:assembly-order:update')")
    public CommonResult<ErpAssemblyRecipeRespVO> recipeHeader(@RequestParam("id") Long id) {
        return success(enabledRecipe(id));
    }

    @GetMapping("/recipe-item-page")
    @PreAuthorize("@ss.hasAnyPermissions('erp:assembly-order:create', 'erp:assembly-order:update')")
    public CommonResult<PageResult<ErpAssemblyRecipeRespVO.Item>> recipeItemPage(@Valid ErpAssemblyRecipeItemPageReqVO req) {
        enabledRecipe(req.getRecipeId());
        return success(recipeReader.readRecipeItemPage(req));
    }

    private ErpAssemblyRecipeRespVO enabledRecipe(Long id) {
        ErpAssemblyRecipeRespVO recipe = recipeReader.readRecipe(id, false);
        if (recipe == null || !Objects.equals(recipe.getStatus(), 1)) {
            throw exception(cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ASSEMBLY_RECIPE_NOT_EXISTS);
        }
        return recipe;
    }

    private void validatePage(PageParam req) {
        if (req.getPageSize() == null || req.getPageSize() < 1 || req.getPageSize() > 100) {
            throw exception(cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ASSEMBLY_ORDER_ITEM_INVALID, "单次最多查询100条");
        }
    }

    @GetMapping("/component-batch-page")
    @PreAuthorize("@ss.hasAnyPermissions('erp:assembly-order:query', 'erp:assembly-order:create', 'erp:assembly-order:update', 'erp:assembly-order:complete')")
    public CommonResult<PageResult<ErpStockBatchNoRespVO>> componentBatchPage(
            @RequestParam("productId") Long productId, @RequestParam("warehouseId") Long warehouseId,
            @Valid PageParam req) {
        validatePage(req);
        warehouseService.validateCurrentUserWarehousePermission(Collections.singleton(warehouseId));
        productService.validProductList(Collections.singleton(productId));
        com.baomidou.mybatisplus.core.metadata.IPage<Map<String, Object>> page = DataPermissionUtils.executeIgnore(() ->
                purchaseInItemMapper.selectAvailableBatchNoPage(new com.baomidou.mybatisplus.extension.plugins.pagination.Page<>(req.getPageNo(), req.getPageSize()),
                        productId, warehouseId, 20, req.getKeyword()));
        List<ErpStockBatchNoRespVO> rows = page.getRecords().stream().map(row -> {
            ErpStockBatchNoRespVO vo = new ErpStockBatchNoRespVO();
            vo.setBatchNo((String) row.get("batch_no"));
            vo.setAvailableCount(new BigDecimal(String.valueOf(row.get("available_count"))));
            return vo;
        }).collect(Collectors.toList());
        return success(new PageResult<>(rows, page.getTotal()));
    }

    @GetMapping("/availability")
    @Operation(summary = "计算组装配方在指定配件来源仓的最大可组装数量")
    @PreAuthorize("@ss.hasAnyPermissions('erp:assembly-order:query', 'erp:assembly-order:create', "
            + "'erp:assembly-order:update', 'erp:assembly-order:complete')")
    public CommonResult<ErpAssemblyAvailabilityRespVO> getAvailability(
            @RequestParam(value = "recipeId", required = false) Long recipeId,
            @RequestParam(value = "orderId", required = false) Long orderId,
            @RequestParam(value = "includeItems", defaultValue = "true") Boolean includeItems,
            @RequestParam(value = "productIds", required = false) List<Long> visibleProductIds,
            @RequestParam(value = "warehouseId", required = false) Long warehouseId,
            @RequestParam(value = "outWarehouseId", required = false) Long legacyOutWarehouseId) {
        Long sourceWarehouseId = warehouseId != null ? warehouseId : legacyOutWarehouseId;
        if (sourceWarehouseId == null) {
            throw exception(cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ASSEMBLY_ORDER_ITEM_INVALID,
                    "配件来源仓不能为空");
        }
        warehouseService.validateCurrentUserWarehousePermission(Collections.singleton(sourceWarehouseId));
        if (visibleProductIds != null && visibleProductIds.size() > 100) {
            throw exception(cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ASSEMBLY_ORDER_ITEM_INVALID, "当前页产品最多100个");
        }
        List<ErpAssemblyRecipeItemDO> recipeItems;
        if (orderId != null) {
            if (orderService.getOrder(orderId) == null) throw exception(ASSEMBLY_ORDER_NOT_EXISTS);
            Map<Long, ErpAssemblyRecipeItemDO> snapshots = new LinkedHashMap<>();
            for (ErpAssemblyOrderItemDO item : orderService.getOrderItemList(orderId)) {
                snapshots.putIfAbsent(item.getRecipeItemId(), new ErpAssemblyRecipeItemDO()
                        .setProductId(item.getProductId()).setQuantityPerSet(item.getQuantityPerSet()));
            }
            recipeItems = new ArrayList<>(snapshots.values());
        } else {
            // Legacy availability also supports an already selected, subsequently disabled recipe.
            if (recipeReader.readRecipe(recipeId, false) == null) {
                throw exception(cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.ASSEMBLY_RECIPE_NOT_EXISTS);
            }
            recipeItems = recipeItemMapper.selectListByRecipeId(recipeId);
        }
        if (CollUtil.isEmpty(recipeItems)) {
            return success(new ErpAssemblyAvailabilityRespVO()
                    .setMaxAssemblableCount(0).setItems(Collections.emptyList()));
        }
        Set<Long> productIds = recipeItems.stream().map(ErpAssemblyRecipeItemDO::getProductId)
                .collect(Collectors.toSet());
        Map<String, ErpStockDO> stockMap = DataPermissionUtils.executeIgnore(() ->
                stockService.getStockMap(productIds, Collections.singleton(sourceWarehouseId)));
        List<ErpAssemblyAvailabilityRespVO.Item> items = new ArrayList<>();
        int maxCount = Integer.MAX_VALUE;
        BigDecimal quantityTotal = BigDecimal.ZERO;
        for (ErpAssemblyRecipeItemDO recipeItem : recipeItems) {
            ErpStockDO stock = stockMap.get(recipeItem.getProductId() + "_" + sourceWarehouseId);
            BigDecimal stockCount = stock == null || stock.getCount() == null ? BigDecimal.ZERO : stock.getCount();
            BigDecimal occupiedCount = stock == null || stock.getLockCount() == null
                    ? BigDecimal.ZERO : stock.getLockCount();
            BigDecimal availableCount = stockCount.subtract(occupiedCount).max(BigDecimal.ZERO);
            BigDecimal quantityPerSet = recipeItem.getQuantityPerSet();
            quantityTotal = quantityTotal.add(quantityPerSet == null ? BigDecimal.ZERO : quantityPerSet);
            int itemMax = quantityPerSet == null || quantityPerSet.signum() <= 0 ? 0
                    : availableCount.divide(quantityPerSet, 0, RoundingMode.FLOOR).min(BigDecimal.valueOf(Integer.MAX_VALUE)).intValue();
            maxCount = Math.min(maxCount, itemMax);
            if (Boolean.TRUE.equals(includeItems) || (visibleProductIds != null && visibleProductIds.contains(recipeItem.getProductId()))) items.add(new ErpAssemblyAvailabilityRespVO.Item()
                    .setProductId(recipeItem.getProductId())
                    .setQuantityPerSet(quantityPerSet)
                    .setStockCount(stockCount)
                    .setOccupiedCount(occupiedCount)
                    .setAvailableCount(availableCount)
                    .setMaxAssemblableCount(itemMax)
                    .setShelf(stock == null ? null : stock.getShelf()));
        }
        return success(new ErpAssemblyAvailabilityRespVO()
                .setQuantityPerSetTotal(quantityTotal)
                .setMaxAssemblableCount(maxCount == Integer.MAX_VALUE ? 0 : maxCount)
                .setItems(items));
    }

    @GetMapping("/user-simple-page")
    @Operation(summary = "获取套件组装经办人分页")
    @PreAuthorize("@ss.hasAnyPermissions('erp:assembly-order:query', 'erp:assembly-order:create', "
            + "'erp:assembly-order:update')")
    public CommonResult<PageResult<UserSimpleRespVO>> getUserSimplePage(@Valid PageParam pageReqVO) {
        validatePage(pageReqVO);
        PageResult<AdminUserRespDTO> page = adminUserApi.getUserSimplePage(
                ENABLE.getStatus(), pageReqVO.getKeyword(), pageReqVO);
        List<UserSimpleRespVO> list = page.getList().stream()
                .map(user -> new UserSimpleRespVO(user.getId(), user.getNickname(), user.getDeptId(), null))
                .collect(Collectors.toList());
        return success(new PageResult<>(list, page.getTotal()));
    }

    private void fillRelations(List<ErpAssemblyOrderRespVO> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        Set<Long> productIds = list.stream().map(ErpAssemblyOrderRespVO::getProductId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Set<Long> warehouseIds = list.stream().map(ErpAssemblyOrderRespVO::getWarehouseId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        warehouseIds.addAll(list.stream().map(ErpAssemblyOrderRespVO::getOutWarehouseId)
                .filter(Objects::nonNull).collect(Collectors.toSet()));
        Set<Long> customerIds = list.stream().map(ErpAssemblyOrderRespVO::getCustomerId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, ErpProductRespVO> productMap = productIds.isEmpty() ? Collections.emptyMap()
                : DataPermissionUtils.executeIgnore(() -> productService.getProductDisplayVOMap(productIds));
        Map<Long, ErpWarehouseDO> warehouseMap = warehouseIds.isEmpty() ? Collections.emptyMap()
                : DataPermissionUtils.executeIgnore(() -> warehouseService.getWarehouseMap(warehouseIds));
        Map<Long, ErpCustomerDO> customerMap = customerIds.isEmpty() ? Collections.emptyMap()
                : DataPermissionUtils.executeIgnore(() -> customerService.getCustomerMap(customerIds));
        Set<Long> userIds = new HashSet<>();
        list.forEach(vo -> {
            addUserId(userIds, vo.getCreator());
            addUserId(userIds, vo.getUpdater());
            if (vo.getCompleteUserId() != null) {
                userIds.add(vo.getCompleteUserId());
            }
            if (vo.getConfirmUserId() != null) userIds.add(vo.getConfirmUserId());
            if (vo.getReverseConfirmUserId() != null) userIds.add(vo.getReverseConfirmUserId());
            if (vo.getStartUserId() != null) userIds.add(vo.getStartUserId());
            if (vo.getCancelStartUserId() != null) userIds.add(vo.getCancelStartUserId());
            if (vo.getReverseAssemblyUserId() != null) userIds.add(vo.getReverseAssemblyUserId());
            if (vo.getHandlerId() != null) {
                userIds.add(vo.getHandlerId());
            }
            if (vo.getVoidUserId() != null) {
                userIds.add(vo.getVoidUserId());
            }
        });
        Map<Long, AdminUserRespDTO> userMap = userIds.isEmpty()
                ? Collections.emptyMap() : adminUserApi.getUserMap(userIds);
        Set<Long> warehouseMoveIds = list.stream().map(ErpAssemblyOrderRespVO::getWarehouseMoveId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, ErpWarehouseMoveDO> warehouseMoveMap = warehouseMoveIds.isEmpty()
                ? Collections.emptyMap() : warehouseMoveService.getWarehouseMoveList(warehouseMoveIds).stream()
                .collect(Collectors.toMap(ErpWarehouseMoveDO::getId, move -> move));
        Set<Long> orderIds = list.stream().map(ErpAssemblyOrderRespVO::getId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, Long> printCountMap = printService.getPrintCountMap(PRINT_MODULE_KEY, orderIds);
        Map<Long, LocalDateTime> printTimeMap = printService.getLastPrintTimeMap(PRINT_MODULE_KEY, orderIds);
        list.forEach(vo -> {
            ErpProductRespVO product = productMap.get(vo.getProductId());
            if (product != null) {
                vo.setProductCode(product.getCode()).setProductName(product.getName())
                        .setProductFactoryCode(product.getFactoryCode()).setProductStandard(product.getStandard())
                        .setProductUnitName(product.getUnitName()).setProductDrawingNo(product.getDrawingNo())
                        .setProductOriginPlace(product.getOriginPlace()).setProductFeatureCode(product.getFeatureCode())
                        .setProductBrand(product.getBrand()).setProductVehicleModel(product.getVehicleModel())
                        .setProductRetailPrice(product.getRetailPrice()).setProductShelf(product.getShelf())
                        .setBatchNoEnabled(product.getBatchNoEnabled());
            }
            ErpWarehouseDO warehouse = warehouseMap.get(vo.getWarehouseId());
            if (warehouse != null) {
                vo.setWarehouseName(warehouse.getName());
            }
            ErpWarehouseDO outWarehouse = warehouseMap.get(vo.getOutWarehouseId());
            if (outWarehouse != null) {
                vo.setOutWarehouseName(outWarehouse.getName());
            }
            ErpWarehouseMoveDO warehouseMove = warehouseMoveMap.get(vo.getWarehouseMoveId());
            if (warehouseMove != null) {
                vo.setWarehouseMoveNo(warehouseMove.getNo())
                        .setWarehouseMoveStatus(warehouseMove.getStatus());
            }
            ErpCustomerDO customer = customerMap.get(vo.getCustomerId());
            if (customer != null) {
                vo.setCustomerName(customer.getName());
            }
            AdminUserRespDTO handler = userMap.get(vo.getHandlerId());
            if (handler != null) {
                vo.setHandlerName(handler.getNickname());
            }
            AdminUserRespDTO creator = userMap.get(parseUserId(vo.getCreator()));
            if (creator != null) {
                vo.setCreatorName(creator.getNickname());
            }
            AdminUserRespDTO updater = userMap.get(parseUserId(vo.getUpdater()));
            if (updater != null) {
                vo.setUpdaterName(updater.getNickname());
            }
            AdminUserRespDTO completeUser = userMap.get(vo.getCompleteUserId());
            if (completeUser != null) {
                vo.setCompleteUserName(completeUser.getNickname());
            }
            AdminUserRespDTO confirmUser = userMap.get(vo.getConfirmUserId());
            if (confirmUser != null) vo.setConfirmUserName(confirmUser.getNickname());
            AdminUserRespDTO reverseConfirmUser = userMap.get(vo.getReverseConfirmUserId());
            if (reverseConfirmUser != null) vo.setReverseConfirmUserName(reverseConfirmUser.getNickname());
            AdminUserRespDTO startUser = userMap.get(vo.getStartUserId());
            if (startUser != null) vo.setStartUserName(startUser.getNickname());
            AdminUserRespDTO cancelStartUser = userMap.get(vo.getCancelStartUserId());
            if (cancelStartUser != null) vo.setCancelStartUserName(cancelStartUser.getNickname());
            AdminUserRespDTO reverseAssemblyUser = userMap.get(vo.getReverseAssemblyUserId());
            if (reverseAssemblyUser != null) vo.setReverseAssemblyUserName(reverseAssemblyUser.getNickname());
            AdminUserRespDTO voidUser = userMap.get(vo.getVoidUserId());
            if (voidUser != null) {
                vo.setVoidUserName(voidUser.getNickname());
            }
            Long printCount = printCountMap.get(vo.getId());
            vo.setPrintCount(printCount == null ? 0 : printCount.intValue());
            vo.setPrintTime(printTimeMap.get(vo.getId()));
        });
    }

    private List<ErpAssemblyOrderRespVO.Item> buildItemVOList(
            ErpAssemblyOrderDO order, List<ErpAssemblyOrderItemDO> items) {
        if (CollUtil.isEmpty(items)) {
            return Collections.emptyList();
        }
        List<ErpAssemblyOrderRespVO.Item> result = BeanUtils.toBean(items, ErpAssemblyOrderRespVO.Item.class);
        Set<Long> productIds = items.stream().map(ErpAssemblyOrderItemDO::getProductId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, ErpProductRespVO> productMap = DataPermissionUtils.executeIgnore(() ->
                productService.getProductDisplayVOMap(productIds));
        Long sourceWarehouseId = order.getOutWarehouseId() != null
                ? order.getOutWarehouseId() : order.getWarehouseId();
        Map<String, ErpStockDO> stockMap = sourceWarehouseId == null
                ? Collections.emptyMap() : DataPermissionUtils.executeIgnore(() ->
                stockService.getStockMap(productIds, Collections.singleton(sourceWarehouseId)));
        result.forEach(item -> {
            ErpProductRespVO product = productMap.get(item.getProductId());
            if (product != null) {
                item.setProductDrawingNo(product.getDrawingNo())
                        .setProductOriginPlace(product.getOriginPlace())
                        .setProductBrand(product.getBrand())
                        .setProductVehicleModel(product.getVehicleModel())
                        .setProductFeatureCode(product.getFeatureCode())
                        .setProductRetailPrice(product.getRetailPrice());
            }
            ErpStockDO stock = stockMap.get(item.getProductId() + "_" + sourceWarehouseId);
            BigDecimal stockCount = stock == null || stock.getCount() == null ? BigDecimal.ZERO : stock.getCount();
            BigDecimal occupiedCount = stock == null || stock.getLockCount() == null
                    ? BigDecimal.ZERO : stock.getLockCount();
            BigDecimal available = stockCount.subtract(occupiedCount).max(BigDecimal.ZERO);
            item.setStockCount(stockCount)
                    .setOccupiedCount(occupiedCount)
                    .setShortageCount(item.getRequiredQuantity() == null ? BigDecimal.ZERO
                            : item.getRequiredQuantity().subtract(available).max(BigDecimal.ZERO));
        });
        return result;
    }

    private String statusName(Integer status) {
        if (Objects.equals(status, 15)) {
            return "已确认";
        }
        if (Objects.equals(status, 18)) {
            return "装配中";
        }
        if (Objects.equals(status, 20)) {
            return "已完成";
        }
        if (Objects.equals(status, 30)) {
            return "历史已作废";
        }
        return "草稿";
    }

    private void addUserId(Set<Long> userIds, String value) {
        Long userId = parseUserId(value);
        if (userId != null) {
            userIds.add(userId);
        }
    }

    private Long parseUserId(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private Map<Long, Integer> toCountMap(List<Map<String, Object>> rows) {
        Map<Long, Integer> result = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Object id = row.get("orderId");
            if (id == null) id = row.get("orderid");
            Object count = row.get("itemCount");
            if (count == null) count = row.get("itemcount");
            if (id != null && count != null) {
                result.put(Long.valueOf(String.valueOf(id)), Integer.valueOf(String.valueOf(count)));
            }
        }
        return result;
    }

}
