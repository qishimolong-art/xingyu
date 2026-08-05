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
import cn.iocoder.yudao.module.erp.controller.admin.product.vo.product.ErpProductRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.ErpStockUpdateRemarkReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.imports.ErpStockImportResultRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockMovePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockMoveRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move.ErpStockMoveSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockMoveItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockMoveService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockMoveApprovePermission;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockMoveOperationPermission;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpStockTransferOutPermissionScope;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMultiMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.STOCK_MOVE_LEGACY_READ_ONLY;

@Tag(name = "Admin - ERP stock move")
@RestController
@RequestMapping("/erp/stock-move")
@Validated
public class ErpStockMoveController {

    private static final int EXPORT_MAX_COUNT = 5000;
    private static final ErrorCode EXPORT_COUNT_EXCEEDED = new ErrorCode(1_030_590_004,
            "单次最多导出 5000 条调拨单，请缩小筛选范围后重试");
    private static final String FIELD_PERMISSION_MODULE = "erp_stock_move";
    @Resource
    private ErpStockMoveService stockMoveService;
    @Resource
    private ErpStockService stockService;
    @Resource
    private ErpProductService productService;
    @Resource
    private ErpStockFieldPermissionMasker fieldPermissionMasker;
    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private DeptApi deptApi;
    @Resource
    private AdminUserApi adminUserApi;

    @PostMapping("/create")
    @Operation(summary = "Create stock move")
    @PreAuthorize("@ss.hasPermission('erp:stock-move:create')")
    public CommonResult<Long> createStockMove(@Valid @RequestBody ErpStockMoveSaveReqVO createReqVO) {
        throw exception(STOCK_MOVE_LEGACY_READ_ONLY);
    }

    @PutMapping("/update")
    @Operation(summary = "Update stock move")
    @PreAuthorize("@ss.hasPermission('erp:stock-move:update')")
    public CommonResult<Boolean> updateStockMove(@Valid @RequestBody ErpStockMoveSaveReqVO updateReqVO) {
        throw exception(STOCK_MOVE_LEGACY_READ_ONLY);
    }

