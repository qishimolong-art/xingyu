package cn.iocoder.yudao.module.system.controller.admin.user;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.*;
import cn.iocoder.yudao.module.system.convert.user.UserConvert;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.MenuDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.enums.common.SexEnum;
import cn.iocoder.yudao.module.system.enums.permission.MenuTypeEnum;
import cn.iocoder.yudao.module.system.enums.permission.DataScopeEnum;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.module.system.service.permission.MenuService;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.permission.RoleService;
import cn.iocoder.yudao.module.system.service.user.AdminUserService;
import cn.iocoder.yudao.module.system.service.user.UserPriceFieldService;
import cn.iocoder.yudao.module.system.service.user.dto.UserPriceFieldConfigDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertList;

@Tag(name = "管理后台 - 用户")
@RestController
@RequestMapping("/system/user")
@Validated
public class UserController {

    private static final String FIELD_PERMISSION_MODULE = "system_users";

    @Resource
    private AdminUserService userService;
    @Resource
    private DeptService deptService;
    @Resource
    private PermissionService permissionService;
    @Resource
    private RoleService roleService;
    @Resource
    private MenuService menuService;
    @Resource
    private UserPriceFieldService userPriceFieldService;

    @PostMapping("/create")
    @Operation(summary = "新增用户")
    @PreAuthorize("@ss.hasPermission('system:user:create')")
    public CommonResult<Long> createUser(@Valid @RequestBody UserSaveReqVO reqVO) {
        syncUsernameWithMobile(reqVO, false);
        validateUserDeptAndRole(reqVO);
        Long id = userService.createUser(reqVO);
        return success(id);
    }

    @PutMapping("update")
    @Operation(summary = "修改用户")
    @PreAuthorize("@ss.hasPermission('system:user:update')")
    public CommonResult<Boolean> updateUser(@Valid @RequestBody UserSaveReqVO reqVO) {
        Set<String> hiddenFields = getHiddenFieldSet();
        syncUsernameWithMobile(reqVO, isFieldHidden(hiddenFields, "mobile"));
        validateUserDeptAndRole(reqVO, hiddenFields);
        userService.updateUser(reqVO);
        return success(true);
    }

    private void syncUsernameWithMobile(UserSaveReqVO reqVO, boolean mobileHidden) {
        if (mobileHidden) {
            return;
        }
        if (StrUtil.isBlank(reqVO.getMobile())) {
            throw invalidParamException("手机号码不能为空");
        }
        reqVO.setMobile(StrUtil.trim(reqVO.getMobile()));
        reqVO.setUsername(reqVO.getMobile());
    }

    @PutMapping("/update-batch")
    @Operation(summary = "批量修改用户")
    @PreAuthorize("@ss.hasPermission('system:user:update')")
    public CommonResult<Boolean> updateUserBatch(@Valid @RequestBody UserBatchUpdateReqVO reqVO) {
        validateBatchUpdateHiddenFields(reqVO);
        userService.updateUserBatch(reqVO);
        return success(true);
    }

    private void validateUserDeptAndRole(UserSaveReqVO reqVO) {
        if (CollUtil.isEmpty(reqVO.getDeptIds())) {
            throw invalidParamException("所属部门不能为空");
        }
        if (CollUtil.isEmpty(reqVO.getRoleIds())) {
            throw invalidParamException("角色不能为空");
        }
    }

    private void validateUserDeptAndRole(UserSaveReqVO reqVO, Set<String> hiddenFields) {
        boolean deptHidden = isFieldHidden(hiddenFields, "deptIds");
        boolean roleHidden = isFieldHidden(hiddenFields, "roleIds");
        if (!deptHidden && CollUtil.isEmpty(reqVO.getDeptIds())) {
            throw invalidParamException("所属部门不能为空");
        }
        if (!roleHidden && CollUtil.isEmpty(reqVO.getRoleIds())) {
            throw invalidParamException("角色不能为空");
        }
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除用户")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:user:delete')")
    public CommonResult<Boolean> deleteUser(@RequestParam("id") Long id) {
        userService.deleteUser(id);
        return success(true);
    }

