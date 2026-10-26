package cn.iocoder.yudao.module.erp.controller.admin.stock;

import cn.iocoder.yudao.module.erp.service.stock.ErpStockItemPriceReferenceFiller;
import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.common.ErpAuditStatusRequestValidator;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpExportFieldRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutPickDeliveryOperationItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery.ErpSalePickDeliveryItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery.ErpSalePickDeliverySummaryRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery.ErpSalePickSourceDetailRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.ErpStockUpdateRemarkReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.ErpStockItemMatchReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.ErpStockItemMatchRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMoveDraftCreateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMoveDraftUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.imports.ErpStockImportResultRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMoveImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMoveItemPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMovePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMovePickPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMoveRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMoveSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMoveSummaryRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseMoveDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseMoveItemDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseMoveItemMapper;
import cn.iocoder.yudao.module.erp.framework.excel.ErpExportFieldUtils;
import cn.iocoder.yudao.module.erp.service.base.ErpDataPermissionDeptService;
import cn.iocoder.yudao.module.erp.service.common.ErpImportExportRecordService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSalePickDeliveryService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockImportService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseMoveService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.erp.enums.stock.ErpWarehouseMoveStatusEnum;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleBizSourceTypeEnum;
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
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMoveExportRespVO;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.IMPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.WAREHOUSE_MOVE_NOT_EXISTS;

@Tag(name = "Admin - ERP warehouse move")
@RestController
@RequestMapping("/erp/warehouse-move")
@Validated
public class ErpWarehouseMoveController {

    private static final String FIELD_PERMISSION_MODULE = "erp_warehouse_move";
    private static final Set<String> IMPORT_TEMPLATE_FIELDS = new LinkedHashSet<>(Arrays.asList(
            "fromWarehouseName", "toWarehouseName", "productCode", "productName", "factoryCode", "count", "fromShelf",
            "toShelf", "batchNo", "productPrice", "remark", "itemRemark"));
    private static final Map<String, String> EXPORT_FIELD_GROUP_MAP = buildExportFieldGroupMap();
    private static final Map<String, String> EXPORT_FIELD_PERMISSION_MAP = buildExportFieldPermissionMap();

    @Resource
    private ErpWarehouseMoveService warehouseMoveService;
    @Resource
    private ErpImportExportRecordService importExportRecordService;
    @Resource
    private ErpStockImportService stockImportService;
    @Resource
    private ErpProductService productService;
    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private ErpStockService stockService;
    @Resource
    private ErpStockFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private ErpStockItemPriceReferenceFiller itemPriceReferenceFiller;
    @Resource
    private ErpDataPermissionDeptService dataPermissionDeptService;
    @Resource
    private DeptApi deptApi;
    @Resource
    private AdminUserApi adminUserApi;
    @Resource
    private ErpWarehouseMoveItemMapper warehouseMoveItemMapper;
    @Resource
    private ErpSalePickDeliveryService salePickDeliveryService;

    @PostMapping("/create")
    @Operation(summary = "Create warehouse move")
    @PreAuthorize("@ss.hasPermission('erp:warehouse-move:create')")
    public CommonResult<Long> createWarehouseMove(@Valid @RequestBody ErpWarehouseMoveSaveReqVO createReqVO) {
        return success(warehouseMoveService.createWarehouseMove(createReqVO));
    }

