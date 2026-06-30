package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehousePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpUserWarehousePermissionDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpUserWarehousePermissionMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseBranchMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseMapper;
import cn.iocoder.yudao.module.erp.service.base.ErpBaseArchiveReferenceService;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.enums.permission.RoleCodeEnum;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.WAREHOUSE_NOT_ENABLE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.WAREHOUSE_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ErpWarehouseServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpWarehouseServiceImpl warehouseService;

    @Mock
    private ErpWarehouseMapper warehouseMapper;
    @Mock
    private ErpStockMapper stockMapper;
    @Mock
    private ErpWarehouseBranchMapper warehouseBranchMapper;
    @Mock
    private ErpUserWarehousePermissionMapper userWarehousePermissionMapper;
    @Mock
    private ErpStockFieldPermissionMasker fieldPermissionMasker;
    @Mock
    private ErpOperateLogService operateLogService;
    @Mock
    private ErpBaseArchiveReferenceService baseArchiveReferenceService;
    @Mock
    private DeptApi deptApi;
    @Mock
    private PermissionApi permissionApi;

    @BeforeEach
    public void setUpTenant() {
        TenantContextHolder.setTenantId(1L);
    }

    @AfterEach
    public void tearDown() {
        TenantContextHolder.clear();
    }

    @Test
    public void testCreateWarehouse_nullSort_defaultsToZero() {
        ErpWarehouseSaveReqVO reqVO = new ErpWarehouseSaveReqVO();
        reqVO.setName("A");
        reqVO.setStatus(CommonStatusEnum.ENABLE.getStatus());
        when(warehouseMapper.insert(any(ErpWarehouseDO.class))).thenAnswer(invocation -> {
            ErpWarehouseDO warehouse = invocation.getArgument(0);
            warehouse.setId(100L);
            return 1;
        });

        Long id = warehouseService.createWarehouse(reqVO);

        assertEquals(100L, id);
        ArgumentCaptor<ErpWarehouseDO> captor = ArgumentCaptor.forClass(ErpWarehouseDO.class);
        verify(warehouseMapper).insert(captor.capture());
        assertEquals(0L, captor.getValue().getSort());
    }

    @Test
    public void testGetWarehouseList_emptyIds_returnsEmptyList() {
        List<ErpWarehouseDO> result = warehouseService.getWarehouseList(Collections.emptyList());

        assertTrue(result.isEmpty());
        verify(warehouseMapper, never()).selectByIds(any());
    }

    @Test
    public void testGetWarehouseList_success() {
        List<Long> ids = Arrays.asList(1L, 2L);
        List<ErpWarehouseDO> warehouses = Arrays.asList(
                new ErpWarehouseDO().setId(1L), new ErpWarehouseDO().setId(2L));
        when(warehouseMapper.selectByIds(eq(ids))).thenReturn(warehouses);

        List<ErpWarehouseDO> result = warehouseService.getWarehouseList(ids);

        assertSame(warehouses, result);
    }

    @Test
    public void testGetUserWarehouseIds_success() {
        TenantContextHolder.setTenantId(7L);
        List<ErpUserWarehousePermissionDO> permissions = Arrays.asList(
                ErpUserWarehousePermissionDO.builder().userId(104L).warehouseId(11L).build(),
                ErpUserWarehousePermissionDO.builder().userId(104L).warehouseId(12L).build());
        when(userWarehousePermissionMapper.selectListByUserId(eq(104L))).thenReturn(permissions);

        List<Long> result = warehouseService.getUserWarehouseIds(104L);

        assertEquals(Arrays.asList(11L, 12L), result);
    }

    @Test
    public void testUpdateUserWarehousePermissions_savesCurrentTenantAndDeduplicates() {
        TenantContextHolder.setTenantId(7L);
        List<ErpWarehouseDO> warehouses = Arrays.asList(
                new ErpWarehouseDO().setId(11L).setStatus(CommonStatusEnum.ENABLE.getStatus()),
                new ErpWarehouseDO().setId(12L).setStatus(CommonStatusEnum.ENABLE.getStatus()));
        when(warehouseMapper.selectByIds(any())).thenReturn(warehouses);

        warehouseService.updateUserWarehousePermissions(104L, Arrays.asList(11L, 12L, 11L, null));

        verify(userWarehousePermissionMapper).deleteListByUserId(eq(104L), eq(7L));
        verify(userWarehousePermissionMapper).insertIgnore(eq(104L), eq(11L), eq(7L));
        verify(userWarehousePermissionMapper).insertIgnore(eq(104L), eq(12L), eq(7L));
    }

    @Test
    public void testGetWarehouseUserIds_success() {
        when(warehouseMapper.selectById(eq(11L))).thenReturn(new ErpWarehouseDO().setId(11L));
        List<ErpUserWarehousePermissionDO> permissions = Arrays.asList(
                ErpUserWarehousePermissionDO.builder().userId(104L).warehouseId(11L).build(),
                ErpUserWarehousePermissionDO.builder().userId(105L).warehouseId(11L).build());
        when(userWarehousePermissionMapper.selectListByWarehouseId(eq(11L))).thenReturn(permissions);

        List<Long> result = warehouseService.getWarehouseUserIds(11L);

        assertEquals(Arrays.asList(104L, 105L), result);
    }

    @Test
    public void testUpdateWarehouseUserPermissions_savesCurrentTenantAndDeduplicates() {
        TenantContextHolder.setTenantId(7L);
        when(warehouseMapper.selectById(eq(11L))).thenReturn(new ErpWarehouseDO().setId(11L));

        warehouseService.updateWarehouseUserPermissions(11L, Arrays.asList(104L, 105L, 104L, null));

        verify(userWarehousePermissionMapper).deleteListByWarehouseId(eq(11L), eq(7L));
        verify(userWarehousePermissionMapper).insertIgnore(eq(104L), eq(11L), eq(7L));
        verify(userWarehousePermissionMapper).insertIgnore(eq(105L), eq(11L), eq(7L));
    }

    @Test
    public void testUpdateWarehouseUserPermissions_emptyUsers_clearsCurrentTenant() {
        TenantContextHolder.setTenantId(7L);
        when(warehouseMapper.selectById(eq(11L))).thenReturn(new ErpWarehouseDO().setId(11L));

        warehouseService.updateWarehouseUserPermissions(11L, Collections.emptyList());

        verify(userWarehousePermissionMapper).deleteListByWarehouseId(eq(11L), eq(7L));
        verify(userWarehousePermissionMapper, never()).insertIgnore(any(), any(), any());
    }

    @Test
    public void testGetCurrentUserAuthorizedWarehouseList_normalUserUsesPermissionIds() {
        TenantContextHolder.setTenantId(7L);
        List<ErpUserWarehousePermissionDO> permissions = Arrays.asList(
                ErpUserWarehousePermissionDO.builder().userId(104L).warehouseId(11L).build(),
                ErpUserWarehousePermissionDO.builder().userId(104L).warehouseId(12L).build());
        List<ErpWarehouseDO> deptWarehouses = Collections.singletonList(
                new ErpWarehouseDO().setId(10L).setStatus(CommonStatusEnum.ENABLE.getStatus()));
        List<ErpWarehouseDO> warehouses = Arrays.asList(
                new ErpWarehouseDO().setId(10L).setStatus(CommonStatusEnum.ENABLE.getStatus()),
                new ErpWarehouseDO().setId(11L).setStatus(CommonStatusEnum.ENABLE.getStatus()),
                new ErpWarehouseDO().setId(12L).setStatus(CommonStatusEnum.ENABLE.getStatus()));
        when(permissionApi.hasAnyRoles(eq(104L), eq(RoleCodeEnum.SUPER_ADMIN.getCode()))).thenReturn(false);
        when(warehouseMapper.selectListByStatusIfPresent(eq(CommonStatusEnum.ENABLE.getStatus())))
                .thenReturn(deptWarehouses);
        when(userWarehousePermissionMapper.selectListByUserId(eq(104L))).thenReturn(permissions);
        when(warehouseMapper.selectListByStatusAndIds(eq(CommonStatusEnum.ENABLE.getStatus()), any()))
                .thenReturn(warehouses);

        try (MockedStatic<SecurityFrameworkUtils> mock = mockStatic(SecurityFrameworkUtils.class)) {
            mock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(104L);

            List<ErpWarehouseDO> result = warehouseService.getCurrentUserAuthorizedWarehouseList();

            assertSame(warehouses, result);
            ArgumentCaptor<Collection<Long>> captor = ArgumentCaptor.forClass(Collection.class);
            verify(warehouseMapper).selectListByStatusAndIds(eq(CommonStatusEnum.ENABLE.getStatus()), captor.capture());
            assertEquals(Arrays.asList(10L, 11L, 12L), captor.getValue().stream().collect(java.util.stream.Collectors.toList()));
        }
    }

    @Test
    public void testValidWarehouseList_emptyIds_returnsEmptyList() {
        List<ErpWarehouseDO> result = warehouseService.validWarehouseList(Collections.emptyList());

        assertTrue(result.isEmpty());
        verify(warehouseMapper, never()).selectByIds(any());
    }

    @Test
    public void testValidWarehouseList_missingWarehouse_throwException() {
        when(warehouseMapper.selectByIds(eq(Collections.singletonList(1L))))
                .thenReturn(Collections.emptyList());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> warehouseService.validWarehouseList(Collections.singletonList(1L)));

        assertEquals(WAREHOUSE_NOT_EXISTS.getCode(), ex.getCode());
    }

    @Test
    public void testValidWarehouseList_disabledWarehouse_throwException() {
        ErpWarehouseDO warehouse = new ErpWarehouseDO().setId(1L).setName("A仓")
                .setStatus(CommonStatusEnum.DISABLE.getStatus());
        when(warehouseMapper.selectByIds(eq(Collections.singletonList(1L))))
                .thenReturn(Collections.singletonList(warehouse));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> warehouseService.validWarehouseList(Collections.singletonList(1L)));

        assertEquals(WAREHOUSE_NOT_ENABLE.getCode(), ex.getCode());
    }

    @Test
    public void testGetWarehouse() {
        ErpWarehouseDO warehouse = new ErpWarehouseDO().setId(10L);
        when(warehouseMapper.selectById(eq(10L))).thenReturn(warehouse);

        assertSame(warehouse, warehouseService.getWarehouse(10L));
    }

    @Test
    public void testGetCurrentUserVisibleWarehouse_extraPermissionCanViewDetail() {
        ErpWarehouseDO warehouse = new ErpWarehouseDO().setId(11L);
        when(permissionApi.hasAnyRoles(eq(104L), eq(RoleCodeEnum.SUPER_ADMIN.getCode()))).thenReturn(false);
        when(warehouseMapper.selectListByStatusIfPresent(eq(null)))
                .thenReturn(Collections.singletonList(new ErpWarehouseDO().setId(10L)));
        when(userWarehousePermissionMapper.selectListByUserId(eq(104L))).thenReturn(Collections.singletonList(
                ErpUserWarehousePermissionDO.builder().userId(104L).warehouseId(11L).build()));
        when(warehouseMapper.selectById(eq(11L))).thenReturn(warehouse);

        try (MockedStatic<SecurityFrameworkUtils> mock = mockStatic(SecurityFrameworkUtils.class)) {
            mock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(104L);

            assertSame(warehouse, warehouseService.getCurrentUserVisibleWarehouse(11L));
        }
    }

    @Test
    public void testGetCurrentUserVisibleWarehouse_noPermissionReturnsNull() {
        when(permissionApi.hasAnyRoles(eq(104L), eq(RoleCodeEnum.SUPER_ADMIN.getCode()))).thenReturn(false);
        when(warehouseMapper.selectListByStatusIfPresent(eq(null)))
                .thenReturn(Collections.singletonList(new ErpWarehouseDO().setId(10L)));
        when(userWarehousePermissionMapper.selectListByUserId(eq(104L))).thenReturn(Collections.singletonList(
                ErpUserWarehousePermissionDO.builder().userId(104L).warehouseId(11L).build()));

        try (MockedStatic<SecurityFrameworkUtils> mock = mockStatic(SecurityFrameworkUtils.class)) {
            mock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(104L);

            assertNull(warehouseService.getCurrentUserVisibleWarehouse(12L));
            verify(warehouseMapper, never()).selectById(eq(12L));
        }
    }

    @Test
    public void testDeleteWarehouseList_reuseSingleDeleteLogic() {
        ErpWarehouseDO warehouse1 = new ErpWarehouseDO().setId(1L).setName("A仓");
        ErpWarehouseDO warehouse2 = new ErpWarehouseDO().setId(2L).setName("B仓");
        when(warehouseMapper.selectById(eq(1L))).thenReturn(warehouse1);
        when(warehouseMapper.selectById(eq(2L))).thenReturn(warehouse2);

        warehouseService.deleteWarehouseList(Arrays.asList(1L, 2L));

        verify(baseArchiveReferenceService).validateWarehouseNotReferenced(eq(1L));
        verify(baseArchiveReferenceService).validateWarehouseNotReferenced(eq(2L));
        verify(warehouseMapper).deleteById(eq(1L));
        verify(warehouseMapper).deleteById(eq(2L));
        verify(warehouseBranchMapper).deleteByWarehouseId(eq(1L));
        verify(warehouseBranchMapper).deleteByWarehouseId(eq(2L));
    }

    @Test
    public void testBatchUpdateWarehouse_updateSelectedFieldsAndSyncDept() {
        ErpWarehouseDO warehouse = new ErpWarehouseDO().setId(1L).setName("A仓").setDeptId(10L);
        when(warehouseMapper.selectById(eq(1L))).thenReturn(warehouse);
        ErpWarehouseBatchUpdateReqVO reqVO = new ErpWarehouseBatchUpdateReqVO();
        reqVO.setIds(Collections.singletonList(1L));
        reqVO.setDeptId(20L);
        reqVO.setStatus(CommonStatusEnum.DISABLE.getStatus());
        reqVO.setSaleEnabled(false);

        warehouseService.batchUpdateWarehouse(reqVO);

        ArgumentCaptor<ErpWarehouseDO> captor = ArgumentCaptor.forClass(ErpWarehouseDO.class);
        verify(warehouseMapper).updateById(captor.capture());
        assertEquals(1L, captor.getValue().getId());
        assertEquals(20L, captor.getValue().getDeptId());
        assertEquals(CommonStatusEnum.DISABLE.getStatus(), captor.getValue().getStatus());
        assertEquals(false, captor.getValue().getSaleEnabled());
        assertEquals(null, captor.getValue().getPurchaseEnabled());
        verify(stockMapper).updateDeptIdByWarehouseId(eq(1L), eq(20L));
    }

    @Test
    public void testBatchUpdateWarehouse_emptyUpdate_skipMapper() {
        ErpWarehouseBatchUpdateReqVO reqVO = new ErpWarehouseBatchUpdateReqVO();
        reqVO.setIds(Collections.singletonList(1L));

        warehouseService.batchUpdateWarehouse(reqVO);

        verify(warehouseMapper, never()).selectById(any());
        verify(warehouseMapper, never()).updateById(any(ErpWarehouseDO.class));
    }

    @Test
    public void testImportWarehouseList_createUpdateAndFailure() {
        ErpWarehouseImportExcelVO createRow = new ErpWarehouseImportExcelVO();
        createRow.setName("新增仓");
        createRow.setWarehouseCode("WH001");
        ErpWarehouseImportExcelVO updateRow = new ErpWarehouseImportExcelVO();
        updateRow.setName("更新仓");
        updateRow.setWarehouseCode("WH002");
        updateRow.setDeptName("销售部");
        ErpWarehouseImportExcelVO failureRow = new ErpWarehouseImportExcelVO();
        failureRow.setWarehouseCode("WH003");
        ErpWarehouseDO existing = new ErpWarehouseDO().setId(2L).setName("旧仓").setWarehouseCode("WH002").setDeptId(10L);
        when(warehouseMapper.selectByWarehouseCode(anyString())).thenAnswer(invocation ->
                "WH002".equals(invocation.getArgument(0)) ? existing : null);
        when(deptApi.getDeptListByName(eq("销售部"))).thenReturn(Collections.singletonList(dept(20L, "销售部")));

        ErpWarehouseImportRespVO result = warehouseService.importWarehouseList(
                Arrays.asList(createRow, updateRow, failureRow));

        assertEquals(2, result.getSuccessCount(), result.getFailureDetails().toString());
        assertEquals(1, result.getCreateCount());
        assertEquals(1, result.getUpdateCount());
        assertEquals(1, result.getFailureCount());
        assertEquals(1, result.getFailureDetails().size());
        ArgumentCaptor<ErpWarehouseDO> insertCaptor = ArgumentCaptor.forClass(ErpWarehouseDO.class);
        verify(warehouseMapper).insert(insertCaptor.capture());
        assertEquals(CommonStatusEnum.ENABLE.getStatus(), insertCaptor.getValue().getStatus());
        assertEquals(0L, insertCaptor.getValue().getSort());
        ArgumentCaptor<ErpWarehouseDO> updateCaptor = ArgumentCaptor.forClass(ErpWarehouseDO.class);
        verify(warehouseMapper).updateById(updateCaptor.capture());
        assertEquals(20L, updateCaptor.getValue().getDeptId());
        assertEquals(null, updateCaptor.getValue().getStatus());
        assertEquals(null, updateCaptor.getValue().getSort());
        verify(stockMapper).updateDeptIdByWarehouseId(eq(2L), eq(20L));
    }

    @Test
    public void testImportWarehouseList_createWithDeptName() {
        ErpWarehouseImportExcelVO row = new ErpWarehouseImportExcelVO();
        row.setName("新增仓");
        row.setWarehouseCode("WH001");
        row.setDeptName(" 仓储部 ");
        when(deptApi.getDeptListByName(eq("仓储部"))).thenReturn(Collections.singletonList(dept(10L, "仓储部")));

        ErpWarehouseImportRespVO result = warehouseService.importWarehouseList(Collections.singletonList(row));

        assertEquals(1, result.getSuccessCount(), result.getFailureDetails().toString());
        assertEquals(1, result.getCreateCount());
        assertEquals(0, result.getFailureCount());
        ArgumentCaptor<ErpWarehouseDO> insertCaptor = ArgumentCaptor.forClass(ErpWarehouseDO.class);
        verify(warehouseMapper).insert(insertCaptor.capture());
        assertEquals(10L, insertCaptor.getValue().getDeptId());
        assertEquals(CommonStatusEnum.ENABLE.getStatus(), insertCaptor.getValue().getStatus());
        assertEquals(0L, insertCaptor.getValue().getSort());
    }

    @Test
    public void testImportWarehouseList_deptNameNotExists_failure() {
        ErpWarehouseImportExcelVO row = new ErpWarehouseImportExcelVO();
        row.setName("新增仓");
        row.setWarehouseCode("WH001");
        row.setDeptName("不存在部门");
        when(deptApi.getDeptListByName(eq("不存在部门"))).thenReturn(Collections.emptyList());

        ErpWarehouseImportRespVO result = warehouseService.importWarehouseList(Collections.singletonList(row));

        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertEquals(1, result.getFailureDetails().size());
        assertTrue(result.getFailureDetails().get(0).getReason().contains("所属部门不存在：不存在部门"));
        verify(warehouseMapper, never()).insert(any(ErpWarehouseDO.class));
        verify(warehouseMapper, never()).updateById(any(ErpWarehouseDO.class));
    }

    @Test
    public void testImportWarehouseList_duplicateDeptName_failure() {
        ErpWarehouseImportExcelVO row = new ErpWarehouseImportExcelVO();
        row.setName("新增仓");
        row.setWarehouseCode("WH001");
        row.setDeptName("销售部");
        when(deptApi.getDeptListByName(eq("销售部")))
                .thenReturn(Arrays.asList(dept(20L, "销售部"), dept(21L, "销售部")));

        ErpWarehouseImportRespVO result = warehouseService.importWarehouseList(Collections.singletonList(row));

        assertEquals(0, result.getSuccessCount());
        assertEquals(1, result.getFailureCount());
        assertEquals(1, result.getFailureDetails().size());
        assertTrue(result.getFailureDetails().get(0).getReason().contains("所属部门名称重复，请使用唯一部门名称：销售部"));
        verify(warehouseMapper, never()).insert(any(ErpWarehouseDO.class));
        verify(warehouseMapper, never()).updateById(any(ErpWarehouseDO.class));
    }

    @Test
    public void testImportWarehouseList_blankDeptName_keepExistingBehavior() {
        ErpWarehouseImportExcelVO row = new ErpWarehouseImportExcelVO();
        row.setName("更新仓");
        row.setWarehouseCode("WH002");
        row.setDeptName(" ");
        ErpWarehouseDO existing = new ErpWarehouseDO().setId(2L).setName("旧仓").setWarehouseCode("WH002").setDeptId(10L);
        when(warehouseMapper.selectByWarehouseCode(eq("WH002"))).thenReturn(existing);

        ErpWarehouseImportRespVO result = warehouseService.importWarehouseList(Collections.singletonList(row));

        assertEquals(1, result.getSuccessCount(), result.getFailureDetails().toString());
        assertEquals(1, result.getUpdateCount());
        verify(deptApi, never()).getDeptListByName(anyString());
        verify(stockMapper, never()).updateDeptIdByWarehouseId(any(), any());
    }

    @Test
    public void testGetWarehouseListByStatus() {
        Integer status = CommonStatusEnum.ENABLE.getStatus();
        List<ErpWarehouseDO> warehouses = Arrays.asList(
                new ErpWarehouseDO().setId(1L).setStatus(status),
                new ErpWarehouseDO().setId(2L).setStatus(status));
        when(warehouseMapper.selectListByStatus(eq(status))).thenReturn(warehouses);

        List<ErpWarehouseDO> result = warehouseService.getWarehouseListByStatus(status);

        assertSame(warehouses, result);
    }

    @Test
    public void testGetWarehousePage() {
        ErpWarehousePageReqVO reqVO = new ErpWarehousePageReqVO();
        when(permissionApi.hasAnyRoles(eq(104L), eq(RoleCodeEnum.SUPER_ADMIN.getCode()))).thenReturn(false);
        when(warehouseMapper.selectListByStatusIfPresent(eq(null)))
                .thenReturn(Collections.singletonList(new ErpWarehouseDO().setId(10L)));
        when(userWarehousePermissionMapper.selectListByUserId(eq(104L))).thenReturn(Collections.singletonList(
                ErpUserWarehousePermissionDO.builder().userId(104L).warehouseId(11L).build()));
        PageResult<ErpWarehouseDO> pageResult = new PageResult<>(Collections.singletonList(new ErpWarehouseDO().setId(11L)), 1L);
        when(warehouseMapper.selectPageByIds(eq(reqVO), any())).thenReturn(pageResult);

        try (MockedStatic<SecurityFrameworkUtils> mock = mockStatic(SecurityFrameworkUtils.class)) {
            mock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(104L);

            assertSame(pageResult, warehouseService.getWarehousePage(reqVO));
            ArgumentCaptor<Collection<Long>> captor = ArgumentCaptor.forClass(Collection.class);
            verify(warehouseMapper).selectPageByIds(eq(reqVO), captor.capture());
            assertEquals(Arrays.asList(10L, 11L), captor.getValue().stream().collect(java.util.stream.Collectors.toList()));
        }
    }

    private DeptRespDTO dept(Long id, String name) {
        DeptRespDTO dept = new DeptRespDTO();
        dept.setId(id);
        dept.setName(name);
        dept.setStatus(CommonStatusEnum.ENABLE.getStatus());
        return dept;
    }

}
