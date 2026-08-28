package cn.iocoder.yudao.module.erp.controller.admin.stock;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.datapermission.core.annotation.DataPermission;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseBatchDisableReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehousePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseSaleDeptPermissionRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseSaleDeptPermissionSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseUserPermissionRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseUserPermissionSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpUserWarehousePermissionRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpUserWarehousePermissionSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.service.base.ErpDataPermissionDeptService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertMap;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

@Tag(name = "Admin - ERP warehouse")
@RestController
@RequestMapping("/erp/warehouse")
@Validated
public class ErpWarehouseController {

    private static final String DIRECT_WAREHOUSE_NAME = "直发仓";

    private static final String FIELD_PERMISSION_MODULE = "erp_warehouse";
    private static final Set<String> WAREHOUSE_IMPORT_TEMPLATE_FIELDS = new LinkedHashSet<>(Arrays.asList(
            "name", "warehouseCode", "deptName", "warehouseType", "status", "saleEnabled", "purchaseEnabled",
            "stockBillEnabled", "scanControl", "splitOrder", "sort", "remark"));

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

    @PostMapping("/create")
    @Operation(summary = "Create warehouse")
    @PreAuthorize("@ss.hasPermission('erp:warehouse:create')")
    public CommonResult<Long> createWarehouse(@Valid @RequestBody ErpWarehouseSaveReqVO createReqVO) {
        return success(warehouseService.createWarehouse(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "Update warehouse")
    @PreAuthorize("@ss.hasPermission('erp:warehouse:update')")
    public CommonResult<Boolean> updateWarehouse(@Valid @RequestBody ErpWarehouseSaveReqVO updateReqVO) {
        warehouseService.updateWarehouse(updateReqVO);
        return success(true);
    }

    @PutMapping("/batch-update")
    @Operation(summary = "Batch update warehouse")
    @PreAuthorize("@ss.hasPermission('erp:warehouse:update')")
    public CommonResult<Boolean> batchUpdateWarehouse(@Valid @RequestBody ErpWarehouseBatchUpdateReqVO updateReqVO) {
        warehouseService.batchUpdateWarehouse(updateReqVO);
        return success(true);
    }

    @PutMapping("/batch-disable")
    @Operation(summary = "Batch disable warehouse")
    @PreAuthorize("@ss.hasPermission('erp:warehouse:update')")
    public CommonResult<Boolean> batchDisableWarehouse(@Valid @RequestBody ErpWarehouseBatchDisableReqVO reqVO) {
        warehouseService.batchDisableWarehouse(reqVO.getIds());
        return success(true);
    }

    @PutMapping("/restore")
    @Operation(summary = "Restore warehouse")
    @PreAuthorize("@ss.hasPermission('erp:warehouse:update')")
    public CommonResult<Boolean> restoreWarehouse(@Valid @RequestBody ErpWarehouseBatchDisableReqVO reqVO) {
        warehouseService.restoreWarehouse(reqVO.getIds());
        return success(true);
    }

    @PutMapping("/update-default-status")
    @Operation(summary = "Update warehouse default status")
    @Parameters({
            @Parameter(name = "id", description = "id", required = true),
            @Parameter(name = "defaultStatus", description = "default status", required = true)
    })
    public CommonResult<Boolean> updateWarehouseDefaultStatus(@RequestParam("id") Long id,
                                                              @RequestParam("defaultStatus") Boolean defaultStatus) {
        warehouseService.updateWarehouseDefaultStatus(id, defaultStatus);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "Delete warehouse")
    @Parameter(name = "id", description = "id", required = true)
    @PreAuthorize("@ss.hasPermission('erp:warehouse:delete')")
    public CommonResult<Boolean> deleteWarehouse(@RequestParam("id") Long id) {
        warehouseService.deleteWarehouse(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "Batch delete warehouse")
    @Parameter(name = "ids", description = "ids", required = true)
    @PreAuthorize("@ss.hasPermission('erp:warehouse:delete')")
    public CommonResult<Boolean> deleteWarehouseList(@RequestParam("ids") List<Long> ids) {
        warehouseService.deleteWarehouseList(ids);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "Get warehouse")
    @Parameter(name = "id", description = "id", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:warehouse:query')")
    public CommonResult<ErpWarehouseRespVO> getWarehouse(@RequestParam("id") Long id) {
        ErpWarehouseDO warehouse = warehouseService.getCurrentUserVisibleWarehouse(id);
        if (warehouse == null) {
            return success(null);
        }
        ErpWarehouseRespVO respVO = buildWarehouseVOList(Collections.singletonList(warehouse), true).get(0);
        respVO.setBranchTenantIds(warehouseService.getWarehouseBranchTenantIds(id));
        fieldPermissionMasker.maskForm(FIELD_PERMISSION_MODULE, respVO);
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "Get warehouse page")
    @PreAuthorize("@ss.hasPermission('erp:warehouse:query')")
    public CommonResult<PageResult<ErpWarehouseRespVO>> getWarehousePage(@Valid ErpWarehousePageReqVO pageReqVO) {
        PageResult<ErpWarehouseDO> pageResult = warehouseService.getWarehousePage(pageReqVO);
        return success(new PageResult<>(buildWarehouseVOList(pageResult.getList(), true), pageResult.getTotal()));
    }

    @GetMapping("/dept-simple-list")
    @Operation(summary = "Get visible department list for warehouse filter")
    @PreAuthorize("@ss.hasPermission('erp:warehouse:query')")
    public CommonResult<List<DeptSimpleRespVO>> getVisibleDeptSimpleList() {
        return success(dataPermissionDeptService.getDeptSimpleList(FIELD_PERMISSION_MODULE));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "Get warehouse simple list")
    public CommonResult<List<ErpWarehouseRespVO>> getWarehouseSimpleList(
            @RequestParam(value = "bizType", required = false) String bizType,
            @RequestParam(value = "deptId", required = false) Long deptId) {
        List<ErpWarehouseDO> list;
        if ("stockMoveFrom".equalsIgnoreCase(bizType)) {
            list = warehouseService.getCurrentUserStockMoveFromWarehouseList();
        } else if ("stock".equalsIgnoreCase(bizType)) {
            list = warehouseService.getCurrentUserStockVisibleWarehouseList();
        } else if ("purchase".equalsIgnoreCase(bizType)) {
            list = warehouseService.getCurrentUserAuthorizedPurchaseWarehouseList();
        } else if ("sale".equalsIgnoreCase(bizType) && deptId != null) {
            list = warehouseService.getCurrentUserSaleSelectableWarehouseListByDept(deptId);
        } else if ("sale".equalsIgnoreCase(bizType)) {
            list = warehouseService.getCurrentUserVisibleSaleWarehouseList();
        } else {
            list = warehouseService.getCurrentUserAuthorizedWarehouseList();
        }
        return success(buildSimpleWarehouseVOList(list));
    }

    @GetMapping("/sale-list-by-dept")
    @Operation(summary = "Get sale warehouse list by department")
    @Parameter(name = "deptId", description = "sales department id", required = true)
    public CommonResult<List<ErpWarehouseRespVO>> getSaleWarehouseListByDept(@RequestParam("deptId") Long deptId) {
        List<ErpWarehouseDO> list = warehouseService.getCurrentUserSaleSelectableWarehouseListByDept(deptId);
        return success(buildSimpleWarehouseVOList(list));
    }

    @GetMapping("/sale-owner-dept-simple-list")
    @Operation(summary = "Get owner department list of sale-selectable warehouses")
    @Parameter(name = "deptId", description = "sales department id", required = true)
    @DataPermission(enable = false)
    public CommonResult<List<DeptRespDTO>> getSaleWarehouseOwnerDeptSimpleList(@RequestParam("deptId") Long deptId) {
        Set<Long> ownerDeptIds = warehouseService.getCurrentUserSaleSelectableWarehouseListByDept(deptId).stream()
                .map(ErpWarehouseDO::getDeptId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (ownerDeptIds.isEmpty()) {
            return success(Collections.emptyList());
        }
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(ownerDeptIds);
        return success(ownerDeptIds.stream()
                .map(deptMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
    }

    @GetMapping("/purchase-owner-dept-simple-list")
    @Operation(summary = "Get owner department list of purchase-selectable warehouses")
    @DataPermission(enable = false)
    public CommonResult<List<DeptRespDTO>> getPurchaseWarehouseOwnerDeptSimpleList() {
        Set<Long> ownerDeptIds = warehouseService.getCurrentUserAuthorizedPurchaseWarehouseList().stream()
                .map(ErpWarehouseDO::getDeptId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (ownerDeptIds.isEmpty()) {
            return success(Collections.emptyList());
        }
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(ownerDeptIds);
        return success(ownerDeptIds.stream()
                .map(deptMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
    }

    @GetMapping("/stock-owner-dept-simple-list")
    @Operation(summary = "Get owner department list of stock-visible warehouses")
    @PreAuthorize("@ss.hasPermission('erp:stock:query')")
    @DataPermission(enable = false)
    public CommonResult<List<DeptRespDTO>> getStockWarehouseOwnerDeptSimpleList() {
        Set<Long> ownerDeptIds = warehouseService.getCurrentUserStockVisibleWarehouseList().stream()
                .map(ErpWarehouseDO::getDeptId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        if (ownerDeptIds.isEmpty()) {
            return success(Collections.emptyList());
        }
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(ownerDeptIds);
        return success(ownerDeptIds.stream()
                .map(deptMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
    }

    @GetMapping("/assignable-list")
    @Operation(summary = "Get assignable warehouse list")
    @PreAuthorize("@ss.hasPermission('erp:warehouse-permission:query') or @ss.hasPermission('erp:warehouse-permission:update')")
    public CommonResult<List<ErpWarehouseRespVO>> getWarehouseAssignableList() {
        List<ErpWarehouseDO> list = warehouseService.getAssignableWarehouseList();
        return success(buildSimpleWarehouseVOList(list));
    }

    @GetMapping("/user-permissions")
    @Operation(summary = "Get user warehouse permissions")
    @Parameter(name = "userId", description = "user id", required = true)
    @DataPermission(enable = false)
    @PreAuthorize("@ss.hasPermission('erp:warehouse-permission:query') or @ss.hasPermission('erp:warehouse-permission:update')")
    public CommonResult<ErpUserWarehousePermissionRespVO> getUserWarehousePermissions(
            @RequestParam("userId") Long userId) {
        ErpUserWarehousePermissionRespVO respVO = new ErpUserWarehousePermissionRespVO()
                .setUserId(userId)
                .setWarehouseIds(warehouseService.getUserWarehouseIds(userId));
        return success(respVO);
    }

    @PutMapping("/user-permissions")
    @Operation(summary = "Update user warehouse permissions")
    @DataPermission(enable = false)
    @PreAuthorize("@ss.hasPermission('erp:warehouse-permission:update')")
    public CommonResult<Boolean> updateUserWarehousePermissions(
            @Valid @RequestBody ErpUserWarehousePermissionSaveReqVO updateReqVO) {
        adminUserApi.validateUser(updateReqVO.getUserId());
        warehouseService.updateUserWarehousePermissions(updateReqVO.getUserId(), updateReqVO.getWarehouseIds());
        return success(true);
    }

    @GetMapping("/warehouse-user-permissions")
    @Operation(summary = "Get warehouse user permissions")
    @Parameter(name = "warehouseId", description = "warehouse id", required = true)
    @DataPermission(enable = false)
    @PreAuthorize("@ss.hasPermission('erp:warehouse-permission:query') or @ss.hasPermission('erp:warehouse-permission:update')")
    public CommonResult<ErpWarehouseUserPermissionRespVO> getWarehouseUserPermissions(
            @RequestParam("warehouseId") Long warehouseId) {
        ErpWarehouseUserPermissionRespVO respVO = new ErpWarehouseUserPermissionRespVO()
                .setWarehouseId(warehouseId)
                .setUserIds(warehouseService.getWarehouseUserIds(warehouseId));
        return success(respVO);
    }

    @PutMapping("/warehouse-user-permissions")
    @Operation(summary = "Update warehouse user permissions")
    @DataPermission(enable = false)
    @PreAuthorize("@ss.hasPermission('erp:warehouse-permission:update')")
    public CommonResult<Boolean> updateWarehouseUserPermissions(
            @Valid @RequestBody ErpWarehouseUserPermissionSaveReqVO updateReqVO) {
        if (updateReqVO.getUserIds() != null && !updateReqVO.getUserIds().isEmpty()) {
            adminUserApi.validateUserList(updateReqVO.getUserIds());
        }
        warehouseService.updateWarehouseUserPermissions(updateReqVO.getWarehouseId(), updateReqVO.getUserIds());
        return success(true);
    }

    @GetMapping("/sale-dept-permissions")
    @Operation(summary = "Get warehouse sale department permissions")
    @Parameter(name = "warehouseId", description = "warehouse id", required = true)
    @DataPermission(enable = false)
    @PreAuthorize("@ss.hasPermission('erp:warehouse-permission:query') or @ss.hasPermission('erp:warehouse-permission:update')")
    public CommonResult<ErpWarehouseSaleDeptPermissionRespVO> getWarehouseSaleDeptPermissions(
            @RequestParam("warehouseId") Long warehouseId) {
        ErpWarehouseSaleDeptPermissionRespVO respVO = new ErpWarehouseSaleDeptPermissionRespVO();
        respVO.setWarehouseId(warehouseId);
        respVO.setDeptIds(warehouseService.getWarehouseSaleDeptIds(warehouseId));
        return success(respVO);
    }

    @PutMapping("/sale-dept-permissions")
    @Operation(summary = "Update warehouse sale department permissions")
    @DataPermission(enable = false)
    @PreAuthorize("@ss.hasPermission('erp:warehouse-permission:update')")
    public CommonResult<Boolean> updateWarehouseSaleDeptPermissions(
            @Valid @RequestBody ErpWarehouseSaleDeptPermissionSaveReqVO updateReqVO) {
        warehouseService.updateWarehouseSaleDeptPermissions(updateReqVO.getWarehouseId(), updateReqVO.getDeptIds());
        return success(true);
    }

    @GetMapping("/assignable-users")
    @Operation(summary = "Get assignable user list for warehouse permissions")
    @DataPermission(enable = false)
    @PreAuthorize("@ss.hasPermission('erp:warehouse-permission:query') or @ss.hasPermission('erp:warehouse-permission:update')")
    public CommonResult<List<AdminUserRespDTO>> getWarehouseAssignableUsers() {
        return success(adminUserApi.getUserListByStatus(CommonStatusEnum.ENABLE.getStatus()));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "Export warehouse")
    @PreAuthorize("@ss.hasPermission('erp:warehouse:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportWarehouseExcel(@Valid ErpWarehousePageReqVO pageReqVO,
                                     HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<ErpWarehouseRespVO> list = buildWarehouseVOList(warehouseService.getWarehousePage(pageReqVO).getList());
        ExcelUtils.write(response, "warehouse.xls", "data", ErpWarehouseRespVO.class, list);
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "Get warehouse import template")
    @PreAuthorize("@ss.hasPermission('erp:warehouse:import')")
    public void getImportTemplate(HttpServletResponse response) throws IOException {
        ExcelUtils.writeImportTemplate(response, "仓库导入模板.xls", "仓库", ErpWarehouseImportExcelVO.class,
                Collections.singletonList(new ErpWarehouseImportExcelVO()), WAREHOUSE_IMPORT_TEMPLATE_FIELDS);
    }

    @PostMapping("/import")
    @Operation(summary = "Import warehouse")
    @PreAuthorize("@ss.hasPermission('erp:warehouse:import')")
    public CommonResult<ErpWarehouseImportRespVO> importWarehouse(@RequestParam("file") MultipartFile file) throws Exception {
        List<ErpWarehouseImportExcelVO> list = ExcelUtils.read(file, ErpWarehouseImportExcelVO.class);
        return success(warehouseService.importWarehouseList(list));
    }

    private List<ErpWarehouseRespVO> buildWarehouseVOList(List<ErpWarehouseDO> list) {
        return buildWarehouseVOList(list, false);
    }

    private List<ErpWarehouseRespVO> buildSimpleWarehouseVOList(List<ErpWarehouseDO> list) {
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(convertSet(list, ErpWarehouseDO::getDeptId));
        return convertList(list, warehouse -> {
            ErpWarehouseRespVO vo = new ErpWarehouseRespVO().setId(warehouse.getId())
                    .setName(warehouse.getName()).setDeptId(warehouse.getDeptId())
                    .setDirectWarehouse(DIRECT_WAREHOUSE_NAME.equals(warehouse.getName()))
                    .setDefaultStatus(warehouse.getDefaultStatus());
            MapUtils.findAndThen(deptMap, warehouse.getDeptId(), dept -> vo.setDeptName(dept.getName()));
            return vo;
        });
    }

    private List<ErpWarehouseRespVO> buildWarehouseVOList(List<ErpWarehouseDO> list, boolean markReadonlyBySaleDistribution) {
        Map<Long, DeptRespDTO> deptMap = deptApi.getDeptMap(convertSet(list, ErpWarehouseDO::getDeptId));
        Set<Long> storageWarehouseIds = list.stream()
                .map(ErpWarehouseDO::getStorageWarehouseId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, ErpWarehouseDO> storageWarehouseMap = convertMap(
                warehouseService.getWarehouseList(storageWarehouseIds), ErpWarehouseDO::getId);
        Set<Long> userIds = new HashSet<>();
        list.forEach(warehouse -> {
            addUserId(userIds, warehouse.getCreator());
            addUserId(userIds, warehouse.getUpdater());
        });
        Map<Long, AdminUserRespDTO> userMap = adminUserApi.getUserMap(userIds);
        Set<Long> readonlyWarehouseIds = markReadonlyBySaleDistribution
                ? warehouseService.getCurrentUserSaleDistributedVisibleWarehouseIds() : Collections.emptySet();
        return BeanUtils.toBean(list, ErpWarehouseRespVO.class, vo -> {
            MapUtils.findAndThen(deptMap, vo.getDeptId(), dept -> vo.setDeptName(dept.getName()));
            MapUtils.findAndThen(storageWarehouseMap, vo.getStorageWarehouseId(),
                    warehouse -> vo.setStorageWarehouseName(warehouse.getName()));
            fillUserNames(vo, userMap);
            vo.setReadonlyBySaleDistribution(readonlyWarehouseIds.contains(vo.getId()));
        });
    }

    private void fillUserNames(ErpWarehouseRespVO warehouse, Map<Long, AdminUserRespDTO> userMap) {
        Long creatorId = parseUserId(warehouse.getCreator());
        if (creatorId != null) {
            MapUtils.findAndThen(userMap, creatorId, user -> warehouse.setCreatorName(user.getNickname()));
        }
        Long updaterId = parseUserId(warehouse.getUpdater());
        if (updaterId != null) {
            MapUtils.findAndThen(userMap, updaterId, user -> warehouse.setUpdaterName(user.getNickname()));
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
