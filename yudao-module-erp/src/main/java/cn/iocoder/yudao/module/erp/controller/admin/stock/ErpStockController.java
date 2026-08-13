package cn.iocoder.yudao.module.erp.controller.admin.stock;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockAdjustReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockBatchNoRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockInTransitDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockOccupiedDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockPendingInDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockSummaryRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock.ErpStockUpdateReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockBatchQuantityDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.purchase.ErpPurchaseOrderItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleCartItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutItemMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockBatchQuantityMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockOccupiedDetailMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockPendingInDetailMapper;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleCartStatusEnum;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockCheckTypeEnum;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockTransferDirectionEnum;
import cn.iocoder.yudao.module.erp.service.product.ErpProductPriceSystemService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.config.ErpStockSelectPriceConfigService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils.getLoginUserDeptId;

@Tag(name = "管理后台 - ERP 产品库存")
@RestController
@RequestMapping("/erp/stock")
@Validated
public class ErpStockController {

    private static final Set<String> STOCK_PRICE_ORDER_FIELDS = new LinkedHashSet<>(Arrays.asList(
            "costPrice", "costAmount", "purchasePrice", "productPurchasePrice", "lastPurchasePrice",
            "lastSalePrice", "salePrice", "minPrice", "referencePrice", "retailPrice", "grossProfitRate",
            "backupPrice1", "wholesalePrice", "sharePrice", "currentPrice", "currentPriceAmount"));

    @Resource
    private ErpStockService stockService;
    @Resource
    private ErpStockMapper stockMapper;
    @Resource
    private ErpStockBatchQuantityMapper stockBatchQuantityMapper;
    @Resource
    private ErpProductService productService;
    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private ErpStockPendingInDetailMapper stockPendingInDetailMapper;
    @Resource
    private ErpStockOccupiedDetailMapper stockOccupiedDetailMapper;
    @Resource
    private ErpPurchaseOrderItemMapper purchaseOrderItemMapper;
    @Resource
    private ErpSaleCartItemMapper saleCartItemMapper;
    @Resource
    private ErpSaleOutItemMapper saleOutItemMapper;
    @Resource
    private ErpProductPriceSystemService productPriceSystemService;
    @Resource
    private DeptApi deptApi;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private PermissionApi permissionApi;
    @Resource
    private ErpStockSelectPriceConfigService stockSelectPriceConfigService;

    @GetMapping("/get")
    @Operation(summary = "获得产品库存")
    @Parameters({
            @Parameter(name = "id", description = "编号", example = "1"), // 方案一：传递 id
            @Parameter(name = "productId", description = "产品编号", example = "10"), // 方案二：传递 productId + warehouseId
            @Parameter(name = "warehouseId", description = "仓库编号", example = "2")
    })
    @PreAuthorize("@ss.hasPermission('erp:stock:query')")
    public CommonResult<ErpStockRespVO> getStock(@RequestParam(value = "id", required = false) Long id,
                                                 @RequestParam(value = "productId", required = false) Long productId,
                                                 @RequestParam(value = "warehouseId", required = false) Long warehouseId) {
        ErpStockDO stock = DataPermissionUtils.executeIgnore(() ->
                id != null ? stockService.getStock(id) : stockService.getStock(productId, warehouseId));
        if (stock == null) {
            return success(null);
        }
        warehouseService.validateCurrentUserStockPermission(Collections.singleton(stock));
        return success(buildStockVOPageResult(new PageResult<>(Collections.singletonList(stock), 1L), null).getList().get(0));
    }

    @GetMapping("/get-count")
    @Operation(summary = "获得产品库存数量")
    @Parameters({
            @Parameter(name = "productId", description = "产品编号", example = "10"),
            @Parameter(name = "warehouseId", description = "仓库编号", example = "2"),
            @Parameter(name = "bizType", description = "业务类型：sale 时按销售可见仓库查询", example = "sale")
    })
    public CommonResult<BigDecimal> getStockCount(@RequestParam("productId") Long productId,
                                                  @RequestParam(value = "warehouseId", required = false) Long warehouseId,
                                                  @RequestParam(value = "bizType", required = false) String bizType) {
        if (warehouseId != null && isSaleBizType(bizType)) {
            warehouseService.validSaleWarehouseList(Collections.singleton(warehouseId));
            return success(DataPermissionUtils.executeIgnore(() -> stockService.getStockCount(productId, warehouseId)));
        }
        return success(warehouseId != null ? stockService.getStockCount(productId, warehouseId)
                : stockService.getStockCount(productId));
    }

    private boolean isSaleBizType(String bizType) {
        return "sale".equalsIgnoreCase(bizType);
    }

    @GetMapping("/batch-nos")
    @Operation(summary = "获得产品库存可用批次号列表")
    @Parameters({
            @Parameter(name = "productId", description = "产品编号", required = true, example = "10"),
            @Parameter(name = "warehouseId", description = "仓库编号", required = true, example = "2"),
            @Parameter(name = "bizType", description = "业务类型：sale 时按销售可见仓库查询", example = "sale")
    })
    @PreAuthorize("@ss.hasPermission('erp:stock:query')")
    public CommonResult<List<ErpStockBatchNoRespVO>> getAvailableBatchNoList(@RequestParam("productId") Long productId,
                                                                             @RequestParam("warehouseId") Long warehouseId,
                                                                             @RequestParam(value = "bizType", required = false) String bizType) {
        if (isSaleBizType(bizType)) {
            warehouseService.validSaleWarehouseList(Collections.singleton(warehouseId));
        } else {
            validateCurrentUserStockAccess(productId, warehouseId);
        }
        return success(DataPermissionUtils.executeIgnore(() ->
                stockService.getAvailableBatchNoList(productId, warehouseId)));
    }

