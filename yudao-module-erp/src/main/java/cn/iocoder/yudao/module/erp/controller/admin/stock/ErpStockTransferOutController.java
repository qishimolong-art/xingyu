package cn.iocoder.yudao.module.erp.controller.admin.stock;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.exception.ErrorCode;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.common.ErpAuditStatusRequestValidator;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpExportFieldRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.ErpStockUpdateRemarkReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.imports.ErpStockImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.imports.ErpStockImportResultRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockMoveItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockMovePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockMoveRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockMoveSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockTransferOutDraftCreateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockTransferOutDraftUpdateReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveDO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.framework.excel.ErpExportFieldUtils;
import cn.iocoder.yudao.module.erp.service.base.ErpDataPermissionDeptService;
import cn.iocoder.yudao.module.erp.service.common.ErpImportExportRecordService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleCartService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockImportService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockMoveService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockTransferOutPermissionScope;
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
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.IMPORT;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_MOVE_NOT_EXISTS;

@Tag(name = "Admin - ERP stock transfer out")
@RestController
@RequestMapping("/erp/stock-transfer-out")
@Validated
public class ErpStockTransferOutController {

    private static final int EXPORT_MAX_COUNT = 5000;
    private static final ErrorCode EXPORT_COUNT_EXCEEDED = new ErrorCode(1_030_505_006,
            "单次最多导出 5000 条调拨出库单，请缩小筛选范围后重试");

    private static final String FIELD_PERMISSION_MODULE = "erp_stock_transfer_out";
    private static final int TRANSFER_DIRECTION_OUT = 10;
    private static final Set<String> IMPORT_TEMPLATE_FIELDS = new LinkedHashSet<>(Arrays.asList(
            "orderNo", "toDeptName", "fromWarehouseName", "toWarehouseName", "productCode", "productName", "factoryCode", "count",
            "productPrice", "remark", "itemRemark"));
    private static final Set<String> IMPORT_REQUIRED_FIELDS = new LinkedHashSet<>(Arrays.asList(
            "toDeptName", "fromWarehouseName", "toWarehouseName", "count", "productPrice"));
    private static final Map<String, String> EXPORT_FIELD_GROUP_MAP = buildExportFieldGroupMap();
    private static final Map<String, String> EXPORT_FIELD_PERMISSION_MAP = buildExportFieldPermissionMap();

    @Resource
    private ErpStockMoveService stockMoveService;
    @Resource
    private ErpStockMoveController stockMoveController;
    @Resource
    private ErpImportExportRecordService importExportRecordService;
    @Resource
    private ErpStockImportService stockImportService;
    @Resource
    private ErpStockFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private ErpSaleCartService saleCartService;
    @Resource
    private ErpDataPermissionDeptService dataPermissionDeptService;

    @PostMapping("/create")
    @Operation(summary = "Create stock transfer-out draft")
    @PreAuthorize("@ss.hasPermission('erp:stock-transfer-out:create')")
    public CommonResult<Long> createStockTransferOut(@Valid @RequestBody ErpStockMoveSaveReqVO createReqVO) {
        createReqVO.setTransferDirection(TRANSFER_DIRECTION_OUT);
        return success(stockMoveService.createStockMove(createReqVO));
    }

    @PostMapping("/create-draft")
    @Operation(summary = "保存调拨出库单草稿")
    @PreAuthorize("@ss.hasPermission('erp:stock-transfer-out:create')")
    public CommonResult<Long> createStockTransferOutDraft(
            @RequestBody ErpStockTransferOutDraftCreateReqVO createReqVO) {
        createReqVO.setTransferDirection(TRANSFER_DIRECTION_OUT);
        return success(stockMoveService.createStockTransferOutDraft(createReqVO));
    }

