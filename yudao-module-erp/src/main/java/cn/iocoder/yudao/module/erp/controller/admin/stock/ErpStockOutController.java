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
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.imports.ErpStockImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.imports.ErpStockImportResultRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.out.ErpStockOutItemBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.out.ErpStockOutPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.out.ErpStockOutRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.out.ErpStockOutSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.service.base.ErpDataPermissionDeptService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockImportService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockOutService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
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
import java.util.Arrays;
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

@Tag(name = "Admin - ERP stock out")
@RestController
@RequestMapping("/erp/stock-out")
@Validated
public class ErpStockOutController {

    private static final int EXPORT_MAX_COUNT = 5000;
    private static final ErrorCode EXPORT_COUNT_EXCEEDED = new ErrorCode(1_030_590_002,
            "单次最多导出 5000 条出库单，请缩小筛选范围后重试");
    private static final String FIELD_PERMISSION_MODULE = "erp_stock_out";
    private static final Set<String> IMPORT_TEMPLATE_FIELDS = new LinkedHashSet<>(Arrays.asList(
            "orderNo", "customerName", "bizTime", "warehouseName", "productCode", "count", "productPrice",
            "remark", "itemRemark"));

    @Resource
    private ErpStockOutService stockOutService;
    @Resource
    private ErpStockImportService stockImportService;
    @Resource
    private ErpStockService stockService;
    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private ErpProductService productService;
    @Resource
    private ErpCustomerService customerService;
    @Resource
    private ErpStockFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private ErpDataPermissionDeptService dataPermissionDeptService;
    @Resource
    private DeptApi deptApi;
    @Resource
    private AdminUserApi adminUserApi;

