package cn.iocoder.yudao.module.system.service.user;

import cn.hutool.core.util.RandomUtil;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.ArrayUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.infra.api.config.ConfigApi;
import cn.iocoder.yudao.module.infra.api.file.FileApi;
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
import cn.iocoder.yudao.module.system.dal.dataobject.permission.RoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.UserRoleDO;
import cn.iocoder.yudao.module.system.dal.dataobject.tenant.TenantDO;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.dept.DeptMapper;
import cn.iocoder.yudao.module.system.dal.mysql.dept.UserDeptMapper;
import cn.iocoder.yudao.module.system.dal.mysql.dept.UserPostMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.RoleMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.UserRoleMapper;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.system.enums.common.SexEnum;
import cn.iocoder.yudao.module.system.enums.permission.DataScopeEnum;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import cn.iocoder.yudao.module.system.service.oauth2.OAuth2TokenService;
import cn.iocoder.yudao.module.system.service.permission.PermissionService;
import cn.iocoder.yudao.module.system.service.permission.RoleService;
import cn.iocoder.yudao.module.system.service.tenant.TenantService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static cn.hutool.core.util.RandomUtil.randomEle;
import static cn.iocoder.yudao.framework.common.util.collection.SetUtils.asSet;
import static cn.iocoder.yudao.framework.common.util.date.LocalDateTimeUtils.buildBetweenTime;
import static cn.iocoder.yudao.framework.common.util.date.LocalDateTimeUtils.buildTime;
import static cn.iocoder.yudao.framework.common.util.object.ObjectUtils.cloneIgnoreId;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertPojoEquals;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.*;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.*;
import static cn.iocoder.yudao.module.system.service.user.AdminUserServiceImpl.USER_INIT_PASSWORD_KEY;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.assertj.core.util.Lists.newArrayList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@Import({AdminUserServiceImpl.class, UserErpBizDataReferenceService.class})
public class AdminUserServiceImplTest extends BaseDbUnitTest {

    @Resource
    private AdminUserServiceImpl userService;

    @Resource
    private AdminUserMapper userMapper;
    @Resource
    private DeptMapper deptMapper;
    @Resource
    private UserDeptMapper userDeptMapper;
    @Resource
    private UserPostMapper userPostMapper;
    @Resource
    private RoleMapper roleMapper;
    @Resource
    private UserRoleMapper userRoleMapper;
    @Resource
    private DataSource dataSource;

    @MockBean
    private DeptService deptService;
    @MockBean
    private PermissionService permissionService;
    @MockBean
    private RoleService roleService;
    @MockBean
    private PasswordEncoder passwordEncoder;
    @MockBean
    private TenantService tenantService;
    @MockBean
    private FileApi fileApi;
    @MockBean
    private ConfigApi configApi;
    @MockBean
    private OAuth2TokenService oauth2TokenService;
    @MockBean
    private AdminUserBatchUpdateExtension batchUpdateExtension;

    @BeforeEach
    public void before() {
        // mock 初始化密码
        when(configApi.getConfigValueByKey(USER_INIT_PASSWORD_KEY)).thenReturn("yudaoyuanma");
    }

    @AfterEach
    public void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    public void testCreatUser_success() {
        // 准备参数
        UserSaveReqVO reqVO = randomPojo(UserSaveReqVO.class, o -> {
            o.setSex(RandomUtil.randomEle(SexEnum.values()).getSex());
            o.setMobile(randomString());
            o.setDataScope(DataScopeEnum.DEPT_ONLY.getScope());
        }).setId(null); // 避免 id 被赋值
        // mock 账户额度充足
        TenantDO tenant = randomPojo(TenantDO.class, o -> o.setAccountCount(1));
        doNothing().when(tenantService).handleTenantInfo(argThat(handler -> {
            handler.handle(tenant);
            return true;
        }));
        // mock deptService 的方法
        DeptDO dept = randomPojo(DeptDO.class, o -> {
            o.setId(reqVO.getDeptId());
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
        });
        when(deptService.getDept(eq(dept.getId()))).thenReturn(dept);
        // mock passwordEncoder 的方法
        when(passwordEncoder.encode(eq(reqVO.getPassword()))).thenReturn("yudaoyuanma");

        // 调用
        Long userId = userService.createUser(reqVO);
        // 断言
        AdminUserDO user = userMapper.selectById(userId);
        assertPojoEquals(reqVO, user, "password", "id", "username");
        assertEquals(reqVO.getMobile(), user.getUsername());
        assertEquals("yudaoyuanma", user.getPassword());
        assertEquals(CommonStatusEnum.ENABLE.getStatus(), user.getStatus());
        assertTrue(userPostMapper.selectListByUserId(user.getId()).isEmpty());
    }

    @Test
    public void testCreatUser_max() {
        // 准备参数
        UserSaveReqVO reqVO = randomPojo(UserSaveReqVO.class);
        // mock 账户额度不足
        TenantDO tenant = randomPojo(TenantDO.class, o -> o.setAccountCount(-1));
        doNothing().when(tenantService).handleTenantInfo(argThat(handler -> {
            handler.handle(tenant);
            return true;
        }));

        // 调用，并断言异常
        assertServiceException(() -> userService.createUser(reqVO), USER_COUNT_MAX, -1);
    }