    @PostMapping("/create-and-submit")
    @Operation(summary = "创建并提交调拨出库单")
    @PreAuthorize("@ss.hasPermission('erp:stock-transfer-out:create')")
    public CommonResult<Long> createAndSubmitStockTransferOut(
            @Valid @RequestBody ErpStockMoveSaveReqVO createReqVO) {
        createReqVO.setTransferDirection(TRANSFER_DIRECTION_OUT);
        return success(stockMoveService.createAndSubmitStockTransferOut(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "Update stock transfer-out draft")
    @PreAuthorize("@ss.hasPermission('erp:stock-transfer-out:update')")
    public CommonResult<Boolean> updateStockTransferOut(@Valid @RequestBody ErpStockMoveSaveReqVO updateReqVO) {
        updateReqVO.setTransferDirection(TRANSFER_DIRECTION_OUT);
        stockMoveService.updateStockMove(updateReqVO, FIELD_PERMISSION_MODULE);
        return success(true);
    }

    @PutMapping("/update-draft")
    @Operation(summary = "保存调拨出库单草稿修改")
    @PreAuthorize("@ss.hasPermission('erp:stock-transfer-out:update')")
    public CommonResult<Boolean> updateStockTransferOutDraft(
            @RequestBody ErpStockTransferOutDraftUpdateReqVO updateReqVO) {
        updateReqVO.setTransferDirection(TRANSFER_DIRECTION_OUT);
        stockMoveService.updateStockTransferOutDraft(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-and-submit")
    @Operation(summary = "更新并提交调拨出库单草稿")
    @PreAuthorize("@ss.hasPermission('erp:stock-transfer-out:update')")
    public CommonResult<Boolean> updateAndSubmitStockTransferOutDraft(
            @Valid @RequestBody ErpStockMoveSaveReqVO updateReqVO) {
        updateReqVO.setTransferDirection(TRANSFER_DIRECTION_OUT);
        stockMoveService.updateAndSubmitStockTransferOutDraft(updateReqVO);
        return success(true);
    }

    @PutMapping("/submit")
    @Operation(summary = "提交调拨出库单草稿")
    @PreAuthorize("@ss.hasPermission('erp:stock-transfer-out:update')")
    public CommonResult<Boolean> submitStockTransferOutDraft(@RequestParam("id") Long id) {
        stockMoveService.submitStockTransferOutDraft(id);
        return success(true);
    }

    @PutMapping("/update-remark")
    @Operation(summary = "Update stock transfer-out remark")
    @PreAuthorize("@ss.hasPermission('erp:stock-transfer-out:update')")
    public CommonResult<Boolean> updateStockTransferOutRemark(
            @Valid @RequestBody ErpStockUpdateRemarkReqVO updateReqVO) {
        stockMoveService.updateStockTransferOutRemark(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "Delete stock transfer-out draft")
    @Parameter(name = "ids", description = "ids", required = true)
    @PreAuthorize("@ss.hasPermission('erp:stock-transfer-out:delete')")
    public CommonResult<Boolean> deleteStockTransferOut(@RequestParam("ids") List<Long> ids) {
        stockMoveService.deleteStockMove(ids);
        return success(true);
    }

    @PutMapping("/unlock-cart")
    @Operation(summary = "Unlock the source sale cart and delete its unapproved transfer-out draft")
    @Parameter(name = "id", description = "stock transfer-out id", required = true)
    @PreAuthorize("@ss.hasPermission('erp:stock-transfer-out:unlock-cart')")
    public CommonResult<Boolean> unlockSaleCart(@RequestParam("id") Long id) {
        stockMoveService.validateStockTransferOutVisible(id);
        saleCartService.unlockSaleCartByTransferOutId(id);
        return success(true);
    }

    @PutMapping("/approve")
    @Operation(summary = "Approve stock transfer-out")
    @PreAuthorize("@ss.hasPermission('erp:stock-transfer-out:approve')")
    public CommonResult<Boolean> approveStockTransferOut(@RequestParam("id") Long id) {
        stockMoveService.updateStockTransferOutStatus(id, ErpAuditStatus.APPROVE.getStatus());
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "Approve stock transfer-out")
    @PreAuthorize("@ss.hasPermission('erp:stock-transfer-out:approve')")
    public CommonResult<Boolean> updateStockTransferOutStatus(@RequestParam("id") Long id,
                                                              @RequestParam("status") Integer status) {
        ErpAuditStatusRequestValidator.validateApproveStatus(status);
        stockMoveService.updateStockTransferOutStatus(id, status);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "Get stock transfer-out")
    @Parameter(name = "id", description = "id", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:stock-transfer-out:query')")
    public CommonResult<ErpStockMoveRespVO> getStockTransferOut(@RequestParam("id") Long id,
                                                                @RequestParam(value = "includeItems", required = false,
                                                                        defaultValue = "true") Boolean includeItems) {
        ErpStockMoveDO stockMove = stockMoveService.getVisibleStockTransferOut(id);
        if (stockMove == null || TRANSFER_DIRECTION_OUT != getTransferDirection(stockMove)) {
            throw exception(STOCK_MOVE_NOT_EXISTS);
        }
        return stockMoveController.buildStockMoveDetail(stockMove, FIELD_PERMISSION_MODULE, includeItems);
    }

    public CommonResult<ErpStockMoveRespVO> getStockTransferOut(Long id) {
        ErpStockMoveDO stockMove = stockMoveService.getVisibleStockTransferOut(id);
        if (stockMove == null || TRANSFER_DIRECTION_OUT != getTransferDirection(stockMove)) {
            throw exception(STOCK_MOVE_NOT_EXISTS);
        }
        return stockMoveController.buildStockMoveDetail(stockMove, FIELD_PERMISSION_MODULE);
    }

    @GetMapping("/item-page")
    @Operation(summary = "Get stock transfer-out item page")
    @PreAuthorize("@ss.hasPermission('erp:stock-transfer-out:query')")
    public CommonResult<PageResult<ErpStockMoveRespVO.Item>> getStockTransferOutItemPage(
            @Valid ErpStockMoveItemPageReqVO pageReqVO) {
        ErpStockMoveDO stockMove = stockMoveService.getVisibleStockTransferOut(pageReqVO.getMoveId());
        if (stockMove == null || TRANSFER_DIRECTION_OUT != getTransferDirection(stockMove)) {
            throw exception(STOCK_MOVE_NOT_EXISTS);
        }
        return stockMoveController.getStockMoveItemPage(pageReqVO, FIELD_PERMISSION_MODULE);
    }

    @GetMapping("/page")
    @Operation(summary = "Get stock transfer-out page")
    @PreAuthorize("@ss.hasPermission('erp:stock-transfer-out:query')")
    public CommonResult<PageResult<ErpStockMoveRespVO>> getStockTransferOutPage(@Valid ErpStockMovePageReqVO pageReqVO) {
        pageReqVO.setTransferDirection(TRANSFER_DIRECTION_OUT);
        ErpStockTransferOutPermissionScope permissionScope = stockMoveService.getTransferOutPermissionScope();
        return success(stockMoveController.buildStockMoveVOPageResult(
                stockMoveService.getVisibleStockTransferOutPage(pageReqVO, permissionScope),
                FIELD_PERMISSION_MODULE, permissionScope));
    }

    @GetMapping("/from-dept-simple-list")
    @Operation(summary = "Get visible stock transfer-out from department simple list")
    @PreAuthorize("@ss.hasPermission('erp:stock-transfer-out:query')")
    public CommonResult<List<DeptSimpleRespVO>> getFromDeptSimpleList() {
        return success(dataPermissionDeptService.getDeptSimpleList(FIELD_PERMISSION_MODULE));
    }

    @GetMapping("/to-dept-simple-list")
    @Operation(summary = "Get visible stock transfer-out to department simple list")
    @PreAuthorize("@ss.hasPermission('erp:stock-transfer-out:query')")
    public CommonResult<List<DeptSimpleRespVO>> getToDeptSimpleList() {
        return success(stockMoveService.getVisibleStockTransferOutToDeptSimpleList());
    }

    @GetMapping("/export-excel")
    @Operation(summary = "Export stock transfer-out")
    @PreAuthorize("@ss.hasPermission('erp:stock-transfer-out:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportStockTransferOutExcel(@Valid ErpStockMovePageReqVO pageReqVO,
                                            @RequestParam(value = "fields", required = false) String fields,
                                            HttpServletResponse response) throws IOException {
        pageReqVO.setTransferDirection(TRANSFER_DIRECTION_OUT);
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(EXPORT_MAX_COUNT);
        ErpStockTransferOutPermissionScope permissionScope = stockMoveService.getTransferOutPermissionScope();
        PageResult<ErpStockMoveDO> pageResult = stockMoveService.getVisibleStockTransferOutPage(
                pageReqVO, permissionScope);
        if (pageResult.getTotal() > EXPORT_MAX_COUNT) {
            throw exception(EXPORT_COUNT_EXCEEDED);
        }
        List<ErpStockMoveRespVO> list = stockMoveController.buildStockMoveVOPageResult(
                pageResult, FIELD_PERMISSION_MODULE, permissionScope).getList();
        Set<String> includeFields = ErpExportFieldUtils.resolveIncludeFields(ErpStockMoveRespVO.class,
                ErpExportFieldUtils.parseFieldParam(fields),
                fieldPermissionMasker.getHiddenFieldSet(FIELD_PERMISSION_MODULE), EXPORT_FIELD_PERMISSION_MAP);
        ExcelUtils.write(response, "stock-transfer-out.xls", "data", ErpStockMoveRespVO.class, list, includeFields);
    }

    @GetMapping("/export-fields")
    @Operation(summary = "Get stock transfer-out export fields")
    @PreAuthorize("@ss.hasPermission('erp:stock-transfer-out:export')")
    public CommonResult<List<ErpExportFieldRespVO>> getStockTransferOutExportFields() {
        return success(ErpExportFieldUtils.listFields(ErpStockMoveRespVO.class, EXPORT_FIELD_GROUP_MAP,
                fieldPermissionMasker.getHiddenFieldSet(FIELD_PERMISSION_MODULE), EXPORT_FIELD_PERMISSION_MAP));
    }

    @GetMapping("/import-template")
    @Operation(summary = "Get stock transfer-out import template")
    @PreAuthorize("@ss.hasPermission('erp:stock-transfer-out:import')")
    public void getStockTransferOutImportTemplate(HttpServletResponse response) throws IOException {
        ErpStockImportExcelVO first = new ErpStockImportExcelVO();
        first.setOrderNo("STO-001");
        first.setToDeptName("示例调入部门");
        first.setFromWarehouseName("示例调出仓库");
        first.setToWarehouseName("示例调入仓库");
        first.setProductCode("P0001");
        first.setCount(BigDecimal.ONE);
        first.setProductPrice(new BigDecimal("100.00"));
        first.setRemark("单据备注");
        first.setItemRemark("明细备注");
        ErpStockImportExcelVO second = new ErpStockImportExcelVO();
        second.setOrderNo("STO-001");
        second.setToDeptName("示例调入部门");
        second.setFromWarehouseName("示例调出仓库");
        second.setToWarehouseName("示例调入仓库");
        second.setProductCode("P0002");
        second.setCount(new BigDecimal("2"));
        second.setProductPrice(new BigDecimal("50.00"));
        ExcelUtils.writeImportTemplate(response, "调拨出库导入模板.xls", "调拨出库",
                ErpStockImportExcelVO.class, Arrays.asList(first, second), IMPORT_TEMPLATE_FIELDS,
                IMPORT_REQUIRED_FIELDS);
    }

    @PostMapping("/import")
    @Operation(summary = "Import stock transfer-out draft")
    @PreAuthorize("@ss.hasPermission('erp:stock-transfer-out:import')")
    @ApiAccessLog(operateType = IMPORT)
    public CommonResult<ErpStockImportResultRespVO> importStockTransferOut(@RequestParam("file") MultipartFile file)
            throws Exception {
        return success(stockImportService.importStockTransferOutList(ExcelUtils.read(file, ErpStockImportExcelVO.class)));
    }

    @GetMapping("/import-failure-details/download")
    @Operation(summary = "Download stock transfer-out import failure details")
    @PreAuthorize("@ss.hasPermission('erp:stock-transfer-out:import')")
    public void downloadImportFailureDetails(@RequestParam("recordId") Long recordId,
                                             HttpServletResponse response) throws IOException {
        importExportRecordService.downloadOwnImportFailureDetails(recordId, FIELD_PERMISSION_MODULE, response);
    }

    private int getTransferDirection(ErpStockMoveDO stockMove) {
        return stockMove.getTransferDirection() != null ? stockMove.getTransferDirection() : TRANSFER_DIRECTION_OUT;
    }

    private static Map<String, String> buildExportFieldGroupMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("id", "system");
        map.put("no", "main");
        map.put("deptName", "main");
        map.put("relatedMoveNo", "main");
        map.put("fromDeptName", "main");
        map.put("toDeptName", "main");
        map.put("moveTime", "main");
        map.put("sourceNo", "main");
        map.put("totalCount", "main");
        map.put("totalPrice", "main");
        map.put("status", "main");
        map.put("approveUserName", "system");
        map.put("approveTime", "system");
        map.put("remark", "main");
        map.put("createTime", "system");
        map.put("lastPrintTime", "system");
        map.put("printCount", "system");
        map.put("productNames", "detail");
        map.put("productCodes", "detail");
        return map;
    }

    private static Map<String, String> buildExportFieldPermissionMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("deptName", "deptId");
        map.put("relatedMoveNo", "relatedMoveNo");
        map.put("fromDeptName", "fromDeptId");
        map.put("toDeptName", "toDeptId");
        map.put("sourceNo", "sourceNo");
        map.put("approveUserName", "approveUserName");
        map.put("productNames", "item_productId");
        map.put("productCodes", "item_productId");
        return map;
    }

}