    @PutMapping("/update-remark")
    @Operation(summary = "Update stock move remark")
    @PreAuthorize("@ss.hasPermission('erp:stock-move:update')")
    public CommonResult<Boolean> updateStockMoveRemark(
            @Valid @RequestBody ErpStockUpdateRemarkReqVO updateReqVO) {
        stockMoveService.updateStockMoveRemark(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "Update stock move status")
    @PreAuthorize("@ss.hasPermission('erp:stock-move:update-status')")
    public CommonResult<Boolean> updateStockMoveStatus(@RequestParam("id") Long id,
                                                       @RequestParam("status") Integer status) {
        throw exception(STOCK_MOVE_LEGACY_READ_ONLY);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "Delete stock move")
    @Parameter(name = "ids", description = "ids", required = true)
    @PreAuthorize("@ss.hasPermission('erp:stock-move:delete')")
    public CommonResult<Boolean> deleteStockMove(@RequestParam("ids") List<Long> ids) {
        throw exception(STOCK_MOVE_LEGACY_READ_ONLY);
    }

    @GetMapping("/get")
    @Operation(summary = "Get stock move")
    @Parameter(name = "id", description = "id", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:stock-move:query')")
    public CommonResult<ErpStockMoveRespVO> getStockMove(@RequestParam("id") Long id) {
        return getStockMove(id, FIELD_PERMISSION_MODULE);
    }

    public CommonResult<ErpStockMoveRespVO> getStockMove(Long id, String fieldPermissionModule) {
        ErpStockMoveDO stockMove = stockMoveService.getStockMove(id);
        return buildStockMoveDetail(stockMove, fieldPermissionModule);
    }

    public CommonResult<ErpStockMoveRespVO> buildStockMoveDetail(ErpStockMoveDO stockMove,
                                                                 String fieldPermissionModule) {
        if (stockMove == null) {
            return success(null);
        }
        Long id = stockMove.getId();
        List<ErpStockMoveItemDO> itemList = stockMoveService.getStockMoveItemListByMoveId(id);
        Map<Long, ErpProductRespVO> productMap = getProductVOMapIgnoreDataPermission(
                convertSet(itemList, ErpStockMoveItemDO::getProductId));
        Map<Long, ErpWarehouseDO> warehouseMap = getWarehouseMapIgnoreDataPermission(collectWarehouseIds(itemList));
        Map<Long, DeptRespDTO> deptMap = getDeptMap(Collections.singletonList(stockMove), itemList, warehouseMap);
        Set<Long> userIds = new HashSet<>();
        addUserId(userIds, stockMove.getCreator());
        addUserId(userIds, stockMove.getUpdater());
        if (stockMove.getApproveUserId() != null) {
            userIds.add(stockMove.getApproveUserId());
        }
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);

        ErpStockMoveRespVO respVO = BeanUtils.toBean(stockMove, ErpStockMoveRespVO.class, vo -> {
            vo.setItems(BeanUtils.toBean(itemList, ErpStockMoveRespVO.Item.class, item -> {
                ErpStockDO stock = DataPermissionUtils.executeIgnore(() ->
                        stockService.getStock(item.getProductId(), item.getFromWarehouseId()));
                item.setStockCount(stock != null ? stock.getCount() : BigDecimal.ZERO);
                fillProduct(item, productMap.get(item.getProductId()));
                fillWarehouse(item, warehouseMap, deptMap);
                fillItemDeptNames(item, deptMap);
            }));
            vo.setProductNames(CollUtil.join(vo.getItems(), ", ", ErpStockMoveRespVO.Item::getProductName));
            vo.setProductCodes(CollUtil.join(vo.getItems(), ", ", ErpStockMoveRespVO.Item::getProductCode));
            fillWarehouseNames(vo);
            MapUtils.findAndThen(deptMap, vo.getDeptId(), dept -> vo.setDeptName(dept.getName()));
            fillMoveDeptNames(vo, deptMap);
            fillApprovePermission(vo, stockMove, itemList);
            fillDeletePermission(vo, stockMove, itemList);
            fillUnlockCartPermission(vo, stockMove, itemList);
            fillUserNames(vo, userMap);
        });
        fieldPermissionMasker.maskFormWithItems(fieldPermissionModule, respVO);
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "Get stock move page")
    @PreAuthorize("@ss.hasPermission('erp:stock-move:query')")
    public CommonResult<PageResult<ErpStockMoveRespVO>> getStockMovePage(@Valid ErpStockMovePageReqVO pageReqVO) {
        return success(buildStockMoveVOPageResult(stockMoveService.getStockMovePage(pageReqVO)));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "Export stock move")
    @PreAuthorize("@ss.hasPermission('erp:stock-move:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportStockMoveExcel(@Valid ErpStockMovePageReqVO pageReqVO,
                                     HttpServletResponse response) throws IOException {
        pageReqVO.setPageNo(1);
        pageReqVO.setPageSize(EXPORT_MAX_COUNT);
        PageResult<ErpStockMoveDO> pageResult = stockMoveService.getStockMovePage(pageReqVO);
        if (pageResult.getTotal() > EXPORT_MAX_COUNT) {
            throw exception(EXPORT_COUNT_EXCEEDED);
        }
        List<ErpStockMoveRespVO> list = buildStockMoveVOPageResult(pageResult).getList();
        ExcelUtils.write(response, "stock-move.xls", "data", ErpStockMoveRespVO.class, list);
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "Get stock move import template")
    @PreAuthorize("@ss.hasPermission('erp:stock-move:import')")
    public void getImportTemplate(HttpServletResponse response) throws IOException {
        throw exception(STOCK_MOVE_LEGACY_READ_ONLY);
    }

    @PostMapping("/import")
    @Operation(summary = "Import stock move")
    @PreAuthorize("@ss.hasPermission('erp:stock-move:import')")
    public CommonResult<ErpStockImportResultRespVO> importStockMove(@RequestParam("file") MultipartFile file)
            throws Exception {
        throw exception(STOCK_MOVE_LEGACY_READ_ONLY);
    }

    private PageResult<ErpStockMoveRespVO> buildStockMoveVOPageResult(PageResult<ErpStockMoveDO> pageResult) {
        return buildStockMoveVOPageResult(pageResult, FIELD_PERMISSION_MODULE);
    }

    public PageResult<ErpStockMoveRespVO> buildStockMoveVOPageResult(PageResult<ErpStockMoveDO> pageResult,
                                                                     String fieldPermissionModule) {
        return buildStockMoveVOPageResult(pageResult, fieldPermissionModule, null, false);
    }

    public PageResult<ErpStockMoveRespVO> buildStockMoveVOPageResult(
            PageResult<ErpStockMoveDO> pageResult, String fieldPermissionModule,
            ErpStockTransferOutPermissionScope transferOutPermissionScope) {
        return buildStockMoveVOPageResult(pageResult, fieldPermissionModule, transferOutPermissionScope, true);
    }

    private PageResult<ErpStockMoveRespVO> buildStockMoveVOPageResult(
            PageResult<ErpStockMoveDO> pageResult, String fieldPermissionModule,
            ErpStockTransferOutPermissionScope transferOutPermissionScope, boolean reuseTransferOutPermissionScope) {
        if (CollUtil.isEmpty(pageResult.getList())) {
            return PageResult.empty(pageResult.getTotal());
        }
        List<ErpStockMoveItemDO> itemList = stockMoveService.getStockMoveItemListByMoveIds(
                convertSet(pageResult.getList(), ErpStockMoveDO::getId));
        Map<Long, List<ErpStockMoveItemDO>> itemMap = convertMultiMap(itemList, ErpStockMoveItemDO::getMoveId);
        Map<Long, ErpStockMoveDO> stockMoveMap = convertMap(pageResult.getList(), ErpStockMoveDO::getId);
        Map<Long, ErpProductRespVO> productMap = getProductVOMapIgnoreDataPermission(
                convertSet(itemList, ErpStockMoveItemDO::getProductId));
        Map<Long, ErpWarehouseDO> warehouseMap = getWarehouseMapIgnoreDataPermission(collectWarehouseIds(itemList));
        Map<Long, DeptRespDTO> deptMap = getDeptMap(pageResult.getList(), itemList, warehouseMap);
        Map<Long, ErpStockMoveApprovePermission> approvePermissionMap = reuseTransferOutPermissionScope
                ? stockMoveService.getApprovePermissionMap(
                        pageResult.getList(), itemMap, transferOutPermissionScope)
                : stockMoveService.getApprovePermissionMap(pageResult.getList(), itemMap);
        Set<Long> userIds = new HashSet<>();
        pageResult.getList().forEach(stockMove -> {
            addUserId(userIds, stockMove.getCreator());
            addUserId(userIds, stockMove.getUpdater());
            if (stockMove.getApproveUserId() != null) {
                userIds.add(stockMove.getApproveUserId());
            }
        });
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);

        PageResult<ErpStockMoveRespVO> result = BeanUtils.toBean(pageResult, ErpStockMoveRespVO.class, vo -> {
            vo.setItems(BeanUtils.toBean(itemMap.get(vo.getId()), ErpStockMoveRespVO.Item.class,
                    item -> {
                        fillProduct(item, productMap.get(item.getProductId()));
                        fillWarehouse(item, warehouseMap, deptMap);
                        fillItemDeptNames(item, deptMap);
                    }));
            vo.setProductNames(CollUtil.join(vo.getItems(), ", ", ErpStockMoveRespVO.Item::getProductName));
            vo.setProductCodes(CollUtil.join(vo.getItems(), ", ", ErpStockMoveRespVO.Item::getProductCode));
            fillWarehouseNames(vo);
            MapUtils.findAndThen(deptMap, vo.getDeptId(), dept -> vo.setDeptName(dept.getName()));
            MapUtils.findAndThen(stockMoveMap, vo.getId(), stockMove -> {
                List<ErpStockMoveItemDO> stockMoveItems = itemMap.get(vo.getId());
                fillApprovePermission(vo, approvePermissionMap.get(vo.getId()));
                fillDeletePermission(vo, stockMove, stockMoveItems);
            });
            fillMoveDeptNames(vo, deptMap);
            fillUserNames(vo, userMap);
        });
        fieldPermissionMasker.maskListColumns(fieldPermissionModule, result.getList());
        return result;
    }

