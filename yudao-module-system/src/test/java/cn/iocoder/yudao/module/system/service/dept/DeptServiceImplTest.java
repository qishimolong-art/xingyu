package cn.iocoder.yudao.module.system.service.dept;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.util.object.ObjectUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseDbUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptBatchUpdateReqVO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptImportExcelVO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptImportRespVO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptListReqVO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptSaveReqVO;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptUpdateSortReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.UserDeptDO;
import cn.iocoder.yudao.module.system.dal.mysql.dept.UserDeptMapper;
import cn.iocoder.yudao.module.system.dal.dataobject.user.AdminUserDO;
import cn.iocoder.yudao.module.system.dal.mysql.dept.DeptMapper;
import cn.iocoder.yudao.module.system.dal.mysql.user.AdminUserMapper;
import cn.iocoder.yudao.module.system.enums.common.SexEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Import;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertPojoEquals;
import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.framework.test.core.util.RandomUtils.*;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.*;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

/**
 * {@link DeptServiceImpl} 的单元测试类
 *
 * @author niudehua
 */
@Import({DeptServiceImpl.class, DeptErpBizDataReferenceService.class})
public class DeptServiceImplTest extends BaseDbUnitTest {

    @Resource
    private DeptServiceImpl deptService;
    @Resource
    private DeptMapper deptMapper;
    @Resource
    private AdminUserMapper userMapper;
    @Resource
    private UserDeptMapper userDeptMapper;
    @Resource
    private DataSource dataSource;

    @AfterEach
    public void clearTenantContext() {
        TenantContextHolder.clear();
    }

    @Test
    public void testCreateDept() {
        // 准备参数
        DeptSaveReqVO reqVO = randomPojo(DeptSaveReqVO.class, o -> {
            o.setId(null); // 防止 id 被设置
            o.setParentId(DeptDO.PARENT_ID_ROOT);
            o.setStatus(randomCommonStatus());
        });

        // 调用
        Long deptId = deptService.createDept(reqVO);
        // 断言
        assertNotNull(deptId);
        // 校验记录的属性是否正确
        DeptDO deptDO = deptMapper.selectById(deptId);
        assertPojoEquals(reqVO, deptDO, "id");
    }

    @Test
    public void testCreateDept_defaultSortWhenSortNull() {
        DeptDO siblingDeptDO = randomPojo(DeptDO.class, o -> {
            o.setParentId(DeptDO.PARENT_ID_ROOT);
            o.setSort(20);
            o.setStatus(randomCommonStatus());
        });
        deptMapper.insert(siblingDeptDO);
        DeptSaveReqVO reqVO = randomPojo(DeptSaveReqVO.class, o -> {
            o.setId(null);
            o.setParentId(DeptDO.PARENT_ID_ROOT);
            o.setSort(null);
            o.setStatus(randomCommonStatus());
        });

        Long deptId = deptService.createDept(reqVO);

        DeptDO deptDO = deptMapper.selectById(deptId);
        assertEquals(30, deptDO.getSort());
    }

    @Test
    public void testCreateDept_defaultSortWhenNoSibling() {
        DeptSaveReqVO reqVO = randomPojo(DeptSaveReqVO.class, o -> {
            o.setId(null);
            o.setParentId(DeptDO.PARENT_ID_ROOT);
            o.setSort(null);
            o.setStatus(randomCommonStatus());
        });

        Long deptId = deptService.createDept(reqVO);

        DeptDO deptDO = deptMapper.selectById(deptId);
        assertEquals(10, deptDO.getSort());
    }