    @Test
    public void testUpdateUser_success() {
        // mock 数据
        AdminUserDO dbUser = randomAdminUserDO(o -> o.setPostIds(asSet(1L, 2L)));
        userMapper.insert(dbUser);
        userPostMapper.insert(new UserPostDO().setUserId(dbUser.getId()).setPostId(1L));
        userPostMapper.insert(new UserPostDO().setUserId(dbUser.getId()).setPostId(2L));
        // 准备参数
        UserSaveReqVO reqVO = randomPojo(UserSaveReqVO.class, o -> {
            o.setId(dbUser.getId());
            o.setSex(RandomUtil.randomEle(SexEnum.values()).getSex());
            o.setMobile(randomString());
            o.setDataScope(DataScopeEnum.DEPT_ONLY.getScope());
        });
        // mock deptService 的方法
        DeptDO dept = randomPojo(DeptDO.class, o -> {
            o.setId(reqVO.getDeptId());
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
        });
        when(deptService.getDept(eq(dept.getId()))).thenReturn(dept);
        // 调用
        userService.updateUser(reqVO);
        // 断言
        AdminUserDO user = userMapper.selectById(reqVO.getId());
        assertPojoEquals(reqVO, user, "password", "username");
        assertEquals(reqVO.getMobile(), user.getUsername());
        // 断言历史岗位关联不随用户编辑被清空或修改
        List<UserPostDO> userPosts = userPostMapper.selectListByUserId(user.getId());
        assertEquals(1L, userPosts.get(0).getPostId());
        assertEquals(2L, userPosts.get(1).getPostId());
    }

    @Test
    public void testUpdateUserBatch_mobileSyncUsername() {
        // mock 数据
        AdminUserDO dbUser = randomAdminUserDO(o -> o.setMobile(randomMobile()));
        userMapper.insert(dbUser);
        String mobile = randomMobile();
        UserBatchUpdateReqVO reqVO = new UserBatchUpdateReqVO()
                .setIds(singletonList(dbUser.getId()))
                .setUpdateMobile(true)
                .setMobile(mobile);

        // 调用
        userService.updateUserBatch(reqVO);

        // 断言
        AdminUserDO user = userMapper.selectById(dbUser.getId());
        assertEquals(mobile, user.getMobile());
        assertEquals(mobile, user.getUsername());
    }

    @Test
    public void testUpdateUserBatch_dataScopeCustom() {
        // mock 数据
        AdminUserDO dbUser = randomAdminUserDO(o -> {
            o.setDataScope(DataScopeEnum.SELF.getScope());
            o.setDataScopeDeptIds(asSet(10L));
        });
        userMapper.insert(dbUser);
        Set<Long> dataScopeDeptIds = asSet(20L, 30L);
        UserBatchUpdateReqVO reqVO = new UserBatchUpdateReqVO()
                .setIds(singletonList(dbUser.getId()))
                .setUpdateDataScope(true)
                .setDataScope(DataScopeEnum.DEPT_CUSTOM.getScope())
                .setDataScopeDeptIds(dataScopeDeptIds);

        // 调用
        userService.updateUserBatch(reqVO);

        // 断言
        AdminUserDO user = userMapper.selectById(dbUser.getId());
        assertEquals(DataScopeEnum.DEPT_CUSTOM.getScope(), user.getDataScope());
        assertEquals(dataScopeDeptIds, user.getDataScopeDeptIds());
        verify(deptService, times(1)).validateDeptList(eq(dataScopeDeptIds));
    }

    @Test
    public void testUpdateUserBatch_dataScopeInheritRole() {
        // mock 数据
        AdminUserDO dbUser = randomAdminUserDO(o -> {
            o.setDataScope(DataScopeEnum.DEPT_CUSTOM.getScope());
            o.setDataScopeDeptIds(asSet(10L, 20L));
        });
        userMapper.insert(dbUser);
        UserBatchUpdateReqVO reqVO = new UserBatchUpdateReqVO()
                .setIds(singletonList(dbUser.getId()))
                .setUpdateDataScope(true)
                .setDataScope(0)
                .setDataScopeDeptIds(asSet(10L));

        // 调用，并断言异常
        ServiceException exception = assertThrows(ServiceException.class, () -> userService.updateUserBatch(reqVO));
        assertEquals("数据范围不能选择继承角色", exception.getMessage());

        // 断言原数据未被覆盖
        AdminUserDO user = userMapper.selectById(dbUser.getId());
        assertEquals(DataScopeEnum.DEPT_CUSTOM.getScope(), user.getDataScope());
        assertEquals(asSet(10L, 20L), user.getDataScopeDeptIds());
        verify(deptService, never()).validateDeptList(eq(reqVO.getDataScopeDeptIds()));
    }

    @Test
    public void testUpdateUserBatch_dataScopeInvalid() {
        // mock 数据
        AdminUserDO dbUser = randomAdminUserDO();
        userMapper.insert(dbUser);
        UserBatchUpdateReqVO reqVO = new UserBatchUpdateReqVO()
                .setIds(singletonList(dbUser.getId()))
                .setUpdateDataScope(true)
                .setDataScope(99);

        // 调用，并断言异常
        ServiceException exception = assertThrows(ServiceException.class, () -> userService.updateUserBatch(reqVO));
        assertEquals("数据范围不正确", exception.getMessage());
    }

