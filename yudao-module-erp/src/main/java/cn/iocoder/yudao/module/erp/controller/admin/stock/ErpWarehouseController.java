package cn.iocoder.yudao.module.erp.controller.admin.stock;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehousePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
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

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
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

    private static final String FIELD_PERMISSION_MODULE = "erp_warehouse";

    @Resource
    private ErpWarehouseService warehouseService;
    @Resource
    private ErpStockFieldPermissionMasker fieldPermissionMasker;
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

    @GetMapping("/get")
    @Operation(summary = "Get warehouse")
    @Parameter(name = "id", description = "id", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:warehouse:query')")
    public CommonResult<ErpWarehouseRespVO> getWarehouse(@RequestParam("id") Long id) {
        ErpWarehouseDO warehouse = warehouseService.getWarehouse(id);
        if (warehouse == null) {
            return success(null);
        }
        ErpWarehouseRespVO respVO = buildWarehouseVOList(Collections.singletonList(warehouse)).get(0);
        respVO.setBranchTenantIds(warehouseService.getWarehouseBranchTenantIds(id));
        fieldPermissionMasker.maskForm(FIELD_PERMISSION_MODULE, respVO);
        return success(respVO);
    }

    @GetMapping("/page")
    @Operation(summary = "Get warehouse page")
    @PreAuthorize("@ss.hasPermission('erp:warehouse:query')")
    public CommonResult<PageResult<ErpWarehouseRespVO>> getWarehousePage(@Valid ErpWarehousePageReqVO pageReqVO) {
        PageResult<ErpWarehouseDO> pageResult = warehouseService.getWarehousePage(pageReqVO);
        return success(new PageResult<>(buildWarehouseVOList(pageResult.getList()), pageResult.getTotal()));
    }

    @GetMapping("/simple-list")
    @Operation(summary = "Get warehouse simple list")
    public CommonResult<List<ErpWarehouseRespVO>> getWarehouseSimpleList() {
        List<ErpWarehouseDO> list = warehouseService.getWarehouseListByStatus(CommonStatusEnum.ENABLE.getStatus());
        return success(convertList(list, warehouse -> new ErpWarehouseRespVO().setId(warehouse.getId())
                .setName(warehouse.getName()).setDeptId(warehouse.getDeptId())
                .setDefaultStatus(warehouse.getDefaultStatus())));
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

    private List<ErpWarehouseRespVO> buildWarehouseVOList(List<ErpWarehouseDO> list) {
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
        return BeanUtils.toBean(list, ErpWarehouseRespVO.class, vo -> {
            MapUtils.findAndThen(deptMap, vo.getDeptId(), dept -> vo.setDeptName(dept.getName()));
            MapUtils.findAndThen(storageWarehouseMap, vo.getStorageWarehouseId(),
                    warehouse -> vo.setStorageWarehouseName(warehouse.getName()));
            fillUserNames(vo, userMap);
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
