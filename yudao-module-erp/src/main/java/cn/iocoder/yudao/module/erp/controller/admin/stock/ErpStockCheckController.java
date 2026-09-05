package cn.iocoder.yudao.module.erp.controller.admin.stock;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.common.ErpAuditStatusRequestValidator;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.ErpStockUpdateRemarkReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.check.ErpStockCheckDraftCreateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.check.ErpStockCheckDraftUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.check.ErpStockCheckImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.check.ErpStockCheckItemBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.check.ErpStockCheckItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.check.ErpStockCheckPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.check.ErpStockCheckRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.check.ErpStockCheckSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.imports.ErpStockImportResultRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockCheckDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockCheckItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.enums.print.ErpPrintModuleEnum;
import cn.iocoder.yudao.module.erp.service.base.ErpDataPermissionDeptService;
import cn.iocoder.yudao.module.erp.service.common.ErpImportExportRecordService;
import cn.iocoder.yudao.module.erp.service.common.ErpPrintService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockCheckService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockImportService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMultiMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

@Tag(name = "Admin - ERP stock check")
@RestController
@RequestMapping("/erp/stock-check")
@Validated
public class ErpStockCheckController {

    private static final int EXPORT_MAX_COUNT = 5000;
    private static final ErrorCode EXPORT_COUNT_EXCEEDED = new ErrorCode(1_030_590_005,
            "单次最多导出 5000 条盘点单，请缩小筛选范围后重试");
    private static final String FIELD_PERMISSION_MODULE = "erp_stock_check";
    private static final Set<String> IMPORT_TEMPLATE_FIELDS = new LinkedHashSet<>(Arrays.asList(
            "checkTypeName", "warehouseName", "productCode", "productName", "factoryCode", "batchNo", "actualCount",
            "productPrice", "totalPrice", "remark", "itemRemark"));

    @Resource
    private ErpStockCheckService stockCheckService;
    @Resource
    private ErpImportExportRecordService importExportRecordService;
    @Resource
    private ErpStockImportService stockImportService;
    @Resource
    private ErpProductService productService;
    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private ErpStockFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private ErpDataPermissionDeptService dataPermissionDeptService;
    @Resource
    private DeptApi deptApi;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private ErpPrintService printService;

    @PostMapping("/create")
    @Operation(summary = "Create stock check")
    @PreAuthorize("@ss.hasPermission('erp:stock-check:create')")
    public CommonResult<Long> createStockCheck(@Valid @RequestBody ErpStockCheckSaveReqVO createReqVO) {
        return success(stockCheckService.createStockCheck(createReqVO));
    }