    @Test
    public void testUpdateUserBatch_warehousePermissionOnly_callsExtension() {
        AdminUserDO dbUser = randomAdminUserDO();
        userMapper.insert(dbUser);
        UserBatchUpdateReqVO reqVO = new UserBatchUpdateReqVO()
                .setIds(singletonList(dbUser.getId()))
                .setUpdateWarehousePermissions(true)
                .setWarehousePermissionMode(UserBatchUpdateReqVO.WAREHOUSE_PERMISSION_MODE_ADD)
                .setWarehouseIds(asSet(11L, 12L));

        userService.updateUserBatch(reqVO);

        verify(batchUpdateExtension).validate(eq(reqVO), eq(asSet(dbUser.getId())));
        verify(batchUpdateExtension).update(eq(dbUser.getId()), eq(reqVO));
        AdminUserDO user = userMapper.selectById(dbUser.getId());
        assertPojoEquals(dbUser, user);
    }

    @Test
    public void testUpdateUserBatch_extensionValidateFail_doesNotUpdateUser() {
        AdminUserDO dbUser = randomAdminUserDO(o -> o.setNickname("old"));
        userMapper.insert(dbUser);
        UserBatchUpdateReqVO reqVO = new UserBatchUpdateReqVO()
                .setIds(singletonList(dbUser.getId()))
                .setUpdateNickname(true)
                .setNickname("new");
        doThrow(new IllegalStateException("extension validate failed"))
                .when(batchUpdateExtension).validate(eq(reqVO), eq(asSet(dbUser.getId())));

        assertThrows(IllegalStateException.class, () -> userService.updateUserBatch(reqVO));

        AdminUserDO user = userMapper.selectById(dbUser.getId());
        assertEquals("old", user.getNickname());
        verify(batchUpdateExtension, never()).update(anyLong(), any());
    }

    @Test
    public void testUpdateUserLogin() {
        // mock 数据
        AdminUserDO user = randomAdminUserDO(o -> o.setLoginDate(null));
        userMapper.insert(user);
        // 准备参数
        Long id = user.getId();
        String loginIp = randomString();

        // 调用
        userService.updateUserLogin(id, loginIp);
        // 断言
        AdminUserDO dbUser = userMapper.selectById(id);
        assertEquals(loginIp, dbUser.getLoginIp());
        assertNotNull(dbUser.getLoginDate());
    }

    @Test
    public void testUpdateUserProfile_success() {
        // mock 数据
        AdminUserDO dbUser = randomAdminUserDO(o -> o.setMobile(randomMobile()));
        userMapper.insert(dbUser);
        // 准备参数
        Long userId = dbUser.getId();
        UserProfileUpdateReqVO reqVO = randomPojo(UserProfileUpdateReqVO.class, o -> {
            o.setMobile(randomString());
            o.setSex(RandomUtil.randomEle(SexEnum.values()).getSex());
            o.setAvatar(randomURL());
        });

        // 调用
        userService.updateUserProfile(userId, reqVO);
        // 断言
        AdminUserDO user = userMapper.selectById(userId);
        assertPojoEquals(reqVO, user);
    }

    @Test
    public void testUpdateUserPassword_success() {
        // mock 数据
        AdminUserDO dbUser = randomAdminUserDO(o -> o.setPassword("encode:tudou"));
        userMapper.insert(dbUser);
        // 准备参数
        Long userId = dbUser.getId();
        UserProfileUpdatePasswordReqVO reqVO = randomPojo(UserProfileUpdatePasswordReqVO.class, o -> {
            o.setOldPassword("tudou");
            o.setNewPassword("yuanma");
        });
        // mock 方法
        when(passwordEncoder.encode(anyString())).then(
                (Answer<String>) invocationOnMock -> "encode:" + invocationOnMock.getArgument(0));
        when(passwordEncoder.matches(eq(reqVO.getOldPassword()), eq(dbUser.getPassword()))).thenReturn(true);

        // 调用
        userService.updateUserPassword(userId, reqVO);
        // 断言
        AdminUserDO user = userMapper.selectById(userId);
        assertEquals("encode:yuanma", user.getPassword());
    }

    @Test
    public void testUpdateUserPassword02_success() {
        // mock 数据
        AdminUserDO dbUser = randomAdminUserDO(o -> o.setMobile(randomMobile()));
        userMapper.insert(dbUser);
        // 准备参数
        Long userId = dbUser.getId();
        String password = "yudao";
        // mock 方法
        when(passwordEncoder.encode(anyString())).then(
                (Answer<String>) invocationOnMock -> "encode:" + invocationOnMock.getArgument(0));

        // 调用
        userService.updateUserPassword(userId, password);
        // 断言
        AdminUserDO user = userMapper.selectById(userId);
        assertEquals("encode:" + password, user.getPassword());
    }

    @Test
    public void testUpdateUserStatus() {
        // mock 数据
        AdminUserDO dbUser = randomAdminUserDO(o -> o.setMobile(randomMobile()));
        userMapper.insert(dbUser);
        // 准备参数
        Long userId = dbUser.getId();
        Integer status = randomCommonStatus();

        // 调用
        userService.updateUserStatus(userId, status);
        // 断言
        AdminUserDO user = userMapper.selectById(userId);
        assertEquals(status, user.getStatus());
    }