    @PostMapping("/create")
    @Operation(summary = "Create stock out")
    @PreAuthorize("@ss.hasPermission('erp:stock-out:create')")
    public CommonResult<Long> createStockOut(@Valid @RequestBody ErpStockOutSaveReqVO createReqVO) {
        return success(stockOutService.createStockOut(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "Update stock out")
    @PreAuthorize("@ss.hasPermission('erp:stock-out:update')")
    public CommonResult<Boolean> updateStockOut(@Valid @RequestBody ErpStockOutSaveReqVO updateReqVO) {
        stockOutService.updateStockOut(updateReqVO);
        return success(true);
    }

    @PutMapping("/batch-update-items")
    @Operation(summary = "Batch update stock out item warehouse")
    @PreAuthorize("@ss.hasPermission('erp:stock-out:update')")
    public CommonResult<Boolean> batchUpdateStockOutItems(
            @Valid @RequestBody ErpStockOutItemBatchUpdateReqVO updateReqVO) {
        stockOutService.batchUpdateStockOutItems(updateReqVO);
        return success(true);
    }

    @GetMapping("/warehouse-dept-simple-list")
    @Operation(summary = "Get available department list for stock out item warehouse")
    @PreAuthorize("@ss.hasPermission('erp:stock-out:update')")
    public CommonResult<List<DeptSimpleRespVO>> getWarehouseDeptSimpleList(@RequestParam("warehouseId") Long warehouseId) {
        return success(stockOutService.getWarehouseDeptSimpleList(warehouseId));
    }

    @PutMapping("/update-remark")
    @Operation(summary = "Update stock out remark")
    @PreAuthorize("@ss.hasPermission('erp:stock-out:update')")
    public CommonResult<Boolean> updateStockOutRemark(
            @Valid @RequestBody ErpStockUpdateRemarkReqVO updateReqVO) {
        stockOutService.updateStockOutRemark(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "Update stock out status")
    @PreAuthorize("@ss.hasPermission('erp:stock-out:update-status')")
    public CommonResult<Boolean> updateStockOutStatus(@RequestParam("id") Long id,
                                                      @RequestParam("status") Integer status) {
        ErpAuditStatusRequestValidator.validateApproveStatus(status);
        stockOutService.updateStockOutStatus(id, status);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "Delete stock out")
    @Parameter(name = "ids", description = "ids", required = true)
    @PreAuthorize("@ss.hasPermission('erp:stock-out:delete')")
    public CommonResult<Boolean> deleteStockOut(@RequestParam("ids") List<Long> ids) {
        stockOutService.deleteStockOut(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "Get stock out")
    @Parameter(name = "id", description = "id", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:stock-out:query')")
    public CommonResult<ErpStockOutRespVO> getStockOut(@RequestParam("id") Long id) {
        ErpStockOutDO stockOut = stockOutService.getStockOut(id);
        if (stockOut == null) {
            return success(null);
        }
        List<ErpStockOutItemDO> itemList = stockOutService.getStockOutItemListByOutId(id);
        Map<Long, ErpProductRespVO> productMap = DataPermissionUtils.executeIgnore(() -> productService.getProductVOMap(
                convertSet(itemList, ErpStockOutItemDO::getProductId)));
        Map<Long, ErpWarehouseDO> warehouseMap = DataPermissionUtils.executeIgnore(() ->
                warehouseService.getWarehouseMap(convertSet(itemList, ErpStockOutItemDO::getWarehouseId)));
        Set<Long> userIds = new HashSet<>();
        addUserId(userIds, stockOut.getCreator());
        addUserId(userIds, stockOut.getUpdater());
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        DeptRespDTO dept = stockOut.getDeptId() == null ? null : deptApi.getDept(stockOut.getDeptId());

        ErpStockOutRespVO respVO = BeanUtils.toBean(stockOut, ErpStockOutRespVO.class, vo -> {
            vo.setItems(BeanUtils.toBean(itemList, ErpStockOutRespVO.Item.class, item -> {
                ErpStockDO stock = DataPermissionUtils.executeIgnore(() ->
                        stockService.getStock(item.getProductId(), item.getWarehouseId()));
                item.setStockCount(stock != null ? stock.getCount() : BigDecimal.ZERO);
                fillProduct(item, productMap.get(item.getProductId()));
            }));
            vo.setProductNames(CollUtil.join(vo.getItems(), ", ", ErpStockOutRespVO.Item::getProductName));
            vo.setProductCodes(CollUtil.join(vo.getItems(), ", ", ErpStockOutRespVO.Item::getProductCode));
            fillListSummary(vo, itemList, warehouseMap);
            if (dept != null) {
                vo.setDeptName(dept.getName());
            }
            fillUserNames(vo, userMap);
        });
        fieldPermissionMasker.maskFormWithItems(FIELD_PERMISSION_MODULE, respVO);
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "Get stock out page")
    @PreAuthorize("@ss.hasPermission('erp:stock-out:query')")
    public CommonResult<PageResult<ErpStockOutRespVO>> getStockOutPage(@Valid ErpStockOutPageReqVO pageReqVO) {
        return success(buildStockOutVOPageResult(stockOutService.getStockOutPage(pageReqVO)));
    }

    @GetMapping("/dept-simple-list")
    @Operation(summary = "Get visible department list for stock out filter")
    @PreAuthorize("@ss.hasPermission('erp:stock-out:query')")
    public CommonResult<List<DeptSimpleRespVO>> getVisibleDeptSimpleList() {
        return success(dataPermissionDeptService.getDeptSimpleList(FIELD_PERMISSION_MODULE));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "Export stock out")
    @PreAuthorize("@ss.hasPermission('erp:stock-out:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportStockOutExcel(@Valid ErpStockOutPageReqVO pageReqVO,
                                    HttpServletResponse response) throws IOException {
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(EXPORT_MAX_COUNT);
        PageResult<ErpStockOutDO> pageResult = stockOutService.getStockOutPage(pageReqVO);
        if (pageResult.getTotal() > EXPORT_MAX_COUNT) {
            throw exception(EXPORT_COUNT_EXCEEDED);
        }
        List<ErpStockOutRespVO> list = buildStockOutVOPageResult(pageResult).getList();
        ExcelUtils.write(response, "stock-out.xls", "data", ErpStockOutRespVO.class, list);
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "Get stock out import template")
    @PreAuthorize("@ss.hasPermission('erp:stock-out:import')")
    public void getImportTemplate(HttpServletResponse response) throws IOException {
        ErpStockImportExcelVO first = new ErpStockImportExcelVO();
        first.setOrderNo("OUT-001");
        first.setCustomerName("示例客户");
        first.setBizTime("2026-07-01 09:00:00");
        first.setWarehouseName("示例仓库");
        first.setProductCode("P0001");
        first.setCount(BigDecimal.ONE);
        first.setProductPrice(new BigDecimal("100.00"));
        first.setRemark("单据备注");
        first.setItemRemark("明细备注");
        ErpStockImportExcelVO second = new ErpStockImportExcelVO();
        second.setOrderNo("OUT-001");
        second.setWarehouseName("示例仓库");
        second.setProductCode("P0002");
        second.setCount(new BigDecimal("2"));
        second.setProductPrice(new BigDecimal("50.00"));
        ExcelUtils.writeImportTemplate(response, "其它出库导入模板.xls", "其它出库",
                ErpStockImportExcelVO.class, Arrays.asList(first, second), IMPORT_TEMPLATE_FIELDS);
    }

    @PostMapping("/import")
    @Operation(summary = "Import stock out")
    @PreAuthorize("@ss.hasPermission('erp:stock-out:import')")
    public CommonResult<ErpStockImportResultRespVO> importStockOut(@RequestParam("file") MultipartFile file)
            throws Exception {
        return success(stockImportService.importStockOutList(ExcelUtils.read(file, ErpStockImportExcelVO.class)));
    }

    private PageResult<ErpStockOutRespVO> buildStockOutVOPageResult(PageResult<ErpStockOutDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        List<ErpStockOutItemDO> itemList = stockOutService.getStockOutItemListByOutIds(
                convertSet(pageResult.getList(), ErpStockOutDO::getId));
        Map<Long, List<ErpStockOutItemDO>> itemMap = convertMultiMap(itemList, ErpStockOutItemDO::getOutId);
        Map<Long, ErpProductRespVO> productMap = DataPermissionUtils.executeIgnore(() -> productService.getProductVOMap(
                convertSet(itemList, ErpStockOutItemDO::getProductId)));
        Map<Long, ErpWarehouseDO> warehouseMap = DataPermissionUtils.executeIgnore(() ->
                warehouseService.getWarehouseMap(convertSet(itemList, ErpStockOutItemDO::getWarehouseId)));
        Map<Long, ErpCustomerDO> customerMap = customerService.getCustomerMap(
                convertSet(pageResult.getList(), ErpStockOutDO::getCustomerId));
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(convertSet(pageResult.getList(), ErpStockOutDO::getDeptId));
        Set<Long> userIds = new HashSet<>();
        pageResult.getList().forEach(stockOut -> {
            addUserId(userIds, stockOut.getCreator());
            addUserId(userIds, stockOut.getUpdater());
        });
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);

        return BeanUtils.toBean(pageResult, ErpStockOutRespVO.class, vo -> {
            vo.setItems(BeanUtils.toBean(itemMap.get(vo.getId()), ErpStockOutRespVO.Item.class,
                    item -> fillProduct(item, productMap.get(item.getProductId()))));
            vo.setProductNames(CollUtil.join(vo.getItems(), ", ", ErpStockOutRespVO.Item::getProductName));
            vo.setProductCodes(CollUtil.join(vo.getItems(), ", ", ErpStockOutRespVO.Item::getProductCode));
            fillListSummary(vo, itemMap.get(vo.getId()), warehouseMap);
            MapUtils.findAndThen(customerMap, vo.getCustomerId(), customer -> vo.setCustomerName(customer.getName()));
            MapUtils.findAndThen(deptMap, vo.getDeptId(), dept -> vo.setDeptName(dept.getName()));
            fillUserNames(vo, userMap);
        });
    }

    private void fillListSummary(ErpStockOutRespVO vo, List<ErpStockOutItemDO> items,
                                 Map<Long, ErpWarehouseDO> warehouseMap) {
        if (CollUtil.isEmpty(items)) {
            vo.setWarehouseNames("");
            vo.setItemCount(0);
            return;
        }
        vo.setItemCount(items.size());
        Set<String> warehouseNames = new LinkedHashSet<>();
        items.forEach(item -> {
            ErpWarehouseDO warehouse = warehouseMap.get(item.getWarehouseId());
            if (warehouse != null && warehouse.getName() != null && !warehouse.getName().trim().isEmpty()) {
                warehouseNames.add(warehouse.getName());
            }
        });
        vo.setWarehouseNames(String.join("、", warehouseNames));
    }

    private void fillProduct(ErpStockOutRespVO.Item item, ErpProductRespVO product) {
        if (product == null) {
            return;
        }
        item.setProductName(product.getName())
                .setProductCode(product.getCode())
                .setProductBarCode(product.getBarCode())
                .setProductUnitName(product.getUnitName());
    }

    private void fillUserNames(ErpStockOutRespVO stockOut, Map<Long, AdminUserRespDTO> userMap) {
        Long creatorId = parseUserId(stockOut.getCreator());
        if (creatorId != null) {
            MapUtils.findAndThen(userMap, creatorId, user -> stockOut.setCreatorName(user.getNickname()));
        }
        Long updaterId = parseUserId(stockOut.getUpdater());
        if (updaterId != null) {
            MapUtils.findAndThen(userMap, updaterId, user -> stockOut.setUpdaterName(user.getNickname()));
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