    @Test
    public void testUpdateDept() {
        // mock 数据
        DeptDO dbDeptDO = randomPojo(DeptDO.class, o -> o.setStatus(randomCommonStatus()));
        deptMapper.insert(dbDeptDO);// @Sql: 先插入出一条存在的数据
        // 准备参数
        DeptSaveReqVO reqVO = randomPojo(DeptSaveReqVO.class, o -> {
            // 设置更新的 ID
            o.setParentId(DeptDO.PARENT_ID_ROOT);
            o.setId(dbDeptDO.getId());
            o.setStatus(randomCommonStatus());
        });

        // 调用
        deptService.updateDept(reqVO);
        // 校验是否更新正确
        DeptDO deptDO = deptMapper.selectById(reqVO.getId()); // 获取最新的
        assertPojoEquals(reqVO, deptDO);
    }

    @Test
    public void testUpdateDept_clearLeaderUserId() {
        AdminUserDO user = randomPojo(AdminUserDO.class, o -> {
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
            o.setSex(SexEnum.UNKNOWN.getSex());
        });
        userMapper.insert(user);
        DeptDO dbDeptDO = randomPojo(DeptDO.class, o -> {
            o.setParentId(DeptDO.PARENT_ID_ROOT);
            o.setLeaderUserId(user.getId());
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
        });
        deptMapper.insert(dbDeptDO);
        DeptSaveReqVO reqVO = new DeptSaveReqVO()
                .setId(dbDeptDO.getId())
                .setName(dbDeptDO.getName())
                .setParentId(dbDeptDO.getParentId())
                .setSort(dbDeptDO.getSort())
                .setLeaderUserId(null)
                .setStatus(CommonStatusEnum.DISABLE.getStatus());

        deptService.updateDept(reqVO);

        DeptDO deptDO = deptMapper.selectById(reqVO.getId());
        assertNull(deptDO.getLeaderUserId());
        assertEquals(CommonStatusEnum.DISABLE.getStatus(), deptDO.getStatus());
    }

    @Test
    public void testUpdateDeptSort() {
        Long parentId = randomLongId();
        DeptDO deptDO1 = randomPojo(DeptDO.class, o -> o.setParentId(parentId).setSort(1));
        deptMapper.insert(deptDO1);
        DeptDO deptDO2 = randomPojo(DeptDO.class, o -> o.setParentId(parentId).setSort(2));
        deptMapper.insert(deptDO2);
        DeptUpdateSortReqVO reqVO = new DeptUpdateSortReqVO();
        reqVO.setItems(Arrays.asList(
                new DeptUpdateSortReqVO.Item().setId(deptDO1.getId()).setSort(20),
                new DeptUpdateSortReqVO.Item().setId(deptDO2.getId()).setSort(10)
        ));

        deptService.updateDeptSort(reqVO);

        assertEquals(20, deptMapper.selectById(deptDO1.getId()).getSort());
        assertEquals(10, deptMapper.selectById(deptDO2.getId()).getSort());
    }

    @Test
    public void testUpdateDeptSort_parentNotSame() {
        DeptDO deptDO1 = randomPojo(DeptDO.class, o -> o.setParentId(1L).setSort(1));
        deptMapper.insert(deptDO1);
        DeptDO deptDO2 = randomPojo(DeptDO.class, o -> o.setParentId(2L).setSort(2));
        deptMapper.insert(deptDO2);
        DeptUpdateSortReqVO reqVO = new DeptUpdateSortReqVO();
        reqVO.setItems(Arrays.asList(
                new DeptUpdateSortReqVO.Item().setId(deptDO1.getId()).setSort(20),
                new DeptUpdateSortReqVO.Item().setId(deptDO2.getId()).setSort(10)
        ));

        assertServiceException(() -> deptService.updateDeptSort(reqVO), DEPT_SORT_PARENT_NOT_SAME);
    }