    @Test
    public void testDeleteUser_success(){
        // mock 数据
        AdminUserDO dbUser = randomAdminUserDO();
        userMapper.insert(dbUser);
        // 准备参数
        Long userId = dbUser.getId();

        // 调用数据
        userService.deleteUser(userId);
        // 校验结果
        assertNull(userMapper.selectById(userId));
        // 校验调用次数
        verify(permissionService, times(1)).processUserDeleted(eq(userId));
    }

    @Test
    public void testDeleteUser_isDeptLeader() {
        AdminUserDO dbUser = randomAdminUserDO();
        userMapper.insert(dbUser);
        DeptDO deptDO = randomPojo(DeptDO.class, o -> o.setLeaderUserId(dbUser.getId()));
        deptMapper.insert(deptDO);

        assertServiceException(() -> userService.deleteUser(dbUser.getId()), USER_IS_DEPT_LEADER);
        assertNotNull(userMapper.selectById(dbUser.getId()));
        verify(permissionService, never()).processUserDeleted(eq(dbUser.getId()));
    }

    @Test
    public void testDeleteUser_existsBizData() throws SQLException {
        TenantContextHolder.setTenantId(1L);
        AdminUserDO dbUser = randomAdminUserDO();
        userMapper.insert(dbUser);
        insertErpPurchaseOrder(dbUser.getId(), false, 1L);

        assertServiceException(() -> userService.deleteUser(dbUser.getId()), USER_EXISTS_BIZ_DATA);
        assertNotNull(userMapper.selectById(dbUser.getId()));
        verify(permissionService, never()).processUserDeleted(eq(dbUser.getId()));
    }

    @Test
    public void testDeleteUser_ignoreDeletedAndOtherTenantBizData() throws SQLException {
        TenantContextHolder.setTenantId(1L);
        AdminUserDO dbUser = randomAdminUserDO();
        userMapper.insert(dbUser);
        insertErpPurchaseOrder(dbUser.getId(), true, 1L);
        insertErpPurchaseOrder(dbUser.getId(), false, 2L);

        userService.deleteUser(dbUser.getId());

        assertNull(userMapper.selectById(dbUser.getId()));
        verify(permissionService, times(1)).processUserDeleted(eq(dbUser.getId()));
    }

    @Test
    public void testDeleteUserList_existsBizData() throws SQLException {
        TenantContextHolder.setTenantId(1L);
        AdminUserDO dbUser1 = randomAdminUserDO();
        userMapper.insert(dbUser1);
        AdminUserDO dbUser2 = randomAdminUserDO();
        userMapper.insert(dbUser2);
        insertErpPurchaseOrder(dbUser2.getId(), false, 1L);

        assertServiceException(() -> userService.deleteUserList(newArrayList(dbUser1.getId(), dbUser2.getId())),
                USER_EXISTS_BIZ_DATA);
        assertNotNull(userMapper.selectById(dbUser1.getId()));
        assertNotNull(userMapper.selectById(dbUser2.getId()));
        verify(permissionService, never()).processUserDeleted(anyLong());
    }

    @Test
    public void testGetUserByUsername() {
        // mock 数据
        AdminUserDO dbUser = randomAdminUserDO();
        userMapper.insert(dbUser);
        // 准备参数
        String username = dbUser.getUsername();

        // 调用
        AdminUserDO user = userService.getUserByUsername(username);
        // 断言
        assertPojoEquals(dbUser, user);
    }

    @Test
    public void testGetUserByMobile() {
        // mock 数据
        AdminUserDO dbUser = randomAdminUserDO();
        userMapper.insert(dbUser);
        // 准备参数
        String mobile = dbUser.getMobile();

        // 调用
        AdminUserDO user = userService.getUserByMobile(mobile);
        // 断言
        assertPojoEquals(dbUser, user);
    }

    @Test
    public void testGetUserPage() {
        // mock 数据
        AdminUserDO dbUser = initGetUserPageData();
        // 准备参数
        UserPageReqVO reqVO = new UserPageReqVO();
        reqVO.setUsername("tu");
        reqVO.setMobile("1560");
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        reqVO.setCreateTime(buildBetweenTime(2020, 12, 1, 2020, 12, 24));
        reqVO.setDeptId(1L); // 其中，1L 是 2L 的父部门
        // mock 方法
        List<DeptDO> deptList = newArrayList(randomPojo(DeptDO.class, o -> o.setId(2L)));
        when(deptService.getChildDeptList(eq(reqVO.getDeptId()))).thenReturn(deptList);

        // 调用
        PageResult<AdminUserDO> pageResult = userService.getUserPage(reqVO);
        // 断言
        assertEquals(1, pageResult.getTotal());
        assertEquals(1, pageResult.getList().size());
        assertPojoEquals(dbUser, pageResult.getList().get(0));
    }