    @PostMapping("/create-draft")
    @Operation(summary = "Create stock check draft")
    @PreAuthorize("@ss.hasPermission('erp:stock-check:create')")
    public CommonResult<Long> createStockCheckDraft(@RequestBody ErpStockCheckDraftCreateReqVO createReqVO) {
        return success(stockCheckService.createStockCheckDraft(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "Update stock check")
    @PreAuthorize("@ss.hasPermission('erp:stock-check:update')")
    public CommonResult<Boolean> updateStockCheck(@Valid @RequestBody ErpStockCheckSaveReqVO updateReqVO) {
        stockCheckService.updateStockCheck(updateReqVO);
        return success(true);
    }

    @PutMapping("/batch-update-items")
    @Operation(summary = "Batch update stock check item warehouse")
    @PreAuthorize("@ss.hasPermission('erp:stock-check:update')")
    public CommonResult<Boolean> batchUpdateStockCheckItems(
            @Valid @RequestBody ErpStockCheckItemBatchUpdateReqVO updateReqVO) {
        stockCheckService.batchUpdateStockCheckItems(updateReqVO);
        return success(true);
    }

    @GetMapping("/warehouse-dept-simple-list")
    @Operation(summary = "Get available department list for stock check item warehouse")
    @PreAuthorize("@ss.hasPermission('erp:stock-check:update')")
    public CommonResult<List<DeptSimpleRespVO>> getWarehouseDeptSimpleList(@RequestParam("warehouseId") Long warehouseId) {
        return success(stockCheckService.getWarehouseDeptSimpleList(warehouseId));
    }

    @PutMapping("/update-draft")
    @Operation(summary = "Update stock check draft")
    @PreAuthorize("@ss.hasPermission('erp:stock-check:update')")
    public CommonResult<Boolean> updateStockCheckDraft(@RequestBody ErpStockCheckDraftUpdateReqVO updateReqVO) {
        stockCheckService.updateStockCheckDraft(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-and-submit")
    @Operation(summary = "Update and submit stock check draft")
    @PreAuthorize("@ss.hasPermission('erp:stock-check:update') and " +
            "@ss.hasPermission('erp:stock-check:update-status')")
    public CommonResult<Boolean> updateAndSubmitStockCheckDraft(
            @Valid @RequestBody ErpStockCheckSaveReqVO updateReqVO) {
        stockCheckService.updateAndSubmitStockCheckDraft(updateReqVO);
        return success(true);
    }

    @PutMapping("/submit")
    @Operation(summary = "Submit stock check draft")
    @PreAuthorize("@ss.hasPermission('erp:stock-check:update-status')")
    public CommonResult<Boolean> submitStockCheck(@RequestParam("id") Long id) {
        stockCheckService.submitStockCheck(id);
        return success(true);
    }

    @PutMapping("/update-remark")
    @Operation(summary = "Update stock check remark")
    @PreAuthorize("@ss.hasPermission('erp:stock-check:update')")
    public CommonResult<Boolean> updateStockCheckRemark(
            @Valid @RequestBody ErpStockUpdateRemarkReqVO updateReqVO) {
        stockCheckService.updateStockCheckRemark(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "Update stock check status")
    @PreAuthorize("@ss.hasPermission('erp:stock-check:update-status')")
    public CommonResult<Boolean> updateStockCheckStatus(@RequestParam("id") Long id,
                                                        @RequestParam("status") Integer status) {
        ErpAuditStatusRequestValidator.validateApproveStatus(status);
        stockCheckService.updateStockCheckStatus(id, status);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "Delete stock check")
    @Parameter(name = "ids", description = "ids", required = true)
    @PreAuthorize("@ss.hasPermission('erp:stock-check:delete')")
    public CommonResult<Boolean> deleteStockCheck(@RequestParam("ids") List<Long> ids) {
        stockCheckService.deleteStockCheck(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "Get stock check")
    @Parameter(name = "id", description = "id", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:stock-check:query')")
    public CommonResult<ErpStockCheckRespVO> getStockCheck(@RequestParam("id") Long id,
                                                           @RequestParam(value = "includeItems", required = false,
                                                                   defaultValue = "true") Boolean includeItems) {
        ErpStockCheckDO stockCheck = stockCheckService.getStockCheck(id);
        if (stockCheck == null) {
            return success(null);
        }
        List<ErpStockCheckItemDO> itemList = Boolean.TRUE.equals(includeItems)
                ? stockCheckService.getStockCheckItemListByCheckId(id) : Collections.emptyList();
        Map<Long, ErpWarehouseDO> warehouseMap = getWarehouseMapIgnoreDataPermission(
                convertSet(itemList, ErpStockCheckItemDO::getWarehouseId));
        Set<Long> deptIds = new HashSet<>(convertSet(warehouseMap.values(), ErpWarehouseDO::getDeptId));
        if (stockCheck.getDeptId() != null) {
            deptIds.add(stockCheck.getDeptId());
        }
        deptIds.remove(null);
        Map<Long, DeptRespDTO> deptMap = CollUtil.isEmpty(deptIds) ? Collections.emptyMap() : deptApi.getDeptMap(deptIds);
        Set<Long> userIds = new HashSet<>();
        addUserId(userIds, stockCheck.getCreator());
        addUserId(userIds, stockCheck.getUpdater());
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);

        ErpStockCheckRespVO respVO = BeanUtils.toBean(stockCheck, ErpStockCheckRespVO.class, vo -> {
            if (Boolean.TRUE.equals(includeItems)) {
                vo.setItems(buildStockCheckItemVOList(itemList));
                vo.setProductNames(CollUtil.join(vo.getItems(), ", ", ErpStockCheckRespVO.Item::getProductName));
                vo.setProductCodes(CollUtil.join(vo.getItems(), ", ", ErpStockCheckRespVO.Item::getProductCode));
            }
            MapUtils.findAndThen(deptMap, vo.getDeptId(), dept -> vo.setDeptName(dept.getName()));
            fillUserNames(vo, userMap);
        });
        fieldPermissionMasker.maskFormWithItems(FIELD_PERMISSION_MODULE, respVO);
        return success(respVO);
    }

    @GetMapping("/item-page")
    @Operation(summary = "Get stock check item page")
    @PreAuthorize("@ss.hasPermission('erp:stock-check:query')")
    public CommonResult<PageResult<ErpStockCheckRespVO.Item>> getStockCheckItemPage(
            @Valid ErpStockCheckItemPageReqVO pageReqVO) {
        PageResult<ErpStockCheckItemDO> pageResult = stockCheckService.getStockCheckItemPage(pageReqVO);
        PageResult<ErpStockCheckRespVO.Item> respResult = new PageResult<>(
                buildStockCheckItemVOList(pageResult.getList()), pageResult.getTotal());
        if (Boolean.TRUE.equals(pageReqVO.getMask())) {
            fieldPermissionMasker.clearHiddenItemFields(FIELD_PERMISSION_MODULE, respResult.getList());
        }
        return success(respResult);
    }

    @GetMapping("/page")
    @Operation(summary = "Get stock check page")
    @PreAuthorize("@ss.hasPermission('erp:stock-check:query')")
    public CommonResult<PageResult<ErpStockCheckRespVO>> getStockCheckPage(@Valid ErpStockCheckPageReqVO pageReqVO) {
        return success(buildStockCheckVOPageResult(stockCheckService.getStockCheckPage(pageReqVO)));
    }

    @GetMapping("/dept-simple-list")
    @Operation(summary = "Get visible department list for stock check filter")
    @PreAuthorize("@ss.hasPermission('erp:stock-check:query')")
    public CommonResult<List<DeptSimpleRespVO>> getVisibleDeptSimpleList() {
        return success(dataPermissionDeptService.getDeptSimpleList(FIELD_PERMISSION_MODULE));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "Export stock check")
    @PreAuthorize("@ss.hasPermission('erp:stock-check:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportStockCheckExcel(@Valid ErpStockCheckPageReqVO pageReqVO,
                                      HttpServletResponse response) throws IOException {
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(EXPORT_MAX_COUNT);
        PageResult<ErpStockCheckDO> pageResult = stockCheckService.getStockCheckPage(pageReqVO);
        if (pageResult.getTotal() > EXPORT_MAX_COUNT) {
            throw exception(EXPORT_COUNT_EXCEEDED);
        }
        List<ErpStockCheckRespVO> list = buildStockCheckVOPageResult(pageResult).getList();
        ExcelUtils.write(response, "stock-check.xls", "data", ErpStockCheckRespVO.class, list);
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "Get stock check import template")
    @PreAuthorize("@ss.hasPermission('erp:stock-check:import')")
    public void getImportTemplate(HttpServletResponse response) throws IOException {
        ErpStockCheckImportExcelVO first = new ErpStockCheckImportExcelVO();
        first.setCheckTypeName("盘数量");
        first.setWarehouseName("示例仓库");
        first.setProductCode("P0001");
        first.setBatchNo("BATCH-001");
        first.setActualCount(new BigDecimal("12"));
        first.setProductPrice(new BigDecimal("100.00"));
        first.setRemark("单据备注");
        first.setItemRemark("明细备注");
        ErpStockCheckImportExcelVO second = new ErpStockCheckImportExcelVO();
        second.setWarehouseName("示例仓库");
        second.setProductCode("P0002");
        second.setActualCount(new BigDecimal("18"));
        second.setProductPrice(new BigDecimal("50.00"));
        ExcelUtils.writeImportTemplate(response, "库存盘点导入模板.xls", "库存盘点",
                ErpStockCheckImportExcelVO.class, Arrays.asList(first, second), IMPORT_TEMPLATE_FIELDS);
    }

    @PostMapping("/import")
    @Operation(summary = "Import stock check")
    @PreAuthorize("@ss.hasPermission('erp:stock-check:import')")
    public CommonResult<ErpStockImportResultRespVO> importStockCheck(@RequestParam("file") MultipartFile file)
            throws Exception {
        return success(stockImportService.importStockCheckList(ExcelUtils.read(file, ErpStockCheckImportExcelVO.class)));
    }

    @GetMapping("/import-failure-details/download")
    @Operation(summary = "Download stock check import failure details")
    @PreAuthorize("@ss.hasPermission('erp:stock-check:import')")
    public void downloadImportFailureDetails(@RequestParam("recordId") Long recordId,
                                             HttpServletResponse response) throws IOException {
        importExportRecordService.downloadOwnImportFailureDetails(recordId, FIELD_PERMISSION_MODULE, response);
    }

    private PageResult<ErpStockCheckRespVO> buildStockCheckVOPageResult(PageResult<ErpStockCheckDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        List<ErpStockCheckItemDO> itemList = stockCheckService.getStockCheckItemListByCheckIds(
                convertSet(pageResult.getList(), ErpStockCheckDO::getId));
        Map<Long, List<ErpStockCheckItemDO>> itemMap = convertMultiMap(itemList, ErpStockCheckItemDO::getCheckId);
        Map<Long, ErpProductRespVO> productMap = DataPermissionUtils.executeIgnore(() -> productService.getProductVOMap(
                convertSet(itemList, ErpStockCheckItemDO::getProductId)));
        Map<Long, ErpWarehouseDO> warehouseMap = getWarehouseMapIgnoreDataPermission(
                convertSet(itemList, ErpStockCheckItemDO::getWarehouseId));
        Set<Long> deptIds = new HashSet<>(convertSet(pageResult.getList(), ErpStockCheckDO::getDeptId));
        deptIds.addAll(convertSet(warehouseMap.values(), ErpWarehouseDO::getDeptId));
        deptIds.remove(null);
        Map<Long, DeptRespDTO> deptMap = CollUtil.isEmpty(deptIds) ? Collections.emptyMap() : deptApi.getDeptMap(deptIds);
        Set<Long> userIds = new HashSet<>();
        pageResult.getList().forEach(stockCheck -> {
            addUserId(userIds, stockCheck.getCreator());
            addUserId(userIds, stockCheck.getUpdater());
        });
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        Set<Long> ids = convertSet(pageResult.getList(), ErpStockCheckDO::getId);
        Map<Long, Long> printCountMap = printService.getPrintCountMap(ErpPrintModuleEnum.STOCK_CHECK.getKey(), ids);
        Map<Long, LocalDateTime> printTimeMap = printService.getLastPrintTimeMap(
                ErpPrintModuleEnum.STOCK_CHECK.getKey(), ids);

        return BeanUtils.toBean(pageResult, ErpStockCheckRespVO.class, vo -> {
            vo.setItems(BeanUtils.toBean(itemMap.get(vo.getId()), ErpStockCheckRespVO.Item.class,
                    item -> fillItemRelation(item, productMap.get(item.getProductId()), warehouseMap, deptMap)));
            vo.setProductNames(CollUtil.join(vo.getItems(), ", ", ErpStockCheckRespVO.Item::getProductName));
            vo.setProductCodes(CollUtil.join(vo.getItems(), ", ", ErpStockCheckRespVO.Item::getProductCode));
            MapUtils.findAndThen(deptMap, vo.getDeptId(), dept -> vo.setDeptName(dept.getName()));
            fillUserNames(vo, userMap);
            Long printCount = printCountMap.get(vo.getId());
            vo.setPrintCount(printCount == null ? 0 : printCount.intValue());
            vo.setPrintTime(printTimeMap.get(vo.getId()));
        });
    }

    private List<ErpStockCheckRespVO.Item> buildStockCheckItemVOList(List<ErpStockCheckItemDO> itemList) {
        if (CollUtil.isEmpty(itemList)) {
            return Collections.emptyList();
        }
        Map<Long, ErpProductRespVO> productMap = DataPermissionUtils.executeIgnore(() -> productService.getProductVOMap(
                convertSet(itemList, ErpStockCheckItemDO::getProductId)));
        Map<Long, ErpWarehouseDO> warehouseMap = getWarehouseMapIgnoreDataPermission(
                convertSet(itemList, ErpStockCheckItemDO::getWarehouseId));
        Set<Long> deptIds = new HashSet<>(convertSet(warehouseMap.values(), ErpWarehouseDO::getDeptId));
        deptIds.remove(null);
        Map<Long, DeptRespDTO> deptMap = CollUtil.isEmpty(deptIds) ? Collections.emptyMap() : deptApi.getDeptMap(deptIds);
        return BeanUtils.toBean(itemList, ErpStockCheckRespVO.Item.class,
                item -> fillItemRelation(item, productMap.get(item.getProductId()), warehouseMap, deptMap));
    }

    private Map<Long, ErpWarehouseDO> getWarehouseMapIgnoreDataPermission(Collection<Long> warehouseIds) {
        if (CollUtil.isEmpty(warehouseIds)) {
            return Collections.emptyMap();
        }
        Map<Long, ErpWarehouseDO> warehouseMap = DataPermissionUtils.executeIgnore(
                () -> warehouseService.getWarehouseMap(warehouseIds));
        return warehouseMap == null ? Collections.emptyMap() : warehouseMap;
    }

    private void fillItemRelation(ErpStockCheckRespVO.Item item, ErpProductRespVO product,
                                  Map<Long, ErpWarehouseDO> warehouseMap,
                                  Map<Long, DeptRespDTO> deptMap) {
        fillProduct(item, product);
        MapUtils.findAndThen(warehouseMap, item.getWarehouseId(), warehouse -> {
            item.setWarehouseName(warehouse.getName());
            item.setWarehouseDeptId(warehouse.getDeptId());
            MapUtils.findAndThen(deptMap, warehouse.getDeptId(), dept -> item.setWarehouseDeptName(dept.getName()));
        });
    }

    private void fillProduct(ErpStockCheckRespVO.Item item, ErpProductRespVO product) {
        if (product == null) {
            return;
        }
        item.setProductName(product.getName())
                .setProductCode(product.getCode())
                .setProductBarCode(product.getBarCode())
                .setProductUnitName(product.getUnitName())
                .setBatchNoEnabled(product.getBatchNoEnabled());
    }

    private void fillUserNames(ErpStockCheckRespVO stockCheck, Map<Long, AdminUserRespDTO> userMap) {
        Long creatorId = parseUserId(stockCheck.getCreator());
        if (creatorId != null) {
            MapUtils.findAndThen(userMap, creatorId, user -> stockCheck.setCreatorName(user.getNickname()));
        }
        Long updaterId = parseUserId(stockCheck.getUpdater());
        if (updaterId != null) {
            MapUtils.findAndThen(userMap, updaterId, user -> stockCheck.setUpdaterName(user.getNickname()));
        }
    }

    private void addUserId(Set<Long> userIds, String userId) {
        Long parsed = parseUserId(userId);
        if (parsed != null) {
            userIds.add(parsed);
        }
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

}