    @GetMapping("/page")
    @Operation(summary = "获得产品库存分页")
    @PreAuthorize("@ss.hasPermission('erp:stock:query')")
    public CommonResult<PageResult<ErpStockRespVO>> getStockPage(@Valid ErpStockPageReqVO pageReqVO) {
        return success(getStockVOPageResult(pageReqVO));
    }

    @GetMapping("/summary")
    @Operation(summary = "获得产品库存汇总")
    @PreAuthorize("@ss.hasPermission('erp:stock:query')")
    public CommonResult<ErpStockSummaryRespVO> getStockSummary(@Valid ErpStockPageReqVO pageReqVO) {
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        PageResult<ErpStockRespVO> pageResult = getStockVOPageResult(pageReqVO, false);
        return success(buildStockSummary(pageResult, pageReqVO.getPriceSystemId() != null));
    }

    @GetMapping("/in-transit-details")
    @Operation(summary = "获得产品库存在途明细")
    @Parameters({
            @Parameter(name = "productId", description = "产品编号", required = true, example = "10"),
            @Parameter(name = "warehouseId", description = "仓库编号", required = true, example = "2")
    })
    @PreAuthorize("@ss.hasPermission('erp:stock:query')")
    public CommonResult<List<ErpStockInTransitDetailRespVO>> getInTransitDetails(
            @RequestParam("productId") Long productId,
            @RequestParam("warehouseId") Long warehouseId,
            @RequestParam(value = "batchNo", required = false) String batchNo,
            @RequestParam(value = "unassignedBatch", required = false) Boolean unassignedBatch) {
        validateBatchFilter(batchNo, unassignedBatch);
        validateCurrentUserStockAccess(productId, warehouseId);
        List<ErpStockInTransitDetailRespVO> list = DataPermissionUtils.executeIgnore(() ->
                purchaseOrderItemMapper.selectInTransitDetails(productId, warehouseId,
                        Collections.singletonList(ErpAuditStatus.APPROVE.getStatus()), batchNo, unassignedBatch));
        fillCreatorNames(list);
        return success(list);
    }

    @GetMapping("/pending-in-details")
    @Operation(summary = "Get ERP stock pending-in details")
    @Parameters({
            @Parameter(name = "productId", description = "Product id", required = true, example = "10"),
            @Parameter(name = "warehouseId", description = "Warehouse id", required = true, example = "2")
    })
    @PreAuthorize("@ss.hasPermission('erp:stock:query')")
    public CommonResult<List<ErpStockPendingInDetailRespVO>> getPendingInDetails(
            @RequestParam("productId") Long productId,
            @RequestParam("warehouseId") Long warehouseId,
            @RequestParam(value = "batchNo", required = false) String batchNo,
            @RequestParam(value = "unassignedBatch", required = false) Boolean unassignedBatch) {
        validateBatchFilter(batchNo, unassignedBatch);
        validateCurrentUserStockAccess(productId, warehouseId);
        List<ErpStockPendingInDetailRespVO> list = DataPermissionUtils.executeIgnore(() ->
                stockPendingInDetailMapper.selectList(productId, warehouseId,
                        ErpAuditStatus.PROCESS.getStatus(), ErpStockCheckTypeEnum.COUNT.getType(),
                        ErpStockTransferDirectionEnum.TRANSFER_IN.getDirection(), batchNo, unassignedBatch));
        fillCreatorNames(list, ErpStockPendingInDetailRespVO::getCreator,
                ErpStockPendingInDetailRespVO::setCreatorName);
        return success(list);
    }