    @Test
    public void testGetUserPage_orderByDeptName() {
        TenantContextHolder.setTenantId(1L);
        DeptDO deptA = insertDept("Alpha Dept");
        DeptDO deptB = insertDept("Beta Dept");
        AdminUserDO userB = insertUser("user-b", deptB.getId());
        AdminUserDO userNoDept = insertUser("user-no-dept", null);
        AdminUserDO userA = insertUser("user-a", deptA.getId());
        UserPageReqVO reqVO = new UserPageReqVO();
        reqVO.setOrderField("deptName");
        reqVO.setOrderDirection("asc");

        PageResult<AdminUserDO> pageResult = userService.getUserPage(reqVO);

        assertEquals(3, pageResult.getTotal());
        assertEquals(userA.getId(), pageResult.getList().get(0).getId());
        assertEquals(userB.getId(), pageResult.getList().get(1).getId());
        assertEquals(userNoDept.getId(), pageResult.getList().get(2).getId());

        reqVO.setOrderDirection("desc");
        pageResult = userService.getUserPage(reqVO);
        assertEquals(userB.getId(), pageResult.getList().get(0).getId());
        assertEquals(userA.getId(), pageResult.getList().get(1).getId());
        assertEquals(userNoDept.getId(), pageResult.getList().get(2).getId());
    }

    @Test
    public void testGetUserPage_orderByRoleNames() {
        TenantContextHolder.setTenantId(1L);
        RoleDO roleAlpha = insertRole("Alpha Role");
        RoleDO roleBeta = insertRole("Beta Role");
        RoleDO roleGamma = insertRole("Gamma Role");
        AdminUserDO userBeta = insertUser("user-beta-role", null);
        AdminUserDO userNoRole = insertUser("user-no-role", null);
        AdminUserDO userAlpha = insertUser("user-alpha-role", null);
        insertUserRole(userBeta.getId(), roleBeta.getId());
        insertUserRole(userAlpha.getId(), roleGamma.getId());
        insertUserRole(userAlpha.getId(), roleAlpha.getId());
        UserPageReqVO reqVO = new UserPageReqVO();
        reqVO.setOrderField("roleNames");
        reqVO.setOrderDirection("asc");

        PageResult<AdminUserDO> pageResult = userService.getUserPage(reqVO);

        assertEquals(3, pageResult.getTotal());
        assertEquals(userAlpha.getId(), pageResult.getList().get(0).getId());
        assertEquals(userBeta.getId(), pageResult.getList().get(1).getId());
        assertEquals(userNoRole.getId(), pageResult.getList().get(2).getId());

        reqVO.setOrderDirection("desc");
        pageResult = userService.getUserPage(reqVO);
        assertEquals(userBeta.getId(), pageResult.getList().get(0).getId());
        assertEquals(userAlpha.getId(), pageResult.getList().get(1).getId());
        assertEquals(userNoRole.getId(), pageResult.getList().get(2).getId());
    }

    @Test
    public void testGetUserPage_invalidOrderFallback() {
        AdminUserDO lowIdUser = insertUser("low-id-user", null);
        AdminUserDO highIdUser = insertUser("high-id-user", null);
        UserPageReqVO reqVO = new UserPageReqVO();
        reqVO.setOrderField("roleNames");
        reqVO.setOrderDirection("invalid");

        PageResult<AdminUserDO> pageResult = userService.getUserPage(reqVO);

        assertEquals(2, pageResult.getTotal());
        assertEquals(highIdUser.getId(), pageResult.getList().get(0).getId());
        assertEquals(lowIdUser.getId(), pageResult.getList().get(1).getId());
    }

    /**
     * 初始化 getUserPage 方法的测试数据
     */
    private AdminUserDO initGetUserPageData() {
        // mock 数据
        AdminUserDO dbUser = randomAdminUserDO(o -> { // 等会查询到
            o.setUsername("tudou");
            o.setMobile("15601691300");
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
            o.setCreateTime(buildTime(2020, 12, 12));
            o.setDeptId(2L);
        });
        userMapper.insert(dbUser);
        // 测试 username 不匹配
        userMapper.insert(cloneIgnoreId(dbUser, o -> o.setUsername("dou")));
        // 测试 mobile 不匹配
        userMapper.insert(cloneIgnoreId(dbUser, o -> o.setMobile("18818260888")));
        // 测试 status 不匹配
        userMapper.insert(cloneIgnoreId(dbUser, o -> o.setStatus(CommonStatusEnum.DISABLE.getStatus())));
        // 测试 createTime 不匹配
        userMapper.insert(cloneIgnoreId(dbUser, o -> o.setCreateTime(buildTime(2020, 11, 11))));
        // 测试 dept 不匹配
        userMapper.insert(cloneIgnoreId(dbUser, o -> o.setDeptId(0L)));
        return dbUser;
    }

    @Test
    public void testGetUser() {
        // mock 数据
        AdminUserDO dbUser = randomAdminUserDO();
        userMapper.insert(dbUser);
        // 准备参数
        Long userId = dbUser.getId();

        // 调用
        AdminUserDO user = userService.getUser(userId);
        // 断言
        assertPojoEquals(dbUser, user);
    }