    @DeleteMapping("/delete-list")
    @Parameter(name = "ids", description = "编号列表", required = true)
    @Operation(summary = "批量删除用户")
    @PreAuthorize("@ss.hasPermission('system:user:delete')")
    public CommonResult<Boolean> deleteUserList(@RequestParam("ids") List<Long> ids) {
        userService.deleteUserList(ids);
        return success(true);
    }

    @PutMapping("/update-password")
    @Operation(summary = "重置用户密码")
    @PreAuthorize("@ss.hasPermission('system:user:update-password')")
    public CommonResult<Boolean> updateUserPassword(@Valid @RequestBody UserUpdatePasswordReqVO reqVO) {
        userService.updateUserPassword(reqVO.getId(), reqVO.getPassword());
        return success(true);
    }

    @PutMapping("/update-status")
    @Operation(summary = "修改用户状态")
    @PreAuthorize("@ss.hasPermission('system:user:update')")
    public CommonResult<Boolean> updateUserStatus(@Valid @RequestBody UserUpdateStatusReqVO reqVO) {
        userService.updateUserStatus(reqVO.getId(), reqVO.getStatus());
        return success(true);
    }

    @GetMapping("/page")
    @Operation(summary = "获得用户分页列表")
    @PreAuthorize("@ss.hasPermission('system:user:query')")
    public CommonResult<PageResult<UserRespVO>> getUserPage(@Valid UserPageReqVO pageReqVO) {
        ignoreHiddenSearchFields(pageReqVO);
        // 获得用户分页列表
        PageResult<AdminUserDO> pageResult = userService.getUserPage(pageReqVO);
        if (CollUtil.isEmpty(pageResult.getList())) {
            return success(new PageResult<>(pageResult.getTotal()));
        }
        // 拼接数据
        Map<Long, DeptDO> deptMap = deptService.getDeptMap(
                convertList(pageResult.getList(), AdminUserDO::getDeptId));
        List<UserRespVO> list = UserConvert.INSTANCE.convertList(pageResult.getList(), deptMap);
        fillUserRoleInfo(list);
        maskUserRespList(list);
        return success(new PageResult<>(list, pageResult.getTotal()));
    }

    @GetMapping({"/list-all-simple", "/simple-list"})
    @Operation(summary = "获取用户精简信息列表", description = "只包含被开启的用户，主要用于前端的下拉选项")
    public CommonResult<List<UserSimpleRespVO>> getSimpleUserList() {
        List<AdminUserDO> list = userService.getUserListByStatus(CommonStatusEnum.ENABLE.getStatus());
        // 拼接数据
        Map<Long, DeptDO> deptMap = deptService.getDeptMap(
                convertList(list, AdminUserDO::getDeptId));
        return success(UserConvert.INSTANCE.convertSimpleList(list, deptMap));
    }

