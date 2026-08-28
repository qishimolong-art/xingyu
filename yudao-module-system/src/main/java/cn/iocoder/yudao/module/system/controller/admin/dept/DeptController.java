package cn.iocoder.yudao.module.system.controller.admin.dept;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptBatchUpdateReqVO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptExportExcelVO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptImportExcelVO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptImportRespVO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptListReqVO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptRespVO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSaveReqVO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSimpleRespVO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptUpdateSortReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;

@Tag(name = "管理后台 - 部门")
@RestController
@RequestMapping("/system/dept")
@Validated
public class DeptController {

    private static final String FIELD_PERMISSION_MODULE = "system_dept";

    @Resource
    private DeptService deptService;
    @Resource
    private PermissionService permissionService;
    @Resource
    private AdminUserService userService;

    @PostMapping("create")
    @Operation(summary = "创建部门")
    @PreAuthorize("@ss.hasPermission('system:dept:create')")
    public CommonResult<Long> createDept(@Valid @RequestBody DeptSaveReqVO createReqVO) {
        Long deptId = deptService.createDept(createReqVO);
        return success(deptId);
    }

    @PutMapping("update")
    @Operation(summary = "更新部门")
    @PreAuthorize("@ss.hasPermission('system:dept:update')")
    public CommonResult<Boolean> updateDept(@Valid @RequestBody DeptSaveReqVO updateReqVO) {
        preserveHiddenFields(updateReqVO);
        deptService.updateDept(updateReqVO);
        return success(true);
    }

    @PutMapping("/update-sort")
    @Operation(summary = "批量更新部门排序")
    @PreAuthorize("@ss.hasPermission('system:dept:update')")
    public CommonResult<Boolean> updateDeptSort(@Valid @RequestBody DeptUpdateSortReqVO reqVO) {
        deptService.updateDeptSort(reqVO);
        return success(true);
    }

    @PutMapping("/batch-update")
    @Operation(summary = "批量修改部门")
    @PreAuthorize("@ss.hasPermission('system:dept:update')")
    public CommonResult<Boolean> batchUpdateDept(@Valid @RequestBody DeptBatchUpdateReqVO reqVO) {
        validateBatchUpdateHiddenFields(reqVO);
        deptService.batchUpdateDept(reqVO);
        return success(true);
    }