    @PostMapping("/create-draft")
    @Operation(summary = "创建仓库移货单草稿")
    @PreAuthorize("@ss.hasPermission('erp:warehouse-move:create')")
    public CommonResult<Long> createWarehouseMoveDraft(
            @RequestBody ErpWarehouseMoveDraftCreateReqVO createReqVO) {
        return success(warehouseMoveService.createWarehouseMoveDraft(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "Update warehouse move")
    @PreAuthorize("@ss.hasPermission('erp:warehouse-move:update')")
    public CommonResult<Boolean> updateWarehouseMove(@Valid @RequestBody ErpWarehouseMoveSaveReqVO updateReqVO) {
        warehouseMoveService.updateWarehouseMove(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-draft")
    @Operation(summary = "保存仓库移货单草稿")
    @PreAuthorize("@ss.hasPermission('erp:warehouse-move:update')")
    public CommonResult<Boolean> updateWarehouseMoveDraft(
            @RequestBody ErpWarehouseMoveDraftUpdateReqVO updateReqVO) {
        warehouseMoveService.updateWarehouseMoveDraft(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-and-submit")
    @Operation(summary = "更新并提交仓库移货单草稿")
    @PreAuthorize("@ss.hasPermission('erp:warehouse-move:update') and " +
            "@ss.hasPermission('erp:warehouse-move:update-status')")
    public CommonResult<Boolean> updateAndSubmitWarehouseMoveDraft(
            @Valid @RequestBody ErpWarehouseMoveSaveReqVO updateReqVO) {
        warehouseMoveService.updateAndSubmitWarehouseMoveDraft(updateReqVO);
        return success(true);
    }

    @PutMapping("/submit")
    @Operation(summary = "提交仓库移货单草稿")
    @PreAuthorize("@ss.hasPermission('erp:warehouse-move:update-status')")
    public CommonResult<Boolean> submitWarehouseMove(@RequestParam("id") Long id) {
        warehouseMoveService.submitWarehouseMove(id);
        return success(true);
    }

    @PutMapping("/confirm")
    @Operation(summary = "下单确认仓库移货单并生成拣货任务")
    @PreAuthorize("@ss.hasPermission('erp:warehouse-move:confirm')")
    public CommonResult<Boolean> confirmWarehouseMove(@RequestParam("id") Long id) {
        warehouseMoveService.confirmWarehouseMove(id);
        return success(true);
    }

    @PutMapping("/cancel-confirm")
    @Operation(summary = "撤销尚未开始拣货的仓库移货单")
    @PreAuthorize("@ss.hasPermission('erp:warehouse-move:cancel-confirm')")
    public CommonResult<Boolean> cancelConfirmWarehouseMove(@RequestParam("id") Long id) {
        warehouseMoveService.cancelConfirmWarehouseMove(id);
        return success(true);
    }

    @PutMapping("/update-remark")
    @Operation(summary = "Update warehouse move remark")
    @PreAuthorize("@ss.hasPermission('erp:warehouse-move:update')")
    public CommonResult<Boolean> updateWarehouseMoveRemark(
            @Valid @RequestBody ErpStockUpdateRemarkReqVO updateReqVO) {
        warehouseMoveService.updateWarehouseMoveRemark(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "Update warehouse move status")
    @PreAuthorize("@ss.hasPermission('erp:warehouse-move:update-status')")
    public CommonResult<Boolean> updateWarehouseMoveStatus(@RequestParam("id") Long id,
                                                           @RequestParam("status") Integer status) {
        ErpAuditStatusRequestValidator.validateApproveStatus(status);
        warehouseMoveService.updateWarehouseMoveStatus(id, status);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "Delete warehouse move")
    @Parameter(name = "ids", description = "ids", required = true)
    @PreAuthorize("@ss.hasPermission('erp:warehouse-move:delete')")
    public CommonResult<Boolean> deleteWarehouseMove(@RequestParam("ids") List<Long> ids) {
        warehouseMoveService.deleteWarehouseMove(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "Get warehouse move")
    @PreAuthorize("@ss.hasPermission('erp:warehouse-move:query')")
    public CommonResult<ErpWarehouseMoveRespVO> getWarehouseMove(@RequestParam("id") Long id,
                                                                 @RequestParam(value = "includeItems", required = false,
                                                                         defaultValue = "true") Boolean includeItems) {
        ErpWarehouseMoveDO warehouseMove = warehouseMoveService.getWarehouseMove(id);
        if (warehouseMove == null) {
            return success(null);
        }
        List<ErpWarehouseMoveItemDO> itemList = Boolean.TRUE.equals(includeItems)
                ? warehouseMoveService.getWarehouseMoveItemListByMoveId(id) : Collections.emptyList();
        return success(buildWarehouseMoveVO(warehouseMove, itemList));
    }

    @GetMapping("/pick-detail")
    @Operation(summary = "获得仓库移货拣货详情")
    @Parameter(name = "id", description = "仓库移货编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:warehouse-move:query')")
    public CommonResult<ErpSalePickSourceDetailRespVO> getWarehouseMovePickDetail(
            @RequestParam("id") Long id) {
        validateReadableWarehouseMove(id);
        return success(salePickDeliveryService.getSourcePickDetail(
                ErpSaleBizSourceTypeEnum.WAREHOUSE_MOVE.getType(), id));
    }

    @GetMapping("/pick-item-page")
    @Operation(summary = "获得仓库移货累计拣货明细分页")
    @PreAuthorize("@ss.hasPermission('erp:warehouse-move:query')")
    public CommonResult<PageResult<ErpSalePickDeliveryItemRespVO>> getWarehouseMovePickItemPage(
            @Valid ErpWarehouseMovePickPageReqVO pageReqVO) {
        validateReadableWarehouseMove(pageReqVO.getMoveId());
        return success(salePickDeliveryService.getSourcePickItemPage(
                ErpSaleBizSourceTypeEnum.WAREHOUSE_MOVE.getType(), pageReqVO.getMoveId(), pageReqVO));
    }

    @GetMapping("/pick-operation-item-page")
    @Operation(summary = "获得仓库移货分批拣货人员明细分页")
    @PreAuthorize("@ss.hasPermission('erp:warehouse-move:query')")
    public CommonResult<PageResult<ErpSaleOutPickDeliveryOperationItemRespVO>>
    getWarehouseMovePickOperationItemPage(@Valid ErpWarehouseMovePickPageReqVO pageReqVO) {
        validateReadableWarehouseMove(pageReqVO.getMoveId());
        return success(salePickDeliveryService.getSourcePickOperationItemPage(
                ErpSaleBizSourceTypeEnum.WAREHOUSE_MOVE.getType(), pageReqVO.getMoveId(), pageReqVO));
    }

    private ErpWarehouseMoveDO validateReadableWarehouseMove(Long id) {
        ErpWarehouseMoveDO warehouseMove = warehouseMoveService.getWarehouseMove(id);
        if (warehouseMove == null) {
            throw exception(WAREHOUSE_MOVE_NOT_EXISTS);
        }
        return warehouseMove;
    }

    @GetMapping("/item-page")
    @Operation(summary = "Get warehouse move item page")
    @PreAuthorize("@ss.hasPermission('erp:warehouse-move:query')")
    public CommonResult<PageResult<ErpWarehouseMoveRespVO.Item>> getWarehouseMoveItemPage(
            @Valid ErpWarehouseMoveItemPageReqVO pageReqVO) {
        PageResult<ErpWarehouseMoveItemDO> pageResult = warehouseMoveService.getWarehouseMoveItemPage(pageReqVO);
        PageResult<ErpWarehouseMoveRespVO.Item> respResult = new PageResult<>(
                buildWarehouseMoveItemVOList(pageResult.getList(), true), pageResult.getTotal());
        itemPriceReferenceFiller.fill(respResult.getList());
        if (Boolean.TRUE.equals(pageReqVO.getMask())) {
            fieldPermissionMasker.clearHiddenItemFields(FIELD_PERMISSION_MODULE, respResult.getList());
        }
        return success(respResult);
    }

    @PostMapping("/item-page")
    @Operation(summary = "Get warehouse move item page for editing")
    @PreAuthorize("@ss.hasPermission('erp:warehouse-move:query')")
    public CommonResult<PageResult<ErpWarehouseMoveRespVO.Item>> getWarehouseMoveEditItemPage(
            @Valid @RequestBody ErpWarehouseMoveItemPageReqVO pageReqVO) {
        return getWarehouseMoveItemPage(pageReqVO);
    }

    @PostMapping("/item-match")
    @Operation(summary = "Match warehouse move items across pages")
    @PreAuthorize("@ss.hasPermission('erp:warehouse-move:query')")
    public CommonResult<ErpStockItemMatchRespVO> matchWarehouseMoveItems(
            @Valid @RequestBody ErpStockItemMatchReqVO reqVO) {
        if (warehouseMoveService.getWarehouseMove(reqVO.getDocumentId()) == null) {
            throw exception(WAREHOUSE_MOVE_NOT_EXISTS);
        }
        List<ErpWarehouseMoveItemDO> items = warehouseMoveService.getWarehouseMoveItemListByMoveId(
                reqVO.getDocumentId());
        return success(ErpStockItemMatchUtils.match(items, reqVO, ErpWarehouseMoveItemDO::getId,
                (item, key) -> java.util.Objects.equals(item.getProductId(), key.getProductId())
                        && java.util.Objects.equals(item.getFromWarehouseId(), key.getFromWarehouseId())
                        && java.util.Objects.equals(item.getToWarehouseId(), key.getToWarehouseId())
                        && ErpStockItemMatchUtils.normalizeText(item.getFromShelf())
                        .equals(ErpStockItemMatchUtils.normalizeText(key.getFromShelf()))));
    }

    @GetMapping("/page")
    @Operation(summary = "Get warehouse move page")
    @PreAuthorize("@ss.hasPermission('erp:warehouse-move:query')")
    public CommonResult<PageResult<ErpWarehouseMoveRespVO>> getWarehouseMovePage(
            @Valid ErpWarehouseMovePageReqVO pageReqVO) {
        PageResult<ErpWarehouseMoveDO> pageResult = warehouseMoveService.getWarehouseMovePage(pageReqVO);
        PageResult<ErpWarehouseMoveRespVO> respResult = Boolean.FALSE.equals(pageReqVO.getIncludeItems())
                ? buildWarehouseMoveVOPageResultWithoutItems(pageResult) : buildWarehouseMoveVOPageResult(pageResult);
        fieldPermissionMasker.maskListColumns(FIELD_PERMISSION_MODULE, respResult.getList());
        return success(respResult);
    }

    @GetMapping("/dept-simple-page")
    @Operation(summary = "Get visible department page for warehouse move filter")
    @PreAuthorize("@ss.hasPermission('erp:warehouse-move:query')")
    public CommonResult<PageResult<DeptSimpleRespVO>> getVisibleDeptSimplePage(@Valid PageParam pageReqVO) {
        return success(dataPermissionDeptService.getDeptSimplePage(FIELD_PERMISSION_MODULE, pageReqVO));
    }

    @GetMapping("/user-simple-page")
    @Operation(summary = "Get user page for warehouse move filter")
    @PreAuthorize("@ss.hasPermission('erp:warehouse-move:query')")
    public CommonResult<PageResult<UserSimpleRespVO>> getUserSimplePage(@Valid PageParam pageReqVO) {
        return success(buildUserSimplePage(pageReqVO));
    }

    @GetMapping("/dept-simple-list")
    @Operation(summary = "Get visible department list for warehouse move filter")
    @PreAuthorize("@ss.hasPermission('erp:warehouse-move:query')")
    public CommonResult<List<DeptSimpleRespVO>> getVisibleDeptSimpleList() {
        return success(dataPermissionDeptService.getDeptSimpleList(FIELD_PERMISSION_MODULE));
    }

    @GetMapping("/summary")
    @Operation(summary = "Get warehouse move summary")
    @PreAuthorize("@ss.hasPermission('erp:warehouse-move:query')")
    public CommonResult<ErpWarehouseMoveSummaryRespVO> getWarehouseMoveSummary(
            @Valid ErpWarehouseMovePageReqVO pageReqVO) {
        ErpWarehouseMoveSummaryRespVO summary = warehouseMoveService.getWarehouseMoveSummary(pageReqVO);
        fieldPermissionMasker.maskListColumns(FIELD_PERMISSION_MODULE, summary);
        return success(summary);
    }

    @GetMapping("/export-excel")
    @Operation(summary = "Export warehouse move")
    @PreAuthorize("@ss.hasPermission('erp:warehouse-move:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportWarehouseMoveExcel(@Valid ErpWarehouseMovePageReqVO pageReqVO,
                                         @RequestParam(value = "fields", required = false) String fields,
                                         HttpServletResponse response) throws IOException {
                ErpStockMovementExportSupport.write(response, "warehouse-move", fields,
                ErpWarehouseMoveRespVO.class, ErpWarehouseMoveExportRespVO.class,
                movementExportHiddenFields(),
                pageNo -> { pageReqVO.setPageNo(pageNo); pageReqVO.setPageSize(100); pageReqVO.setIncludeItems(false); return warehouseMoveService.getWarehouseMovePage(pageReqVO); },
                page -> buildWarehouseMoveVOPageResultWithoutItems(page).getList(),
                (ids, pageNo) -> warehouseMoveItemMapper.selectExportPageByMoveIds(ids, pageNo).getList(),
                ErpWarehouseMoveItemDO::getMoveId,
                batch -> {
                    List<ErpWarehouseMoveRespVO.Item> display = buildWarehouseMoveItemVOList(batch, true);
                    fieldPermissionMasker.clearHiddenItemFields(FIELD_PERMISSION_MODULE, display);
                    return display;
                });
    }

    private Set<String> movementExportHiddenFields() {
        return ErpStockMovementExportSupport.hidden(ErpWarehouseMoveExportRespVO.class,
                fieldPermissionMasker.getHiddenFieldSet(FIELD_PERMISSION_MODULE), EXPORT_FIELD_PERMISSION_MAP);
    }

    @GetMapping("/export-fields")
    @Operation(summary = "Get warehouse move export fields")
    @PreAuthorize("@ss.hasPermission('erp:warehouse-move:export')")
    public CommonResult<List<ErpExportFieldRespVO>> getWarehouseMoveExportFields() {
        return success(ErpStockMovementExportSupport.fields(ErpWarehouseMoveExportRespVO.class, movementExportHiddenFields()));
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "Get warehouse move import template")
    @PreAuthorize("@ss.hasPermission('erp:warehouse-move:create')")
    public void getWarehouseMoveImportTemplate(HttpServletResponse response) throws IOException {
        ErpWarehouseMoveImportExcelVO first = new ErpWarehouseMoveImportExcelVO();
        first.setFromWarehouseName("示例移出仓库");
        first.setToWarehouseName("示例移入仓库");
        first.setProductCode("P0001");
        first.setCount(BigDecimal.ONE);
        first.setFromShelf("A-01");
        first.setToShelf("B-01");
        first.setBatchNo("BATCH-001");
        first.setProductPrice(new BigDecimal("100.00"));
        first.setRemark("单据备注");
        first.setItemRemark("明细备注");
        ErpWarehouseMoveImportExcelVO second = new ErpWarehouseMoveImportExcelVO();
        second.setProductCode("P0002");
        second.setCount(new BigDecimal("2"));
        second.setFromShelf("A-02");
        second.setToShelf("B-02");
        second.setProductPrice(new BigDecimal("50.00"));
        second.setItemRemark("第二行明细备注");
        ExcelUtils.writeImportTemplate(response, "仓库移货导入模板.xls", "仓库移货",
                ErpWarehouseMoveImportExcelVO.class, Arrays.asList(first, second), IMPORT_TEMPLATE_FIELDS);
    }

    @PostMapping("/import")
    @Operation(summary = "Import warehouse move")
    @PreAuthorize("@ss.hasPermission('erp:warehouse-move:create')")
    @ApiAccessLog(operateType = IMPORT)
    public CommonResult<ErpStockImportResultRespVO> importWarehouseMove(@RequestParam("file") MultipartFile file)
            throws Exception {
        return success(stockImportService.importWarehouseMoveList(
                ExcelUtils.read(file, ErpWarehouseMoveImportExcelVO.class)));
    }

    @GetMapping("/import-failure-details/download")
    @Operation(summary = "Download warehouse move import failure details")
    @PreAuthorize("@ss.hasPermission('erp:warehouse-move:create')")
    public void downloadImportFailureDetails(@RequestParam("recordId") Long recordId,
                                             HttpServletResponse response) throws IOException {
        importExportRecordService.downloadOwnImportFailureDetails(recordId, FIELD_PERMISSION_MODULE, response);
    }

    private PageResult<ErpWarehouseMoveRespVO> buildWarehouseMoveVOPageResult(PageResult<ErpWarehouseMoveDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        List<ErpWarehouseMoveItemDO> itemList = warehouseMoveService.getWarehouseMoveItemListByMoveIds(
                convertSet(pageResult.getList(), ErpWarehouseMoveDO::getId));
        Map<Long, ErpSalePickDeliverySummaryRespVO> pickSummaryMap = salePickDeliveryService
                .getSummaryMapByWarehouseMoveIds(convertSet(pageResult.getList(), ErpWarehouseMoveDO::getId));
        Map<Long, List<ErpWarehouseMoveItemDO>> itemMap = convertMultiMap(itemList, ErpWarehouseMoveItemDO::getMoveId);
        Map<Long, ErpProductRespVO> productMap = getProductVOMapIgnoreDataPermission(
                convertSet(itemList, ErpWarehouseMoveItemDO::getProductId));
        Map<Long, ErpWarehouseDO> warehouseMap = getWarehouseMapIgnoreDataPermission(collectWarehouseIds(pageResult.getList()));
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(convertSet(pageResult.getList(), ErpWarehouseMoveDO::getDeptId));
        Map<Long, AdminUserRespDTO> userMap = getUserMap(pageResult.getList());

        return BeanUtils.toBean(pageResult, ErpWarehouseMoveRespVO.class, vo -> {
            List<ErpWarehouseMoveItemDO> items = itemMap.get(vo.getId());
            vo.setItems(BeanUtils.toBean(items, ErpWarehouseMoveRespVO.Item.class,
                    item -> fillItem(item, productMap.get(item.getProductId()), warehouseMap)));
            vo.setItemCount(items == null ? 0 : items.size());
            vo.setProductNames(CollUtil.join(vo.getItems(), ", ", ErpWarehouseMoveRespVO.Item::getProductName));
            vo.setProductCodes(CollUtil.join(vo.getItems(), ", ", ErpWarehouseMoveRespVO.Item::getProductCode));
            fillWarehouse(vo, warehouseMap);
            MapUtils.findAndThen(deptMap, vo.getDeptId(), dept -> vo.setDeptName(dept.getName()));
            fillUserNames(vo, userMap);
            fillWorkflow(vo, pickSummaryMap.get(vo.getId()));
        });
    }

    private PageResult<ErpWarehouseMoveRespVO> buildWarehouseMoveVOPageResultWithoutItems(
            PageResult<ErpWarehouseMoveDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        Set<Long> moveIds = convertSet(pageResult.getList(), ErpWarehouseMoveDO::getId);
        Map<Long, Map<String, Object>> summaryMap = warehouseMoveItemMapper.selectSummaryMapByMoveIds(moveIds);
        Map<Long, ErpSalePickDeliverySummaryRespVO> pickSummaryMap = salePickDeliveryService
                .getSummaryMapByWarehouseMoveIds(moveIds);
        Map<Long, ErpWarehouseDO> warehouseMap = getWarehouseMapIgnoreDataPermission(collectWarehouseIds(pageResult.getList()));
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(convertSet(pageResult.getList(), ErpWarehouseMoveDO::getDeptId));
        Map<Long, AdminUserRespDTO> userMap = getUserMap(pageResult.getList());
        return BeanUtils.toBean(pageResult, ErpWarehouseMoveRespVO.class, vo -> {
            fillSummary(vo, summaryMap.get(vo.getId()));
            fillWarehouse(vo, warehouseMap);
            MapUtils.findAndThen(deptMap, vo.getDeptId(), dept -> vo.setDeptName(dept.getName()));
            fillUserNames(vo, userMap);
            fillWorkflow(vo, pickSummaryMap.get(vo.getId()));
        });
    }

    private void fillSummary(ErpWarehouseMoveRespVO vo, Map<String, Object> summary) {
        if (summary == null) {
            vo.setItemCount(0);
            vo.setTotalCount(BigDecimal.ZERO);
            vo.setTotalPrice(BigDecimal.ZERO);
            vo.setTotalCostAmount(BigDecimal.ZERO);
            vo.setTotalWeight(BigDecimal.ZERO);
            return;
        }
        vo.setProductNames((String) summary.get("productNames"));
        vo.setProductCodes((String) summary.get("productCodes"));
        Object count = summary.get("itemCount");
        vo.setItemCount(count instanceof Number ? ((Number) count).intValue() : 0);
        vo.setTotalCount(toBigDecimal(summary.get("totalCount")));
        vo.setTotalPrice(toBigDecimal(summary.get("totalPrice")));
        vo.setTotalCostAmount(toBigDecimal(summary.get("totalCostAmount")));
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

    private ErpWarehouseMoveRespVO buildWarehouseMoveVO(ErpWarehouseMoveDO warehouseMove,
                                                        List<ErpWarehouseMoveItemDO> itemList) {
        Map<Long, ErpProductRespVO> productMap = getProductVOMapIgnoreDataPermission(
                convertSet(itemList, ErpWarehouseMoveItemDO::getProductId));
        Map<Long, ErpWarehouseDO> warehouseMap = getWarehouseMapIgnoreDataPermission(collectWarehouseIds(Collections.singletonList(warehouseMove)));
        Map<String, ErpStockDO> stockMap = getStockMapIgnoreDataPermission(itemList);
        Set<String> userIds = new HashSet<>();
        addUserId(userIds, warehouseMove.getCreator());
        addUserId(userIds, warehouseMove.getUpdater());
        if (warehouseMove.getHandlerId() != null) {
            userIds.add(String.valueOf(warehouseMove.getHandlerId()));
        }
        if (warehouseMove.getApproveUserId() != null) {
            userIds.add(String.valueOf(warehouseMove.getApproveUserId()));
        }
        if (warehouseMove.getConfirmUserId() != null) {
            userIds.add(String.valueOf(warehouseMove.getConfirmUserId()));
        }
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(convertSet(userIds, Long::valueOf));
        DeptRespDTO dept = warehouseMove.getDeptId() == null ? null : deptApi.getDept(warehouseMove.getDeptId());
        ErpWarehouseMoveRespVO respVO = BeanUtils.toBean(warehouseMove, ErpWarehouseMoveRespVO.class, vo -> {
            vo.setItems(BeanUtils.toBean(itemList, ErpWarehouseMoveRespVO.Item.class,
                    item -> {
                        fillItem(item, productMap.get(item.getProductId()), warehouseMap);
                        fillStockCount(item, stockMap);
                    }));
            vo.setItemCount(itemList == null ? 0 : itemList.size());
            vo.setProductNames(CollUtil.join(vo.getItems(), ", ", ErpWarehouseMoveRespVO.Item::getProductName));
            vo.setProductCodes(CollUtil.join(vo.getItems(), ", ", ErpWarehouseMoveRespVO.Item::getProductCode));
            if (dept != null) {
                vo.setDeptName(dept.getName());
            }
            fillWarehouse(vo, warehouseMap);
            fillUserNames(vo, userMap);
            fillWorkflow(vo, salePickDeliveryService.getSummaryMapByWarehouseMoveIds(
                    Collections.singleton(warehouseMove.getId())).get(warehouseMove.getId()));
        });
        fillSummary(respVO, warehouseMoveItemMapper.selectSummaryMapByMoveIds(
                Collections.singleton(warehouseMove.getId())).get(warehouseMove.getId()));
        itemPriceReferenceFiller.fill(respVO.getItems());
        fieldPermissionMasker.maskFormWithItems(FIELD_PERMISSION_MODULE, respVO);
        return respVO;
    }

    private List<ErpWarehouseMoveRespVO.Item> buildWarehouseMoveItemVOList(List<ErpWarehouseMoveItemDO> itemList,
                                                                          boolean includeStockCount) {
        if (CollUtil.isEmpty(itemList)) {
            return Collections.emptyList();
        }
        Map<Long, ErpProductRespVO> productMap = getProductVOMapIgnoreDataPermission(
                convertSet(itemList, ErpWarehouseMoveItemDO::getProductId));
        Map<Long, ErpWarehouseDO> warehouseMap = getWarehouseMapIgnoreDataPermission(collectWarehouseIdsFromItems(itemList));
        Map<String, ErpStockDO> stockMap = includeStockCount
                ? getStockMapIgnoreDataPermission(itemList) : Collections.emptyMap();
        Set<Long> deptIds = new java.util.HashSet<>();
        warehouseMap.values().forEach(warehouse -> { if (warehouse.getDeptId() != null) deptIds.add(warehouse.getDeptId()); });
        Map<Long, DeptRespDTO> exportDeptMap = deptIds.isEmpty() ? Collections.emptyMap() : deptApi.getDeptMap(deptIds);
        return BeanUtils.toBean(itemList, ErpWarehouseMoveRespVO.Item.class, item -> {
            fillItem(item, productMap.get(item.getProductId()), warehouseMap);
            ErpWarehouseDO from = warehouseMap.get(item.getFromWarehouseId()), to = warehouseMap.get(item.getToWarehouseId());
            if (from != null && exportDeptMap.containsKey(from.getDeptId())) item.setFromWarehouseDeptName(exportDeptMap.get(from.getDeptId()).getName());
            if (to != null && exportDeptMap.containsKey(to.getDeptId())) item.setToWarehouseDeptName(exportDeptMap.get(to.getDeptId()).getName());
            if (includeStockCount) {
                fillStockCount(item, stockMap);
            }
        });
    }

    private void fillItem(ErpWarehouseMoveRespVO.Item item, ErpProductRespVO product,
                          Map<Long, ErpWarehouseDO> warehouseMap) {
        if (product != null) {
            item.setProductName(product.getName());
            item.setProductCode(product.getCode());
            item.setProductBarCode(product.getBarCode());
            item.setProductUnitName(product.getUnitName());
            item.setVehicleModel(product.getVehicleModel());
            item.setOriginPlace(product.getOriginPlace());
            item.setStandard(product.getStandard());
            item.setFeatureCode(product.getFeatureCode());
            item.setBrand(product.getBrand());
            item.setDrawingNo(product.getDrawingNo());
        }
        MapUtils.findAndThen(warehouseMap, item.getFromWarehouseId(),
                warehouse -> item.setFromWarehouseName(warehouse.getName()));
        MapUtils.findAndThen(warehouseMap, item.getToWarehouseId(),
                warehouse -> item.setToWarehouseName(warehouse.getName()));
    }

    private void fillStockCount(ErpWarehouseMoveRespVO.Item item, Map<String, ErpStockDO> stockMap) {
        ErpStockDO fromStock = stockMap.get(item.getProductId() + "_" + item.getFromWarehouseId());
        item.setFromStockCount(fromStock != null ? fromStock.getCount() : BigDecimal.ZERO);
        ErpStockDO toStock = stockMap.get(item.getProductId() + "_" + item.getToWarehouseId());
        item.setToStockCount(toStock != null ? toStock.getCount() : BigDecimal.ZERO);
    }

    private Map<String, ErpStockDO> getStockMapIgnoreDataPermission(List<ErpWarehouseMoveItemDO> itemList) {
        if (CollUtil.isEmpty(itemList)) {
            return Collections.emptyMap();
        }
        return DataPermissionUtils.executeIgnore(() -> stockService.getStockMap(
                convertSet(itemList, ErpWarehouseMoveItemDO::getProductId), collectWarehouseIdsFromItems(itemList)));
    }

    private void fillWarehouse(ErpWarehouseMoveRespVO vo, Map<Long, ErpWarehouseDO> warehouseMap) {
        MapUtils.findAndThen(warehouseMap, vo.getFromWarehouseId(),
                warehouse -> vo.setFromWarehouseName(warehouse.getName()));
        MapUtils.findAndThen(warehouseMap, vo.getToWarehouseId(),
                warehouse -> vo.setToWarehouseName(warehouse.getName()));
    }

    private Map<Long, ErpProductRespVO> getProductVOMapIgnoreDataPermission(Set<Long> productIds) {
        if (CollUtil.isEmpty(productIds)) {
            return Collections.emptyMap();
        }
        return DataPermissionUtils.executeIgnore(() -> productService.getProductVOMap(productIds));
    }

    private Map<Long, ErpWarehouseDO> getWarehouseMapIgnoreDataPermission(Set<Long> warehouseIds) {
        if (CollUtil.isEmpty(warehouseIds)) {
            return Collections.emptyMap();
        }
        return DataPermissionUtils.executeIgnore(() -> warehouseService.getWarehouseMap(warehouseIds));
    }

    private Set<Long> collectWarehouseIds(List<ErpWarehouseMoveDO> moves) {
        Set<Long> warehouseIds = new HashSet<>();
        if (CollUtil.isEmpty(moves)) {
            return warehouseIds;
        }
        moves.forEach(move -> {
            if (move.getFromWarehouseId() != null) {
                warehouseIds.add(move.getFromWarehouseId());
            }
            if (move.getToWarehouseId() != null) {
                warehouseIds.add(move.getToWarehouseId());
            }
        });
        return warehouseIds;
    }

    private Set<Long> collectWarehouseIdsFromItems(List<ErpWarehouseMoveItemDO> items) {
        Set<Long> warehouseIds = new HashSet<>();
        if (CollUtil.isEmpty(items)) {
            return warehouseIds;
        }
        items.forEach(item -> {
            if (item.getFromWarehouseId() != null) {
                warehouseIds.add(item.getFromWarehouseId());
            }
            if (item.getToWarehouseId() != null) {
                warehouseIds.add(item.getToWarehouseId());
            }
        });
        return warehouseIds;
    }

    private Map<Long, AdminUserRespDTO> getUserMap(List<ErpWarehouseMoveDO> moves) {
        Set<String> userIds = new HashSet<>();
        moves.forEach(move -> {
            addUserId(userIds, move.getCreator());
            addUserId(userIds, move.getUpdater());
            if (move.getHandlerId() != null) {
                userIds.add(String.valueOf(move.getHandlerId()));
            }
            if (move.getApproveUserId() != null) {
                userIds.add(String.valueOf(move.getApproveUserId()));
            }
            if (move.getConfirmUserId() != null) {
                userIds.add(String.valueOf(move.getConfirmUserId()));
            }
        });
        if (CollUtil.isEmpty(userIds)) {
            return Collections.emptyMap();
        }
        return adminUserApi.getUserMap(convertSet(userIds, Long::valueOf));
    }

    private void fillUserNames(ErpWarehouseMoveRespVO vo, Map<Long, AdminUserRespDTO> userMap) {
        Long creatorId = parseUserId(vo.getCreator());
        if (creatorId != null) {
            MapUtils.findAndThen(userMap, creatorId, user -> vo.setCreatorName(user.getNickname()));
        }
        Long updaterId = parseUserId(vo.getUpdater());
        if (updaterId != null) {
            MapUtils.findAndThen(userMap, updaterId, user -> vo.setUpdaterName(user.getNickname()));
        }
        MapUtils.findAndThen(userMap, vo.getHandlerId(), user -> vo.setHandlerName(user.getNickname()));
        MapUtils.findAndThen(userMap, vo.getApproveUserId(), user -> vo.setApproveUserName(user.getNickname()));
        MapUtils.findAndThen(userMap, vo.getConfirmUserId(), user -> vo.setConfirmUserName(user.getNickname()));
    }

    private void fillWorkflow(ErpWarehouseMoveRespVO vo, ErpSalePickDeliverySummaryRespVO summary) {
        vo.setStatusName(Arrays.stream(ErpWarehouseMoveStatusEnum.values())
                .filter(item -> item.getStatus().equals(vo.getStatus()))
                .findFirst().map(ErpWarehouseMoveStatusEnum::getName).orElse("未知状态"));
        if (summary == null) {
            return;
        }
        vo.setPickStatus(summary.getPickStatus());
        vo.setPickedItemCount(summary.getPickedItemCount());
        vo.setPickTotalItemCount(summary.getTotalItemCount());
        vo.setPickProgress(summary.getPickProgress());
    }

    private void addUserId(Set<String> userIds, String userId) {
        if (parseUserId(userId) != null) {
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

    private static Map<String, String> buildExportFieldGroupMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("id", "system");
        map.put("no", "main");
        map.put("deptName", "main");
        map.put("moveTime", "main");
        map.put("fromWarehouseName", "main");
        map.put("toWarehouseName", "main");
        map.put("handlerName", "main");
        map.put("sourceNo", "main");
        map.put("itemCount", "main");
        map.put("totalCount", "main");
        map.put("totalPrice", "main");
        map.put("totalCostAmount", "main");
        map.put("statusName", "main");
        map.put("pickProgress", "main");
        map.put("confirmUserName", "system");
        map.put("confirmTime", "system");
        map.put("approveUserName", "system");
        map.put("approveTime", "system");
        map.put("remark", "main");
        map.put("creatorName", "system");
        map.put("createTime", "system");
        map.put("productNames", "detail");
        map.put("productCodes", "detail");
        return map;
    }

    private static Map<String, String> buildExportFieldPermissionMap() {
        Map<String, String> map = new LinkedHashMap<>();
        map.put("deptName", "deptId");
        map.put("fromWarehouseName", "fromWarehouseId");
        map.put("toWarehouseName", "toWarehouseId");
        map.put("handlerName", "handlerId");
        map.put("approveUserName", "approveUserName");
        map.put("productNames", "item_productId");
        map.put("productCodes", "item_productId");
        return map;
    }

}
