package cn.iocoder.yudao.module.erp.controller.admin.stock;

import cn.iocoder.yudao.module.erp.service.stock.ErpStockItemPriceReferenceFiller;
import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.common.ErpAuditStatusRequestValidator;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutPickDeliveryOperationItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery.ErpSalePickDeliveryItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery.ErpSalePickDeliverySummaryRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery.ErpSalePickSourceDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.ErpStockDocumentSummaryRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.ErpStockItemMatchReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.ErpStockItemMatchRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.ErpStockUpdateRemarkReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.imports.ErpStockImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.imports.ErpStockImportResultRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.in.ErpStockInItemBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.in.ErpStockInItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.in.ErpStockInPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.in.ErpStockInPickPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.in.ErpStockInRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.in.ErpStockInSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpSupplierDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockInItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockInItemMapper;
import cn.iocoder.yudao.module.erp.enums.print.ErpPrintModuleEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleBizSourceTypeEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSalePickStatusEnum;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockInStatusEnum;
import cn.iocoder.yudao.module.erp.service.base.ErpDataPermissionDeptService;
import cn.iocoder.yudao.module.erp.service.common.ErpImportExportRecordService;
import cn.iocoder.yudao.module.erp.service.common.ErpPrintService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.purchase.ErpSupplierService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSalePickDeliveryService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockImportService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockInService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserSimpleRespVO;
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
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpExportFieldRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.in.ErpStockInExportRespVO;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMultiMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_IN_NOT_EXISTS;

@Tag(name = "Admin - ERP stock in")
@RestController
@RequestMapping("/erp/stock-in")
@Validated
public class ErpStockInController {

    private static final int EXPORT_MAX_COUNT = 5000;
    private static final ErrorCode EXPORT_COUNT_EXCEEDED = new ErrorCode(1_030_590_001,
            "单次最多导出 5000 条入库单，请缩小筛选范围后重试");
    private static final String FIELD_PERMISSION_MODULE = "erp_stock_in";
    private static final Set<String> IMPORT_TEMPLATE_FIELDS = new LinkedHashSet<>(Arrays.asList(
            "orderNo", "supplierName", "warehouseName", "productCode", "productName", "factoryCode", "count", "productPrice",
            "remark", "itemRemark"));

    @Resource
    private ErpStockInService stockInService;
    @Resource
    private ErpSalePickDeliveryService pickDeliveryService;
    @Resource
    private ErpPrintService printService;
    @Resource
    private ErpImportExportRecordService importExportRecordService;
    @Resource
    private ErpStockImportService stockImportService;
    @Resource
    private ErpStockService stockService;
    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private ErpProductService productService;
    @Resource
    private ErpSupplierService supplierService;
    @Resource
    private ErpStockFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private ErpStockItemPriceReferenceFiller itemPriceReferenceFiller;
    @Resource
    private ErpDataPermissionDeptService dataPermissionDeptService;
    @Resource
    private ErpStockInItemMapper stockInItemMapper;
    @Resource
    private DeptApi deptApi;
    @Resource
    private AdminUserApi adminUserApi;

    @PostMapping("/create")
    @Operation(summary = "Create stock in")
    @PreAuthorize("@ss.hasPermission('erp:stock-in:create')")
    public CommonResult<Long> createStockIn(@Valid @RequestBody ErpStockInSaveReqVO createReqVO) {
        return success(stockInService.createStockIn(createReqVO));
    }

