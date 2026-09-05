package cn.iocoder.yudao.module.system.service.user;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjUtil;
import cn.hutool.core.util.StrUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.enums.UserTypeEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.CollectionUtils;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.common.util.validation.ValidationUtils;
import cn.iocoder.yudao.framework.datapermission.core.util.DataPermissionUtils;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import cn.iocoder.yudao.module.system.controller.admin.auth.vo.AuthRegisterReqVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.profile.UserProfileUpdatePasswordReqVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.profile.UserProfileUpdateReqVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserBatchUpdateReqVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserImportExcelVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserImportRespVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserPageReqVO;
import cn.iocoder.yudao.module.system.controller.admin.user.vo.user.UserSaveReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.UserDeptDO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.UserPostDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.dept.DeptMapper;
import cn.iocoder.yudao.module.system.dal.mysql.dept.UserDeptMapper;
import cn.iocoder.yudao.module.system.dal.mysql.dept.UserPostMapper;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.system.enums.common.SexEnum;
import cn.iocoder.yudao.module.system.enums.permission.DataScopeEnum;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.module.system.service.oauth2.OAuth2TokenService;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.permission.RoleService;
import cn.iocoder.yudao.module.system.service.logger.SystemOperateLogService;
import cn.iocoder.yudao.module.system.service.tenant.TenantService;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.google.common.annotations.VisibleForTesting;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.service.impl.DiffParseFunction;
import com.mzt.logapi.starter.annotation.LogRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.validation.ConstraintViolationException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.invalidParamException;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.*;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.system.enums.LogRecordConstants.*;

/**
 * 后台用户 Service 实现类
 *
 * @author 芋道源码
 */
@Service("adminUserService")
@Slf4j
public class AdminUserServiceImpl implements AdminUserService {

    private static final String FIELD_PERMISSION_MODULE = "system_users";

    static final String USER_INIT_PASSWORD_KEY = "system.user.init-password";

    static final String USER_REGISTER_ENABLED_KEY = "system.user.register-enabled";

    @Resource
    private AdminUserMapper userMapper;
    @Resource
    private DeptMapper deptMapper;

    @Resource
    private DeptService deptService;
    @Resource
    private PermissionService permissionService;
    @Resource
    private RoleService roleService;
    @Resource
    private PasswordEncoder passwordEncoder;
    @Resource
    @Lazy // 延迟，避免循环依赖报错
    private TenantService tenantService;
    @Resource
    @Lazy // 懒加载，避免循环依赖
    private OAuth2TokenService oauth2TokenService;

    @Resource
    private UserPostMapper userPostMapper;
    @Resource
    private UserDeptMapper userDeptMapper;