    @GetMapping("/get")
    @Operation(summary = "获得用户详情")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:user:query')")
    public CommonResult<UserRespVO> getUser(@RequestParam("id") Long id) {
        AdminUserDO user = userService.getUser(id);
        if (user == null) {
            return success(null);
        }
        // 拼接数据
        DeptDO dept = deptService.getDept(user.getDeptId());
        UserRespVO respVO = UserConvert.INSTANCE.convert(user, dept);
        respVO.setDeptIds(userService.getUserDeptIdListByUserId(id));
        respVO.setRoleIds(permissionService.getUserRoleIdListByUserId(id));
        fillUserRoleNames(respVO);
        maskUserResp(respVO);
        return success(respVO);
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出用户")
    @PreAuthorize("@ss.hasPermission('system:user:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportUserList(@Validated UserPageReqVO exportReqVO,
                               HttpServletResponse response) throws IOException {
        ignoreHiddenSearchFields(exportReqVO);
        exportReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        List<AdminUserDO> list = userService.getUserPage(exportReqVO).getList();
        // 输出 Excel
        Map<Long, DeptDO> deptMap = deptService.getDeptMap(
                convertList(list, AdminUserDO::getDeptId));
        List<UserRespVO> rows = UserConvert.INSTANCE.convertList(list, deptMap);
        fillUserRoleInfo(rows);
        maskUserRespList(rows);
        ExcelUtils.write(response, "用户数据.xls", "数据", UserRespVO.class,
                rows);
    }

    @GetMapping("/get-import-template")
    @Operation(summary = "获得导入用户模板")
    public void importTemplate(HttpServletResponse response) throws IOException {
        // 手动创建导出 demo
        List<UserImportExcelVO> list = Arrays.asList(
                UserImportExcelVO.builder().deptId(1L).email("yunai@iocoder.cn").mobile("15601691300")
                        .nickname("芋道").status(CommonStatusEnum.ENABLE.getStatus()).sex(SexEnum.MALE.getSex()).build(),
                UserImportExcelVO.builder().deptId(2L).email("yuanma@iocoder.cn").mobile("15601701300")
                        .nickname("源码").status(CommonStatusEnum.DISABLE.getStatus()).sex(SexEnum.FEMALE.getSex()).build()
        );
        // 输出
        ExcelUtils.write(response, "用户导入模板.xls", "用户列表", UserImportExcelVO.class, list);
    }

    @PostMapping("/import")
    @Operation(summary = "导入用户")
    @Parameters({
            @Parameter(name = "file", description = "Excel 文件", required = true),
            @Parameter(name = "updateSupport", description = "是否支持更新，默认为 false", example = "true")
    })
    @PreAuthorize("@ss.hasPermission('system:user:import')")
    public CommonResult<UserImportRespVO> importExcel(@RequestParam("file") MultipartFile file,
                                                      @RequestParam(value = "updateSupport", required = false, defaultValue = "false") Boolean updateSupport) throws Exception {
        List<UserImportExcelVO> list = ExcelUtils.read(file, UserImportExcelVO.class);
        return success(userService.importUserList(list, updateSupport));
    }

    private Set<String> getHiddenFieldSet() {
        return new HashSet<>(permissionService.getCurrentUserHiddenFields(FIELD_PERMISSION_MODULE));
    }

    private boolean isFieldHidden(Set<String> hiddenFields, String fieldKey) {
        return hiddenFields.contains(fieldKey)
                || hiddenFields.contains("col_" + fieldKey)
                || hiddenFields.contains("search_" + fieldKey);
    }

    private void ignoreHiddenSearchFields(UserPageReqVO reqVO) {
        Set<String> hiddenFields = getHiddenFieldSet();
        if (isFieldHidden(hiddenFields, "username")) {
            reqVO.setUsername(null);
            ignoreOrderField(reqVO, "username");
        }
        if (isFieldHidden(hiddenFields, "id")) {
            ignoreOrderField(reqVO, "id");
        }
        if (isFieldHidden(hiddenFields, "nickname")) {
            reqVO.setNickname(null);
            ignoreOrderField(reqVO, "nickname");
        }
        if (isFieldHidden(hiddenFields, "mobile")) {
            reqVO.setMobile(null);
            ignoreOrderField(reqVO, "mobile");
        }
        if (isFieldHidden(hiddenFields, "email")) {
            reqVO.setEmail(null);
            ignoreOrderField(reqVO, "email");
        }
        if (isFieldHidden(hiddenFields, "sex")) {
            reqVO.setSex(null);
            ignoreOrderField(reqVO, "sex");
        }
        if (isFieldHidden(hiddenFields, "status")) {
            reqVO.setStatus(null);
            ignoreOrderField(reqVO, "status");
        }
        if (isFieldHidden(hiddenFields, "dataScope")) {
            reqVO.setDataScope(null);
            reqVO.setDataScopeDeptIds(null);
            ignoreOrderField(reqVO, "dataScope");
        }
        if (isFieldHidden(hiddenFields, "createTime")) {
            ignoreOrderField(reqVO, "createTime");
        }
        if (isFieldHidden(hiddenFields, "roleIds") || isFieldHidden(hiddenFields, "roleNames")) {
            reqVO.setRoleId(null);
            ignoreOrderField(reqVO, "roleNames");
        }
        if (isFieldHidden(hiddenFields, "deptIds") || isFieldHidden(hiddenFields, "deptName")) {
            reqVO.setDeptId(null);
            ignoreOrderField(reqVO, "deptName");
        }
    }

    private void ignoreOrderField(UserPageReqVO reqVO, String orderField) {
        String currentOrderField = reqVO.getOrderField() == null ? null : reqVO.getOrderField().trim();
        if (!Objects.equals(orderField, currentOrderField)) {
            return;
        }
        reqVO.setOrderField(null);
        reqVO.setOrderDirection(null);
    }

    private void validateBatchUpdateHiddenFields(UserBatchUpdateReqVO reqVO) {
        Set<String> hiddenFields = getHiddenFieldSet();
        if (hiddenFields.isEmpty()) {
            return;
        }
        if (Boolean.TRUE.equals(reqVO.getUpdateNickname()) && isFieldHidden(hiddenFields, "nickname")) {
            throw invalidParamException("当前无权批量修改名称");
        }
        if (Boolean.TRUE.equals(reqVO.getUpdateDeptIds())
                && (isFieldHidden(hiddenFields, "deptIds") || isFieldHidden(hiddenFields, "deptName"))) {
            throw invalidParamException("当前无权批量修改归属部门");
        }
        if (Boolean.TRUE.equals(reqVO.getUpdateRoleIds())
                && (isFieldHidden(hiddenFields, "roleIds") || isFieldHidden(hiddenFields, "roleNames"))) {
            throw invalidParamException("当前无权批量修改角色");
        }
        if (Boolean.TRUE.equals(reqVO.getUpdateEmail()) && isFieldHidden(hiddenFields, "email")) {
            throw invalidParamException("当前无权批量修改邮箱");
        }
        if (Boolean.TRUE.equals(reqVO.getUpdateMobile()) && isFieldHidden(hiddenFields, "mobile")) {
            throw invalidParamException("当前无权批量修改手机号码");
        }
        if (Boolean.TRUE.equals(reqVO.getUpdateSex()) && isFieldHidden(hiddenFields, "sex")) {
            throw invalidParamException("当前无权批量修改用户性别");
        }
        if (Boolean.TRUE.equals(reqVO.getUpdateStatus()) && isFieldHidden(hiddenFields, "status")) {
            throw invalidParamException("当前无权批量修改状态");
        }
        if (Boolean.TRUE.equals(reqVO.getUpdateDataScope()) && isFieldHidden(hiddenFields, "dataScope")) {
            throw invalidParamException("当前无权批量修改数据范围");
        }
        if (Boolean.TRUE.equals(reqVO.getUpdateDataScope()) && isFieldHidden(hiddenFields, "dataScopeDeptIds")
                && Objects.equals(reqVO.getDataScope(), DataScopeEnum.DEPT_CUSTOM.getScope())) {
            throw invalidParamException("当前无权批量修改指定部门");
        }
        if (Boolean.TRUE.equals(reqVO.getUpdateRemark()) && isFieldHidden(hiddenFields, "remark")) {
            throw invalidParamException("当前无权批量修改备注");
        }
    }

    private void fillUserRoleInfo(List<UserRespVO> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        Map<Long, Set<Long>> userRoleIdMap = permissionService.getUserRoleIdListByUserIds(
                convertList(list, UserRespVO::getId));
        if (CollUtil.isEmpty(userRoleIdMap)) {
            return;
        }
        Set<Long> roleIds = userRoleIdMap.values().stream()
                .filter(Objects::nonNull)
                .flatMap(Set::stream)
                .collect(Collectors.toSet());
        Map<Long, String> roleNameMap = roleService.getRoleList(roleIds).stream()
                .collect(Collectors.toMap(RoleDO::getId, RoleDO::getName));
        list.forEach(item -> {
            Set<Long> itemRoleIds = userRoleIdMap.get(item.getId());
            item.setRoleIds(itemRoleIds);
            item.setRoleNames(buildRoleNames(itemRoleIds, roleNameMap));
        });
    }

    private void fillUserRoleNames(UserRespVO respVO) {
        if (respVO == null || CollUtil.isEmpty(respVO.getRoleIds())) {
            return;
        }
        Map<Long, String> roleNameMap = roleService.getRoleList(respVO.getRoleIds()).stream()
                .collect(Collectors.toMap(RoleDO::getId, RoleDO::getName));
        respVO.setRoleNames(buildRoleNames(respVO.getRoleIds(), roleNameMap));
    }

    private String buildRoleNames(Set<Long> roleIds, Map<Long, String> roleNameMap) {
        if (CollUtil.isEmpty(roleIds) || CollUtil.isEmpty(roleNameMap)) {
            return null;
        }
        return roleIds.stream()
                .map(roleNameMap::get)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("，"));
    }