    private void fillMoveDeptNames(ErpStockMoveRespVO vo, Map<Long, DeptRespDTO> deptMap) {
        MapUtils.findAndThen(deptMap, vo.getFromDeptId(), dept -> vo.setFromDeptName(dept.getName()));
        MapUtils.findAndThen(deptMap, vo.getToDeptId(), dept -> vo.setToDeptName(dept.getName()));
    }

    private void fillWarehouseNames(ErpStockMoveRespVO vo) {
        vo.setFromWarehouseNames(joinDistinctItemValues(
                vo.getItems(), ErpStockMoveRespVO.Item::getFromWarehouseName));
        vo.setToWarehouseNames(joinDistinctItemValues(
                vo.getItems(), ErpStockMoveRespVO.Item::getToWarehouseName));
    }

    private String joinDistinctItemValues(List<ErpStockMoveRespVO.Item> items,
                                          Function<ErpStockMoveRespVO.Item, String> getter) {
        if (CollUtil.isEmpty(items)) {
            return "";
        }
        return items.stream()
                .map(getter)
                .filter(value -> value != null && !value.trim().isEmpty())
                .distinct()
                .collect(Collectors.joining("、"));
    }

    private void fillItemDeptNames(ErpStockMoveRespVO.Item item, Map<Long, DeptRespDTO> deptMap) {
        MapUtils.findAndThen(deptMap, item.getFromDeptId(), dept -> item.setFromDeptName(dept.getName()));
        MapUtils.findAndThen(deptMap, item.getToDeptId(), dept -> item.setToDeptName(dept.getName()));
    }