    @Resource
    private ConfigApi configApi;
    @Resource
    private UserErpBizDataReferenceService userErpBizDataReferenceService;
    @Resource
    private SystemOperateLogService operateLogService;
    @Autowired(required = false)
    private List<AdminUserBatchUpdateExtension> batchUpdateExtensions = Collections.emptyList();

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = SYSTEM_USER_TYPE, subType = SYSTEM_USER_CREATE_SUB_TYPE, bizNo = "{{#user.id}}",
            success = SYSTEM_USER_CREATE_SUCCESS)
    public Long createUser(UserSaveReqVO createReqVO) {
        // 1.1 校验账户配合
        tenantService.handleTenantInfo(tenant -> {
            long count = userMapper.selectCount();
            if (count >= tenant.getAccountCount()) {
                throw exception(USER_COUNT_MAX, tenant.getAccountCount());
            }
        });
        normalizeUserDept(createReqVO);
        normalizeUserDataScope(createReqVO, false);
        syncUsernameWithMobile(createReqVO);
        // 1.2 校验正确性
        validateUserForCreateOrUpdate(null, createReqVO.getUsername(),
                createReqVO.getMobile(), createReqVO.getEmail(), createReqVO.getDeptIds(), createReqVO.getRoleIds());
        // 2.1 插入用户
        AdminUserDO user = BeanUtils.toBean(createReqVO, AdminUserDO.class);
        user.setStatus(CommonStatusEnum.ENABLE.getStatus()); // 默认开启
        user.setPassword(encodePassword(createReqVO.getPassword())); // 加密密码
        userMapper.insert(user);
        // 2.2 插入关联部门和角色
        insertUserDept(user.getId(), createReqVO.getDeptIds());
        permissionService.assignUserRole(user.getId(), createReqVO.getRoleIds());

        // 3. 记录操作日志上下文
        LogRecordContext.putVariable("user", user);
        return user.getId();
    }

    @Override
    public Long registerUser(AuthRegisterReqVO registerReqVO) {
        // 1.1 校验是否开启注册
        if (ObjUtil.notEqual(configApi.getConfigValueByKey(USER_REGISTER_ENABLED_KEY), "true")) {
            throw exception(USER_REGISTER_DISABLED);
        }
        // 1.2 校验账户配合
        tenantService.handleTenantInfo(tenant -> {
            long count = userMapper.selectCount();
            if (count >= tenant.getAccountCount()) {
                throw exception(USER_COUNT_MAX, tenant.getAccountCount());
            }
        });
        // 1.3 校验正确性
        validateUserForCreateOrUpdate(null, registerReqVO.getUsername(), null, null, null, null);

        // 2. 插入用户
        AdminUserDO user = BeanUtils.toBean(registerReqVO, AdminUserDO.class);
        user.setStatus(CommonStatusEnum.ENABLE.getStatus()); // 默认开启
        user.setPassword(encodePassword(registerReqVO.getPassword())); // 加密密码
        userMapper.insert(user);
        return user.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = SYSTEM_USER_TYPE, subType = SYSTEM_USER_UPDATE_SUB_TYPE, bizNo = "{{#updateReqVO.id}}",
            success = SYSTEM_USER_UPDATE_SUCCESS)
    public void updateUser(UserSaveReqVO updateReqVO) {
        updateReqVO.setPassword(null); // 特殊：此处不更新密码
        normalizeUserDept(updateReqVO);
        List<String> hiddenFields = permissionService.getCurrentUserHiddenFields(FIELD_PERMISSION_MODULE);
        preserveHiddenFields(updateReqVO, hiddenFields);
        boolean dataScopeHidden = isFieldHidden(hiddenFields, "dataScope");
        if (updateReqVO.getDataScope() == null) {
            preserveUserDataScope(updateReqVO);
            dataScopeHidden = true;
        } else if (!dataScopeHidden) {
            validateUserDataScope(updateReqVO);
        }
        normalizeUserDataScope(updateReqVO, dataScopeHidden);
        syncUsernameWithMobile(updateReqVO);
        // 1. 校验正确性
        AdminUserDO oldUser = validateUserForCreateOrUpdate(updateReqVO.getId(), updateReqVO.getUsername(),
                updateReqVO.getMobile(), updateReqVO.getEmail(), updateReqVO.getDeptIds(), updateReqVO.getRoleIds());

        // 2.1 更新用户
        AdminUserDO updateObj = BeanUtils.toBean(updateReqVO, AdminUserDO.class);
        userMapper.updateById(updateObj);
        // 2.2 更新部门和角色
        if (!isFieldHidden(hiddenFields, "deptIds")) {
            updateUserDept(updateReqVO);
        }
        if (!isFieldHidden(hiddenFields, "roleIds")) {
            permissionService.assignUserRole(updateReqVO.getId(), updateReqVO.getRoleIds());
        }

        // 3. 记录操作日志上下文
        LogRecordContext.putVariable(DiffParseFunction.OLD_OBJECT, BeanUtils.toBean(oldUser, UserSaveReqVO.class));
        LogRecordContext.putVariable("user", oldUser);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUserBatch(UserBatchUpdateReqVO reqVO) {
        validateBatchUpdateUser(reqVO);
        Set<Long> deptIds = CollUtil.emptyIfNull(reqVO.getDeptIds());
        for (Long id : new LinkedHashSet<>(reqVO.getIds())) {
            AdminUserDO oldUser = userMapper.selectById(id);
            updateUserBaseFields(id, reqVO, deptIds);
            if (Boolean.TRUE.equals(reqVO.getUpdateDeptIds())) {
                updateUserDept(new UserSaveReqVO().setId(id).setDeptIds(deptIds));
            }
            if (Boolean.TRUE.equals(reqVO.getUpdateRoleIds())) {
                permissionService.assignUserRole(id, reqVO.getRoleIds());
            }
            if (Boolean.TRUE.equals(reqVO.getUpdateStatus()) && CommonStatusEnum.isDisable(reqVO.getStatus())) {
                oauth2TokenService.removeAccessToken(id, UserTypeEnum.ADMIN.getValue());
            }
            for (AdminUserBatchUpdateExtension extension : batchUpdateExtensions) {
                extension.update(id, reqVO);
            }
            operateLogService.recordUpdate(SYSTEM_USER_TYPE, SYSTEM_USER_BATCH_UPDATE_SUB_TYPE,
                    id, oldUser, userMapper.selectById(id));
        }
    }

    private void updateUserBaseFields(Long id, UserBatchUpdateReqVO reqVO, Set<Long> deptIds) {
        boolean updateBaseFields = Boolean.TRUE.equals(reqVO.getUpdateNickname())
                || Boolean.TRUE.equals(reqVO.getUpdateDeptIds())
                || Boolean.TRUE.equals(reqVO.getUpdateEmail())
                || Boolean.TRUE.equals(reqVO.getUpdateMobile())
                || Boolean.TRUE.equals(reqVO.getUpdateSex())
                || Boolean.TRUE.equals(reqVO.getUpdateStatus())
                || Boolean.TRUE.equals(reqVO.getUpdateDataScope())
                || Boolean.TRUE.equals(reqVO.getUpdateRemark());
        if (!updateBaseFields) {
            return;
        }
        LambdaUpdateWrapper<AdminUserDO> updateWrapper = new LambdaUpdateWrapper<AdminUserDO>()
                .eq(AdminUserDO::getId, id);
        if (Boolean.TRUE.equals(reqVO.getUpdateNickname())) {
            updateWrapper.set(AdminUserDO::getNickname, reqVO.getNickname().trim());
        }
        if (Boolean.TRUE.equals(reqVO.getUpdateDeptIds())) {
            updateWrapper.set(AdminUserDO::getDeptId, deptIds.iterator().next());
        }
        if (Boolean.TRUE.equals(reqVO.getUpdateEmail())) {
            updateWrapper.set(AdminUserDO::getEmail, trimToNull(reqVO.getEmail()));
        }
        if (Boolean.TRUE.equals(reqVO.getUpdateMobile())) {
            updateWrapper.set(AdminUserDO::getMobile, trimToNull(reqVO.getMobile()));
            updateWrapper.set(AdminUserDO::getUsername, trimToNull(reqVO.getMobile()));
        }
        if (Boolean.TRUE.equals(reqVO.getUpdateSex())) {
            updateWrapper.set(AdminUserDO::getSex, reqVO.getSex());
        }
        if (Boolean.TRUE.equals(reqVO.getUpdateStatus())) {
            updateWrapper.set(AdminUserDO::getStatus, reqVO.getStatus());
        }
        if (Boolean.TRUE.equals(reqVO.getUpdateDataScope())) {
            updateWrapper.set(AdminUserDO::getDataScope, reqVO.getDataScope());
            updateWrapper.set(AdminUserDO::getDataScopeDeptIds, JsonUtils.toJsonString(reqVO.getDataScopeDeptIds()));
        }
        if (Boolean.TRUE.equals(reqVO.getUpdateRemark())) {
            updateWrapper.set(AdminUserDO::getRemark, reqVO.getRemark());
        }
        userMapper.update(null, updateWrapper);
    }

    private String trimToNull(String value) {
        return StrUtil.emptyToNull(StrUtil.trim(value));
    }

    private void validateBatchUpdateUser(UserBatchUpdateReqVO reqVO) {
        Set<Long> userIds = new LinkedHashSet<>(reqVO.getIds());
        if (userIds.size() != reqVO.getIds().size() || userIds.contains(null)) {
            throw exception(USER_NOT_EXISTS);
        }
        if (userMapper.selectByIds(userIds).size() != userIds.size()) {
            throw exception(USER_NOT_EXISTS);
        }
        if (Boolean.TRUE.equals(reqVO.getUpdateDeptIds())) {
            deptService.validateDeptList(reqVO.getDeptIds());
        }
        if (Boolean.TRUE.equals(reqVO.getUpdateRoleIds())) {
            roleService.validateRoleList(reqVO.getRoleIds());
        }
        if (Boolean.TRUE.equals(reqVO.getUpdateEmail()) && StrUtil.isNotBlank(reqVO.getEmail())) {
            if (userIds.size() > 1) {
                throw invalidParamException("邮箱不能批量设置为同一个非空值");
            }
            validateEmailUnique(userIds.iterator().next(), reqVO.getEmail());
        }
        if (Boolean.TRUE.equals(reqVO.getUpdateMobile())) {
            if (StrUtil.isBlank(reqVO.getMobile())) {
                throw invalidParamException("手机号码不能为空");
            }
            if (userIds.size() > 1) {
                throw invalidParamException("手机号码不能批量设置为同一个非空值");
            }
            String mobile = StrUtil.trim(reqVO.getMobile());
            reqVO.setMobile(mobile);
            validateMobileUnique(userIds.iterator().next(), mobile);
            validateUsernameUnique(userIds.iterator().next(), mobile);
        }
        if (Boolean.TRUE.equals(reqVO.getUpdateSex()) && !isValidSex(reqVO.getSex())) {
            throw invalidParamException("用户性别不正确");
        }
        if (Boolean.TRUE.equals(reqVO.getUpdateDataScope())) {
            normalizeBatchUserDataScope(reqVO);
            validateBatchDataScope(reqVO);
        }
        for (AdminUserBatchUpdateExtension extension : batchUpdateExtensions) {
            extension.validate(reqVO, userIds);
        }
    }

    private void normalizeBatchUserDataScope(UserBatchUpdateReqVO reqVO) {
        if (Objects.equals(reqVO.getDataScope(), 0)) {
            throw invalidParamException("数据范围不能选择继承角色");
        }
        if (!Objects.equals(reqVO.getDataScope(), DataScopeEnum.DEPT_CUSTOM.getScope())) {
            reqVO.setDataScopeDeptIds(Collections.emptySet());
        }
    }

    private void validateBatchDataScope(UserBatchUpdateReqVO reqVO) {
        if (reqVO.getDataScope() == null) {
            throw invalidParamException("数据范围不能为空");
        }
        boolean validScope = Arrays.stream(DataScopeEnum.values())
                .anyMatch(item -> item.getScope().equals(reqVO.getDataScope()));
        if (!validScope) {
            throw invalidParamException("数据范围不正确");
        }
        if (Objects.equals(reqVO.getDataScope(), DataScopeEnum.DEPT_CUSTOM.getScope())) {
            if (CollUtil.isEmpty(reqVO.getDataScopeDeptIds())) {
                throw invalidParamException("指定部门数据范围时，部门不能为空");
            }
            deptService.validateDeptList(reqVO.getDataScopeDeptIds());
        }
    }

    private boolean isValidSex(Integer sex) {
        if (sex == null) {
            return false;
        }
        return Arrays.stream(SexEnum.values()).anyMatch(item -> item.getSex().equals(sex));
    }

    private void preserveHiddenFields(UserSaveReqVO reqVO, List<String> hiddenFields) {
        if (reqVO.getId() == null || CollUtil.isEmpty(hiddenFields)) {
            return;
        }
        AdminUserDO oldUser = userMapper.selectById(reqVO.getId());
        if (oldUser == null) {
            return;
        }
        if (isFieldHidden(hiddenFields, "username")) {
            reqVO.setUsername(oldUser.getUsername());
        }
        if (isFieldHidden(hiddenFields, "nickname")) {
            reqVO.setNickname(oldUser.getNickname());
        }
        if (isFieldHidden(hiddenFields, "email")) {
            reqVO.setEmail(oldUser.getEmail());
        }
        if (isFieldHidden(hiddenFields, "mobile")) {
            reqVO.setMobile(oldUser.getMobile());
        }
        if (isFieldHidden(hiddenFields, "sex")) {
            reqVO.setSex(oldUser.getSex());
        }
        if (isFieldHidden(hiddenFields, "remark")) {
            reqVO.setRemark(oldUser.getRemark());
        }
        if (isFieldHidden(hiddenFields, "deptIds")) {
            reqVO.setDeptId(oldUser.getDeptId());
            reqVO.setDeptIds(getUserDeptIdListByUserId(reqVO.getId()));
        }
        if (isFieldHidden(hiddenFields, "roleIds")) {
            reqVO.setRoleIds(permissionService.getUserRoleIdListByUserId(reqVO.getId()));
        }
        if (isFieldHidden(hiddenFields, "dataScope")) {
            reqVO.setDataScope(oldUser.getDataScope());
            reqVO.setDataScopeDeptIds(oldUser.getDataScopeDeptIds());
        }
    }

    private boolean isFieldHidden(Collection<String> hiddenFields, String fieldKey) {
        return hiddenFields != null && (hiddenFields.contains(fieldKey) || hiddenFields.contains("col_" + fieldKey));
    }

    private void preserveUserDataScope(UserSaveReqVO reqVO) {
        if (reqVO.getId() == null) {
            return;
        }
        AdminUserDO oldUser = userMapper.selectById(reqVO.getId());
        if (oldUser == null) {
            return;
        }
        reqVO.setDataScope(oldUser.getDataScope());
        reqVO.setDataScopeDeptIds(oldUser.getDataScopeDeptIds());
    }

    private void insertUserDept(Long userId, Set<Long> deptIds) {
        if (CollectionUtil.isEmpty(deptIds)) {
            return;
        }
        userDeptMapper.insertBatch(convertList(deptIds,
                deptId -> new UserDeptDO().setUserId(userId).setDeptId(deptId)));
    }

    private void updateUserDept(UserSaveReqVO reqVO) {
        Long userId = reqVO.getId();
        Set<Long> dbDeptIds = convertSet(userDeptMapper.selectListByUserId(userId), UserDeptDO::getDeptId);
        Set<Long> deptIds = CollUtil.emptyIfNull(reqVO.getDeptIds());
        Collection<Long> createDeptIds = CollUtil.subtract(deptIds, dbDeptIds);
        Collection<Long> deleteDeptIds = CollUtil.subtract(dbDeptIds, deptIds);
        if (CollectionUtil.isNotEmpty(createDeptIds)) {
            userDeptMapper.insertBatch(convertList(createDeptIds,
                    deptId -> new UserDeptDO().setUserId(userId).setDeptId(deptId)));
        }
        if (CollectionUtil.isNotEmpty(deleteDeptIds)) {
            userDeptMapper.deleteByUserIdAndDeptId(userId, deleteDeptIds);
        }
    }

    private void normalizeUserDept(UserSaveReqVO reqVO) {
        if (CollUtil.isEmpty(reqVO.getDeptIds())) {
            return;
        }
        if (reqVO.getDeptId() != null && reqVO.getDeptIds().contains(reqVO.getDeptId())) {
            return;
        }
        reqVO.setDeptId(reqVO.getDeptIds().iterator().next());
    }

    private void validateUserDataScope(UserSaveReqVO reqVO) {
        if (reqVO.getDataScope() == null) {
            throw invalidParamException("数据范围不能为空");
        }
        if (Objects.equals(reqVO.getDataScope(), 0)) {
            throw invalidParamException("数据范围不能选择继承角色");
        }
    }

    private void normalizeUserDataScope(UserSaveReqVO reqVO, boolean allowLegacyInherit) {
        if (Objects.equals(reqVO.getDataScope(), 0)) {
            if (allowLegacyInherit) {
                return;
            }
            throw invalidParamException("数据范围不能选择继承角色");
        }
        if (reqVO.getDataScope() == null && allowLegacyInherit) {
            return;
        }
        if (!Objects.equals(reqVO.getDataScope(), DataScopeEnum.DEPT_CUSTOM.getScope())) {
            reqVO.setDataScopeDeptIds(Collections.emptySet());
        }
    }

    private void syncUsernameWithMobile(UserSaveReqVO reqVO) {
        if (StrUtil.isBlank(reqVO.getMobile())) {
            throw invalidParamException("手机号码不能为空");
        }
        reqVO.setMobile(StrUtil.trim(reqVO.getMobile()));
        reqVO.setUsername(reqVO.getMobile());
    }

    @Override
    public void updateUserLogin(Long id, String loginIp) {
        userMapper.updateById(new AdminUserDO().setId(id).setLoginIp(loginIp).setLoginDate(LocalDateTime.now()));
    }

    @Override
    public void updateUserProfile(Long id, UserProfileUpdateReqVO reqVO) {
        // 校验正确性
        validateUserExists(id);
        validateEmailUnique(id, reqVO.getEmail());
        validateMobileUnique(id, reqVO.getMobile());
        // 执行更新
        userMapper.updateById(BeanUtils.toBean(reqVO, AdminUserDO.class).setId(id));
    }

    @Override
    public void updateUserPassword(Long id, UserProfileUpdatePasswordReqVO reqVO) {
        // 校验旧密码密码
        validateOldPassword(id, reqVO.getOldPassword());
        // 执行更新
        AdminUserDO updateObj = new AdminUserDO().setId(id);
        updateObj.setPassword(encodePassword(reqVO.getNewPassword())); // 加密密码
        userMapper.updateById(updateObj);
    }

    @Override
    @LogRecord(type = SYSTEM_USER_TYPE, subType = SYSTEM_USER_UPDATE_PASSWORD_SUB_TYPE, bizNo = "{{#id}}",
            success = SYSTEM_USER_UPDATE_PASSWORD_SUCCESS)
    public void updateUserPassword(Long id, String password) {
        // 1. 校验用户存在
        AdminUserDO user = validateUserExists(id);

        // 2. 更新密码
        AdminUserDO updateObj = new AdminUserDO();
        updateObj.setId(id);
        updateObj.setPassword(encodePassword(password)); // 加密密码
        userMapper.updateById(updateObj);

        // 3. 记录操作日志上下文
        LogRecordContext.putVariable("user", user);
        LogRecordContext.putVariable("newPassword", updateObj.getPassword());
    }

    @Override
    public void updateUserStatus(Long id, Integer status) {
        // 校验用户存在
        AdminUserDO oldUser = validateUserExists(id);
        // 更新状态
        AdminUserDO updateObj = new AdminUserDO();
        updateObj.setId(id);
        updateObj.setStatus(status);
        userMapper.updateById(updateObj);
        operateLogService.recordUpdate(SYSTEM_USER_TYPE, SYSTEM_USER_UPDATE_STATUS_SUB_TYPE,
                id, oldUser, userMapper.selectById(id));

        // 如果是禁用用户，则删除其 Token 信息
        if (CommonStatusEnum.isDisable(status)) {
            oauth2TokenService.removeAccessToken(id, UserTypeEnum.ADMIN.getValue());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    @LogRecord(type = SYSTEM_USER_TYPE, subType = SYSTEM_USER_DELETE_SUB_TYPE, bizNo = "{{#id}}",
            success = SYSTEM_USER_DELETE_SUCCESS)
    public void deleteUser(Long id) {
        // 1. 校验用户存在
        AdminUserDO user = validateUserExists(id);

        // 2.1 删除用户
        validateUserCanDelete(Collections.singletonList(id));
        userMapper.deleteById(id);
        // 2.2 删除用户关联数据
        permissionService.processUserDeleted(id);
        // 2.2 删除用户岗位
        userPostMapper.deleteByUserId(id);
        // 2.3 删除用户部门
        userDeptMapper.deleteByUserId(id);

        // 3. 记录操作日志上下文
        LogRecordContext.putVariable("user", user);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUserList(List<Long> ids) {
        // 1. 批量删除用户
        validateUserCanDelete(ids);
        List<AdminUserDO> users = userMapper.selectByIds(ids);
        userMapper.deleteByIds(ids);

        // 2. 批量删除用户关联数据
        ids.forEach(id -> {
            permissionService.processUserDeleted(id);
            userPostMapper.deleteByUserId(id);
            userDeptMapper.deleteByUserId(id);
        });
        users.forEach(user -> operateLogService.recordDelete(SYSTEM_USER_TYPE, SYSTEM_USER_BATCH_DELETE_SUB_TYPE,
                user.getId(), user));
    }

    @Override
    public AdminUserDO getUserByUsername(String username) {
        return userMapper.selectByUsername(username);
    }

    private void validateUserCanDelete(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        if (ids.stream().anyMatch(Objects::isNull)) {
            throw exception(USER_NOT_EXISTS);
        }
        Set<Long> userIds = new HashSet<>(ids);
        if (userMapper.selectByIds(userIds).size() != userIds.size()) {
            throw exception(USER_NOT_EXISTS);
        }
        if (CollUtil.isNotEmpty(deptMapper.selectListByLeaderUserIds(userIds))) {
            throw exception(USER_IS_DEPT_LEADER);
        }
        if (userErpBizDataReferenceService.existsByUserIds(userIds)) {
            throw exception(USER_EXISTS_BIZ_DATA);
        }
    }

    @Override
    public AdminUserDO getUserByMobile(String mobile) {
        return userMapper.selectByMobile(mobile);
    }

    @Override
    public List<AdminUserDO> getUserListByMobile(String mobile) {
        return userMapper.selectListByMobile(mobile);
    }

    @Override
    public PageResult<AdminUserDO> getUserPage(UserPageReqVO reqVO) {
        // 如果有角色编号，查询角色对应的用户编号
        Set<Long> userIds = null;
        if (reqVO.getRoleId() != null) {
            userIds = permissionService.getUserRoleIdListByRoleId(singleton(reqVO.getRoleId()));
            if (CollUtil.isEmpty(userIds)) {
                return PageResult.empty();
            }
        }

        Set<Long> deptIds = getDeptCondition(reqVO.getDeptId());
        if (CollUtil.isNotEmpty(deptIds)) {
            Set<Long> deptUserIds = convertSet(userDeptMapper.selectListByDeptIds(deptIds), UserDeptDO::getUserId);
            CollUtil.addAll(deptUserIds, convertSet(userMapper.selectListByDeptIds(deptIds), AdminUserDO::getId));
            if (CollUtil.isEmpty(deptUserIds)) {
                return PageResult.empty();
            }
            userIds = intersectUserIds(userIds, deptUserIds);
            if (CollUtil.isEmpty(userIds)) {
                return PageResult.empty();
            }
        }

        // 分页查询
        return userMapper.selectPage(reqVO, Collections.emptySet(), userIds);
    }

    @Override
    public AdminUserDO getUser(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    public Set<Long> getUserDeptIdListByUserId(Long userId) {
        Set<Long> deptIds = convertSet(userDeptMapper.selectListByUserId(userId), UserDeptDO::getDeptId);
        AdminUserDO user = userMapper.selectById(userId);
        if (user != null) {
            CollectionUtils.addIfNotNull(deptIds, user.getDeptId());
        }
        return deptIds;
    }

    @Override
    public List<AdminUserDO> getUserListByDeptIds(Collection<Long> deptIds) {
        if (CollUtil.isEmpty(deptIds)) {
            return Collections.emptyList();
        }
        Set<Long> userIds = convertSet(userDeptMapper.selectListByDeptIds(deptIds), UserDeptDO::getUserId);
        CollUtil.addAll(userIds, convertSet(userMapper.selectListByDeptIds(deptIds), AdminUserDO::getId));
        if (CollUtil.isEmpty(userIds)) {
            return Collections.emptyList();
        }
        return userMapper.selectByIds(userIds);
    }

    @Override
    public List<AdminUserDO> getUserListByPostIds(Collection<Long> postIds) {
        if (CollUtil.isEmpty(postIds)) {
            return Collections.emptyList();
        }
        Set<Long> userIds = convertSet(userPostMapper.selectListByPostIds(postIds), UserPostDO::getUserId);
        if (CollUtil.isEmpty(userIds)) {
            return Collections.emptyList();
        }
        return userMapper.selectByIds(userIds);
    }

    @Override
    public List<AdminUserDO> getUserList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return Collections.emptyList();
        }
        return userMapper.selectByIds(ids);
    }

    @Override
    public void validateUserList(Collection<Long> ids) {
        if (CollUtil.isEmpty(ids)) {
            return;
        }
        // 获得岗位信息
        List<AdminUserDO> users = userMapper.selectByIds(ids);
        Map<Long, AdminUserDO> userMap = CollectionUtils.convertMap(users, AdminUserDO::getId);
        // 校验
        ids.forEach(id -> {
            AdminUserDO user = userMap.get(id);
            if (user == null) {
                throw exception(USER_NOT_EXISTS);
            }
            if (!CommonStatusEnum.ENABLE.getStatus().equals(user.getStatus())) {
                throw exception(USER_IS_DISABLE, user.getNickname());
            }
        });
    }

    @Override
    public List<AdminUserDO> getUserListByNickname(String nickname) {
        return userMapper.selectListByNickname(nickname);
    }

    private Set<Long> intersectUserIds(Set<Long> sourceUserIds, Set<Long> filterUserIds) {
        if (sourceUserIds == null) {
            return filterUserIds;
        }
        sourceUserIds.retainAll(filterUserIds);
        return sourceUserIds;
    }

    /**
     * 获得部门条件：查询指定部门的子部门编号们，包括自身
     *
     * @param deptId 部门编号
     * @return 部门编号集合
     */
    private Set<Long> getDeptCondition(Long deptId) {
        if (deptId == null) {
            return Collections.emptySet();
        }
        Set<Long> deptIds = convertSet(deptService.getChildDeptList(deptId), DeptDO::getId);
        deptIds.add(deptId); // 包括自身
        return deptIds;
    }

    private AdminUserDO validateUserForCreateOrUpdate(Long id, String username, String mobile, String email,
                                                      Set<Long> deptIds, Set<Long> roleIds) {
        // 关闭数据权限，避免因为没有数据权限，查询不到数据，进而导致唯一校验不正确
        return DataPermissionUtils.executeIgnore(() -> {
            // 校验用户存在
            AdminUserDO user = validateUserExists(id);
            // 校验用户名唯一
            validateUsernameUnique(id, username);
            // 校验手机号唯一
            validateMobileUnique(id, mobile);
            // 校验邮箱唯一
            validateEmailUnique(id, email);
            // 校验部门处于开启状态
            deptService.validateDeptList(deptIds);
            // 校验角色处于开启状态
            roleService.validateRoleList(roleIds);
            return user;
        });
    }

    @VisibleForTesting
    AdminUserDO validateUserExists(Long id) {
        if (id == null) {
            return null;
        }
        AdminUserDO user = userMapper.selectById(id);
        if (user == null) {
            throw exception(USER_NOT_EXISTS);
        }
        return user;
    }

    @VisibleForTesting
    void validateUsernameUnique(Long id, String username) {
        if (StrUtil.isBlank(username)) {
            return;
        }
        AdminUserDO user = userMapper.selectByUsername(username);
        if (user == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同 id 的用户
        if (id == null) {
            throw exception(USER_USERNAME_EXISTS);
        }
        if (!user.getId().equals(id)) {
            throw exception(USER_USERNAME_EXISTS);
        }
    }

    @VisibleForTesting
    void validateEmailUnique(Long id, String email) {
        if (StrUtil.isBlank(email)) {
            return;
        }
        AdminUserDO user = userMapper.selectByEmail(email);
        if (user == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同 id 的用户
        if (id == null) {
            throw exception(USER_EMAIL_EXISTS);
        }
        if (!user.getId().equals(id)) {
            throw exception(USER_EMAIL_EXISTS);
        }
    }

    @VisibleForTesting
    void validateMobileUnique(Long id, String mobile) {
        if (StrUtil.isBlank(mobile)) {
            return;
        }
        AdminUserDO user = userMapper.selectByMobile(mobile);
        if (user == null) {
            return;
        }
        // 如果 id 为空，说明不用比较是否为相同 id 的用户
        if (id == null) {
            throw exception(USER_MOBILE_EXISTS);
        }
        if (!user.getId().equals(id)) {
            throw exception(USER_MOBILE_EXISTS);
        }
    }

    /**
     * 校验旧密码
     * @param id          用户 id
     * @param oldPassword 旧密码
     */
    @VisibleForTesting
    void validateOldPassword(Long id, String oldPassword) {
        AdminUserDO user = userMapper.selectById(id);
        if (user == null) {
            throw exception(USER_NOT_EXISTS);
        }
        if (!isPasswordMatch(oldPassword, user.getPassword())) {
            throw exception(USER_PASSWORD_FAILED);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 添加事务，异常则回滚所有导入
    public UserImportRespVO importUserList(List<UserImportExcelVO> importUsers, boolean isUpdateSupport) {
        // 1.1 参数校验
        if (CollUtil.isEmpty(importUsers)) {
            throw exception(USER_IMPORT_LIST_IS_EMPTY);
        }
        // 1.2 初始化密码不能为空
        String initPassword = configApi.getConfigValueByKey(USER_INIT_PASSWORD_KEY);
        if (StrUtil.isEmpty(initPassword)) {
            throw exception(USER_IMPORT_INIT_PASSWORD);
        }

        // 2. 遍历，逐个创建 or 更新
        UserImportRespVO respVO = UserImportRespVO.builder().createUsernames(new ArrayList<>())
                .updateUsernames(new ArrayList<>()).failureUsernames(new LinkedHashMap<>()).build();
        AtomicInteger index = new AtomicInteger(1);
        importUsers.forEach(importUser -> {
            int currentIndex = index.getAndIncrement();
            String mobile = StrUtil.trim(importUser.getMobile());
            String rowKey = StrUtil.blankToDefault(mobile, "第 " + currentIndex + " 行");
            if (StrUtil.isBlank(mobile)) {
                respVO.getFailureUsernames().put(rowKey, "手机号码不能为空");
                return;
            }
            // 2.1.1 校验字段是否符合要求
            try {
                ValidationUtils.validate(BeanUtils.toBean(importUser, UserSaveReqVO.class)
                        .setMobile(mobile).setUsername(mobile).setPassword(initPassword));
            } catch (ConstraintViolationException ex) {
                respVO.getFailureUsernames().put(rowKey, ex.getMessage());
                return;
            }
            AdminUserDO existUser = userMapper.selectByMobile(mobile);
            if (existUser == null) {
                existUser = userMapper.selectByUsername(mobile);
            }
            // 2.1.2 校验，判断是否有不符合的原因
            try {
                validateUserForCreateOrUpdate(existUser == null ? null : existUser.getId(), mobile, mobile, importUser.getEmail(),
                        importUser.getDeptId() == null ? null : Collections.singleton(importUser.getDeptId()), null);
            } catch (ServiceException ex) {
                respVO.getFailureUsernames().put(mobile, ex.getMessage());
                return;
            }

            // 2.2.1 判断如果不存在，在进行插入
            if (existUser == null) {
                AdminUserDO user = BeanUtils.toBean(importUser, AdminUserDO.class)
                        .setUsername(mobile)
                        .setMobile(mobile)
                        .setPassword(encodePassword(initPassword)); // 设置默认密码
                userMapper.insert(user);
                insertUserDept(user.getId(), importUser.getDeptId() == null ? null : Collections.singleton(importUser.getDeptId()));
                respVO.getCreateUsernames().add(mobile);
                return;
            }
            // 2.2.2 如果存在，判断是否允许更新
            if (!isUpdateSupport) {
                respVO.getFailureUsernames().put(mobile, USER_USERNAME_EXISTS.getMsg());
                return;
            }
            AdminUserDO updateUser = BeanUtils.toBean(importUser, AdminUserDO.class)
                    .setUsername(mobile)
                    .setMobile(mobile);
            updateUser.setId(existUser.getId());
            userMapper.updateById(updateUser);
            updateUserDept(new UserSaveReqVO().setId(existUser.getId())
                    .setDeptIds(importUser.getDeptId() == null ? Collections.emptySet() : Collections.singleton(importUser.getDeptId())));
            respVO.getUpdateUsernames().add(mobile);
        });
        return respVO;
    }

    @Override
    public List<AdminUserDO> getUserListByStatus(Integer status) {
        return userMapper.selectListByStatus(status);
    }

    @Override
    public boolean isPasswordMatch(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    /**
     * 对密码进行加密
     *
     * @param password 密码
     * @return 加密后的密码
     */
    private String encodePassword(String password) {
        return passwordEncoder.encode(password);
    }

}