    @GetMapping("/occupied-details")
    @Operation(summary = "Get ERP stock occupied details")
    @Parameters({
            @Parameter(name = "productId", description = "Product id", required = true, example = "10"),
            @Parameter(name = "warehouseId", description = "Warehouse id", required = true, example = "2")
    })
    @PreAuthorize("@ss.hasPermission('erp:stock:query')")
    public CommonResult<List<ErpStockOccupiedDetailRespVO>> getOccupiedDetails(
            @RequestParam("productId") Long productId,
            @RequestParam("warehouseId") Long warehouseId,
            @RequestParam(value = "batchNo", required = false) String batchNo,
            @RequestParam(value = "unassignedBatch", required = false) Boolean unassignedBatch) {
        validateBatchFilter(batchNo, unassignedBatch);
        validateCurrentUserStockAccess(productId, warehouseId);
        List<ErpStockOccupiedDetailRespVO> list = DataPermissionUtils.executeIgnore(() ->
                stockOccupiedDetailMapper.selectList(productId, warehouseId,
                        ErpAuditStatus.PROCESS.getStatus(), Arrays.asList(ErpSaleCartStatusEnum.PROCESS.getStatus(),
                                ErpSaleCartStatusEnum.SUBMITTED.getStatus(),
                        ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus()),
                        ErpStockCheckTypeEnum.COUNT.getType(),
                        ErpStockTransferDirectionEnum.TRANSFER_OUT.getDirection(), batchNo, unassignedBatch));
        fillCreatorNames(list, ErpStockOccupiedDetailRespVO::getCreator,
                ErpStockOccupiedDetailRespVO::setCreatorName);
        return success(list);
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出产品库存 Excel")
    @PreAuthorize("@ss.hasPermission('erp:stock:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportStockExcel(@Valid ErpStockPageReqVO pageReqVO,
              HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ErpStockRespVO> list = getStockVOPageResult(pageReqVO, false).getList();
        // 导出 Excel
        ExcelUtils.write(response, "产品库存.xls", "数据", ErpStockRespVO.class, list);
    }

    @PutMapping("/adjust")
    @Operation(summary = "手动调整库存数量（盘点调整）")
    @PreAuthorize("@ss.hasPermission('erp:stock:adjust')")
    public CommonResult<BigDecimal> adjustStock(@Valid @RequestBody ErpStockAdjustReqVO reqVO) {
        validateCurrentUserStockAccess(reqVO.getProductId(), reqVO.getWarehouseId());
        return success(stockService.adjustStock(reqVO));
    }

    @PutMapping("/update-shelf")
    @Operation(summary = "更新产品库存货架")
    @PreAuthorize("@ss.hasPermission('erp:stock:update')")
    public CommonResult<Boolean> updateStockShelf(@RequestParam("ids") List<Long> ids,
                                                  @RequestParam(value = "shelf", required = false) String shelf) {
        List<ErpStockDO> stocks = DataPermissionUtils.executeIgnore(() -> stockMapper.selectByIds(ids));
        if (stocks.size() != ids.stream().filter(java.util.Objects::nonNull).distinct().count()) {
            throw new IllegalArgumentException("Product stock does not exist or is not accessible");
        }
        warehouseService.validateCurrentUserStockPermission(stocks);
        stockService.updateStockShelf(ids, shelf);
        return success(true);
    }

    @PutMapping("/update")
    @Operation(summary = "更新产品库存可编辑字段")
    @PreAuthorize("@ss.hasPermission('erp:stock:update')")
    public CommonResult<ErpStockRespVO> updateStock(@Valid @RequestBody ErpStockUpdateReqVO reqVO) {
        ErpStockDO existing = DataPermissionUtils.executeIgnore(() -> stockService.getStock(reqVO.getId()));
        if (existing == null) {
            throw new IllegalArgumentException("Product stock does not exist or is not accessible");
        }
        warehouseService.validateCurrentUserStockPermission(Collections.singleton(existing));
        ErpStockDO stock = stockService.updateStockEditableFields(reqVO);
        return success(buildStockVOPageResult(new PageResult<>(Collections.singletonList(stock), 1L), null)
                .getList().get(0));
    }

    private PageResult<ErpStockRespVO> buildStockVOPageResult(PageResult<ErpStockDO> pageResult, Long priceSystemId) {
        Long pricePermissionDeptId = getLoginUserDeptId();
        return buildStockVOPageResult(pageResult, priceSystemId, true, pricePermissionDeptId,
                getHiddenPriceFieldSet(pricePermissionDeptId));
    }

    private PageResult<ErpStockRespVO> getStockVOPageResult(ErpStockPageReqVO pageReqVO) {
        return getStockVOPageResult(pageReqVO, true);
    }

    private PageResult<ErpStockRespVO> getStockVOPageResult(ErpStockPageReqVO pageReqVO,
                                                            boolean includeBatchNoSummary) {
        Long pricePermissionDeptId = resolvePricePermissionDeptId(pageReqVO);
        Set<String> hiddenPriceFields = getHiddenPriceFieldSet(
                pricePermissionDeptId, pageReqVO.getBizType());
        sanitizePriceSort(pageReqVO, hiddenPriceFields);
        PageResult<ErpStockRespVO> result;
        if (Boolean.TRUE.equals(pageReqVO.getShowBatchNo())) {
            result = buildBatchStockVOPageResult(pageReqVO, pricePermissionDeptId, hiddenPriceFields);
        } else {
            result = buildStockVOPageResult(stockService.getStockPage(pageReqVO), pageReqVO.getPriceSystemId(),
                    includeBatchNoSummary, pricePermissionDeptId, hiddenPriceFields);
        }
        result.getList().forEach(stock -> stock.setPriceVisible(true));
        return result;
    }

    private void sanitizePriceSort(ErpStockPageReqVO pageReqVO, Set<String> hiddenPriceFields) {
        String orderField = pageReqVO.getOrderField();
        if (STOCK_PRICE_ORDER_FIELDS.contains(orderField)
                && isStockPriceFieldHidden(hiddenPriceFields, orderField)) {
            pageReqVO.setOrderField(null);
            pageReqVO.setOrderDirection(null);
        }
    }

    private Long resolveSalePriceDeptId(ErpStockPageReqVO pageReqVO) {
        return pageReqVO.getSaleDeptId() != null ? pageReqVO.getSaleDeptId() : getLoginUserDeptId();
    }

    private Long resolvePricePermissionDeptId(ErpStockPageReqVO pageReqVO) {
        return isSaleBizType(pageReqVO.getBizType())
                ? resolveSalePriceDeptId(pageReqVO) : getLoginUserDeptId();
    }

    /**
     * 批次展开视图仍复用原库存查询的产品、仓库与权限过滤，仅将数量条件延后到批次行应用。
     * 批次余额来自库存流水；流水无法识别的差额统一归入“未指定批次”，保证批次行合计等于库存主表。
     */
    private PageResult<ErpStockRespVO> buildBatchStockVOPageResult(ErpStockPageReqVO pageReqVO,
                                                                  Long pricePermissionDeptId,
                                                                  Set<String> hiddenPriceFields) {
        boolean fullBatchPage = PageParam.PAGE_SIZE_NONE.equals(pageReqVO.getPageSize());
        ErpStockPageReqVO baseReqVO = BeanUtils.toBean(pageReqVO, ErpStockPageReqVO.class);
        if (fullBatchPage) {
            baseReqVO.setPageNo(1);
            baseReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        }
        clearBatchCountFilters(baseReqVO);
        PageResult<ErpStockDO> stockPageResult = stockService.getStockPage(baseReqVO);
        if (CollUtil.isEmpty(stockPageResult.getList())) {
            return PageResult.empty(0L);
        }
        Map<String, List<ErpStockBatchNoRespVO>> batchBalanceMap =
                stockService.getStockBatchBalanceListMap(stockPageResult.getList());
        PageResult<ErpStockRespVO> aggregatePageResult = buildStockVOPageResult(
                stockPageResult, pageReqVO.getPriceSystemId(), false, pricePermissionDeptId, hiddenPriceFields);
        Set<Long> productIds = convertSet(stockPageResult.getList(), ErpStockDO::getProductId);
        Set<Long> warehouseIds = convertSet(stockPageResult.getList(), ErpStockDO::getWarehouseId);
        List<ErpStockBatchQuantityDO> occupiedList = DataPermissionUtils.executeIgnore(() ->
                stockBatchQuantityMapper.selectOccupiedList(productIds, warehouseIds,
                        ErpAuditStatus.PROCESS.getStatus(), Arrays.asList(ErpSaleCartStatusEnum.PROCESS.getStatus(),
                                ErpSaleCartStatusEnum.SUBMITTED.getStatus(),
                                ErpSaleCartStatusEnum.FIRST_APPROVE.getStatus()),
                        ErpStockCheckTypeEnum.COUNT.getType(),
                        ErpStockTransferDirectionEnum.TRANSFER_OUT.getDirection()));
        List<ErpStockBatchQuantityDO> pendingInList = DataPermissionUtils.executeIgnore(() ->
                stockBatchQuantityMapper.selectPendingInList(productIds, warehouseIds,
                        ErpAuditStatus.PROCESS.getStatus(), ErpStockCheckTypeEnum.COUNT.getType(),
                        ErpStockTransferDirectionEnum.TRANSFER_IN.getDirection()));
        List<ErpStockBatchQuantityDO> inTransitList = DataPermissionUtils.executeIgnore(() ->
                stockBatchQuantityMapper.selectInTransitList(productIds, warehouseIds,
                        Collections.singletonList(ErpAuditStatus.APPROVE.getStatus())));
        Map<String, BigDecimal> batchOccupiedMap = buildBatchQuantityMap(occupiedList);
        Map<String, BigDecimal> batchPendingInMap = buildBatchQuantityMap(pendingInList);
        Map<String, BigDecimal> batchInTransitMap = buildBatchQuantityMap(inTransitList);
        Map<String, Set<String>> associatedBatchNoMap = buildAssociatedBatchNoMap(
                occupiedList, pendingInList, inTransitList);
        List<ErpStockRespVO> rows = new ArrayList<>();
        for (ErpStockRespVO stock : aggregatePageResult.getList()) {
            if (!Boolean.TRUE.equals(stock.getBatchNoEnabled())) {
                stock.setRowKey(buildStockRowKey(stock))
                        .setBatchRow(false)
                        .setBatchNo(null)
                        .setFirstInTime(null);
                if (matchesBatchCountFilters(stock.getCount(), pageReqVO)) {
                    rows.add(stock);
                }
                continue;
            }
            List<ErpStockBatchNoRespVO> balances = batchBalanceMap.getOrDefault(
                    buildBatchNoMapKey(stock.getProductId(), stock.getWarehouseId()), Collections.emptyList());
            BigDecimal namedBatchTotal = BigDecimal.ZERO;
            Map<String, ErpStockBatchNoRespVO> namedBatchMap = new LinkedHashMap<>();
            for (ErpStockBatchNoRespVO balance : balances) {
                if (balance.getBatchNo() == null) {
                    continue;
                }
                namedBatchTotal = namedBatchTotal.add(defaultZero(balance.getAvailableCount()));
                namedBatchMap.put(balance.getBatchNo(), balance);
            }
            Set<String> associatedBatchNos = associatedBatchNoMap.getOrDefault(
                    buildBatchNoMapKey(stock.getProductId(), stock.getWarehouseId()), Collections.emptySet());
            for (String batchNo : associatedBatchNos) {
                if (batchNo != null) {
                    namedBatchMap.computeIfAbsent(batchNo, key -> new ErpStockBatchNoRespVO()
                            .setBatchNo(key).setAvailableCount(BigDecimal.ZERO));
                }
            }
            for (ErpStockBatchNoRespVO balance : namedBatchMap.values()) {
                ErpStockRespVO batchRow = buildBatchStockRow(stock, balance.getBatchNo(),
                        balance.getAvailableCount(), balance.getFirstInTime(), batchOccupiedMap,
                        batchPendingInMap, batchInTransitMap);
                if (matchesBatchCountFilters(batchRow.getCount(), pageReqVO)) {
                    rows.add(batchRow);
                }
            }
            BigDecimal unassignedCount = defaultZero(stock.getCount()).subtract(namedBatchTotal);
            boolean hasUnassignedQuantity = associatedBatchNos.contains(null);
            if (unassignedCount.signum() != 0 || namedBatchMap.isEmpty() || hasUnassignedQuantity) {
                ErpStockRespVO unassignedRow = buildBatchStockRow(stock, null, unassignedCount, null,
                        batchOccupiedMap, batchPendingInMap, batchInTransitMap);
                if (matchesBatchCountFilters(unassignedRow.getCount(), pageReqVO)) {
                    rows.add(unassignedRow);
                }
            }
        }
        sortBatchRowsIfNecessary(rows, pageReqVO);
        if (!fullBatchPage) {
            return new PageResult<>(rows, stockPageResult.getTotal());
        }
        return paginateBatchRows(rows, pageReqVO);
    }

    private void clearBatchCountFilters(ErpStockPageReqVO reqVO) {
        reqVO.setCountMin(null);
        reqVO.setCountMax(null);
        reqVO.setCountFilter(null);
        reqVO.setPositiveCountOnly(null);
    }

    private ErpStockRespVO buildBatchStockRow(ErpStockRespVO stock, String batchNo, BigDecimal count,
                                               java.time.LocalDateTime firstInTime,
                                               Map<String, BigDecimal> occupiedMap,
                                               Map<String, BigDecimal> pendingInMap,
                                               Map<String, BigDecimal> inTransitMap) {
        ErpStockRespVO row = BeanUtils.toBean(stock, ErpStockRespVO.class);
        BigDecimal normalizedCount = defaultZero(count);
        String quantityKey = buildBatchQuantityKey(stock.getProductId(), stock.getWarehouseId(), batchNo);
        BigDecimal occupied = occupiedMap.getOrDefault(quantityKey, BigDecimal.ZERO);
        row.setRowKey(buildStockRowKey(stock) + ":batch:" + (batchNo != null ? batchNo : "__UNASSIGNED__"))
                .setBatchRow(true)
                .setBatchNo(batchNo)
                .setFirstInTime(firstInTime)
                .setBatchNoSummary(batchNo)
                .setBatchNoList(Collections.emptyList())
                .setCount(normalizedCount)
                .setOccupiedCount(occupied)
                .setAvailableCount(normalizedCount.subtract(occupied))
                .setPendingInCount(pendingInMap.getOrDefault(quantityKey, BigDecimal.ZERO))
                .setInTransitCount(inTransitMap.getOrDefault(quantityKey, BigDecimal.ZERO))
                .setCostAmount(multiplyNullable(stock.getCostPrice(), normalizedCount))
                .setCurrentPriceAmount(multiplyNullable(stock.getCurrentPrice(), normalizedCount));
        return row;
    }

    private Map<String, BigDecimal> buildBatchQuantityMap(List<ErpStockBatchQuantityDO> quantities) {
        Map<String, BigDecimal> result = new java.util.HashMap<>();
        if (CollUtil.isEmpty(quantities)) {
            return result;
        }
        for (ErpStockBatchQuantityDO quantity : quantities) {
            result.merge(buildBatchQuantityKey(quantity.getProductId(), quantity.getWarehouseId(),
                    normalizeBatchNo(quantity.getBatchNo())), defaultZero(quantity.getCount()), BigDecimal::add);
        }
        return result;
    }

    @SafeVarargs
    private final Map<String, Set<String>> buildAssociatedBatchNoMap(List<ErpStockBatchQuantityDO>... lists) {
        Map<String, Set<String>> result = new java.util.HashMap<>();
        for (List<ErpStockBatchQuantityDO> list : lists) {
            if (CollUtil.isEmpty(list)) {
                continue;
            }
            for (ErpStockBatchQuantityDO quantity : list) {
                result.computeIfAbsent(buildBatchNoMapKey(quantity.getProductId(), quantity.getWarehouseId()),
                                key -> new LinkedHashSet<>())
                        .add(normalizeBatchNo(quantity.getBatchNo()));
            }
        }
        return result;
    }

    private String buildBatchQuantityKey(Long productId, Long warehouseId, String batchNo) {
        String normalizedBatchNo = normalizeBatchNo(batchNo);
        return productId + "_" + warehouseId + "_" +
                (normalizedBatchNo != null ? normalizedBatchNo : "__UNASSIGNED__");
    }

    private String normalizeBatchNo(String batchNo) {
        if (batchNo == null || batchNo.trim().isEmpty()) {
            return null;
        }
        return batchNo.trim();
    }

    private BigDecimal multiplyNullable(BigDecimal unitPrice, BigDecimal count) {
        return unitPrice != null ? unitPrice.multiply(defaultZero(count)) : null;
    }

    private boolean matchesBatchCountFilters(BigDecimal value, ErpStockPageReqVO reqVO) {
        BigDecimal count = defaultZero(value);
        if (reqVO.getCountMin() != null && count.compareTo(reqVO.getCountMin()) < 0) {
            return false;
        }
        if (reqVO.getCountMax() != null && count.compareTo(reqVO.getCountMax()) > 0) {
            return false;
        }
        if ((Integer.valueOf(1).equals(reqVO.getCountFilter()) || Boolean.TRUE.equals(reqVO.getPositiveCountOnly()))
                && count.signum() <= 0) {
            return false;
        }
        return !Integer.valueOf(2).equals(reqVO.getCountFilter()) || count.signum() == 0;
    }

    private void sortBatchRowsIfNecessary(List<ErpStockRespVO> rows, ErpStockPageReqVO reqVO) {
        String field = reqVO.getOrderField();
        if (field == null || !("batchNo".equals(field) || "batchNoSummary".equals(field)
                || "count".equals(field) || "costAmount".equals(field)
                || "availableCount".equals(field) || "currentPriceAmount".equals(field))) {
            return;
        }
        boolean descending = "desc".equalsIgnoreCase(reqVO.getOrderDirection());
        rows.sort((first, second) -> {
            Comparable<?> left = resolveBatchSortValue(first, field);
            Comparable<?> right = resolveBatchSortValue(second, field);
            int result = compareNullable(left, right);
            if (descending && left != null && right != null) {
                result = -result;
            }
            if (result != 0) {
                return result;
            }
            return String.valueOf(first.getRowKey()).compareTo(String.valueOf(second.getRowKey()));
        });
    }

    private Comparable<?> resolveBatchSortValue(ErpStockRespVO row, String field) {
        switch (field) {
            case "batchNo":
            case "batchNoSummary":
                return row.getBatchNo() != null ? row.getBatchNo().toLowerCase(java.util.Locale.ROOT) : null;
            case "count":
                return row.getCount();
            case "costAmount":
                return row.getCostAmount();
            case "availableCount":
                return row.getAvailableCount();
            case "currentPriceAmount":
                return row.getCurrentPriceAmount();
            default:
                return null;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private int compareNullable(Comparable left, Comparable right) {
        if (left == null) {
            return right == null ? 0 : 1;
        }
        if (right == null) {
            return -1;
        }
        return left.compareTo(right);
    }

    private PageResult<ErpStockRespVO> paginateBatchRows(List<ErpStockRespVO> rows,
                                                         ErpStockPageReqVO reqVO) {
        long total = rows.size();
        if (PageParam.PAGE_SIZE_NONE.equals(reqVO.getPageSize())) {
            return new PageResult<>(rows, total);
        }
        int pageNo = reqVO.getPageNo() != null ? reqVO.getPageNo() : 1;
        int pageSize = reqVO.getPageSize() != null ? reqVO.getPageSize() : 10;
        long fromLong = (long) (pageNo - 1) * pageSize;
        if (fromLong >= total) {
            return PageResult.empty(total);
        }
        int fromIndex = (int) fromLong;
        int toIndex = (int) Math.min(fromLong + pageSize, total);
        return new PageResult<>(new ArrayList<>(rows.subList(fromIndex, toIndex)), total);
    }

    private String buildStockRowKey(ErpStockRespVO stock) {
        if (stock.getId() != null) {
            return "stock:" + stock.getId();
        }
        return "stock:" + stock.getProductId() + ":" + stock.getWarehouseId();
    }

    private PageResult<ErpStockRespVO> buildStockVOPageResult(PageResult<ErpStockDO> pageResult, Long priceSystemId,
                                                             boolean includeBatchNo,
                                                             Long pricePermissionDeptId,
                                                             Set<String> hiddenPriceFields) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        Set<Long> productIds = convertSet(pageResult.getList(), ErpStockDO::getProductId);
        Set<Long> warehouseIds = convertSet(pageResult.getList(), ErpStockDO::getWarehouseId);
        Map<Long, ErpProductRespVO> productMap = DataPermissionUtils.executeIgnore(() ->
                pricePermissionDeptId != null
                        ? productService.getProductVOMap(productIds, pricePermissionDeptId)
                        : productService.getProductVOMap(productIds));
        Map<Long, ErpWarehouseDO> warehouseMap = DataPermissionUtils.executeIgnore(
                () -> warehouseService.getWarehouseMap(warehouseIds));
        // 库存主列表已按当前业务可见仓库完成过滤；这里必须沿用同一批仓库，避免跨部门授权仓库
        // 被通用部门数据权限再次过滤，导致占用数显示为 0、可用数被高估。
        Map<String, BigDecimal> occupiedMap = DataPermissionUtils.executeIgnore(
                () -> stockMapper.selectOccupiedCountMap(productIds, warehouseIds));
        Set<Long> deptIds = convertSet(pageResult.getList(), ErpStockDO::getDeptId);
        deptIds.addAll(convertSet(warehouseMap.values(), ErpWarehouseDO::getDeptId));
        deptIds.remove(null);
        Map<Long, DeptRespDTO> deptMap = CollUtil.isEmpty(deptIds)
                ? Collections.emptyMap() : DataPermissionUtils.executeIgnore(() -> deptApi.getDeptMap(deptIds));
        Map<String, BigDecimal> pendingInMap = DataPermissionUtils.executeIgnore(
                () -> stockMapper.selectPendingInCountMap(productIds, warehouseIds));
        Map<String, BigDecimal> inTransitMap = DataPermissionUtils.executeIgnore(
                () -> purchaseOrderItemMapper.selectInTransitCountMap(productIds, warehouseIds,
                        Collections.singletonList(ErpAuditStatus.APPROVE.getStatus())));
        Map<Long, BigDecimal> lastSalePriceMap = DataPermissionUtils.executeIgnore(
                () -> saleOutItemMapper.selectLatestSalePriceMap(productIds));
        Map<String, List<ErpStockBatchNoRespVO>> batchNoListMap = includeBatchNo
                ? DataPermissionUtils.executeIgnore(() ->
                stockService.getAvailableBatchNoListMap(pageResult.getList())) : Collections.emptyMap();
        // 价格体系
        Map<Long, BigDecimal> priceMap = priceSystemId != null
                ? productPriceSystemService.getProductPriceMap(productIds, priceSystemId)
                : Collections.emptyMap();
        return BeanUtils.toBean(pageResult, ErpStockRespVO.class, stock -> {
            stock.setRowKey(buildStockRowKey(stock)).setBatchRow(false);
            MapUtils.findAndThen(productMap, stock.getProductId(), product -> {
                stock.setProductName(product.getName())
                        .setCategoryName(product.getCategoryName())
                        .setUnitId(product.getUnitId())
                        .setUnitName(product.getUnitName());
                // 扩展字段
                stock.setProductCode(product.getCode())
                        .setDrawingNo(product.getDrawingNo())
                        .setStandard(product.getStandard())
                        .setFeatureCode(product.getFeatureCode())
                        .setVehicleModel(product.getVehicleModel())
                        .setBrand(product.getBrand())
                        .setOriginPlace(product.getOriginPlace())
                        .setCategoryId(product.getCategoryId())
                        .setPurchasePrice(stock.getPurchasePrice() != null
                                ? stock.getPurchasePrice() : product.getLastPurchasePrice())
                        .setProductPurchasePrice(product.getPurchasePrice())
                        .setLastPurchasePrice(product.getLastPurchasePrice())
                        .setLastSalePrice(lastSalePriceMap.get(stock.getProductId()))
                        .setOeNumber(product.getOeNumber())
                        .setFactoryCode(product.getFactoryCode())
                        .setProductBarCode(product.getBarCode())
                        .setSalePrice(product.getSalePrice())
                        .setMinPrice(product.getMinPrice())
                        .setReferencePrice(product.getReferencePrice())
                        .setRetailPrice(product.getRetailPrice())
                        .setGrossProfitRate(product.getGrossProfitRate())
                        .setBackupPrice1(product.getBackupPrice1())
                        .setWholesalePrice(product.getWholesalePrice())
                        .setSharePrice(product.getSharePrice())
                        .setCustomFields(copyVisibleCustomFields(product.getCustomFields(), hiddenPriceFields))
                        .setStockMax(product.getStockMax())
                        .setStockMin(product.getStockMin())
                        .setStockStandard(product.getStockStandard())
                        .setPackageQty(product.getPackageQty())
                        .setBatchNoEnabled(product.getBatchNoEnabled())
                        .setWeight(product.getWeight());
            });
            if (includeBatchNo) {
                fillBatchNoInfo(stock, batchNoListMap);
            }
            MapUtils.findAndThen(warehouseMap, stock.getWarehouseId(), warehouse -> {
                stock.setWarehouseName(warehouse.getName());
                if (warehouse.getDeptId() != null) {
                    stock.setDeptId(warehouse.getDeptId());
                }
            });
            Long deptId = stock.getDeptId();
            MapUtils.findAndThen(deptMap, deptId, dept -> stock.setDeptName(dept.getName()));
            // 当前库存列表只返回本部门可见仓库，不再存在“仅销售分配可见”的只读库存行。
            stock.setReadonlyBySaleDistribution(false);
            // 聚合字段
            String stockSummaryKey = buildStockSummaryMapKey(stock.getProductId(), stock.getWarehouseId());
            BigDecimal occupied = occupiedMap.getOrDefault(stockSummaryKey, BigDecimal.ZERO);
            BigDecimal pending = pendingInMap.getOrDefault(stockSummaryKey, BigDecimal.ZERO);
            BigDecimal inTransit = inTransitMap.getOrDefault(stockSummaryKey, BigDecimal.ZERO);
            stock.setOccupiedCount(occupied)
                    .setAvailableCount(defaultZero(stock.getCount()).subtract(occupied))
                    .setPendingInCount(pending)
                    .setInTransitCount(inTransit);
            // 价格体系
            if (priceSystemId != null) {
                BigDecimal unitPrice = priceMap.get(stock.getProductId());
                stock.setCurrentPrice(unitPrice);
                if (unitPrice != null && stock.getCount() != null) {
                    stock.setCurrentPriceAmount(unitPrice.multiply(stock.getCount()));
                }
            }
            clearConfiguredStockPrices(stock, hiddenPriceFields);
        });
    }

    private Set<String> getHiddenPriceFieldSet(Long pricePermissionDeptId) {
        return getHiddenPriceFieldSet(pricePermissionDeptId, null);
    }

    private Set<String> getHiddenPriceFieldSet(Long pricePermissionDeptId, String bizType) {
        List<String> hiddenFields = pricePermissionDeptId != null
                ? permissionApi.getCurrentUserHiddenFields("erp_product", pricePermissionDeptId)
                : permissionApi.getCurrentUserHiddenFields("erp_product");
        Set<String> result = CollUtil.isEmpty(hiddenFields)
                ? new LinkedHashSet<>() : new LinkedHashSet<>(hiddenFields);
        result.addAll(stockSelectPriceConfigService.getSceneHiddenPriceFields(bizType));
        return result;
    }

    private boolean isConfiguredFieldHidden(Set<String> hiddenFields, String fieldKey) {
        return hiddenFields.contains(fieldKey) || hiddenFields.contains("col_" + fieldKey);
    }

    private boolean isStockPriceFieldHidden(Set<String> hiddenFields, String stockField) {
        switch (stockField) {
            case "costPrice":
            case "costAmount":
            case "lastPurchasePrice":
                return isConfiguredFieldHidden(hiddenFields, "lastPurchasePrice");
            case "purchasePrice":
                return isConfiguredFieldHidden(hiddenFields, "purchasePrice")
                        || isConfiguredFieldHidden(hiddenFields, "lastPurchasePrice");
            case "productPurchasePrice":
                return isConfiguredFieldHidden(hiddenFields, "purchasePrice");
            case "lastSalePrice":
                return isConfiguredFieldHidden(hiddenFields, "lastSalePrice")
                        || isConfiguredFieldHidden(hiddenFields, "salePrice");
            case "salePrice":
                return isConfiguredFieldHidden(hiddenFields, "salePrice");
            case "minPrice":
                return isConfiguredFieldHidden(hiddenFields, "minPrice");
            case "referencePrice":
                return isConfiguredFieldHidden(hiddenFields, "referencePrice");
            case "retailPrice":
                return isConfiguredFieldHidden(hiddenFields, "retailPrice");
            case "backupPrice1":
            case "currentPrice":
            case "currentPriceAmount":
                return isConfiguredFieldHidden(hiddenFields, "backupPrice1");
            case "wholesalePrice":
                return isConfiguredFieldHidden(hiddenFields, "wholesalePrice");
            case "grossProfitRate":
                return isConfiguredFieldHidden(hiddenFields, "grossProfitRate");
            case "sharePrice":
                return isConfiguredFieldHidden(hiddenFields, "sharePrice");
            default:
                return false;
        }
    }

    private void clearConfiguredStockPrices(ErpStockRespVO stock, Set<String> hiddenFields) {
        if (isStockPriceFieldHidden(hiddenFields, "costPrice")) {
            stock.setCostPrice(null).setCostAmount(null).setLastPurchasePrice(null);
        }
        if (isStockPriceFieldHidden(hiddenFields, "purchasePrice")) {
            stock.setPurchasePrice(null);
        }
        if (isStockPriceFieldHidden(hiddenFields, "productPurchasePrice")) {
            stock.setProductPurchasePrice(null);
        }
        if (isStockPriceFieldHidden(hiddenFields, "salePrice")) {
            stock.setSalePrice(null);
        }
        if (isStockPriceFieldHidden(hiddenFields, "lastSalePrice")) {
            stock.setLastSalePrice(null);
        }
        if (isStockPriceFieldHidden(hiddenFields, "minPrice")) {
            stock.setMinPrice(null);
        }
        if (isStockPriceFieldHidden(hiddenFields, "referencePrice")) {
            stock.setReferencePrice(null);
        }
        if (isStockPriceFieldHidden(hiddenFields, "retailPrice")) {
            stock.setRetailPrice(null);
        }
        if (isStockPriceFieldHidden(hiddenFields, "backupPrice1")) {
            stock.setBackupPrice1(null).setCurrentPrice(null).setCurrentPriceAmount(null);
        }
        if (isStockPriceFieldHidden(hiddenFields, "wholesalePrice")) {
            stock.setWholesalePrice(null);
        }
        if (isStockPriceFieldHidden(hiddenFields, "grossProfitRate")) {
            stock.setGrossProfitRate(null);
        }
        if (isStockPriceFieldHidden(hiddenFields, "sharePrice")) {
            stock.setSharePrice(null);
        }
    }

    private Map<String, Object> copyVisibleCustomFields(Map<String, Object> customFields,
                                                         Set<String> hiddenFields) {
        if (customFields == null) {
            return null;
        }
        Map<String, Object> visibleFields = new LinkedHashMap<>(customFields);
        visibleFields.keySet().removeIf(fieldKey -> isConfiguredFieldHidden(hiddenFields, fieldKey));
        return visibleFields;
    }

    private ErpStockSummaryRespVO buildStockSummary(PageResult<ErpStockRespVO> pageResult,
                                                    boolean priceSystemSelected) {
        ErpStockSummaryRespVO summary = new ErpStockSummaryRespVO();
        summary.setTotalRows(pageResult.getTotal() != null ? pageResult.getTotal() : 0L);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return summary;
        }
        for (ErpStockRespVO stock : pageResult.getList()) {
            BigDecimal count = defaultZero(stock.getCount());
            summary.setTotalStockCount(summary.getTotalStockCount().add(count));
            summary.setTotalCostAmount(summary.getTotalCostAmount().add(defaultZero(stock.getCostAmount())));
            summary.setTotalCurrentPriceAmount(summary.getTotalCurrentPriceAmount()
                    .add(resolveCurrentPriceAmount(stock, count, priceSystemSelected)));
            summary.setTotalPendingInCount(summary.getTotalPendingInCount()
                    .add(defaultZero(stock.getPendingInCount())));
            summary.setTotalOccupiedCount(summary.getTotalOccupiedCount()
                    .add(defaultZero(stock.getOccupiedCount())));
            summary.setTotalInTransitCount(summary.getTotalInTransitCount()
                    .add(defaultZero(stock.getInTransitCount())));
            summary.setTotalWeight(summary.getTotalWeight().add(count.multiply(defaultZero(stock.getWeight()))));
        }
        return summary;
    }

    private BigDecimal resolveCurrentPriceAmount(ErpStockRespVO stock, BigDecimal count,
                                                 boolean priceSystemSelected) {
        if (stock.getCurrentPriceAmount() != null) {
            return stock.getCurrentPriceAmount();
        }
        BigDecimal unitPrice = priceSystemSelected ? stock.getCurrentPrice() : stock.getBackupPrice1();
        return unitPrice != null ? unitPrice.multiply(count) : BigDecimal.ZERO;
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private void validateCurrentUserStockAccess(Long productId, Long warehouseId) {
        ErpStockDO stock = DataPermissionUtils.executeIgnore(() -> stockService.getStock(productId, warehouseId));
        if (stock == null) {
            warehouseService.validateCurrentUserStockWarehousePermission(Collections.singleton(warehouseId));
            return;
        }
        warehouseService.validateCurrentUserStockPermission(Collections.singleton(stock));
    }

    private void validateBatchFilter(String batchNo, Boolean unassignedBatch) {
        if (Boolean.TRUE.equals(unassignedBatch) && batchNo != null && !batchNo.trim().isEmpty()) {
            throw new IllegalArgumentException("batchNo and unassignedBatch cannot be used together");
        }
    }

    private void fillCreatorNames(List<ErpStockInTransitDetailRespVO> list) {
        fillCreatorNames(list, ErpStockInTransitDetailRespVO::getCreator,
                ErpStockInTransitDetailRespVO::setCreatorName);
    }

    private <T> void fillCreatorNames(List<T> list, java.util.function.Function<T, String> creatorGetter,
                                      java.util.function.BiConsumer<T, String> creatorNameSetter) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        Set<Long> userIds = list.stream()
                .map(creatorGetter)
                .map(this::parseUserId)
                .filter(java.util.Objects::nonNull)
                .collect(Collectors.toSet());
        if (CollUtil.isEmpty(userIds)) {
            return;
        }
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        list.forEach(item -> {
            Long creatorId = parseUserId(creatorGetter.apply(item));
            MapUtils.findAndThen(userMap, creatorId, user -> creatorNameSetter.accept(item, user.getNickname()));
        });
    }

    private Long parseUserId(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return null;
        }
        try {
            return Long.parseLong(userId);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void fillBatchNoInfo(ErpStockRespVO stock, Map<String, List<ErpStockBatchNoRespVO>> batchNoListMap) {
        if (!Boolean.TRUE.equals(stock.getBatchNoEnabled())) {
            stock.setBatchNoCount(0)
                    .setBatchNoSummary(null)
                    .setBatchNoList(Collections.emptyList());
            return;
        }
        List<ErpStockBatchNoRespVO> batchNoList = batchNoListMap.getOrDefault(
                buildBatchNoMapKey(stock.getProductId(), stock.getWarehouseId()), Collections.emptyList());
        stock.setBatchNoCount(batchNoList.size())
                .setBatchNoSummary(buildBatchNoSummary(batchNoList))
                .setBatchNoList(batchNoList);
    }

    private String buildBatchNoSummary(List<ErpStockBatchNoRespVO> batchNoList) {
        if (CollUtil.isEmpty(batchNoList)) {
            return null;
        }
        String summary = batchNoList.stream()
                .limit(3)
                .map(item -> item.getBatchNo() + "：" + item.getAvailableCount())
                .collect(Collectors.joining("，"));
        if (batchNoList.size() > 3) {
            summary += " 等" + batchNoList.size() + " 个批次";
        }
        return summary;
    }

    private String buildBatchNoMapKey(Long productId, Long warehouseId) {
        return buildStockSummaryMapKey(productId, warehouseId);
    }

    private String buildStockSummaryMapKey(Long productId, Long warehouseId) {
        return productId + "_" + warehouseId;
    }

}