    @Test
    public void testBatchUpdateDept_clearLeaderAndUpdateStatus() {
        AdminUserDO user = randomPojo(AdminUserDO.class, o -> {
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
            o.setSex(SexEnum.UNKNOWN.getSex());
        });
        userMapper.insert(user);
        DeptDO deptDO1 = randomPojo(DeptDO.class, o -> {
            o.setParentId(DeptDO.PARENT_ID_ROOT);
            o.setLeaderUserId(user.getId());
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
        });
        deptMapper.insert(deptDO1);
        DeptDO deptDO2 = randomPojo(DeptDO.class, o -> {
            o.setParentId(DeptDO.PARENT_ID_ROOT);
            o.setLeaderUserId(user.getId());
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
        });
        deptMapper.insert(deptDO2);
        DeptBatchUpdateReqVO reqVO = new DeptBatchUpdateReqVO()
                .setIds(Arrays.asList(deptDO1.getId(), deptDO2.getId()))
                .setUpdateLeaderUserId(true).setLeaderUserId(null)
                .setUpdateStatus(true).setStatus(CommonStatusEnum.DISABLE.getStatus());

        deptService.batchUpdateDept(reqVO);

        DeptDO updateDeptDO1 = deptMapper.selectById(deptDO1.getId());
        DeptDO updateDeptDO2 = deptMapper.selectById(deptDO2.getId());
        assertNull(updateDeptDO1.getLeaderUserId());
        assertNull(updateDeptDO2.getLeaderUserId());
        assertEquals(CommonStatusEnum.DISABLE.getStatus(), updateDeptDO1.getStatus());
        assertEquals(CommonStatusEnum.DISABLE.getStatus(), updateDeptDO2.getStatus());
    }

    @Test
    public void testBatchUpdateDept_parentIsSelectedChild() {
        DeptDO parentDept = randomPojo(DeptDO.class, o -> o.setParentId(DeptDO.PARENT_ID_ROOT));
        deptMapper.insert(parentDept);
        DeptDO childDept = randomPojo(DeptDO.class, o -> o.setParentId(parentDept.getId()));
        deptMapper.insert(childDept);
        DeptBatchUpdateReqVO reqVO = new DeptBatchUpdateReqVO()
                .setIds(singletonList(parentDept.getId()))
                .setUpdateParentId(true).setParentId(childDept.getId());

        assertServiceException(() -> deptService.batchUpdateDept(reqVO), DEPT_BATCH_UPDATE_PARENT_IS_SELECTED_CHILD);
    }

    @Test
    public void testImportDeptList_createRootSuccess() {
        DeptDO siblingDeptDO = randomPojo(DeptDO.class, o -> {
            o.setParentId(DeptDO.PARENT_ID_ROOT);
            o.setSort(20);
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
        });
        deptMapper.insert(siblingDeptDO);
        DeptImportExcelVO importDept = DeptImportExcelVO.builder()
                .name("导入部门").status("启用").build();

        DeptImportRespVO respVO = deptService.importDeptList(singletonList(importDept), false);

        assertEquals(singletonList("导入部门"), respVO.getCreateNames(), respVO.getFailureNames().toString());
        assertTrue(respVO.getUpdateNames().isEmpty());
        assertTrue(respVO.getFailureNames().isEmpty());
        DeptDO deptDO = deptMapper.selectByParentIdAndName(DeptDO.PARENT_ID_ROOT, "导入部门");
        assertNotNull(deptDO);
        assertEquals(30, deptDO.getSort());
        assertEquals(CommonStatusEnum.ENABLE.getStatus(), deptDO.getStatus());
    }

    @Test
    public void testImportDeptList_invalidStatus() {
        DeptImportExcelVO importDept = DeptImportExcelVO.builder()
                .name("导入部门").status("1").build();

        DeptImportRespVO respVO = deptService.importDeptList(singletonList(importDept), false);

        assertTrue(respVO.getCreateNames().isEmpty());
        assertTrue(respVO.getUpdateNames().isEmpty());
        assertEquals(1, respVO.getFailureNames().size());
        assertTrue(respVO.getFailureNames().get("导入部门").contains("启用"));
        assertNull(deptMapper.selectByParentIdAndName(DeptDO.PARENT_ID_ROOT, "导入部门"));
    }