    private void maskUserRespList(List<UserRespVO> list) {
        if (CollUtil.isEmpty(list)) {
            return;
        }
        Set<String> hiddenFields = getHiddenFieldSet();
        list.forEach(item -> maskUserResp(item, hiddenFields));
    }

    private void maskUserResp(UserRespVO respVO) {
        maskUserResp(respVO, getHiddenFieldSet());
    }

    private void maskUserResp(UserRespVO respVO, Set<String> hiddenFields) {
        if (respVO == null || CollUtil.isEmpty(hiddenFields)) {
            return;
        }
        if (isFieldHidden(hiddenFields, "id")) {
            respVO.setId(null);
        }
        if (isFieldHidden(hiddenFields, "username")) {
            respVO.setUsername(null);
        }
        if (isFieldHidden(hiddenFields, "nickname")) {
            respVO.setNickname(null);
        }
        if (isFieldHidden(hiddenFields, "deptIds") || isFieldHidden(hiddenFields, "deptName")) {
            respVO.setDeptId(null);
            respVO.setDeptIds(null);
            respVO.setDeptName(null);
        }
        if (isFieldHidden(hiddenFields, "roleIds") || isFieldHidden(hiddenFields, "roleNames")) {
            respVO.setRoleIds(null);
            respVO.setRoleNames(null);
        }
        if (isFieldHidden(hiddenFields, "email")) {
            respVO.setEmail(null);
        }
        if (isFieldHidden(hiddenFields, "mobile")) {
            respVO.setMobile(null);
        }
        if (isFieldHidden(hiddenFields, "sex")) {
            respVO.setSex(null);
        }
        if (isFieldHidden(hiddenFields, "status")) {
            respVO.setStatus(null);
        }
        if (isFieldHidden(hiddenFields, "dataScope")) {
            respVO.setDataScope(null);
            respVO.setDataScopeDeptIds(null);
        }
        if (isFieldHidden(hiddenFields, "remark")) {
            respVO.setRemark(null);
        }
        if (isFieldHidden(hiddenFields, "createTime")) {
            respVO.setCreateTime(null);
        }
    }

