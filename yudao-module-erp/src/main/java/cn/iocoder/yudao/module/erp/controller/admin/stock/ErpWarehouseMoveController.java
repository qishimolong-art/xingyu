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
import cn.iocoder.yudao.module.erp.controller.admin.common.ErpAuditStatusRequestValidator;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.ErpExportFieldRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.ErpStockUpdateRemarkReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMoveDraftCreateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMoveDraftUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.imports.ErpStockImportResultRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMoveImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMovePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMoveRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMoveSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove.ErpWarehouseMoveSummaryRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseMoveDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseMoveItemDO;
import cn.iocoder.yudao.module.erp.framework.excel.ErpExportFieldUtils;
import cn.iocoder.yudao.module.erp.service.base.ErpDataPermissionDeptService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockImportService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseMoveService;
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
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;

@Tag(name = "Admin - ERP warehouse move")
@RestController
@RequestMapping("/erp/warehouse-move")
@Validated
public class ErpWarehouseMoveController {

    private static final String FIELD_PERMISSION_MODULE = "erp_warehouse_move";
    private static final Set<String> IMPORT_TEMPLATE_FIELDS = new LinkedHashSet<>(Arrays.asList(
            "fromWarehouseName", "toWarehouseName", "productCode", "count", "fromShelf",
            "toShelf", "batchNo", "productPrice", "remark", "itemRemark"));
    private static final Map<String, String> EXPORT_FIELD_GROUP_MAP = buildExportFieldGroupMap();
    private static final Map<String, String> EXPORT_FIELD_PERMISSION_MAP = buildExportFieldPermissionMap();

    @Resource
    private ErpWarehouseMoveService warehouseMoveService;
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
    private ErpDataPermissionDeptService dataPermissionDeptService;
    @Resource
    private DeptApi deptApi;
    @Resource
    private AdminUserApi adminUserApi;

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
    public CommonResult<ErpWarehouseMoveRespVO> getWarehouseMove(@RequestParam("id") Long id) {
        ErpWarehouseMoveDO warehouseMove = warehouseMoveService.getWarehouseMove(id);
        if (warehouseMove == null) {
            return success(null);
        }
        List<ErpWarehouseMoveItemDO> itemList = warehouseMoveService.getWarehouseMoveItemListByMoveId(id);
        return success(buildWarehouseMoveVO(warehouseMove, itemList));
    }

    @GetMapping("/page")
    @Operation(summary = "Get warehouse move page")
    @PreAuthorize("@ss.hasPermission('erp:warehouse-move:query')")
    public CommonResult<PageResult<ErpWarehouseMoveRespVO>> getWarehouseMovePage(
            @Valid ErpWarehouseMovePageReqVO pageReqVO) {
        return success(buildWarehouseMoveVOPageResult(warehouseMoveService.getWarehouseMovePage(pageReqVO)));
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
        return success(warehouseMoveService.getWarehouseMoveSummary(pageReqVO));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "Export warehouse move")
    @PreAuthorize("@ss.hasPermission('erp:warehouse-move:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportWarehouseMoveExcel(@Valid ErpWarehouseMovePageReqVO pageReqVO,
                                         @RequestParam(value = "fields", required = false) String fields,
                                         HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ErpWarehouseMoveRespVO> list = buildWarehouseMoveVOPageResult(
                warehouseMoveService.getWarehouseMovePage(pageReqVO)).getList();
        Set<String> includeFields = ErpExportFieldUtils.resolveIncludeFields(ErpWarehouseMoveRespVO.class,
                ErpExportFieldUtils.parseFieldParam(fields),
                fieldPermissionMasker.getHiddenFieldSet(FIELD_PERMISSION_MODULE), EXPORT_FIELD_PERMISSION_MAP);
        ExcelUtils.write(response, "warehouse-move.xls", "data", ErpWarehouseMoveRespVO.class, list, includeFields);
    }

    @GetMapping("/export-fields")
    @Operation(summary = "Get warehouse move export fields")
    @PreAuthorize("@ss.hasPermission('erp:warehouse-move:export')")
    public CommonResult<List<ErpExportFieldRespVO>> getWarehouseMoveExportFields() {
        return success(ErpExportFieldUtils.listFields(ErpWarehouseMoveRespVO.class, EXPORT_FIELD_GROUP_MAP,
                fieldPermissionMasker.getHiddenFieldSet(FIELD_PERMISSION_MODULE), EXPORT_FIELD_PERMISSION_MAP));
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

    private PageResult<ErpWarehouseMoveRespVO> buildWarehouseMoveVOPageResult(PageResult<ErpWarehouseMoveDO> pageResult) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        List<ErpWarehouseMoveItemDO> itemList = warehouseMoveService.getWarehouseMoveItemListByMoveIds(
                convertSet(pageResult.getList(), ErpWarehouseMoveDO::getId));
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
        });
    }

    private ErpWarehouseMoveRespVO buildWarehouseMoveVO(ErpWarehouseMoveDO warehouseMove,
                                                        List<ErpWarehouseMoveItemDO> itemList) {
        Map<Long, ErpProductRespVO> productMap = getProductVOMapIgnoreDataPermission(
                convertSet(itemList, ErpWarehouseMoveItemDO::getProductId));
        Map<Long, ErpWarehouseDO> warehouseMap = getWarehouseMapIgnoreDataPermission(collectWarehouseIds(Collections.singletonList(warehouseMove)));
        Set<String> userIds = new HashSet<>();
        addUserId(userIds, warehouseMove.getCreator());
        addUserId(userIds, warehouseMove.getUpdater());
        if (warehouseMove.getHandlerId() != null) {
            userIds.add(String.valueOf(warehouseMove.getHandlerId()));
        }
        if (warehouseMove.getApproveUserId() != null) {
            userIds.add(String.valueOf(warehouseMove.getApproveUserId()));
        }
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(convertSet(userIds, Long::valueOf));
        DeptRespDTO dept = warehouseMove.getDeptId() == null ? null : deptApi.getDept(warehouseMove.getDeptId());
        return BeanUtils.toBean(warehouseMove, ErpWarehouseMoveRespVO.class, vo -> {
            vo.setItems(BeanUtils.toBean(itemList, ErpWarehouseMoveRespVO.Item.class,
                    item -> {
                        fillItem(item, productMap.get(item.getProductId()), warehouseMap);
                        fillStockCount(item);
                    }));
            vo.setItemCount(itemList == null ? 0 : itemList.size());
            vo.setProductNames(CollUtil.join(vo.getItems(), ", ", ErpWarehouseMoveRespVO.Item::getProductName));
            vo.setProductCodes(CollUtil.join(vo.getItems(), ", ", ErpWarehouseMoveRespVO.Item::getProductCode));
            if (dept != null) {
                vo.setDeptName(dept.getName());
            }
            fillWarehouse(vo, warehouseMap);
            fillUserNames(vo, userMap);
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

    private void fillStockCount(ErpWarehouseMoveRespVO.Item item) {
        ErpStockDO fromStock = DataPermissionUtils.executeIgnore(() ->
                stockService.getStock(item.getProductId(), item.getFromWarehouseId()));
        item.setFromStockCount(fromStock != null ? fromStock.getCount() : BigDecimal.ZERO);
        ErpStockDO toStock = DataPermissionUtils.executeIgnore(() ->
                stockService.getStock(item.getProductId(), item.getToWarehouseId()));
        item.setToStockCount(toStock != null ? toStock.getCount() : BigDecimal.ZERO);
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
        map.put("status", "main");
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