    @Test
    public void testImportDeptList_updateClearLeader() {
        AdminUserDO user = randomPojo(AdminUserDO.class, o -> {
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
            o.setSex(SexEnum.UNKNOWN.getSex());
        });
        userMapper.insert(user);
        DeptDO deptDO = randomPojo(DeptDO.class, o -> {
            o.setName("导入部门");
            o.setParentId(DeptDO.PARENT_ID_ROOT);
            o.setLeaderUserId(user.getId());
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
            o.setSort(10);
        });
        deptMapper.insert(deptDO);
        DeptImportExcelVO importDept = DeptImportExcelVO.builder()
                .name("导入部门").sort(40).leaderUserName("").status("禁用").build();

        DeptImportRespVO respVO = deptService.importDeptList(singletonList(importDept), true);

        assertEquals(singletonList("导入部门"), respVO.getUpdateNames());
        assertTrue(respVO.getCreateNames().isEmpty());
        assertTrue(respVO.getFailureNames().isEmpty());
        DeptDO updateDeptDO = deptMapper.selectById(deptDO.getId());
        assertEquals(40, updateDeptDO.getSort());
        assertNull(updateDeptDO.getLeaderUserId());
        assertEquals(CommonStatusEnum.DISABLE.getStatus(), updateDeptDO.getStatus());
    }

    @Test
    public void testDeleteDept_success() {
        // mock 数据
        DeptDO dbDeptDO = randomPojo(DeptDO.class);
        deptMapper.insert(dbDeptDO);// @Sql: 先插入出一条存在的数据
        // 准备参数
        Long id = dbDeptDO.getId();

        // 调用
        deptService.deleteDept(id);
        // 校验数据不存在了
        assertNull(deptMapper.selectById(id));
    }

    @Test
    public void testDeleteDept_exitsChildren() {
        // mock 数据
        DeptDO parentDept = randomPojo(DeptDO.class);
        deptMapper.insert(parentDept);// @Sql: 先插入出一条存在的数据
        // 准备参数
        DeptDO childrenDeptDO = randomPojo(DeptDO.class, o -> {
            o.setParentId(parentDept.getId());
            o.setStatus(randomCommonStatus());
        });
        // 插入子部门
        deptMapper.insert(childrenDeptDO);

        // 调用, 并断言异常
        assertServiceException(() -> deptService.deleteDept(parentDept.getId()), DEPT_EXITS_CHILDREN);
    }

    @Test
    public void testDeleteDept_existsUserByMainDept() {
        DeptDO deptDO = randomPojo(DeptDO.class);
        deptMapper.insert(deptDO);
        userMapper.insert(randomPojo(AdminUserDO.class, o -> {
            o.setDeptId(deptDO.getId());
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
            o.setSex(SexEnum.UNKNOWN.getSex());
        }));

        assertServiceException(() -> deptService.deleteDept(deptDO.getId()), DEPT_EXISTS_USER);
        assertNotNull(deptMapper.selectById(deptDO.getId()));
    }

    @Test
    public void testDeleteDept_existsUserByUserDept() {
        DeptDO deptDO = randomPojo(DeptDO.class);
        deptMapper.insert(deptDO);
        UserDeptDO userDeptDO = new UserDeptDO();
        userDeptDO.setUserId(randomLongId());
        userDeptDO.setDeptId(deptDO.getId());
        userDeptMapper.insert(userDeptDO);

        assertServiceException(() -> deptService.deleteDept(deptDO.getId()), DEPT_EXISTS_USER);
        assertNotNull(deptMapper.selectById(deptDO.getId()));
    }

    @Test
    public void testDeleteDept_existsBizData() throws SQLException {
        TenantContextHolder.setTenantId(1L);
        DeptDO deptDO = randomPojo(DeptDO.class);
        deptMapper.insert(deptDO);
        insertErpPurchaseOrder(deptDO.getId(), false, 1L);

        assertServiceException(() -> deptService.deleteDept(deptDO.getId()), DEPT_EXISTS_BIZ_DATA);
        assertNotNull(deptMapper.selectById(deptDO.getId()));
    }