    private void fillProduct(ErpStockMoveRespVO.Item item, ErpProductRespVO product) {
        if (product == null) {
            return;
        }
        item.setProductName(product.getName())
                .setProductCode(product.getCode())
                .setProductBarCode(product.getBarCode())
                .setProductUnitName(product.getUnitName());
    }

    private void fillWarehouse(ErpStockMoveRespVO.Item item, Map<Long, ErpWarehouseDO> warehouseMap,
                               Map<Long, DeptRespDTO> deptMap) {
        MapUtils.findAndThen(warehouseMap, item.getFromWarehouseId(),
                warehouse -> {
                    item.setFromWarehouseName(warehouse.getName())
                            .setFromWarehouseDeptId(warehouse.getDeptId());
                    MapUtils.findAndThen(deptMap, warehouse.getDeptId(),
                            dept -> item.setFromWarehouseDeptName(dept.getName()));
                });
        MapUtils.findAndThen(warehouseMap, item.getToWarehouseId(),
                warehouse -> {
                    item.setToWarehouseName(warehouse.getName())
                            .setToWarehouseDeptId(warehouse.getDeptId());
                    MapUtils.findAndThen(deptMap, warehouse.getDeptId(),
                            dept -> item.setToWarehouseDeptName(dept.getName()));
                });
    }

    private void fillApprovePermission(ErpStockMoveRespVO vo, ErpStockMoveDO stockMove,
                                       List<ErpStockMoveItemDO> itemList) {
        ErpStockMoveApprovePermission permission = stockMoveService.getApprovePermission(stockMove, itemList);
        fillApprovePermission(vo, permission);
    }