    @GetMapping("/role-permissions")
    @Operation(summary = "获得用户角色及聚合按钮权限")
    @Parameter(name = "userId", description = "用户编号", required = true)
    @PreAuthorize("@ss.hasPermission('system:user:query')")
    public CommonResult<UserRolePermissionRespVO> getUserRolePermissions(@RequestParam("userId") Long userId) {
        // 1. 查角色ID集合
        Set<Long> roleIds = permissionService.getUserRoleIdListByUserId(userId);
        // 2. 查角色信息
        List<RoleDO> roles = CollUtil.isEmpty(roleIds) ? Collections.emptyList() : roleService.getRoleList(roleIds);
        // 3. 查角色对应的菜单ID（聚合取并集）
        Set<Long> menuIds = CollUtil.isEmpty(roleIds) ? Collections.emptySet() : permissionService.getRoleMenuListByRoleId(roleIds);
        Set<String> deniedPermissions = roleService.hasAnySuperAdmin(roleIds)
                ? Collections.emptySet() : permissionService.getUserDeniedPermissions(userId);
        // 4. 查菜单的 permission 字段（只取 MenuTypeEnum.BUTTON 类型的菜单）
        List<MenuDO> menus = CollUtil.isEmpty(menuIds) ? Collections.emptyList() : menuService.getMenuList(menuIds);
        List<UserRolePermissionRespVO.PermissionInfo> permissions = menus.stream()
                .filter(m -> Objects.equals(m.getType(), MenuTypeEnum.BUTTON.getType()))
                .filter(m -> StrUtil.isNotBlank(m.getPermission()))
                .collect(Collectors.collectingAndThen(
                        Collectors.toMap(
                                MenuDO::getPermission,
                                m -> {
                                    UserRolePermissionRespVO.PermissionInfo info = new UserRolePermissionRespVO.PermissionInfo();
                                    info.setCode(m.getPermission());
                                    info.setName(m.getName());
                                    info.setRoleGranted(true);
                                    info.setDenied(deniedPermissions.contains(m.getPermission()));
                                    info.setChecked(!deniedPermissions.contains(m.getPermission()));
                                    return info;
                                },
                                (a, b) -> a
                        ),
                        map -> map.values().stream()
                                .sorted(Comparator.comparing(info -> info.getCode()))
                                .collect(Collectors.toList())
                ));
        // 5. 查询系统全量启用角色
        List<RoleDO> allRoleList = roleService.getRoleListByStatus(
                Collections.singletonList(CommonStatusEnum.ENABLE.getStatus()));
        // 6. 构造结果
        UserRolePermissionRespVO respVO = new UserRolePermissionRespVO();
        respVO.setRoleInfos(roles.stream().map(r -> {
            UserRolePermissionRespVO.RoleInfo info = new UserRolePermissionRespVO.RoleInfo();
            info.setId(r.getId());
            info.setName(r.getName());
            return info;
        }).collect(Collectors.toList()));
        respVO.setAllRoles(allRoleList.stream().map(r -> {
            UserRolePermissionRespVO.RoleInfo info = new UserRolePermissionRespVO.RoleInfo();
            info.setId(r.getId());
            info.setName(r.getName());
            return info;
        }).collect(Collectors.toList()));
        respVO.setPermissions(permissions);
        return success(respVO);
    }