    @Test
    public void testDeleteDept_ignoreDeletedAndOtherTenantBizData() throws SQLException {
        TenantContextHolder.setTenantId(1L);
        DeptDO deptDO = randomPojo(DeptDO.class);
        deptMapper.insert(deptDO);
        insertErpPurchaseOrder(deptDO.getId(), true, 1L);
        insertErpPurchaseOrder(deptDO.getId(), false, 2L);

        deptService.deleteDept(deptDO.getId());

        assertNull(deptMapper.selectById(deptDO.getId()));
    }

    @Test
    public void testDeleteDeptList_success() {
        // mock 数据
        DeptDO deptDO1 = randomPojo(DeptDO.class);
        deptMapper.insert(deptDO1);
        DeptDO deptDO2 = randomPojo(DeptDO.class);
        deptMapper.insert(deptDO2);
        // 准备参数
        List<Long> ids = Arrays.asList(deptDO1.getId(), deptDO2.getId());

        // 调用
        deptService.deleteDeptList(ids);
        // 校验数据不存在了
        assertNull(deptMapper.selectById(deptDO1.getId()));
        assertNull(deptMapper.selectById(deptDO2.getId()));
    }

    @Test
    public void testDeleteDeptList_exitsChildren() {
        // mock 数据
        DeptDO parentDept = randomPojo(DeptDO.class);
        deptMapper.insert(parentDept);
        DeptDO childrenDeptDO = randomPojo(DeptDO.class, o -> {
            o.setParentId(parentDept.getId());
            o.setStatus(randomCommonStatus());
        });
        deptMapper.insert(childrenDeptDO);
        DeptDO anotherDept = randomPojo(DeptDO.class);
        deptMapper.insert(anotherDept);

        // 准备参数（包含有子部门的 parentDept）
        List<Long> ids = Arrays.asList(parentDept.getId(), anotherDept.getId());

        // 调用, 并断言异常
        assertServiceException(() -> deptService.deleteDeptList(ids), DEPT_EXITS_CHILDREN);
    }

    @Test
    public void testDeleteDeptList_existsUserByMainDept() {
        DeptDO deptDO1 = randomPojo(DeptDO.class);
        deptMapper.insert(deptDO1);
        DeptDO deptDO2 = randomPojo(DeptDO.class);
        deptMapper.insert(deptDO2);
        userMapper.insert(randomPojo(AdminUserDO.class, o -> {
            o.setDeptId(deptDO1.getId());
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
            o.setSex(SexEnum.UNKNOWN.getSex());
        }));

        assertServiceException(() -> deptService.deleteDeptList(Arrays.asList(deptDO1.getId(), deptDO2.getId())),
                DEPT_EXISTS_USER);
        assertNotNull(deptMapper.selectById(deptDO1.getId()));
        assertNotNull(deptMapper.selectById(deptDO2.getId()));
    }

    @Test
    public void testDeleteDeptList_existsUserByUserDept() {
        DeptDO deptDO1 = randomPojo(DeptDO.class);
        deptMapper.insert(deptDO1);
        DeptDO deptDO2 = randomPojo(DeptDO.class);
        deptMapper.insert(deptDO2);
        UserDeptDO userDeptDO = new UserDeptDO();
        userDeptDO.setUserId(randomLongId());
        userDeptDO.setDeptId(deptDO2.getId());
        userDeptMapper.insert(userDeptDO);

        assertServiceException(() -> deptService.deleteDeptList(Arrays.asList(deptDO1.getId(), deptDO2.getId())),
                DEPT_EXISTS_USER);
        assertNotNull(deptMapper.selectById(deptDO1.getId()));
        assertNotNull(deptMapper.selectById(deptDO2.getId()));
    }