    private void fillApprovePermission(ErpStockMoveRespVO vo, ErpStockMoveApprovePermission permission) {
        if (permission == null) {
            return;
        }
        vo.setApproveAllowed(permission.getApproveAllowed());
        vo.setApproveDisabledReason(permission.getApproveDisabledReason());
    }

    private void fillDeletePermission(ErpStockMoveRespVO vo, ErpStockMoveDO stockMove,
                                      List<ErpStockMoveItemDO> itemList) {
        ErpStockMoveOperationPermission permission = stockMoveService.getDeletePermission(stockMove, itemList);
        vo.setDeleteAllowed(permission.getAllowed());
        vo.setDeleteDisabledReason(permission.getDisabledReason());
    }

    private void fillUnlockCartPermission(ErpStockMoveRespVO vo, ErpStockMoveDO stockMove,
                                          List<ErpStockMoveItemDO> itemList) {
        ErpStockMoveOperationPermission permission = stockMoveService.getUnlockCartPermission(stockMove, itemList);
        vo.setUnlockCartAllowed(permission.getAllowed());
        vo.setUnlockCartDisabledReason(permission.getDisabledReason());
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

    private Map<Long, DeptRespDTO> getDeptMap(Collection<ErpStockMoveDO> stockMoves,
                                              List<ErpStockMoveItemDO> itemList,
                                              Map<Long, ErpWarehouseDO> warehouseMap) {
        Set<Long> deptIds = new HashSet<>();
        if (CollUtil.isNotEmpty(stockMoves)) {
            stockMoves.forEach(stockMove -> {
                addDeptId(deptIds, stockMove.getDeptId());
                addDeptId(deptIds, stockMove.getFromDeptId());
                addDeptId(deptIds, stockMove.getToDeptId());
            });
        }
        if (CollUtil.isNotEmpty(itemList)) {
            itemList.forEach(item -> {
                addDeptId(deptIds, item.getFromDeptId());
                addDeptId(deptIds, item.getToDeptId());
            });
        }
        if (CollUtil.isNotEmpty(warehouseMap)) {
            warehouseMap.values().forEach(warehouse -> addDeptId(deptIds, warehouse.getDeptId()));
        }
        if (CollUtil.isEmpty(deptIds)) {
            return Collections.emptyMap();
        }
        return deptApi.getDeptMap(deptIds);
    }

    private void addDeptId(Set<Long> deptIds, Long deptId) {
        if (deptId != null) {
            deptIds.add(deptId);
        }
    }

    private Set<Long> collectWarehouseIds(List<ErpStockMoveItemDO> itemList) {
        Set<Long> warehouseIds = new HashSet<>();
        if (CollUtil.isEmpty(itemList)) {
            return warehouseIds;
        }
        itemList.forEach(item -> {
            if (item.getFromWarehouseId() != null) {
                warehouseIds.add(item.getFromWarehouseId());
            }
            if (item.getToWarehouseId() != null) {
                warehouseIds.add(item.getToWarehouseId());
            }
        });
        return warehouseIds;
    }

    private void fillUserNames(ErpStockMoveRespVO stockMove, Map<Long, AdminUserRespDTO> userMap) {
        Long creatorId = parseUserId(stockMove.getCreator());
        if (creatorId != null) {
            MapUtils.findAndThen(userMap, creatorId, user -> stockMove.setCreatorName(user.getNickname()));
        }
        Long updaterId = parseUserId(stockMove.getUpdater());
        if (updaterId != null) {
            MapUtils.findAndThen(userMap, updaterId, user -> stockMove.setUpdaterName(user.getNickname()));
        }
        if (stockMove.getApproveUserId() != null) {
            MapUtils.findAndThen(userMap, stockMove.getApproveUserId(),
                    user -> stockMove.setApproveUserName(user.getNickname()));
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