    @PutMapping("/role-permissions")
    @Operation(summary = "保存用户角色")
    @PreAuthorize("@ss.hasPermission('system:user:update')")
    public CommonResult<Boolean> saveUserRolePermissions(@Valid @RequestBody UserRolePermissionSaveReqVO reqVO) {
        permissionService.assignUserRoleAndDeniedPermissions(reqVO.getUserId(),
                reqVO.getRoleIds() != null ? reqVO.getRoleIds() : Collections.emptySet(),
                reqVO.getDeniedPermissions() != null ? reqVO.getDeniedPermissions() : Collections.emptySet());
        return success(true);
    }

    // ========== 价格字段配置 ==========

    @GetMapping("/price-fields")
    @Operation(summary = "查询用户价格字段配置")
    @Parameter(name = "userId", description = "用户编号", required = true)
    @PreAuthorize("@ss.hasPermission('system:user:query')")
    public CommonResult<List<UserPriceFieldRespVO>> getUserPriceFields(@RequestParam("userId") Long userId) {
        List<UserPriceFieldConfigDTO> list = userPriceFieldService.getUserPriceFieldConfigs(userId);
        List<UserPriceFieldRespVO> result = list.stream().map(item -> {
            UserPriceFieldRespVO vo = new UserPriceFieldRespVO();
            vo.setId(item.getId());
            vo.setUserId(item.getUserId());
            vo.setPriceFieldCode(item.getPriceFieldCode());
            vo.setPriceFieldLabel(item.getPriceFieldLabel());
            vo.setVisible(item.getVisible());
            vo.setSort(item.getSort());
            return vo;
        }).collect(Collectors.toList());
        return success(result);
    }

    @PutMapping("/price-fields")
    @Operation(summary = "保存用户价格字段配置")
    @PreAuthorize("@ss.hasPermission('system:user:update')")
    public CommonResult<Boolean> saveUserPriceFields(@Valid @RequestBody UserPriceFieldSaveReqVO reqVO) {
        userPriceFieldService.saveUserPriceFields(reqVO.getUserId(), reqVO.getFieldCodes());
        return success(true);
    }

}