    @Test
    public void testDeleteDeptList_existsBizData() throws SQLException {
        TenantContextHolder.setTenantId(1L);
        DeptDO deptDO1 = randomPojo(DeptDO.class);
        deptMapper.insert(deptDO1);
        DeptDO deptDO2 = randomPojo(DeptDO.class);
        deptMapper.insert(deptDO2);
        insertErpPurchaseOrder(deptDO2.getId(), false, 1L);

        assertServiceException(() -> deptService.deleteDeptList(Arrays.asList(deptDO1.getId(), deptDO2.getId())),
                DEPT_EXISTS_BIZ_DATA);
        assertNotNull(deptMapper.selectById(deptDO1.getId()));
        assertNotNull(deptMapper.selectById(deptDO2.getId()));
    }

    @Test
    public void testValidateParentDept_parentError() {
        // 准备参数
        Long id = randomLongId();

        // 调用, 并断言异常
        assertServiceException(() -> deptService.validateParentDept(id, id),
                DEPT_PARENT_ERROR);
    }

    @Test
    public void testValidateParentDept_parentIsChild() {
        // mock 数据（父节点）
        DeptDO parentDept = randomPojo(DeptDO.class);
        deptMapper.insert(parentDept);
        // mock 数据（子节点）
        DeptDO childDept = randomPojo(DeptDO.class, o -> {
            o.setParentId(parentDept.getId());
        });
        deptMapper.insert(childDept);

        // 准备参数
        Long id = parentDept.getId();
        Long parentId = childDept.getId();

        // 调用, 并断言异常
        assertServiceException(() -> deptService.validateParentDept(id, parentId), DEPT_PARENT_IS_CHILD);
    }

    @Test
    public void testValidateNameUnique_duplicate() {
        // mock 数据
        DeptDO deptDO = randomPojo(DeptDO.class);
        deptMapper.insert(deptDO);

        // 准备参数
        Long id = randomLongId();
        Long parentId = deptDO.getParentId();
        String name = deptDO.getName();

        // 调用, 并断言异常
        assertServiceException(() -> deptService.validateDeptNameUnique(id, parentId, name),
                DEPT_NAME_DUPLICATE);
    }

    @Test
    public void testGetDept() {
        // mock 数据
        DeptDO deptDO = randomPojo(DeptDO.class);
        deptMapper.insert(deptDO);
        // 准备参数
        Long id = deptDO.getId();

        // 调用
        DeptDO dbDept = deptService.getDept(id);
        // 断言
        assertEquals(deptDO, dbDept);
    }

    @Test
    public void testGetDeptList_ids() {
        // mock 数据
        DeptDO deptDO01 = randomPojo(DeptDO.class);
        deptMapper.insert(deptDO01);
        DeptDO deptDO02 = randomPojo(DeptDO.class);
        deptMapper.insert(deptDO02);
        // 准备参数
        List<Long> ids = Arrays.asList(deptDO01.getId(), deptDO02.getId());

        // 调用
        List<DeptDO> deptDOList = deptService.getDeptList(ids);
        // 断言
        assertEquals(2, deptDOList.size());
        assertEquals(deptDO01, deptDOList.get(0));
        assertEquals(deptDO02, deptDOList.get(1));
    }

    @Test
    public void testGetDeptList_reqVO() {
        // mock 数据
        DeptDO dept = randomPojo(DeptDO.class, o -> { // 等会查询到
            o.setName("开发部");
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
        });
        deptMapper.insert(dept);
        // 测试 name 不匹配
        deptMapper.insert(ObjectUtils.cloneIgnoreId(dept, o -> o.setName("发")));
        // 测试 status 不匹配
        deptMapper.insert(ObjectUtils.cloneIgnoreId(dept, o -> o.setStatus(CommonStatusEnum.DISABLE.getStatus())));
        // 准备参数
        DeptListReqVO reqVO = new DeptListReqVO();
        reqVO.setName("开");
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());