    @DeleteMapping("delete")
    @Operation(summary = "删除部门")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:dept:delete')")
    public CommonResult<Boolean> deleteDept(@RequestParam("id") Long id) {
        deptService.deleteDept(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Operation(summary = "批量删除部门")
    @Parameter(name = "ids", description = "编号列表", required = true)
    @PreAuthorize("@ss.hasPermission('system:dept:delete')")
    public CommonResult<Boolean> deleteDeptList(@RequestParam("ids") List<Long> ids) {
        deptService.deleteDeptList(ids);
        return success(true);
    }

    @GetMapping("/list")
    @Operation(summary = "获取部门列表")
    @PreAuthorize("@ss.hasPermission('system:dept:query')")
    public CommonResult<List<DeptRespVO>> getDeptList(DeptListReqVO reqVO) {
        ignoreHiddenSearchFields(reqVO);
        List<DeptDO> list = deptService.getDeptList(reqVO);
        List<DeptRespVO> respList = BeanUtils.toBean(list, DeptRespVO.class);
        maskDeptRespList(respList);
        return success(respList);
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出部门 Excel")
    @PreAuthorize("@ss.hasPermission('system:dept:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportDeptList(@Validated DeptListReqVO reqVO, HttpServletResponse response) throws IOException {
        ignoreHiddenSearchFields(reqVO);
        List<DeptDO> list = deptService.getDeptList(reqVO);
        List<DeptRespVO> respList = BeanUtils.toBean(list, DeptRespVO.class);
        maskDeptRespList(respList);
        List<DeptExportExcelVO> exportList = BeanUtils.toBean(respList, DeptExportExcelVO.class);
        fillDeptExportNames(respList, exportList);
        ExcelUtils.write(response, "部门.xls", "部门列表", DeptExportExcelVO.class,
                exportList);
    }

    @GetMapping(value = {"/list-all-simple", "/simple-list"})
    @Operation(summary = "获取部门精简信息列表", description = "只包含被开启的部门，主要用于前端的下拉选项")
    public CommonResult<List<DeptSimpleRespVO>> getSimpleDeptList() {
        List<DeptDO> list = deptService.getDeptList(
                new DeptListReqVO().setStatus(CommonStatusEnum.ENABLE.getStatus()));
        return success(BeanUtils.toBean(list, DeptSimpleRespVO.class));
    }

    @GetMapping("/all-simple-list")
    @Operation(summary = "获取部门精简信息列表（全部）", description = "包含所有部门，主要用于权限配置页面")
    @PreAuthorize("@ss.hasPermission('system:dept:query')")
    public CommonResult<List<DeptSimpleRespVO>> getAllSimpleDeptList() {
        List<DeptDO> list = deptService.getDeptList(new DeptListReqVO());
        return success(BeanUtils.toBean(list, DeptSimpleRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "获得部门信息")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:dept:query')")
    public CommonResult<DeptRespVO> getDept(@RequestParam("id") Long id) {
        DeptDO dept = deptService.getDept(id);
        DeptRespVO respVO = BeanUtils.toBean(dept, DeptRespVO.class);
        maskDeptResp(respVO);
        return success(respVO);
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "获得导入部门模板")
    @PreAuthorize("@ss.hasPermission('system:dept:import')")
    public void importTemplate(HttpServletResponse response) throws IOException {
        List<DeptImportExcelVO> list = Arrays.asList(
                DeptImportExcelVO.builder().name("销售部").parentName("").sort(10).leaderUserName("张三").status("启用").build(),
                DeptImportExcelVO.builder().name("销售一组").parentName("销售部").sort(null).leaderUserName("").status("禁用").build()
        );
        ExcelUtils.write(response, "部门导入模板.xls", "部门列表", DeptImportExcelVO.class, list);
    }

    @PostMapping("/import")
    @Operation(summary = "导入部门")
    @PreAuthorize("@ss.hasPermission('system:dept:import')")
    public CommonResult<DeptImportRespVO> importExcel(@RequestParam("file") MultipartFile file,
                                                      @RequestParam(value = "updateSupport", required = false,
                                                              defaultValue = "false") Boolean updateSupport) throws IOException {
        List<DeptImportExcelVO> list = ExcelUtils.read(file, DeptImportExcelVO.class);
        return success(deptService.importDeptList(list, updateSupport));
    }

    private Set<String> getHiddenFieldSet() {
        return new HashSet<>(permissionService.getCurrentUserHiddenFields(FIELD_PERMISSION_MODULE));
    }

    private boolean isFieldHidden(Set<String> hiddenFields, String fieldKey) {
        return hiddenFields.contains(fieldKey) || hiddenFields.contains("col_" + fieldKey);
    }

    private void ignoreHiddenSearchFields(DeptListReqVO reqVO) {
        Set<String> hiddenFields = getHiddenFieldSet();
        if (isFieldHidden(hiddenFields, "leaderUserId")) {
            reqVO.setLeaderUserId(null);
            reqVO.setLeaderUserName(null);
        }
        if (reqVO.getOrderField() != null && isFieldHidden(hiddenFields, reqVO.getOrderField())) {
            reqVO.setOrderField(null);
            reqVO.setOrderDirection(null);
        }
    }

    private void validateBatchUpdateHiddenFields(DeptBatchUpdateReqVO reqVO) {
        Set<String> hiddenFields = getHiddenFieldSet();
        if (hiddenFields.isEmpty()) {
            return;
        }
        if (Boolean.TRUE.equals(reqVO.getUpdateParentId()) && isFieldHidden(hiddenFields, "parentId")) {
            throw invalidParamException("当前无权批量修改上级部门");
        }
        if (Boolean.TRUE.equals(reqVO.getUpdateLeaderUserId()) && isFieldHidden(hiddenFields, "leaderUserId")) {
            throw invalidParamException("当前无权批量修改负责人");
        }
        if (Boolean.TRUE.equals(reqVO.getUpdateStatus()) && isFieldHidden(hiddenFields, "status")) {
            throw invalidParamException("当前无权批量修改状态");
        }
    }

    private void preserveHiddenFields(DeptSaveReqVO reqVO) {
        if (reqVO.getId() == null) {
            return;
        }
        Set<String> hiddenFields = getHiddenFieldSet();
        if (hiddenFields.isEmpty()) {
            return;
        }
        DeptDO oldDept = deptService.getDept(reqVO.getId());
        if (oldDept == null) {
            return;
        }
        if (isFieldHidden(hiddenFields, "name")) {
            reqVO.setName(oldDept.getName());
        }
        if (isFieldHidden(hiddenFields, "parentId")) {
            reqVO.setParentId(oldDept.getParentId());
        }
        if (isFieldHidden(hiddenFields, "sort")) {
            reqVO.setSort(oldDept.getSort());
        }
        if (isFieldHidden(hiddenFields, "leaderUserId")) {
            reqVO.setLeaderUserId(oldDept.getLeaderUserId());
        }
        if (isFieldHidden(hiddenFields, "phone")) {
            reqVO.setPhone(oldDept.getPhone());
        }
        if (isFieldHidden(hiddenFields, "address")) {
            reqVO.setAddress(oldDept.getAddress());
        }
        if (isFieldHidden(hiddenFields, "longitude")) {
            reqVO.setLongitude(oldDept.getLongitude());
        }
        if (isFieldHidden(hiddenFields, "latitude")) {
            reqVO.setLatitude(oldDept.getLatitude());
        }
        if (isFieldHidden(hiddenFields, "mapName")) {
            reqVO.setMapName(oldDept.getMapName());
        }
        if (isFieldHidden(hiddenFields, "email")) {
            reqVO.setEmail(oldDept.getEmail());
        }
        if (isFieldHidden(hiddenFields, "status")) {
            reqVO.setStatus(oldDept.getStatus());
        }
    }

    private void maskDeptRespList(List<DeptRespVO> list) {
        Set<String> hiddenFields = getHiddenFieldSet();
        list.forEach(item -> maskDeptResp(item, hiddenFields));
    }

    private void maskDeptResp(DeptRespVO respVO) {
        maskDeptResp(respVO, getHiddenFieldSet());
    }

    private void maskDeptResp(DeptRespVO respVO, Set<String> hiddenFields) {
        if (respVO == null || hiddenFields.isEmpty()) {
            return;
        }
        if (isFieldHidden(hiddenFields, "name")) {
            respVO.setName(null);
        }
        if (isFieldHidden(hiddenFields, "parentId")) {
            respVO.setParentId(null);
        }
        if (isFieldHidden(hiddenFields, "sort")) {
            respVO.setSort(null);
        }
        if (isFieldHidden(hiddenFields, "leaderUserId")) {
            respVO.setLeaderUserId(null);
        }
        if (isFieldHidden(hiddenFields, "phone")) {
            respVO.setPhone(null);
        }
        if (isFieldHidden(hiddenFields, "address")) {
            respVO.setAddress(null);
        }
        if (isFieldHidden(hiddenFields, "longitude")) {
            respVO.setLongitude(null);
        }
        if (isFieldHidden(hiddenFields, "latitude")) {
            respVO.setLatitude(null);
        }
        if (isFieldHidden(hiddenFields, "mapName")) {
            respVO.setMapName(null);
        }
        if (isFieldHidden(hiddenFields, "email")) {
            respVO.setEmail(null);
        }
        if (isFieldHidden(hiddenFields, "status")) {
            respVO.setStatus(null);
        }
        if (isFieldHidden(hiddenFields, "createTime")) {
            respVO.setCreateTime(null);
        }
    }

    private void fillDeptExportNames(List<DeptRespVO> respList, List<DeptExportExcelVO> exportList) {
        Set<String> hiddenFields = getHiddenFieldSet();
        Map<Long, DeptDO> parentDeptMap = Collections.emptyMap();
        if (!isFieldHidden(hiddenFields, "name")) {
            Set<Long> parentIds = respList.stream()
                    .map(DeptRespVO::getParentId)
                    .filter(Objects::nonNull)
                    .filter(parentId -> !DeptDO.PARENT_ID_ROOT.equals(parentId))
                    .collect(Collectors.toSet());
            parentDeptMap = parentIds.isEmpty() ? Collections.emptyMap() : deptService.getDeptMap(parentIds);
        }
        Set<Long> leaderUserIds = respList.stream()
                .map(DeptRespVO::getLeaderUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Map<Long, AdminUserDO> userMap = leaderUserIds.isEmpty() ? Collections.emptyMap() : userService.getUserMap(leaderUserIds);

        for (int i = 0; i < respList.size(); i++) {
            DeptRespVO respVO = respList.get(i);
            DeptExportExcelVO exportVO = exportList.get(i);
            DeptDO parentDept = parentDeptMap.get(respVO.getParentId());
            if (parentDept != null) {
                exportVO.setParentName(parentDept.getName());
            }
            AdminUserDO leaderUser = userMap.get(respVO.getLeaderUserId());
            if (leaderUser != null) {
                exportVO.setLeaderUserName(leaderUser.getNickname());
            }
        }
    }

}