    @PostMapping("/create-draft")
    @Operation(summary = "保存其它入库草稿")
    @PreAuthorize("@ss.hasPermission('erp:stock-in:create')")
    public CommonResult<Long> createStockInDraft(@Valid @RequestBody ErpStockInSaveReqVO createReqVO) {
        return success(stockInService.createStockInDraft(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "Update stock in")
    @PreAuthorize("@ss.hasPermission('erp:stock-in:update')")
    public CommonResult<Boolean> updateStockIn(@Valid @RequestBody ErpStockInSaveReqVO updateReqVO) {
        stockInService.updateStockIn(updateReqVO);
        return success(true);
    }

    @PutMapping("/batch-update-items")
    @Operation(summary = "Batch update stock in item warehouse")
    @PreAuthorize("@ss.hasPermission('erp:stock-in:update')")
    public CommonResult<Boolean> batchUpdateStockInItems(
            @Valid @RequestBody ErpStockInItemBatchUpdateReqVO updateReqVO) {
        stockInService.batchUpdateStockInItems(updateReqVO);
        return success(true);
    }

    @GetMapping("/warehouse-dept-simple-list")
    @Operation(summary = "Get available department list for stock in item warehouse")
    @PreAuthorize("@ss.hasPermission('erp:stock-in:update')")
    public CommonResult<List<DeptSimpleRespVO>> getWarehouseDeptSimpleList(@RequestParam("warehouseId") Long warehouseId) {
        return success(stockInService.getWarehouseDeptSimpleList(warehouseId));
    }

    @PutMapping("/update-remark")
    @Operation(summary = "Update stock in remark")
    @PreAuthorize("@ss.hasPermission('erp:stock-in:update')")
    public CommonResult<Boolean> updateStockInRemark(
            @Valid @RequestBody ErpStockUpdateRemarkReqVO updateReqVO) {
        stockInService.updateStockInRemark(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "Update stock in status")
    @PreAuthorize("@ss.hasPermission('erp:stock-in:update-status')")
    public CommonResult<Boolean> updateStockInStatus(@RequestParam("id") Long id,
                                                     @RequestParam("status") Integer status) {
        ErpAuditStatusRequestValidator.validateApproveStatus(status);
        stockInService.updateStockInStatus(id, status);
        return success(true);
    }

    @PutMapping("/confirm")
    @Operation(summary = "Confirm stock in and generate pick tasks")
    @PreAuthorize("@ss.hasPermission('erp:stock-in:confirm')")
    public CommonResult<Boolean> confirmStockIn(@RequestParam("id") Long id) {
        stockInService.confirmStockIn(id);
        return success(true);
    }

    @PutMapping("/cancel-confirm")
    @Operation(summary = "Cancel stock in confirmation")
    @PreAuthorize("@ss.hasPermission('erp:stock-in:cancel-confirm')")
    public CommonResult<Boolean> cancelConfirmStockIn(@RequestParam("id") Long id) {
        stockInService.cancelConfirmStockIn(id);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "Delete stock in")
    @Parameter(name = "ids", description = "ids", required = true)
    @PreAuthorize("@ss.hasPermission('erp:stock-in:delete')")
    public CommonResult<Boolean> deleteStockIn(@RequestParam("ids") List<Long> ids) {
        stockInService.deleteStockIn(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "Get stock in")
    @Parameter(name = "id", description = "id", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:stock-in:query')")
    public CommonResult<ErpStockInRespVO> getStockIn(@RequestParam("id") Long id,
                                                     @RequestParam(value = "includeItems", required = false,
                                                             defaultValue = "true") Boolean includeItems) {
        ErpStockInDO stockIn = stockInService.getStockIn(id);
        if (stockIn == null) {
            return success(null);
        }
        List<ErpStockInItemDO> itemList = Boolean.TRUE.equals(includeItems)
                ? stockInService.getStockInItemListByInId(id) : Collections.emptyList();
        Map<Long, ErpWarehouseDO> warehouseMap = DataPermissionUtils.executeIgnore(() -> warehouseService.getWarehouseMap(
                convertSet(itemList, ErpStockInItemDO::getWarehouseId)));
        Set<Long> userIds = new HashSet<>();
        addUserId(userIds, stockIn.getCreator());
        addUserId(userIds, stockIn.getUpdater());
        addUserId(userIds, stockIn.getConfirmUserId());
        addUserId(userIds, stockIn.getApproveUserId());
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        DeptRespDTO dept = stockIn.getDeptId() == null ? null : deptApi.getDept(stockIn.getDeptId());

        ErpStockInRespVO respVO = BeanUtils.toBean(stockIn, ErpStockInRespVO.class, vo -> {
            if (Boolean.TRUE.equals(includeItems)) {
                vo.setItems(buildStockInItemVOList(itemList, true));
                vo.setWarehouseNames(buildWarehouseNames(itemList, warehouseMap));
                vo.setProductNames(CollUtil.join(vo.getItems(), ", ", ErpStockInRespVO.Item::getProductName));
                vo.setProductCodes(CollUtil.join(vo.getItems(), ", ", ErpStockInRespVO.Item::getProductCode));
            }
            if (dept != null) {
                vo.setDeptName(dept.getName());
            }
            fillUserNames(vo, userMap);
            fillWorkflowInfo(vo, pickDeliveryService.getSummaryMapByStockInIds(
                    Collections.singleton(id)).get(id));
        });
        fillSummary(respVO, stockInItemMapper.selectSummaryMapByInIds(Collections.singleton(id)).get(id));
        fillPrintInfo(Collections.singletonList(respVO));
        itemPriceReferenceFiller.fill(respVO.getItems());
        fieldPermissionMasker.maskFormWithItems(FIELD_PERMISSION_MODULE, respVO);
        return success(respVO);
    }

    @GetMapping("/pick-detail")
    @Operation(summary = "获得其它入库拣货详情")
    @Parameter(name = "id", description = "其它入库编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:stock-in:query')")
    public CommonResult<ErpSalePickSourceDetailRespVO> getStockInPickDetail(@RequestParam("id") Long id) {
        validateReadableStockIn(id);
        return success(pickDeliveryService.getSourcePickDetail(
                ErpSaleBizSourceTypeEnum.STOCK_IN.getType(), id));
    }

    @GetMapping("/pick-item-page")
    @Operation(summary = "获得其它入库累计拣货明细分页")
    @PreAuthorize("@ss.hasPermission('erp:stock-in:query')")
    public CommonResult<PageResult<ErpSalePickDeliveryItemRespVO>> getStockInPickItemPage(
            @Valid ErpStockInPickPageReqVO pageReqVO) {
        validateReadableStockIn(pageReqVO.getInId());
        return success(pickDeliveryService.getSourcePickItemPage(
                ErpSaleBizSourceTypeEnum.STOCK_IN.getType(), pageReqVO.getInId(), pageReqVO));
    }

    @GetMapping("/pick-operation-item-page")
    @Operation(summary = "获得其它入库分批拣货人员明细分页")
    @PreAuthorize("@ss.hasPermission('erp:stock-in:query')")
    public CommonResult<PageResult<ErpSaleOutPickDeliveryOperationItemRespVO>>
    getStockInPickOperationItemPage(@Valid ErpStockInPickPageReqVO pageReqVO) {
        validateReadableStockIn(pageReqVO.getInId());
        return success(pickDeliveryService.getSourcePickOperationItemPage(
                ErpSaleBizSourceTypeEnum.STOCK_IN.getType(), pageReqVO.getInId(), pageReqVO));
    }

    private ErpStockInDO validateReadableStockIn(Long id) {
        ErpStockInDO stockIn = stockInService.getStockIn(id);
        if (stockIn == null) {
            throw exception(STOCK_IN_NOT_EXISTS);
        }
        return stockIn;
    }

    @GetMapping("/item-page")
    @Operation(summary = "Get stock in item page")
    @PreAuthorize("@ss.hasPermission('erp:stock-in:query')")
    public CommonResult<PageResult<ErpStockInRespVO.Item>> getStockInItemPage(
            @Valid ErpStockInItemPageReqVO pageReqVO) {
        PageResult<ErpStockInItemDO> pageResult = stockInService.getStockInItemPage(pageReqVO);
        PageResult<ErpStockInRespVO.Item> respResult = new PageResult<>(
                buildStockInItemVOList(pageResult.getList(), true), pageResult.getTotal());
        itemPriceReferenceFiller.fill(respResult.getList());
        if (Boolean.TRUE.equals(pageReqVO.getMask())) {
            fieldPermissionMasker.clearHiddenItemFields(FIELD_PERMISSION_MODULE, respResult.getList());
        }
        return success(respResult);
    }

    @PostMapping("/item-page")
    @Operation(summary = "Get stock in item page for editing")
    @PreAuthorize("@ss.hasPermission('erp:stock-in:query')")
    public CommonResult<PageResult<ErpStockInRespVO.Item>> getStockInEditItemPage(
            @Valid @RequestBody ErpStockInItemPageReqVO pageReqVO) {
        return getStockInItemPage(pageReqVO);
    }

    @PostMapping("/item-match")
    @Operation(summary = "Match stock in items across pages")
    @PreAuthorize("@ss.hasPermission('erp:stock-in:query')")
    public CommonResult<ErpStockItemMatchRespVO> matchStockInItems(
            @Valid @RequestBody ErpStockItemMatchReqVO reqVO) {
        if (stockInService.getStockIn(reqVO.getDocumentId()) == null) {
            throw exception(STOCK_IN_NOT_EXISTS);
        }
        List<ErpStockInItemDO> items = stockInService.getStockInItemListByInId(reqVO.getDocumentId());
        return success(ErpStockItemMatchUtils.match(items, reqVO, ErpStockInItemDO::getId,
                (item, key) -> java.util.Objects.equals(item.getProductId(), key.getProductId())
                        && java.util.Objects.equals(item.getWarehouseId(), key.getWarehouseId())
                        && ErpStockItemMatchUtils.normalizeText(item.getBatchNo())
                        .equals(ErpStockItemMatchUtils.normalizeText(key.getBatchNo()))));
    }

    @GetMapping("/page")
    @Operation(summary = "Get stock in page")
    @PreAuthorize("@ss.hasPermission('erp:stock-in:query')")
    public CommonResult<PageResult<ErpStockInRespVO>> getStockInPage(@Valid ErpStockInPageReqVO pageReqVO) {
        PageResult<ErpStockInDO> pageResult = stockInService.getStockInPage(pageReqVO);
        PageResult<ErpStockInRespVO> result = Boolean.FALSE.equals(pageReqVO.getIncludeItems())
                ? buildStockInVOPageResultWithoutItems(pageResult) : buildStockInVOPageResult(pageResult);
        fieldPermissionMasker.maskListColumns(FIELD_PERMISSION_MODULE, result.getList());
        Set<String> hidden = otherExportHiddenFields();
        result.getList().forEach(row -> ErpStockBillExportSupport.mask(row, hidden));
        return success(result);
    }

    @GetMapping("/summary")
    @Operation(summary = "Get stock in query summary")
    @PreAuthorize("@ss.hasPermission('erp:stock-in:query')")
    public CommonResult<ErpStockDocumentSummaryRespVO> getStockInSummary(@Valid ErpStockInPageReqVO pageReqVO) {
        ErpStockDocumentSummaryRespVO summary = stockInService.getStockInSummary(pageReqVO);
        fieldPermissionMasker.maskListColumns(FIELD_PERMISSION_MODULE, summary);
        ErpStockBillExportSupport.mask(summary, otherExportHiddenFields());
        return success(summary);
    }

    @GetMapping("/dept-simple-page")
    @Operation(summary = "Get visible department page for stock in filter")
    @PreAuthorize("@ss.hasPermission('erp:stock-in:query')")
    public CommonResult<PageResult<DeptSimpleRespVO>> getVisibleDeptSimplePage(@Valid PageParam pageReqVO) {
        return success(dataPermissionDeptService.getDeptSimplePage(FIELD_PERMISSION_MODULE, pageReqVO));
    }

    @GetMapping("/user-simple-page")
    @Operation(summary = "Get user page for stock in filter")
    @PreAuthorize("@ss.hasPermission('erp:stock-in:query')")
    public CommonResult<PageResult<UserSimpleRespVO>> getUserSimplePage(@Valid PageParam pageReqVO) {
        return success(buildUserSimplePage(pageReqVO));
    }

    @GetMapping("/dept-simple-list")
    @Operation(summary = "Get visible department list for stock in filter")
    @PreAuthorize("@ss.hasPermission('erp:stock-in:query')")
    public CommonResult<List<DeptSimpleRespVO>> getVisibleDeptSimpleList() {
        return success(dataPermissionDeptService.getDeptSimpleList(FIELD_PERMISSION_MODULE));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "Export stock in")
    @PreAuthorize("@ss.hasPermission('erp:stock-in:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportStockInExcel(@Valid ErpStockInPageReqVO pageReqVO,
            @RequestParam(value = "fields", required = false) String fields,
            HttpServletResponse response) throws IOException {
        ErpStockMovementExportSupport.write(response, "stock-in", fields,
                ErpStockInRespVO.class, ErpStockInExportRespVO.class, otherExportHiddenFields(),
                pageNo -> { pageReqVO.setPageNo(pageNo); pageReqVO.setPageSize(100); pageReqVO.setIncludeItems(false); return stockInService.getStockInPage(pageReqVO); },
                page -> buildStockInVOPageResultWithoutItems(page).getList(),
                (ids, pageNo) -> stockInItemMapper.selectExportPageByInIds(ids, pageNo).getList(),
                ErpStockInItemDO::getInId,
                batch -> buildOtherExportItems(batch));
    }

    @GetMapping("/export-fields")
    @Operation(summary = "Get other stock in export fields")
    @PreAuthorize("@ss.hasPermission('erp:stock-in:export')")
    public CommonResult<List<ErpExportFieldRespVO>> getStockInExportFields() {
        return success(ErpStockMovementExportSupport.fields(ErpStockInExportRespVO.class, otherExportHiddenFields()));
    }

    private Set<String> otherExportHiddenFields() {
        return ErpOtherStockExportSupport.hidden(ErpStockInExportRespVO.class,
                fieldPermissionMasker.getHiddenFieldSet(FIELD_PERMISSION_MODULE));
    }

    private List<ErpOtherStockExportSupport.Item> buildOtherExportItems(List<ErpStockInItemDO> batch) {
        Map<Long, ErpWarehouseDO> warehouses = warehouseService.getWarehouseMap(convertSet(batch, ErpStockInItemDO::getWarehouseId));
        Map<Long, DeptRespDTO> departments = deptApi.getDeptMap(convertSet(warehouses.values(), ErpWarehouseDO::getDeptId));
        Map<Long, ErpProductRespVO> products = DataPermissionUtils.executeIgnore(() -> productService.getProductVOMap(convertSet(batch, ErpStockInItemDO::getProductId)));
        return convertList(batch, item -> {
            ErpOtherStockExportSupport.Item display = BeanUtils.toBean(item, ErpOtherStockExportSupport.Item.class);
            ErpProductRespVO product = products.get(item.getProductId());
            if (product != null) display.setProductCode(product.getCode()).setProductName(product.getName())
                    .setProductBarCode(product.getBarCode()).setProductUnitName(product.getUnitName())
                    .setFactoryCode(product.getFactoryCode()).setDrawingNo(product.getDrawingNo()).setBrand(product.getBrand())
                    .setVehicleModel(product.getVehicleModel()).setOriginPlace(product.getOriginPlace()).setShelf(product.getShelf());
            ErpWarehouseDO warehouse = warehouses.get(item.getWarehouseId());
            if (warehouse != null) {
                display.setWarehouseName(warehouse.getName());
                DeptRespDTO department = departments.get(warehouse.getDeptId());
                if (department != null) display.setWarehouseDeptName(department.getName());
            }
            return display;
        });
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "Get stock in import template")
    @PreAuthorize("@ss.hasPermission('erp:stock-in:import')")
    public void getImportTemplate(HttpServletResponse response) throws IOException {
        ErpStockImportExcelVO first = new ErpStockImportExcelVO();
        first.setOrderNo("IN-001");
        first.setSupplierName("示例供应商");
        first.setWarehouseName("示例仓库");
        first.setProductCode("P0001");
        first.setCount(BigDecimal.ONE);
        first.setProductPrice(new BigDecimal("100.00"));
        first.setRemark("单据备注");
        first.setItemRemark("明细备注");
        ErpStockImportExcelVO second = new ErpStockImportExcelVO();
        second.setOrderNo("IN-001");
        second.setWarehouseName("示例仓库");
        second.setProductCode("P0002");
        second.setCount(new BigDecimal("2"));
        second.setProductPrice(new BigDecimal("50.00"));
        ExcelUtils.writeImportTemplate(response, "其它入库导入模板.xls", "其它入库",
                ErpStockImportExcelVO.class, Arrays.asList(first, second), IMPORT_TEMPLATE_FIELDS);
    }

    @PostMapping("/import")
    @Operation(summary = "Import stock in")
    @PreAuthorize("@ss.hasPermission('erp:stock-in:import')")
    public CommonResult<ErpStockImportResultRespVO> importStockIn(@RequestParam("file") MultipartFile file)
            throws Exception {
        return success(stockImportService.importStockInList(ExcelUtils.read(file, ErpStockImportExcelVO.class)));
    }

    @GetMapping("/import-failure-details/download")
    @Operation(summary = "Download stock in import failure details")
    @PreAuthorize("@ss.hasPermission('erp:stock-in:import')")
    public void downloadImportFailureDetails(@RequestParam("recordId") Long recordId,
                                             HttpServletResponse response) throws IOException {
        importExportRecordService.downloadOwnImportFailureDetails(recordId, FIELD_PERMISSION_MODULE, response);
    }

    private PageResult<ErpStockInRespVO> buildStockInVOPageResult(PageResult<ErpStockInDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        List<ErpStockInItemDO> itemList = stockInService.getStockInItemListByInIds(
                convertSet(pageResult.getList(), ErpStockInDO::getId));
        Map<Long, List<ErpStockInItemDO>> itemMap = convertMultiMap(itemList, ErpStockInItemDO::getInId);
        Map<Long, ErpProductRespVO> productMap = DataPermissionUtils.executeIgnore(() -> productService.getProductVOMap(
                convertSet(itemList, ErpStockInItemDO::getProductId)));
        Map<Long, ErpWarehouseDO> warehouseMap = DataPermissionUtils.executeIgnore(() -> warehouseService.getWarehouseMap(
                convertSet(itemList, ErpStockInItemDO::getWarehouseId)));
        Map<Long, ErpSupplierDO> supplierMap = supplierService.getSupplierMap(
                convertSet(pageResult.getList(), ErpStockInDO::getSupplierId));
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(convertSet(pageResult.getList(), ErpStockInDO::getDeptId));
        Set<Long> userIds = new HashSet<>();
        pageResult.getList().forEach(stockIn -> {
            addUserId(userIds, stockIn.getCreator());
            addUserId(userIds, stockIn.getUpdater());
            addUserId(userIds, stockIn.getConfirmUserId());
            addUserId(userIds, stockIn.getApproveUserId());
        });
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        Map<Long, ErpSalePickDeliverySummaryRespVO> pickSummaryMap = pickDeliveryService.getSummaryMapByStockInIds(
                convertSet(pageResult.getList(), ErpStockInDO::getId));

        PageResult<ErpStockInRespVO> result = BeanUtils.toBean(pageResult, ErpStockInRespVO.class, vo -> {
            List<ErpStockInItemDO> stockInItems = itemMap.get(vo.getId());
            vo.setItems(BeanUtils.toBean(stockInItems, ErpStockInRespVO.Item.class,
                    item -> fillProduct(item, productMap.get(item.getProductId()))));
            vo.setWarehouseNames(buildWarehouseNames(stockInItems, warehouseMap));
            vo.setProductNames(CollUtil.join(vo.getItems(), ", ", ErpStockInRespVO.Item::getProductName));
            vo.setProductCodes(CollUtil.join(vo.getItems(), ", ", ErpStockInRespVO.Item::getProductCode));
            MapUtils.findAndThen(supplierMap, vo.getSupplierId(), supplier -> vo.setSupplierName(supplier.getName()));
            MapUtils.findAndThen(deptMap, vo.getDeptId(), dept -> vo.setDeptName(dept.getName()));
            fillUserNames(vo, userMap);
            fillWorkflowInfo(vo, pickSummaryMap.get(vo.getId()));
        });
        fillPrintInfo(result.getList());
        return result;
    }

    private PageResult<ErpStockInRespVO> buildStockInVOPageResultWithoutItems(PageResult<ErpStockInDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        Map<Long, Map<String, Object>> summaryMap = stockInItemMapper.selectSummaryMapByInIds(
                convertSet(pageResult.getList(), ErpStockInDO::getId));
        Map<Long, ErpSupplierDO> supplierMap = supplierService.getSupplierMap(
                convertSet(pageResult.getList(), ErpStockInDO::getSupplierId));
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(convertSet(pageResult.getList(), ErpStockInDO::getDeptId));
        Set<Long> userIds = new HashSet<>();
        pageResult.getList().forEach(stockIn -> {
            addUserId(userIds, stockIn.getCreator());
            addUserId(userIds, stockIn.getUpdater());
            addUserId(userIds, stockIn.getConfirmUserId());
            addUserId(userIds, stockIn.getApproveUserId());
        });
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        Map<Long, ErpSalePickDeliverySummaryRespVO> pickSummaryMap = pickDeliveryService.getSummaryMapByStockInIds(
                convertSet(pageResult.getList(), ErpStockInDO::getId));
        PageResult<ErpStockInRespVO> result = BeanUtils.toBean(pageResult, ErpStockInRespVO.class, vo -> {
            Map<String, Object> summary = summaryMap.get(vo.getId());
            fillSummary(vo, summary);
            MapUtils.findAndThen(supplierMap, vo.getSupplierId(), supplier -> vo.setSupplierName(supplier.getName()));
            MapUtils.findAndThen(deptMap, vo.getDeptId(), dept -> vo.setDeptName(dept.getName()));
            fillUserNames(vo, userMap);
            fillWorkflowInfo(vo, pickSummaryMap.get(vo.getId()));
        });
        fillPrintInfo(result.getList());
        return result;
    }

    private void fillSummary(ErpStockInRespVO vo, Map<String, Object> summary) {
        if (summary == null) {
            vo.setWarehouseNames("");
            vo.setTotalCount(BigDecimal.ZERO);
            vo.setTotalPrice(BigDecimal.ZERO);
            vo.setTotalWeight(BigDecimal.ZERO);
            return;
        }
        Object itemCount = summary.get("itemCount");
        vo.setItemCount(itemCount == null ? 0 : ((Number) itemCount).intValue());
        vo.setProductNames((String) summary.get("productNames"));
        vo.setProductCodes((String) summary.get("productCodes"));
        vo.setWarehouseNames((String) summary.get("warehouseNames"));
        vo.setTotalCount(toBigDecimal(summary.get("totalCount")));
        vo.setTotalPrice(toBigDecimal(summary.get("totalPrice")));
        vo.setTotalWeight(toBigDecimal(summary.get("totalWeight")));
    }

    private BigDecimal toBigDecimal(Object value) {
        return value == null ? BigDecimal.ZERO : new BigDecimal(value.toString());
    }

    private PageResult<UserSimpleRespVO> buildUserSimplePage(PageParam pageReqVO) {
        PageResult<AdminUserRespDTO> page = adminUserApi.getUserSimplePage(
                CommonStatusEnum.ENABLE.getStatus(), pageReqVO.getKeyword(), pageReqVO);
        List<UserSimpleRespVO> list = convertList(page.getList(), user ->
                new UserSimpleRespVO(user.getId(), user.getNickname(), user.getDeptId(), null));
        return new PageResult<>(list, page.getTotal());
    }

    private List<ErpStockInRespVO.Item> buildStockInItemVOList(List<ErpStockInItemDO> itemList,
                                                              boolean includeStockCount) {
        if (CollUtil.isEmpty(itemList)) {
            return Collections.emptyList();
        }
        Map<Long, ErpProductRespVO> productMap = DataPermissionUtils.executeIgnore(() -> productService.getProductVOMap(
                convertSet(itemList, ErpStockInItemDO::getProductId)));
        Map<String, ErpStockDO> stockMap = includeStockCount
                ? DataPermissionUtils.executeIgnore(() -> stockService.getStockMap(
                convertSet(itemList, ErpStockInItemDO::getProductId),
                convertSet(itemList, ErpStockInItemDO::getWarehouseId)))
                : Collections.emptyMap();
        return BeanUtils.toBean(itemList, ErpStockInRespVO.Item.class, item -> {
            if (includeStockCount) {
                ErpStockDO stock = stockMap.get(item.getProductId() + "_" + item.getWarehouseId());
                item.setStockCount(stock != null ? stock.getCount() : BigDecimal.ZERO);
            }
            fillProduct(item, productMap.get(item.getProductId()));
        });
    }

    private void fillPrintInfo(List<ErpStockInRespVO> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        Set<Long> ids = convertSet(list, ErpStockInRespVO::getId);
        Map<Long, Long> countMap = printService.getPrintCountMap(ErpPrintModuleEnum.STOCK_IN.getKey(), ids);
        Map<Long, LocalDateTime> lastPrintTimeMap = printService.getLastPrintTimeMap(
                ErpPrintModuleEnum.STOCK_IN.getKey(), ids);
        list.forEach(vo -> {
            Long printCount = countMap.get(vo.getId());
            vo.setPrintCount(printCount == null ? 0 : printCount.intValue());
            vo.setLastPrintTime(lastPrintTimeMap.get(vo.getId()));
        });
    }

    private String buildWarehouseNames(List<ErpStockInItemDO> itemList, Map<Long, ErpWarehouseDO> warehouseMap) {
        if (CollUtil.isEmpty(itemList) || CollUtil.isEmpty(warehouseMap)) {
            return "";
        }
        Set<String> warehouseNames = new LinkedHashSet<>();
        itemList.forEach(item -> {
            ErpWarehouseDO warehouse = warehouseMap.get(item.getWarehouseId());
            if (warehouse != null && warehouse.getName() != null) {
                warehouseNames.add(warehouse.getName());
            }
        });
        return CollUtil.join(warehouseNames, ", ");
    }

    private void fillProduct(ErpStockInRespVO.Item item, ErpProductRespVO product) {
        if (product == null) {
            return;
        }
        item.setProductName(product.getName())
                .setProductCode(product.getCode())
                .setProductBarCode(product.getBarCode())
                .setProductUnitName(product.getUnitName());
    }

    private void fillUserNames(ErpStockInRespVO stockIn, Map<Long, AdminUserRespDTO> userMap) {
        Long creatorId = parseUserId(stockIn.getCreator());
        if (creatorId != null) {
            MapUtils.findAndThen(userMap, creatorId, user -> stockIn.setCreatorName(user.getNickname()));
        }
        Long updaterId = parseUserId(stockIn.getUpdater());
        if (updaterId != null) {
            MapUtils.findAndThen(userMap, updaterId, user -> stockIn.setUpdaterName(user.getNickname()));
        }
        MapUtils.findAndThen(userMap, stockIn.getConfirmUserId(),
                user -> stockIn.setConfirmUserName(user.getNickname()));
        MapUtils.findAndThen(userMap, stockIn.getApproveUserId(),
                user -> stockIn.setApproveUserName(user.getNickname()));
    }

    private void fillWorkflowInfo(ErpStockInRespVO stockIn, ErpSalePickDeliverySummaryRespVO summary) {
        stockIn.setStatusName(ErpStockInStatusEnum.getNameByStatus(stockIn.getStatus()));
        if (summary == null) {
            return;
        }
        stockIn.setPickStatus(summary.getPickStatus())
                .setPickStatusName(Arrays.stream(ErpSalePickStatusEnum.values())
                        .filter(value -> value.getStatus().equals(summary.getPickStatus()))
                        .map(ErpSalePickStatusEnum::getName).findFirst().orElse(null))
                .setPickTotalItemCount(summary.getTotalItemCount())
                .setPickedItemCount(summary.getPickedItemCount())
                .setPickProgress(summary.getPickProgress());
    }

    private void addUserId(Set<Long> userIds, String userId) {
        Long parsed = parseUserId(userId);
        if (parsed != null) {
            userIds.add(parsed);
        }
    }

    private void addUserId(Set<Long> userIds, Long userId) {
        if (userId != null) {
            userIds.add(userId);
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