        // 调用
        List<DeptDO> sysDeptDOS = deptService.getDeptList(reqVO);
        // 断言
        assertEquals(1, sysDeptDOS.size());
        assertPojoEquals(dept, sysDeptDOS.get(0));
    }

    @Test
    public void testGetDeptList_leaderUserNameLike() {
        AdminUserDO leader01 = randomPojo(AdminUserDO.class, o -> {
            o.setNickname("张三丰");
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
            o.setSex(SexEnum.UNKNOWN.getSex());
        });
        userMapper.insert(leader01);
        AdminUserDO leader02 = randomPojo(AdminUserDO.class, o -> {
            o.setNickname("张三");
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
            o.setSex(SexEnum.UNKNOWN.getSex());
        });
        userMapper.insert(leader02);
        AdminUserDO leader03 = randomPojo(AdminUserDO.class, o -> {
            o.setNickname("李四");
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
            o.setSex(SexEnum.UNKNOWN.getSex());
        });
        userMapper.insert(leader03);
        DeptDO dept01 = randomPojo(DeptDO.class, o -> {
            o.setParentId(DeptDO.PARENT_ID_ROOT);
            o.setLeaderUserId(leader01.getId());
            o.setSort(10);
        });
        deptMapper.insert(dept01);
        DeptDO dept02 = randomPojo(DeptDO.class, o -> {
            o.setParentId(DeptDO.PARENT_ID_ROOT);
            o.setLeaderUserId(leader02.getId());
            o.setSort(20);
        });
        deptMapper.insert(dept02);
        deptMapper.insert(randomPojo(DeptDO.class, o -> {
            o.setParentId(DeptDO.PARENT_ID_ROOT);
            o.setLeaderUserId(leader03.getId());
            o.setSort(30);
        }));
        DeptListReqVO reqVO = new DeptListReqVO();
        reqVO.setLeaderUserName("张三");

        List<DeptDO> deptList = deptService.getDeptList(reqVO);

        assertEquals(2, deptList.size());
        assertEquals(dept01.getId(), deptList.get(0).getId());
        assertEquals(dept02.getId(), deptList.get(1).getId());
    }

    @Test
    public void testGetDeptList_leaderUserNameNotMatch() {
        DeptDO dept = randomPojo(DeptDO.class, o -> o.setParentId(DeptDO.PARENT_ID_ROOT));
        deptMapper.insert(dept);
        DeptListReqVO reqVO = new DeptListReqVO();
        reqVO.setLeaderUserName("不存在的负责人");

        List<DeptDO> deptList = deptService.getDeptList(reqVO);

        assertTrue(deptList.isEmpty());
    }

    @Test
    public void testGetDeptList_sortByNameWhenSortSame() {
        DeptDO deptDO1 = randomPojo(DeptDO.class, o -> {
            o.setParentId(DeptDO.PARENT_ID_ROOT);
            o.setName("深圳分公司");
            o.setSort(0);
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
        });
        deptMapper.insert(deptDO1);
        DeptDO deptDO2 = randomPojo(DeptDO.class, o -> {
            o.setParentId(DeptDO.PARENT_ID_ROOT);
            o.setName("成都分公司");
            o.setSort(0);
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
        });
        deptMapper.insert(deptDO2);

        List<DeptDO> deptList = deptService.getDeptList(new DeptListReqVO());

        assertEquals(deptDO2.getId(), deptList.get(0).getId());
        assertEquals(deptDO1.getId(), deptList.get(1).getId());
    }

    @Test
    public void testGetDeptList_sortBeforeName() {
        DeptDO deptDO1 = randomPojo(DeptDO.class, o -> {
            o.setParentId(DeptDO.PARENT_ID_ROOT);
            o.setName("深圳分公司");
            o.setSort(10);
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
        });
        deptMapper.insert(deptDO1);
        DeptDO deptDO2 = randomPojo(DeptDO.class, o -> {
            o.setParentId(DeptDO.PARENT_ID_ROOT);
            o.setName("成都分公司");
            o.setSort(20);
            o.setStatus(CommonStatusEnum.ENABLE.getStatus());
        });
        deptMapper.insert(deptDO2);

        List<DeptDO> deptList = deptService.getDeptList(new DeptListReqVO());

        assertEquals(deptDO1.getId(), deptList.get(0).getId());
        assertEquals(deptDO2.getId(), deptList.get(1).getId());
    }

    @Test
    public void testGetChildDeptList() {
        // mock 数据（1 级别子节点）
        DeptDO dept1 = randomPojo(DeptDO.class, o -> o.setName("1"));
        deptMapper.insert(dept1);
        DeptDO dept2 = randomPojo(DeptDO.class, o -> o.setName("2"));
        deptMapper.insert(dept2);
        // mock 数据（2 级子节点）
        DeptDO dept1a = randomPojo(DeptDO.class, o -> o.setName("1-a").setParentId(dept1.getId()));
        deptMapper.insert(dept1a);
        DeptDO dept2a = randomPojo(DeptDO.class, o -> o.setName("2-a").setParentId(dept2.getId()));
        deptMapper.insert(dept2a);
        // 准备参数
        Long id = dept1.getParentId();

        // 调用
        List<DeptDO> result = deptService.getChildDeptList(id);
        // 断言
        assertEquals(result.size(), 2);
        assertPojoEquals(dept1, result.get(0));
        assertPojoEquals(dept1a, result.get(1));
    }

    @Test
    public void testGetChildDeptListFromCache() {
        // mock 数据（1 级别子节点）
        DeptDO dept1 = randomPojo(DeptDO.class, o -> o.setName("1"));
        deptMapper.insert(dept1);
        DeptDO dept2 = randomPojo(DeptDO.class, o -> o.setName("2"));
        deptMapper.insert(dept2);
        // mock 数据（2 级子节点）
        DeptDO dept1a = randomPojo(DeptDO.class, o -> o.setName("1-a").setParentId(dept1.getId()));
        deptMapper.insert(dept1a);
        DeptDO dept2a = randomPojo(DeptDO.class, o -> o.setName("2-a").setParentId(dept2.getId()));
        deptMapper.insert(dept2a);
        // 准备参数
        Long id = dept1.getParentId();

        // 调用
        Set<Long> result = deptService.getChildDeptIdListFromCache(id);
        // 断言
        assertEquals(result.size(), 2);
        assertTrue(result.contains(dept1.getId()));
        assertTrue(result.contains(dept1a.getId()));
    }

    @Test
    public void testValidateDeptList_success() {
        // mock 数据
        DeptDO deptDO = randomPojo(DeptDO.class).setStatus(CommonStatusEnum.ENABLE.getStatus());
        deptMapper.insert(deptDO);
        // 准备参数
        List<Long> ids = singletonList(deptDO.getId());

        // 调用，无需断言
        deptService.validateDeptList(ids);
    }

    @Test
    public void testValidateDeptList_notFound() {
        // 准备参数
        List<Long> ids = singletonList(randomLongId());

        // 调用, 并断言异常
        assertServiceException(() -> deptService.validateDeptList(ids), DEPT_NOT_FOUND);
    }

    @Test
    public void testValidateDeptList_notEnable() {
        // mock 数据
        DeptDO deptDO = randomPojo(DeptDO.class).setStatus(CommonStatusEnum.DISABLE.getStatus());
        deptMapper.insert(deptDO);
        // 准备参数
        List<Long> ids = singletonList(deptDO.getId());

        // 调用, 并断言异常
        assertServiceException(() -> deptService.validateDeptList(ids), DEPT_NOT_ENABLE, deptDO.getName());
    }

    private void insertErpPurchaseOrder(Long deptId, boolean deleted, Long tenantId) throws SQLException {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(
                     "INSERT INTO erp_purchase_order (dept_id, deleted, tenant_id) VALUES (?, ?, ?)")) {
            statement.setLong(1, deptId);
            statement.setBoolean(2, deleted);
            statement.setLong(3, tenantId);
            statement.executeUpdate();
        }
    }

}