    @Test
    public void testGetUserListByDeptIds() {
        // mock 数据
        AdminUserDO dbUser = randomAdminUserDO(o -> o.setDeptId(1L));
        userMapper.insert(dbUser);
        // 测试 deptId 不匹配
        userMapper.insert(cloneIgnoreId(dbUser, o -> o.setDeptId(2L)));
        // 准备参数
        Collection<Long> deptIds = singleton(1L);

        // 调用
        List<AdminUserDO> list = userService.getUserListByDeptIds(deptIds);
        // 断言
        assertEquals(1, list.size());
        assertEquals(dbUser, list.get(0));
    }

    /**
     * 情况一，校验不通过，导致插入失败
     */
    @Test
    public void testImportUserList_01() {
        // 准备参数
        UserImportExcelVO importUser = randomPojo(UserImportExcelVO.class, o -> {
            o.setEmail(randomEmail());
            o.setMobile(randomMobile());
        });
        // mock 方法，模拟失败
        doThrow(new ServiceException(DEPT_NOT_FOUND)).when(deptService).validateDeptList(any());

        // 调用
        UserImportRespVO respVO = userService.importUserList(newArrayList(importUser), true);
        // 断言
        assertEquals(0, respVO.getCreateUsernames().size());
        assertEquals(0, respVO.getUpdateUsernames().size());
        assertEquals(1, respVO.getFailureUsernames().size());
        assertEquals(DEPT_NOT_FOUND.getMsg(), respVO.getFailureUsernames().get(importUser.getMobile()));
    }

    /**
     * 情况二，不存在，进行插入
     */
    @Test
    public void testImportUserList_02() {
        // 准备参数
        UserImportExcelVO importUser = randomPojo(UserImportExcelVO.class, o -> {
            o.setStatus(randomEle(CommonStatusEnum.values()).getStatus()); // 保证 status 的范围
            o.setSex(randomEle(SexEnum.values()).getSex()); // 保证 sex 的范围
            o.setEmail(randomEmail());
            o.setMobile(randomMobile());
        });
        // mock deptService 的方法
        DeptDO dept = randomPojo(DeptDO.class, o -> {
            o.setId(importUser.getDeptId());
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
        });
        when(deptService.getDept(eq(dept.getId()))).thenReturn(dept);
        // mock passwordEncoder 的方法
        when(passwordEncoder.encode(eq("yudaoyuanma"))).thenReturn("java");

        // 调用
        UserImportRespVO respVO = userService.importUserList(newArrayList(importUser), true);
        // 断言
        assertEquals(1, respVO.getCreateUsernames().size());
        AdminUserDO user = userMapper.selectByUsername(respVO.getCreateUsernames().get(0));
        assertPojoEquals(importUser, user, "username");
        assertEquals(importUser.getMobile(), user.getUsername());
        assertEquals("java", user.getPassword());
        List<UserDeptDO> userDepts = userDeptMapper.selectListByUserId(user.getId());
        assertEquals(1, userDepts.size());
        assertEquals(importUser.getDeptId(), userDepts.get(0).getDeptId());
        assertEquals(0, respVO.getUpdateUsernames().size());
        assertEquals(0, respVO.getFailureUsernames().size());
    }

    /**
     * 情况三，存在，但是不强制更新
     */
    @Test
    public void testImportUserList_03() {
        // mock 数据
        AdminUserDO dbUser = randomAdminUserDO(o -> o.setMobile(randomMobile()));
        userMapper.insert(dbUser);
        // 准备参数
        UserImportExcelVO importUser = randomPojo(UserImportExcelVO.class, o -> {
            o.setStatus(randomEle(CommonStatusEnum.values()).getStatus()); // 保证 status 的范围
            o.setSex(randomEle(SexEnum.values()).getSex()); // 保证 sex 的范围
            o.setMobile(dbUser.getMobile());
            o.setEmail(randomEmail());
        });
        // mock deptService 的方法
        DeptDO dept = randomPojo(DeptDO.class, o -> {
            o.setId(importUser.getDeptId());
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
        });
        when(deptService.getDept(eq(dept.getId()))).thenReturn(dept);

        // 调用
        UserImportRespVO respVO = userService.importUserList(newArrayList(importUser), false);
        // 断言
        assertEquals(0, respVO.getCreateUsernames().size());
        assertEquals(0, respVO.getUpdateUsernames().size());
        assertEquals(1, respVO.getFailureUsernames().size());
        assertEquals(USER_USERNAME_EXISTS.getMsg(), respVO.getFailureUsernames().get(importUser.getMobile()));
    }

    /**
     * 情况四，存在，强制更新
     */
    @Test
    public void testImportUserList_04() {
        // mock 数据
        AdminUserDO dbUser = randomAdminUserDO(o -> o.setMobile(randomMobile()));
        userMapper.insert(dbUser);
        userDeptMapper.insert(new UserDeptDO().setUserId(dbUser.getId()).setDeptId(dbUser.getDeptId()));
        // 准备参数
        UserImportExcelVO importUser = randomPojo(UserImportExcelVO.class, o -> {
            o.setStatus(randomEle(CommonStatusEnum.values()).getStatus()); // 保证 status 的范围
            o.setSex(randomEle(SexEnum.values()).getSex()); // 保证 sex 的范围
            o.setMobile(dbUser.getMobile());
            o.setEmail(randomEmail());
        });
        // mock deptService 的方法
        DeptDO dept = randomPojo(DeptDO.class, o -> {
            o.setId(importUser.getDeptId());
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
        });
        when(deptService.getDept(eq(dept.getId()))).thenReturn(dept);

        // 调用
        UserImportRespVO respVO = userService.importUserList(newArrayList(importUser), true);
        // 断言
        assertEquals(0, respVO.getCreateUsernames().size());
        assertEquals(1, respVO.getUpdateUsernames().size());
        AdminUserDO user = userMapper.selectByUsername(respVO.getUpdateUsernames().get(0));
        assertPojoEquals(importUser, user, "username");
        assertEquals(importUser.getMobile(), user.getUsername());
        List<UserDeptDO> userDepts = userDeptMapper.selectListByUserId(user.getId());
        assertEquals(1, userDepts.size());
        assertEquals(importUser.getDeptId(), userDepts.get(0).getDeptId());
        assertEquals(0, respVO.getFailureUsernames().size());
    }

