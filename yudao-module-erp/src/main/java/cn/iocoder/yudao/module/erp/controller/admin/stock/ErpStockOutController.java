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
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.out.ErpStockOutItemBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.out.ErpStockOutItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.out.ErpStockOutPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.out.ErpStockOutPickPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.out.ErpStockOutRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.out.ErpStockOutSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockOutItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockOutItemMapper;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSalePickStatusEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleBizSourceTypeEnum;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockOutStatusEnum;
import cn.iocoder.yudao.module.erp.service.base.ErpDataPermissionDeptService;
import cn.iocoder.yudao.module.erp.service.common.ErpImportExportRecordService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSalePickDeliveryService;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpExportFieldRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.out.ErpStockOutExportRespVO;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMultiMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_OUT_NOT_EXISTS;

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
            "orderNo", "customerName", "warehouseName", "productCode", "productName", "factoryCode", "count", "productPrice",
            "remark", "itemRemark"));

    @Resource
    private ErpStockOutService stockOutService;
    @Resource
    private ErpSalePickDeliveryService pickDeliveryService;
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
    private ErpCustomerService customerService;
    @Resource
    private ErpStockFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private ErpStockItemPriceReferenceFiller itemPriceReferenceFiller;
    @Resource
    private ErpDataPermissionDeptService dataPermissionDeptService;
    @Resource
    private ErpStockOutItemMapper stockOutItemMapper;
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

    @PostMapping("/create-draft")
    @Operation(summary = "保存其它出库草稿")
    @PreAuthorize("@ss.hasPermission('erp:stock-out:create')")
    public CommonResult<Long> createStockOutDraft(@Valid @RequestBody ErpStockOutSaveReqVO createReqVO) {
        return success(stockOutService.createStockOutDraft(createReqVO));
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

    @PutMapping("/confirm")
    @Operation(summary = "Confirm stock out and generate pick tasks")
    @PreAuthorize("@ss.hasPermission('erp:stock-out:confirm')")
    public CommonResult<Boolean> confirmStockOut(@RequestParam("id") Long id) {
        stockOutService.confirmStockOut(id);
        return success(true);
    }

    @PutMapping("/cancel-confirm")
    @Operation(summary = "Cancel stock out confirmation")
    @PreAuthorize("@ss.hasPermission('erp:stock-out:cancel-confirm')")
    public CommonResult<Boolean> cancelConfirmStockOut(@RequestParam("id") Long id) {
        stockOutService.cancelConfirmStockOut(id);
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
    public CommonResult<ErpStockOutRespVO> getStockOut(@RequestParam("id") Long id,
                                                       @RequestParam(value = "includeItems", required = false,
                                                               defaultValue = "true") Boolean includeItems) {
        ErpStockOutDO stockOut = stockOutService.getStockOut(id);
        if (stockOut == null) {
            return success(null);
        }
        List<ErpStockOutItemDO> itemList = Boolean.TRUE.equals(includeItems)
                ? stockOutService.getStockOutItemListByOutId(id) : Collections.emptyList();
        Map<Long, ErpWarehouseDO> warehouseMap = DataPermissionUtils.executeIgnore(() ->
                warehouseService.getWarehouseMap(convertSet(itemList, ErpStockOutItemDO::getWarehouseId)));
        Set<Long> userIds = new HashSet<>();
        addUserId(userIds, stockOut.getCreator());
        addUserId(userIds, stockOut.getUpdater());
        addUserId(userIds, stockOut.getConfirmUserId());
        addUserId(userIds, stockOut.getApproveUserId());
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        DeptRespDTO dept = stockOut.getDeptId() == null ? null : deptApi.getDept(stockOut.getDeptId());

        ErpStockOutRespVO respVO = BeanUtils.toBean(stockOut, ErpStockOutRespVO.class, vo -> {
            if (Boolean.TRUE.equals(includeItems)) {
                vo.setItems(buildStockOutItemVOList(itemList, true));
                vo.setProductNames(CollUtil.join(vo.getItems(), ", ", ErpStockOutRespVO.Item::getProductName));
                vo.setProductCodes(CollUtil.join(vo.getItems(), ", ", ErpStockOutRespVO.Item::getProductCode));
                fillListSummary(vo, itemList, warehouseMap);
            }
            if (dept != null) {
                vo.setDeptName(dept.getName());
            }
            fillUserNames(vo, userMap);
            fillWorkflowInfo(vo, pickDeliveryService.getSummaryMapByStockOutIds(
                    Collections.singleton(id)).get(id));
        });
        fillSummary(respVO, stockOutItemMapper.selectSummaryMapByOutIds(Collections.singleton(id)).get(id));
        itemPriceReferenceFiller.fill(respVO.getItems());
        fieldPermissionMasker.maskFormWithItems(FIELD_PERMISSION_MODULE, respVO);
        return success(respVO);
    }

    @GetMapping("/pick-detail")
    @Operation(summary = "获得其它出库拣货详情")
    @Parameter(name = "id", description = "其它出库编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:stock-out:query')")
    public CommonResult<ErpSalePickSourceDetailRespVO> getStockOutPickDetail(@RequestParam("id") Long id) {
        validateReadableStockOut(id);
        return success(pickDeliveryService.getSourcePickDetail(
                ErpSaleBizSourceTypeEnum.STOCK_OUT.getType(), id));
    }

    @GetMapping("/pick-item-page")
    @Operation(summary = "获得其它出库累计拣货明细分页")
    @PreAuthorize("@ss.hasPermission('erp:stock-out:query')")
    public CommonResult<PageResult<ErpSalePickDeliveryItemRespVO>> getStockOutPickItemPage(
            @Valid ErpStockOutPickPageReqVO pageReqVO) {
        validateReadableStockOut(pageReqVO.getOutId());
        return success(pickDeliveryService.getSourcePickItemPage(
                ErpSaleBizSourceTypeEnum.STOCK_OUT.getType(), pageReqVO.getOutId(), pageReqVO));
    }

    @GetMapping("/pick-operation-item-page")
    @Operation(summary = "获得其它出库分批拣货人员明细分页")
    @PreAuthorize("@ss.hasPermission('erp:stock-out:query')")
    public CommonResult<PageResult<ErpSaleOutPickDeliveryOperationItemRespVO>>
    getStockOutPickOperationItemPage(@Valid ErpStockOutPickPageReqVO pageReqVO) {
        validateReadableStockOut(pageReqVO.getOutId());
        return success(pickDeliveryService.getSourcePickOperationItemPage(
                ErpSaleBizSourceTypeEnum.STOCK_OUT.getType(), pageReqVO.getOutId(), pageReqVO));
    }

    private ErpStockOutDO validateReadableStockOut(Long id) {
        ErpStockOutDO stockOut = stockOutService.getStockOut(id);
        if (stockOut == null) {
            throw exception(STOCK_OUT_NOT_EXISTS);
        }
        return stockOut;
    }

    @GetMapping("/item-page")
    @Operation(summary = "Get stock out item page")
    @PreAuthorize("@ss.hasPermission('erp:stock-out:query')")
    public CommonResult<PageResult<ErpStockOutRespVO.Item>> getStockOutItemPage(
            @Valid ErpStockOutItemPageReqVO pageReqVO) {
        PageResult<ErpStockOutItemDO> pageResult = stockOutService.getStockOutItemPage(pageReqVO);
        PageResult<ErpStockOutRespVO.Item> respResult = new PageResult<>(
                buildStockOutItemVOList(pageResult.getList(), true), pageResult.getTotal());
        itemPriceReferenceFiller.fill(respResult.getList());
        if (Boolean.TRUE.equals(pageReqVO.getMask())) {
            fieldPermissionMasker.clearHiddenItemFields(FIELD_PERMISSION_MODULE, respResult.getList());
        }
        return success(respResult);
    }

    @PostMapping("/item-page")
    @Operation(summary = "Get stock out item page for editing")
    @PreAuthorize("@ss.hasPermission('erp:stock-out:query')")
    public CommonResult<PageResult<ErpStockOutRespVO.Item>> getStockOutEditItemPage(
            @Valid @RequestBody ErpStockOutItemPageReqVO pageReqVO) {
        return getStockOutItemPage(pageReqVO);
    }

    @PostMapping("/item-match")
    @Operation(summary = "Match stock out items across pages")
    @PreAuthorize("@ss.hasPermission('erp:stock-out:query')")
    public CommonResult<ErpStockItemMatchRespVO> matchStockOutItems(
            @Valid @RequestBody ErpStockItemMatchReqVO reqVO) {
        if (stockOutService.getStockOut(reqVO.getDocumentId()) == null) {
            throw exception(STOCK_OUT_NOT_EXISTS);
        }
        List<ErpStockOutItemDO> items = stockOutService.getStockOutItemListByOutId(reqVO.getDocumentId());
        return success(ErpStockItemMatchUtils.match(items, reqVO, ErpStockOutItemDO::getId,
                (item, key) -> java.util.Objects.equals(item.getProductId(), key.getProductId())
                        && java.util.Objects.equals(item.getWarehouseId(), key.getWarehouseId())
                        && ErpStockItemMatchUtils.normalizeText(item.getBatchNo())
                        .equals(ErpStockItemMatchUtils.normalizeText(key.getBatchNo()))));
    }

    @GetMapping("/page")
    @Operation(summary = "Get stock out page")
    @PreAuthorize("@ss.hasPermission('erp:stock-out:query')")
    public CommonResult<PageResult<ErpStockOutRespVO>> getStockOutPage(@Valid ErpStockOutPageReqVO pageReqVO) {
        PageResult<ErpStockOutDO> pageResult = stockOutService.getStockOutPage(pageReqVO);
        PageResult<ErpStockOutRespVO> result = Boolean.FALSE.equals(pageReqVO.getIncludeItems())
                ? buildStockOutVOPageResultWithoutItems(pageResult) : buildStockOutVOPageResult(pageResult);
        fieldPermissionMasker.maskListColumns(FIELD_PERMISSION_MODULE, result.getList());
        Set<String> hidden = otherExportHiddenFields();
        result.getList().forEach(row -> ErpStockBillExportSupport.mask(row, hidden));
        return success(result);
    }

    @GetMapping("/summary")
    @Operation(summary = "Get stock out query summary")
    @PreAuthorize("@ss.hasPermission('erp:stock-out:query')")
    public CommonResult<ErpStockDocumentSummaryRespVO> getStockOutSummary(@Valid ErpStockOutPageReqVO pageReqVO) {
        ErpStockDocumentSummaryRespVO summary = stockOutService.getStockOutSummary(pageReqVO);
        fieldPermissionMasker.maskListColumns(FIELD_PERMISSION_MODULE, summary);
        ErpStockBillExportSupport.mask(summary, otherExportHiddenFields());
        return success(summary);
    }

    @GetMapping("/dept-simple-page")
    @Operation(summary = "Get visible department page for stock out filter")
    @PreAuthorize("@ss.hasPermission('erp:stock-out:query')")
    public CommonResult<PageResult<DeptSimpleRespVO>> getVisibleDeptSimplePage(@Valid PageParam pageReqVO) {
        return success(dataPermissionDeptService.getDeptSimplePage(FIELD_PERMISSION_MODULE, pageReqVO));
    }

    @GetMapping("/user-simple-page")
    @Operation(summary = "Get user page for stock out filter")
    @PreAuthorize("@ss.hasPermission('erp:stock-out:query')")
    public CommonResult<PageResult<UserSimpleRespVO>> getUserSimplePage(@Valid PageParam pageReqVO) {
        return success(buildUserSimplePage(pageReqVO));
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
            @RequestParam(value = "fields", required = false) String fields,
            HttpServletResponse response) throws IOException {
        ErpStockMovementExportSupport.write(response, "stock-out", fields,
                ErpStockOutRespVO.class, ErpStockOutExportRespVO.class, otherExportHiddenFields(),
                pageNo -> { pageReqVO.setPageNo(pageNo); pageReqVO.setPageSize(100); pageReqVO.setIncludeItems(false); return stockOutService.getStockOutPage(pageReqVO); },
                page -> buildStockOutVOPageResultWithoutItems(page).getList(),
                (ids, pageNo) -> stockOutItemMapper.selectExportPageByOutIds(ids, pageNo).getList(),
                ErpStockOutItemDO::getOutId,
                batch -> buildOtherExportItems(batch));
    }

    @GetMapping("/export-fields")
    @Operation(summary = "Get other stock out export fields")
    @PreAuthorize("@ss.hasPermission('erp:stock-out:export')")
    public CommonResult<List<ErpExportFieldRespVO>> getStockOutExportFields() {
        return success(ErpStockMovementExportSupport.fields(ErpStockOutExportRespVO.class, otherExportHiddenFields()));
    }

    private Set<String> otherExportHiddenFields() {
        return ErpOtherStockExportSupport.hidden(ErpStockOutExportRespVO.class,
                fieldPermissionMasker.getHiddenFieldSet(FIELD_PERMISSION_MODULE));
    }

    private List<ErpOtherStockExportSupport.Item> buildOtherExportItems(List<ErpStockOutItemDO> batch) {
        Map<Long, ErpWarehouseDO> warehouses = warehouseService.getWarehouseMap(convertSet(batch, ErpStockOutItemDO::getWarehouseId));
        Map<Long, DeptRespDTO> departments = deptApi.getDeptMap(convertSet(warehouses.values(), ErpWarehouseDO::getDeptId));
        Map<Long, ErpProductRespVO> products = DataPermissionUtils.executeIgnore(() -> productService.getProductVOMap(convertSet(batch, ErpStockOutItemDO::getProductId)));
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
    @Operation(summary = "Get stock out import template")
    @PreAuthorize("@ss.hasPermission('erp:stock-out:import')")
    public void getImportTemplate(HttpServletResponse response) throws IOException {
        ErpStockImportExcelVO first = new ErpStockImportExcelVO();
        first.setOrderNo("OUT-001");
        first.setCustomerName("示例客户");
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

    @GetMapping("/import-failure-details/download")
    @Operation(summary = "Download stock out import failure details")
    @PreAuthorize("@ss.hasPermission('erp:stock-out:import')")
    public void downloadImportFailureDetails(@RequestParam("recordId") Long recordId,
                                             HttpServletResponse response) throws IOException {
        importExportRecordService.downloadOwnImportFailureDetails(recordId, FIELD_PERMISSION_MODULE, response);
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
            addUserId(userIds, stockOut.getConfirmUserId());
            addUserId(userIds, stockOut.getApproveUserId());
        });
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        Map<Long, ErpSalePickDeliverySummaryRespVO> pickSummaryMap = pickDeliveryService.getSummaryMapByStockOutIds(
                convertSet(pageResult.getList(), ErpStockOutDO::getId));

        return BeanUtils.toBean(pageResult, ErpStockOutRespVO.class, vo -> {
            vo.setItems(BeanUtils.toBean(itemMap.get(vo.getId()), ErpStockOutRespVO.Item.class,
                    item -> fillProduct(item, productMap.get(item.getProductId()))));
            vo.setProductNames(CollUtil.join(vo.getItems(), ", ", ErpStockOutRespVO.Item::getProductName));
            vo.setProductCodes(CollUtil.join(vo.getItems(), ", ", ErpStockOutRespVO.Item::getProductCode));
            fillListSummary(vo, itemMap.get(vo.getId()), warehouseMap);
            MapUtils.findAndThen(customerMap, vo.getCustomerId(), customer -> vo.setCustomerName(customer.getName()));
            MapUtils.findAndThen(deptMap, vo.getDeptId(), dept -> vo.setDeptName(dept.getName()));
            fillUserNames(vo, userMap);
            fillWorkflowInfo(vo, pickSummaryMap.get(vo.getId()));
        });
    }

    private PageResult<ErpStockOutRespVO> buildStockOutVOPageResultWithoutItems(PageResult<ErpStockOutDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        Map<Long, Map<String, Object>> summaryMap = stockOutItemMapper.selectSummaryMapByOutIds(
                convertSet(pageResult.getList(), ErpStockOutDO::getId));
        Map<Long, ErpCustomerDO> customerMap = customerService.getCustomerMap(
                convertSet(pageResult.getList(), ErpStockOutDO::getCustomerId));
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(convertSet(pageResult.getList(), ErpStockOutDO::getDeptId));
        Set<Long> userIds = new HashSet<>();
        pageResult.getList().forEach(stockOut -> {
            addUserId(userIds, stockOut.getCreator());
            addUserId(userIds, stockOut.getUpdater());
            addUserId(userIds, stockOut.getConfirmUserId());
            addUserId(userIds, stockOut.getApproveUserId());
        });
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        Map<Long, ErpSalePickDeliverySummaryRespVO> pickSummaryMap = pickDeliveryService.getSummaryMapByStockOutIds(
                convertSet(pageResult.getList(), ErpStockOutDO::getId));
        return BeanUtils.toBean(pageResult, ErpStockOutRespVO.class, vo -> {
            fillSummary(vo, summaryMap.get(vo.getId()));
            MapUtils.findAndThen(customerMap, vo.getCustomerId(), customer -> vo.setCustomerName(customer.getName()));
            MapUtils.findAndThen(deptMap, vo.getDeptId(), dept -> vo.setDeptName(dept.getName()));
            fillUserNames(vo, userMap);
            fillWorkflowInfo(vo, pickSummaryMap.get(vo.getId()));
        });
    }

    private void fillSummary(ErpStockOutRespVO vo, Map<String, Object> summary) {
        if (summary == null) {
            vo.setWarehouseNames("");
            vo.setItemCount(0);
            vo.setTotalCount(BigDecimal.ZERO);
            vo.setTotalPrice(BigDecimal.ZERO);
            vo.setTotalWeight(BigDecimal.ZERO);
            return;
        }
        vo.setProductNames((String) summary.get("productNames"));
        vo.setProductCodes((String) summary.get("productCodes"));
        vo.setWarehouseNames((String) summary.get("warehouseNames"));
        Object count = summary.get("itemCount");
        vo.setItemCount(count instanceof Number ? ((Number) count).intValue() : 0);
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

    private List<ErpStockOutRespVO.Item> buildStockOutItemVOList(List<ErpStockOutItemDO> itemList,
                                                                boolean includeStockCount) {
        if (CollUtil.isEmpty(itemList)) {
            return Collections.emptyList();
        }
        Map<Long, ErpProductRespVO> productMap = DataPermissionUtils.executeIgnore(() -> productService.getProductVOMap(
                convertSet(itemList, ErpStockOutItemDO::getProductId)));
        Map<String, ErpStockDO> stockMap = includeStockCount
                ? DataPermissionUtils.executeIgnore(() -> stockService.getStockMap(
                convertSet(itemList, ErpStockOutItemDO::getProductId),
                convertSet(itemList, ErpStockOutItemDO::getWarehouseId)))
                : Collections.emptyMap();
        return BeanUtils.toBean(itemList, ErpStockOutRespVO.Item.class, item -> {
            if (includeStockCount) {
                ErpStockDO stock = stockMap.get(item.getProductId() + "_" + item.getWarehouseId());
                item.setStockCount(stock != null ? stock.getCount() : BigDecimal.ZERO);
            }
            fillProduct(item, productMap.get(item.getProductId()));
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
        MapUtils.findAndThen(userMap, stockOut.getConfirmUserId(),
                user -> stockOut.setConfirmUserName(user.getNickname()));
        MapUtils.findAndThen(userMap, stockOut.getApproveUserId(),
                user -> stockOut.setApproveUserName(user.getNickname()));
    }

    private void fillWorkflowInfo(ErpStockOutRespVO stockOut, ErpSalePickDeliverySummaryRespVO summary) {
        stockOut.setStatusName(ErpStockOutStatusEnum.getNameByStatus(stockOut.getStatus()));
        if (summary == null) {
            return;
        }
        stockOut.setPickStatus(summary.getPickStatus())
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