    @Test
    public void testValidateUserExists_notExists() {
        assertServiceException(() -> userService.validateUserExists(randomLongId()), USER_NOT_EXISTS);
    }

    @Test
    public void testValidateUsernameUnique_usernameExistsForCreate() {
        // 准备参数
        String username = randomString();
        // mock 数据
        userMapper.insert(randomAdminUserDO(o -> o.setUsername(username)));

        // 调用，校验异常
        assertServiceException(() -> userService.validateUsernameUnique(null, username),
                USER_USERNAME_EXISTS);
    }

    @Test
    public void testValidateUsernameUnique_usernameExistsForUpdate() {
        // 准备参数
        Long id = randomLongId();
        String username = randomString();
        // mock 数据
        userMapper.insert(randomAdminUserDO(o -> o.setUsername(username)));

        // 调用，校验异常
        assertServiceException(() -> userService.validateUsernameUnique(id, username),
                USER_USERNAME_EXISTS);
    }

    @Test
    public void testValidateEmailUnique_emailExistsForCreate() {
        // 准备参数
        String email = randomString();
        // mock 数据
        userMapper.insert(randomAdminUserDO(o -> o.setEmail(email)));

        // 调用，校验异常
        assertServiceException(() -> userService.validateEmailUnique(null, email),
                USER_EMAIL_EXISTS);
    }

    @Test
    public void testValidateEmailUnique_emailExistsForUpdate() {
        // 准备参数
        Long id = randomLongId();
        String email = randomString();
        // mock 数据
        userMapper.insert(randomAdminUserDO(o -> o.setEmail(email)));

        // 调用，校验异常
        assertServiceException(() -> userService.validateEmailUnique(id, email),
                USER_EMAIL_EXISTS);
    }

    @Test
    public void testValidateMobileUnique_mobileExistsForCreate() {
        // 准备参数
        String mobile = randomString();
        // mock 数据
        userMapper.insert(randomAdminUserDO(o -> o.setMobile(mobile)));

        // 调用，校验异常
        assertServiceException(() -> userService.validateMobileUnique(null, mobile),
                USER_MOBILE_EXISTS);
    }

    @Test
    public void testValidateMobileUnique_mobileExistsForUpdate() {
        // 准备参数
        Long id = randomLongId();
        String mobile = randomString();
        // mock 数据
        userMapper.insert(randomAdminUserDO(o -> o.setMobile(mobile)));

        // 调用，校验异常
        assertServiceException(() -> userService.validateMobileUnique(id, mobile),
                USER_MOBILE_EXISTS);
    }

    @Test
    public void testValidateOldPassword_notExists() {
        assertServiceException(() -> userService.validateOldPassword(randomLongId(), randomString()),
                USER_NOT_EXISTS);
    }

    @Test
    public void testValidateOldPassword_passwordFailed() {
        // mock 数据
        AdminUserDO user = randomAdminUserDO();
        userMapper.insert(user);
        // 准备参数
        Long id = user.getId();
        String oldPassword = user.getPassword();

        // 调用，校验异常
        assertServiceException(() -> userService.validateOldPassword(id, oldPassword),
                USER_PASSWORD_FAILED);
        // 校验调用
        verify(passwordEncoder, times(1)).matches(eq(oldPassword), eq(user.getPassword()));
    }

    @Test
    public void testUserListByPostIds() {
        // 准备参数
        Collection<Long> postIds = asSet(10L, 20L);
        // mock user1 数据
        AdminUserDO user1 = randomAdminUserDO(o -> o.setPostIds(asSet(10L, 30L)));
        userMapper.insert(user1);
        userPostMapper.insert(new UserPostDO().setUserId(user1.getId()).setPostId(10L));
        userPostMapper.insert(new UserPostDO().setUserId(user1.getId()).setPostId(30L));
        // mock user2 数据
        AdminUserDO user2 = randomAdminUserDO(o -> o.setPostIds(singleton(100L)));
        userMapper.insert(user2);
        userPostMapper.insert(new UserPostDO().setUserId(user2.getId()).setPostId(100L));

        // 调用
        List<AdminUserDO> result = userService.getUserListByPostIds(postIds);
        // 断言
        assertEquals(1, result.size());
        assertEquals(user1, result.get(0));
    }

    @Test
    public void testGetUserList() {
        // mock 数据
        AdminUserDO user = randomAdminUserDO();
        userMapper.insert(user);
        // 测试 id 不匹配
        userMapper.insert(randomAdminUserDO());
        // 准备参数
        Collection<Long> ids = singleton(user.getId());

        // 调用
        List<AdminUserDO> result = userService.getUserList(ids);
        // 断言
        assertEquals(1, result.size());
        assertEquals(user, result.get(0));
    }

    @Test
    public void testGetUserMap() {
        // mock 数据
        AdminUserDO user = randomAdminUserDO();
        userMapper.insert(user);
        // 测试 id 不匹配
        userMapper.insert(randomAdminUserDO());
        // 准备参数
        Collection<Long> ids = singleton(user.getId());

        // 调用
        Map<Long, AdminUserDO> result = userService.getUserMap(ids);
        // 断言
        assertEquals(1, result.size());
        assertEquals(user, result.get(user.getId()));
    }

    @Test
    public void testGetUserListByNickname() {
        // mock 数据
        AdminUserDO user = randomAdminUserDO(o -> o.setNickname("芋头"));
        userMapper.insert(user);
        // 测试 nickname 不匹配
        userMapper.insert(randomAdminUserDO(o -> o.setNickname("源码")));
        // 准备参数
        String nickname = "芋";

        // 调用
        List<AdminUserDO> result = userService.getUserListByNickname(nickname);
        // 断言
        assertEquals(1, result.size());
        assertEquals(user, result.get(0));
    }

    @Test
    public void testGetUserListByStatus() {
        // mock 数据
        AdminUserDO user = randomAdminUserDO(o -> o.setStatus(CommonStatusEnum.DISABLE.getStatus()));
        userMapper.insert(user);
        // 测试 status 不匹配
        userMapper.insert(randomAdminUserDO(o -> o.setStatus(CommonStatusEnum.ENABLE.getStatus())));
        // 准备参数
        Integer status = CommonStatusEnum.DISABLE.getStatus();

        // 调用
        List<AdminUserDO> result = userService.getUserListByStatus(status);
        // 断言
        assertEquals(1, result.size());
        assertEquals(user, result.get(0));
    }

    @Test
    public void testValidateUserList_success() {
        // mock 数据
        AdminUserDO userDO = randomAdminUserDO().setStatus(CommonStatusEnum.ENABLE.getStatus());
        userMapper.insert(userDO);
        // 准备参数
        List<Long> ids = singletonList(userDO.getId());

        // 调用，无需断言
        userService.validateUserList(ids);
    }

    @Test
    public void testValidateUserList_notFound() {
        // 准备参数
        List<Long> ids = singletonList(randomLongId());

        // 调用, 并断言异常
        assertServiceException(() -> userService.validateUserList(ids), USER_NOT_EXISTS);
    }

    @Test
    public void testValidateUserList_notEnable() {
        // mock 数据
        AdminUserDO userDO = randomAdminUserDO().setStatus(CommonStatusEnum.DISABLE.getStatus());
        userMapper.insert(userDO);
        // 准备参数
        List<Long> ids = singletonList(userDO.getId());

        // 调用, 并断言异常
        assertServiceException(() -> userService.validateUserList(ids), USER_IS_DISABLE,
                userDO.getNickname());
    }

    // ========== 随机对象 ==========

    private void insertErpPurchaseOrder(Long purchaser, boolean deleted, Long tenantId) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO erp_purchase_order (purchaser, deleted, tenant_id) VALUES (?, ?, ?)")) {
            statement.setLong(1, purchaser);
            statement.setBoolean(2, deleted);
            statement.setLong(3, tenantId);
            statement.executeUpdate();
        }
    }

    private DeptDO insertDept(String name) {
        DeptDO dept = randomPojo(DeptDO.class, o -> {
            o.setName(name);
            o.setParentId(0L);
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
            o.setTenantId(1L);
        });
        deptMapper.insert(dept);
        return dept;
    }

    private RoleDO insertRole(String name) {
        RoleDO role = randomPojo(RoleDO.class, o -> {
            o.setName(name);
            o.setCode(name.toLowerCase().replace(" ", "_"));
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
            o.setType(1);
            o.setDataScope(1);
            o.setTenantId(1L);
        });
        roleMapper.insert(role);
        return role;
    }

    private AdminUserDO insertUser(String username, Long deptId) {
        AdminUserDO user = randomAdminUserDO(o -> {
            o.setUsername(username);
            o.setMobile(randomMobile());
            o.setNickname(username);
            o.setDeptId(deptId);
            o.setTenantId(1L);
        });
        userMapper.insert(user);
        return user;
    }

    private void insertUserRole(Long userId, Long roleId) {
        UserRoleDO userRole = new UserRoleDO();
        userRole.setUserId(userId);
        userRole.setRoleId(roleId);
        userRoleMapper.insert(userRole);
    }

    @SafeVarargs
    private static AdminUserDO randomAdminUserDO(Consumer<AdminUserDO>... consumers) {
        Consumer<AdminUserDO> consumer = (o) -> {
            o.setStatus(randomEle(CommonStatusEnum.values()).getStatus()); // 保证 status 的范围
            o.setSex(randomEle(SexEnum.values()).getSex()); // 保证 sex 的范围
        };
        return randomPojo(AdminUserDO.class, ArrayUtils.append(consumer, consumers));
    }

}
