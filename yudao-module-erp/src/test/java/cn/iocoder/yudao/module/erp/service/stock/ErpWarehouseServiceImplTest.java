package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.datapermission.core.aop.DataPermissionContextHolder;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehousePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpUserWarehousePermissionDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseSaleDeptPermissionDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpUserWarehousePermissionMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseBranchMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseSaleDeptPermissionMapper;
import cn.iocoder.yudao.module.erp.dal.redis.no.ErpNoRedisDAO;
import cn.iocoder.yudao.module.erp.service.base.ErpBaseArchiveReferenceService;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.erp.service.stock.bo.ErpProductStockPermissionScope;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.enums.permission.RoleCodeEnum;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.WAREHOUSE_CODE_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.WAREHOUSE_DIRECT_MULTIPLE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.WAREHOUSE_DIRECT_NOT_CONFIGURED;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.WAREHOUSE_NOT_ENABLE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.WAREHOUSE_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.WAREHOUSE_SALE_DEPT_PERMISSION_DENIED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
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
    private ErpWarehouseSaleDeptPermissionMapper warehouseSaleDeptPermissionMapper;
    @Mock
    private ErpStockFieldPermissionMasker fieldPermissionMasker;
    @Mock
    private ErpOperateLogService operateLogService;
    @Mock
    private ErpBaseArchiveReferenceService baseArchiveReferenceService;
    @Mock
    private ErpNoRedisDAO noRedisDAO;
    @Mock
    private DeptApi deptApi;
    @Mock
    private PermissionApi permissionApi;
    @Mock
    private RedissonClient redissonClient;
    @Mock
    private RLock directWarehouseCreateLock;

    @BeforeEach
    public void setUpTenant() {
        TenantContextHolder.setTenantId(1L);
        lenient().when(redissonClient.getLock(anyString())).thenReturn(directWarehouseCreateLock);
        lenient().when(directWarehouseCreateLock.isHeldByCurrentThread()).thenReturn(true);
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
        when(noRedisDAO.generatePlain(eq(ErpNoRedisDAO.WAREHOUSE_CODE_PREFIX))).thenReturn("WH000001");
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
        assertEquals("WH000001", captor.getValue().getWarehouseCode());
    }

    @Test
    public void testCreateWarehouse_blankCode_autoGenerate() {
        ErpWarehouseSaveReqVO reqVO = new ErpWarehouseSaveReqVO();
        reqVO.setName("A");
        when(noRedisDAO.generatePlain(eq(ErpNoRedisDAO.WAREHOUSE_CODE_PREFIX))).thenReturn("WH000001");
        when(warehouseMapper.insert(any(ErpWarehouseDO.class))).thenAnswer(invocation -> {
            ErpWarehouseDO warehouse = invocation.getArgument(0);
            warehouse.setId(100L);
            return 1;
        });

        Long id = warehouseService.createWarehouse(reqVO);

        assertEquals(100L, id);
        ArgumentCaptor<ErpWarehouseDO> captor = ArgumentCaptor.forClass(ErpWarehouseDO.class);
        verify(warehouseMapper).insert(captor.capture());
        assertEquals("WH000001", captor.getValue().getWarehouseCode());
    }

    @Test
    public void testCreateWarehouse_recordWarehouseCreateLog() {
        ErpWarehouseSaveReqVO reqVO = new ErpWarehouseSaveReqVO();
        reqVO.setName("新增仓");
        reqVO.setWarehouseCode("WH001");
        when(warehouseMapper.insert(any(ErpWarehouseDO.class))).thenAnswer(invocation -> {
            ErpWarehouseDO warehouse = invocation.getArgument(0);
            warehouse.setId(100L);
            return 1;
        });

        warehouseService.createWarehouse(reqVO);

        ArgumentCaptor<String> actionCaptor = ArgumentCaptor.forClass(String.class);
        verify(operateLogService).record(eq("仓库信息"), eq("新增"), eq(100L),
                actionCaptor.capture(), eq("WH001"));
        assertEquals("数据库编号：100\n名称：新增仓\n编码：WH001", actionCaptor.getValue());
    }

    @Test
    public void testCreateWarehouse_manualCode_trimAndSave() {
        ErpWarehouseSaveReqVO reqVO = new ErpWarehouseSaveReqVO();
        reqVO.setName("A");
        reqVO.setWarehouseCode(" WH001 ");
        when(warehouseMapper.insert(any(ErpWarehouseDO.class))).thenAnswer(invocation -> {
            ErpWarehouseDO warehouse = invocation.getArgument(0);
            warehouse.setId(100L);
            return 1;
        });

        Long id = warehouseService.createWarehouse(reqVO);

        assertEquals(100L, id);
        ArgumentCaptor<ErpWarehouseDO> captor = ArgumentCaptor.forClass(ErpWarehouseDO.class);
        verify(warehouseMapper).insert(captor.capture());
        assertEquals("WH001", captor.getValue().getWarehouseCode());
        verify(noRedisDAO, never()).generatePlain(anyString());
    }

    @Test
    public void testCreateWarehouse_locationFields_success() {
        ErpWarehouseSaveReqVO reqVO = new ErpWarehouseSaveReqVO();
        reqVO.setName("A");
        reqVO.setWarehouseCode("WH001");
        reqVO.setAddress("成都市高新区");
        reqVO.setMapName("兴宇路通仓库");
        reqVO.setLongitude(new BigDecimal("104.066801"));
        reqVO.setLatitude(new BigDecimal("30.572269"));
        when(warehouseMapper.insert(any(ErpWarehouseDO.class))).thenAnswer(invocation -> {
            ErpWarehouseDO warehouse = invocation.getArgument(0);
            warehouse.setId(100L);
            return 1;
        });

        Long id = warehouseService.createWarehouse(reqVO);

        assertEquals(100L, id);
        ArgumentCaptor<ErpWarehouseDO> captor = ArgumentCaptor.forClass(ErpWarehouseDO.class);
        verify(warehouseMapper).insert(captor.capture());
        assertEquals("成都市高新区", captor.getValue().getAddress());
        assertEquals("兴宇路通仓库", captor.getValue().getMapName());
        assertEquals(0, new BigDecimal("104.066801").compareTo(captor.getValue().getLongitude()));
        assertEquals(0, new BigDecimal("30.572269").compareTo(captor.getValue().getLatitude()));
    }

    @Test
    public void testCreateWarehouse_nullLocationFields_success() {
        ErpWarehouseSaveReqVO reqVO = new ErpWarehouseSaveReqVO();
        reqVO.setName("A");
        reqVO.setWarehouseCode("WH001");
        when(warehouseMapper.insert(any(ErpWarehouseDO.class))).thenAnswer(invocation -> {
            ErpWarehouseDO warehouse = invocation.getArgument(0);
            warehouse.setId(100L);
            return 1;
        });

        Long id = warehouseService.createWarehouse(reqVO);

        assertEquals(100L, id);
        ArgumentCaptor<ErpWarehouseDO> captor = ArgumentCaptor.forClass(ErpWarehouseDO.class);
        verify(warehouseMapper).insert(captor.capture());
        assertNull(captor.getValue().getLongitude());
        assertNull(captor.getValue().getLatitude());
        assertNull(captor.getValue().getMapName());
    }

    @Test
    public void testCreateWarehouse_invalidLongitude_throwException() {
        ErpWarehouseSaveReqVO reqVO = new ErpWarehouseSaveReqVO();
        reqVO.setName("A");
        reqVO.setWarehouseCode("WH001");
        reqVO.setLongitude(new BigDecimal("181"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> warehouseService.createWarehouse(reqVO));

        assertEquals("仓库经度必须在 -180 到 180 之间", ex.getMessage());
        verify(warehouseMapper, never()).insert(any(ErpWarehouseDO.class));
    }

    @Test
    public void testCreateWarehouse_invalidLatitude_throwException() {
        ErpWarehouseSaveReqVO reqVO = new ErpWarehouseSaveReqVO();
        reqVO.setName("A");
        reqVO.setWarehouseCode("WH001");
        reqVO.setLatitude(new BigDecimal("-91"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> warehouseService.createWarehouse(reqVO));

        assertEquals("仓库纬度必须在 -90 到 90 之间", ex.getMessage());
        verify(warehouseMapper, never()).insert(any(ErpWarehouseDO.class));
    }

    @Test
    public void testCreateWarehouse_duplicateManualCode_throwException() {
        ErpWarehouseSaveReqVO reqVO = new ErpWarehouseSaveReqVO();
        reqVO.setName("A");
        reqVO.setWarehouseCode("WH001");
        when(warehouseMapper.selectByWarehouseCode(eq("WH001")))
                .thenReturn(new ErpWarehouseDO().setId(200L).setWarehouseCode("WH001"));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> warehouseService.createWarehouse(reqVO));

        assertEquals(WAREHOUSE_CODE_EXISTS.getCode(), ex.getCode());
        verify(warehouseMapper, never()).insert(any(ErpWarehouseDO.class));
    }

    @Test
    public void testUpdateWarehouse_keepExistingCode() {
        ErpWarehouseDO warehouse = new ErpWarehouseDO().setId(1L).setName("A仓").setWarehouseCode("WH001")
                .setDeptId(10L).setSaleEnabled(true);
        when(warehouseMapper.selectById(eq(1L))).thenReturn(warehouse);
        ErpWarehouseSaveReqVO reqVO = new ErpWarehouseSaveReqVO();
        reqVO.setId(1L);
        reqVO.setName("A仓新");
        reqVO.setDeptId(10L);
        reqVO.setWarehouseCode("WH999");

        warehouseService.updateWarehouse(reqVO);

        ArgumentCaptor<ErpWarehouseDO> captor = ArgumentCaptor.forClass(ErpWarehouseDO.class);
        verify(warehouseMapper).updateById(captor.capture());
        assertEquals("WH001", captor.getValue().getWarehouseCode());
    }

    @Test
    public void testUpdateWarehouse_locationFields_success() {
        ErpWarehouseDO warehouse = new ErpWarehouseDO().setId(1L).setName("A仓").setWarehouseCode("WH001")
                .setDeptId(10L).setAddress("旧地址").setMapName("旧地图")
                .setLongitude(new BigDecimal("100.000000")).setLatitude(new BigDecimal("20.000000"))
                .setSaleEnabled(true);
        when(warehouseMapper.selectById(eq(1L))).thenReturn(warehouse);
        ErpWarehouseSaveReqVO reqVO = new ErpWarehouseSaveReqVO();
        reqVO.setId(1L);
        reqVO.setName("A仓");
        reqVO.setDeptId(10L);
        reqVO.setAddress("新地址");
        reqVO.setMapName("新地图");
        reqVO.setLongitude(new BigDecimal("104.066801"));
        reqVO.setLatitude(new BigDecimal("30.572269"));
        reqVO.setSaleEnabled(true);

        warehouseService.updateWarehouse(reqVO);

        ArgumentCaptor<ErpWarehouseDO> captor = ArgumentCaptor.forClass(ErpWarehouseDO.class);
        verify(warehouseMapper).updateById(captor.capture());
        assertEquals("新地址", captor.getValue().getAddress());
        assertEquals("新地图", captor.getValue().getMapName());
        assertEquals(0, new BigDecimal("104.066801").compareTo(captor.getValue().getLongitude()));
        assertEquals(0, new BigDecimal("30.572269").compareTo(captor.getValue().getLatitude()));
    }

    @Test
    public void testUpdateWarehouse_invalidLatitude_throwException() {
        ErpWarehouseDO warehouse = new ErpWarehouseDO().setId(1L).setName("A仓").setWarehouseCode("WH001")
                .setDeptId(10L).setSaleEnabled(true);
        when(warehouseMapper.selectById(eq(1L))).thenReturn(warehouse);
        ErpWarehouseSaveReqVO reqVO = new ErpWarehouseSaveReqVO();
        reqVO.setId(1L);
        reqVO.setName("A仓");
        reqVO.setDeptId(10L);
        reqVO.setLatitude(new BigDecimal("90.000001"));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> warehouseService.updateWarehouse(reqVO));

        assertEquals("仓库纬度必须在 -90 到 90 之间", ex.getMessage());
        verify(warehouseMapper, never()).updateById(any(ErpWarehouseDO.class));
    }

    @Test
    public void testUpdateWarehouse_recordWarehouseUpdateLog() {
        ErpWarehouseDO before = new ErpWarehouseDO().setId(1L).setName("旧仓").setWarehouseCode("WH001")
                .setDeptId(10L).setSaleEnabled(true);
        ErpWarehouseDO after = new ErpWarehouseDO().setId(1L).setName("新仓").setWarehouseCode("WH001")
                .setDeptId(10L).setSaleEnabled(true);
        when(warehouseMapper.selectById(eq(1L))).thenReturn(before, after);
        ErpWarehouseSaveReqVO reqVO = new ErpWarehouseSaveReqVO();
        reqVO.setId(1L);
        reqVO.setName("新仓");
        reqVO.setDeptId(10L);
        reqVO.setSaleEnabled(true);

        warehouseService.updateWarehouse(reqVO);

        ArgumentCaptor<String> actionCaptor = ArgumentCaptor.forClass(String.class);
        verify(operateLogService).record(eq("仓库信息"), eq("修改"), eq(1L),
                actionCaptor.capture(), eq("WH001"));
        assertEquals("数据库编号：1\n名称：新仓\n编码：WH001\n变更明细：\n名称：旧仓->新仓。",
                actionCaptor.getValue());
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
    public void testUpdateWarehouseSaleDeptPermissions_missingWarehouse_throwException() {
        when(warehouseMapper.selectById(eq(11L))).thenReturn(null);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> warehouseService.updateWarehouseSaleDeptPermissions(11L, Collections.singletonList(12L)));

        assertEquals(WAREHOUSE_NOT_EXISTS.getCode(), ex.getCode());
        verify(warehouseSaleDeptPermissionMapper, never()).deleteListByWarehouseId(any(), any());
    }

    @Test
    public void testUpdateWarehouseSaleDeptPermissions_expandChildDeduplicateAndSkipOwnDept() {
        TenantContextHolder.setTenantId(7L);
        when(warehouseMapper.selectById(eq(11L))).thenReturn(new ErpWarehouseDO().setId(11L).setDeptId(10L));
        when(deptApi.getChildDeptList(eq(10L))).thenReturn(Collections.singletonList(dept(13L, "child")));
        when(deptApi.getChildDeptList(eq(12L))).thenReturn(Collections.singletonList(dept(14L, "child")));

        warehouseService.updateWarehouseSaleDeptPermissions(11L, Arrays.asList(10L, 12L, 12L, null));

        verify(deptApi).validateDeptList(eq(new LinkedHashSet<>(Arrays.asList(12L, 13L, 14L))));
        verify(warehouseSaleDeptPermissionMapper).deleteListByWarehouseId(eq(11L), eq(7L));
        verify(warehouseSaleDeptPermissionMapper).insertIgnore(eq(11L), eq(12L), eq(7L));
        verify(warehouseSaleDeptPermissionMapper).insertIgnore(eq(11L), eq(13L), eq(7L));
        verify(warehouseSaleDeptPermissionMapper).insertIgnore(eq(11L), eq(14L), eq(7L));
        verify(warehouseSaleDeptPermissionMapper, never()).insertIgnore(eq(11L), eq(10L), eq(7L));
    }

    @Test
    public void testGetSaleWarehouseListByDeptId_filterOwnDeptAndDistributedWarehouses() {
        List<ErpWarehouseDO> warehouses = Arrays.asList(
                new ErpWarehouseDO().setId(11L).setDeptId(10L).setStatus(CommonStatusEnum.ENABLE.getStatus()).setSaleEnabled(true),
                new ErpWarehouseDO().setId(12L).setDeptId(20L).setStatus(CommonStatusEnum.ENABLE.getStatus()).setSaleEnabled(true),
                new ErpWarehouseDO().setId(13L).setDeptId(10L).setStatus(CommonStatusEnum.ENABLE.getStatus()).setSaleEnabled(false));
        when(warehouseSaleDeptPermissionMapper.selectListByDeptId(eq(10L))).thenReturn(Collections.singletonList(
                ErpWarehouseSaleDeptPermissionDO.builder().warehouseId(12L).deptId(10L).build()));
        when(warehouseMapper.selectListByStatusAndDeptIdOrIds(eq(CommonStatusEnum.ENABLE.getStatus()), eq(10L), any()))
                .thenReturn(warehouses);

        List<ErpWarehouseDO> result = warehouseService.getSaleWarehouseListByDeptId(10L);

        assertEquals(Arrays.asList(11L, 12L), result.stream().map(ErpWarehouseDO::getId)
                .collect(java.util.stream.Collectors.toList()));
        ArgumentCaptor<Collection<Long>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(warehouseMapper).selectListByStatusAndDeptIdOrIds(eq(CommonStatusEnum.ENABLE.getStatus()),
                eq(10L), captor.capture());
        assertEquals(Collections.singletonList(12L), captor.getValue().stream()
                .collect(java.util.stream.Collectors.toList()));
        verify(warehouseSaleDeptPermissionMapper).selectListByDeptId(eq(10L));
    }

    @Test
    public void testGetSaleWarehouseListByDeptId_normalUserCanSeeDistributedWarehouseWithoutUserPermission() {
        ErpWarehouseDO distributedWarehouse = new ErpWarehouseDO().setId(12L).setDeptId(20L)
                .setStatus(CommonStatusEnum.ENABLE.getStatus()).setSaleEnabled(true);
        when(warehouseSaleDeptPermissionMapper.selectListByDeptId(eq(10L))).thenReturn(Collections.singletonList(
                ErpWarehouseSaleDeptPermissionDO.builder().warehouseId(12L).deptId(10L).build()));
        when(warehouseMapper.selectListByStatusAndDeptIdOrIds(eq(CommonStatusEnum.ENABLE.getStatus()), eq(10L), any()))
                .thenReturn(Collections.singletonList(distributedWarehouse));

        List<ErpWarehouseDO> result = warehouseService.getSaleWarehouseListByDeptId(10L);

        assertEquals(Collections.singletonList(12L), result.stream().map(ErpWarehouseDO::getId)
                .collect(java.util.stream.Collectors.toList()));
        verify(permissionApi, never()).getDeptDataPermission(any(), anyString());
        verify(userWarehousePermissionMapper, never()).selectListByUserId(any());
    }

    @Test
    public void testGetCurrentUserSaleSelectableWarehouseListByDept_mergesDeptAndDirectUserWarehouses() {
        ErpWarehouseDO ownWarehouse = new ErpWarehouseDO().setId(11L).setDeptId(10L)
                .setStatus(CommonStatusEnum.ENABLE.getStatus()).setSaleEnabled(true);
        ErpWarehouseDO distributedWarehouse = new ErpWarehouseDO().setId(12L).setDeptId(20L)
                .setStatus(CommonStatusEnum.ENABLE.getStatus()).setSaleEnabled(true);
        ErpWarehouseDO directUserWarehouse = new ErpWarehouseDO().setId(13L).setDeptId(30L)
                .setStatus(CommonStatusEnum.ENABLE.getStatus()).setSaleEnabled(true);
        ErpWarehouseDO saleDisabledWarehouse = new ErpWarehouseDO().setId(14L).setDeptId(40L)
                .setStatus(CommonStatusEnum.ENABLE.getStatus()).setSaleEnabled(false);
        when(warehouseSaleDeptPermissionMapper.selectListByDeptId(eq(10L))).thenReturn(Collections.singletonList(
                ErpWarehouseSaleDeptPermissionDO.builder().warehouseId(12L).deptId(10L).build()));
        when(warehouseMapper.selectListByStatusAndDeptIdOrIds(eq(CommonStatusEnum.ENABLE.getStatus()), eq(10L), any()))
                .thenReturn(Arrays.asList(ownWarehouse, distributedWarehouse));
        when(userWarehousePermissionMapper.selectListByUserId(104L)).thenReturn(Arrays.asList(
                ErpUserWarehousePermissionDO.builder().warehouseId(13L).build(),
                ErpUserWarehousePermissionDO.builder().warehouseId(14L).build()));
        when(warehouseMapper.selectListByStatusAndIds(eq(CommonStatusEnum.ENABLE.getStatus()), any()))
                .thenReturn(Arrays.asList(directUserWarehouse, saleDisabledWarehouse));

        try (MockedStatic<SecurityFrameworkUtils> mock = mockStatic(SecurityFrameworkUtils.class)) {
            mock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(104L);

            List<ErpWarehouseDO> result = warehouseService.getCurrentUserSaleSelectableWarehouseListByDept(10L);

            assertEquals(Arrays.asList(11L, 12L, 13L), result.stream().map(ErpWarehouseDO::getId)
                    .collect(java.util.stream.Collectors.toList()));
        }
    }

    @Test
    public void testGetCurrentUserSaleSelectableWarehouseListByDept_allPermissionStillUsesThreeRuleScope() {
        ErpWarehouseDO ownWarehouse = new ErpWarehouseDO().setId(11L).setDeptId(10L)
                .setStatus(CommonStatusEnum.ENABLE.getStatus()).setSaleEnabled(true);
        when(warehouseSaleDeptPermissionMapper.selectListByDeptId(eq(10L))).thenReturn(Collections.emptyList());
        when(warehouseMapper.selectListByStatusAndDeptIdOrIds(eq(CommonStatusEnum.ENABLE.getStatus()), eq(10L), any()))
                .thenReturn(Collections.singletonList(ownWarehouse));
        when(userWarehousePermissionMapper.selectListByUserId(104L)).thenReturn(Collections.emptyList());

        try (MockedStatic<SecurityFrameworkUtils> mock = mockStatic(SecurityFrameworkUtils.class)) {
            mock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(104L);

            List<ErpWarehouseDO> result = warehouseService.getCurrentUserSaleSelectableWarehouseListByDept(10L);

            assertEquals(Collections.singletonList(11L), result.stream().map(ErpWarehouseDO::getId)
                    .collect(java.util.stream.Collectors.toList()));
            verify(warehouseMapper, never()).selectListByStatus(CommonStatusEnum.ENABLE.getStatus());
        }
    }

    @Test
    public void testValidateWarehouseSaleSelectableForDept_directUserWarehousePassesWithoutDeptDistribution() {
        ErpWarehouseDO warehouse = new ErpWarehouseDO().setId(11L).setName("A").setDeptId(20L)
                .setStatus(CommonStatusEnum.ENABLE.getStatus()).setSaleEnabled(true);
        when(warehouseMapper.selectById(eq(11L))).thenReturn(warehouse);
        when(warehouseSaleDeptPermissionMapper.selectCountByWarehouseIdAndDeptId(eq(11L), eq(10L))).thenReturn(0L);
        when(userWarehousePermissionMapper.selectListByUserId(104L)).thenReturn(Collections.singletonList(
                ErpUserWarehousePermissionDO.builder().warehouseId(11L).build()));

        try (MockedStatic<SecurityFrameworkUtils> mock = mockStatic(SecurityFrameworkUtils.class)) {
            mock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(104L);

            warehouseService.validateWarehouseSaleSelectableForDept(11L, 10L);
        }
    }

    @Test
    public void testValidateWarehouseSaleAllowedForDept_ownDeptAndDistributedPassUnauthorizedThrows() {
        ErpWarehouseDO warehouse = new ErpWarehouseDO().setId(11L).setName("A").setDeptId(10L)
                .setStatus(CommonStatusEnum.ENABLE.getStatus()).setSaleEnabled(true);
        when(warehouseMapper.selectById(eq(11L))).thenReturn(warehouse);
        when(warehouseSaleDeptPermissionMapper.selectCountByWarehouseIdAndDeptId(eq(11L), eq(12L))).thenReturn(1L);
        when(warehouseSaleDeptPermissionMapper.selectCountByWarehouseIdAndDeptId(eq(11L), eq(13L))).thenReturn(0L);

        warehouseService.validateWarehouseSaleAllowedForDept(11L, 10L);
        warehouseService.validateWarehouseSaleAllowedForDept(11L, 12L);
        ServiceException ex = assertThrows(ServiceException.class,
                () -> warehouseService.validateWarehouseSaleAllowedForDept(11L, 13L));
        assertEquals(WAREHOUSE_SALE_DEPT_PERMISSION_DENIED.getCode(), ex.getCode());
    }

    @Test
    public void testValidateWarehouseSaleAllowedForDept_normalUserCanUseDistributedWarehouseWithoutUserPermission() {
        ErpWarehouseDO warehouse = new ErpWarehouseDO().setId(11L).setName("A").setDeptId(20L)
                .setStatus(CommonStatusEnum.ENABLE.getStatus()).setSaleEnabled(true);
        when(warehouseMapper.selectById(eq(11L))).thenReturn(warehouse);
        when(warehouseSaleDeptPermissionMapper.selectCountByWarehouseIdAndDeptId(eq(11L), eq(10L))).thenReturn(1L);

        warehouseService.validateWarehouseSaleAllowedForDept(11L, 10L);

        verify(permissionApi, never()).getDeptDataPermission(any(), anyString());
        verify(userWarehousePermissionMapper, never()).selectListByUserId(any());
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
        DeptDataPermissionRespDTO dataPermission = new DeptDataPermissionRespDTO();
        dataPermission.setDeptIds(new LinkedHashSet<>(Collections.singletonList(10L)));
        when(permissionApi.getDeptDataPermission(eq(104L), eq("erp_warehouse"))).thenReturn(dataPermission);
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
            assertEquals(Arrays.asList(11L, 12L), captor.getValue().stream().collect(java.util.stream.Collectors.toList()));
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
        lenient().when(warehouseMapper.selectListByStatusIfPresent(eq(null)))
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
        lenient().when(warehouseMapper.selectListByStatusIfPresent(eq(null)))
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
    public void testGetCurrentUserVisibleWarehouse_saleDistributedCanViewDetailWithoutBasePermission() {
        ErpWarehouseDO warehouse = new ErpWarehouseDO().setId(12L).setDeptId(20L);
        when(permissionApi.hasAnyRoles(eq(104L), eq(RoleCodeEnum.SUPER_ADMIN.getCode()))).thenReturn(false);
        DeptDataPermissionRespDTO dataPermission = new DeptDataPermissionRespDTO();
        dataPermission.setDeptIds(new LinkedHashSet<>(Collections.singletonList(10L)));
        when(permissionApi.getDeptDataPermission(eq(104L), eq("erp_warehouse"))).thenReturn(dataPermission);
        when(warehouseMapper.selectListByStatusIfPresent(eq(null))).thenReturn(Collections.emptyList());
        when(userWarehousePermissionMapper.selectListByUserId(eq(104L))).thenReturn(Collections.emptyList());
        when(warehouseSaleDeptPermissionMapper.selectListByDeptIds(eq(new LinkedHashSet<>(Collections.singletonList(10L)))))
                .thenReturn(Collections.singletonList(
                        ErpWarehouseSaleDeptPermissionDO.builder().warehouseId(12L).deptId(10L).build()));
        when(warehouseMapper.selectById(eq(12L))).thenReturn(warehouse);

        try (MockedStatic<SecurityFrameworkUtils> mock = mockStatic(SecurityFrameworkUtils.class)) {
            mock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(104L);
            mock.when(SecurityFrameworkUtils::getLoginUserDeptId).thenReturn(null);

            assertSame(warehouse, warehouseService.getCurrentUserVisibleWarehouse(12L));
        }
    }

    @Test
    public void testGetCurrentUserSaleDistributedVisibleWarehouseIds_excludesBaseVisibleWarehouse() {
        when(permissionApi.hasAnyRoles(eq(104L), eq(RoleCodeEnum.SUPER_ADMIN.getCode()))).thenReturn(false);
        DeptDataPermissionRespDTO dataPermission = new DeptDataPermissionRespDTO();
        dataPermission.setDeptIds(new LinkedHashSet<>(Collections.singletonList(10L)));
        when(permissionApi.getDeptDataPermission(eq(104L), eq("erp_warehouse"))).thenReturn(dataPermission);
        when(warehouseMapper.selectListByStatusIfPresent(eq(null))).thenReturn(Collections.singletonList(
                new ErpWarehouseDO().setId(11L).setDeptId(10L)));
        when(userWarehousePermissionMapper.selectListByUserId(eq(104L))).thenReturn(Collections.emptyList());
        when(warehouseSaleDeptPermissionMapper.selectListByDeptIds(eq(new LinkedHashSet<>(Collections.singletonList(10L)))))
                .thenReturn(Arrays.asList(
                        ErpWarehouseSaleDeptPermissionDO.builder().warehouseId(11L).deptId(10L).build(),
                        ErpWarehouseSaleDeptPermissionDO.builder().warehouseId(12L).deptId(10L).build()));

        try (MockedStatic<SecurityFrameworkUtils> mock = mockStatic(SecurityFrameworkUtils.class)) {
            mock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(104L);
            mock.when(SecurityFrameworkUtils::getLoginUserDeptId).thenReturn(null);

            assertEquals(Collections.singletonList(12L), warehouseService.getCurrentUserSaleDistributedVisibleWarehouseIds()
                    .stream().collect(java.util.stream.Collectors.toList()));
        }
    }

    @Test
    public void testGetCurrentUserVisibleSaleWarehouseList_mergesDistributedAndFiltersSaleEnabled() {
        when(permissionApi.hasAnyRoles(eq(104L), eq(RoleCodeEnum.SUPER_ADMIN.getCode()))).thenReturn(false);
        DeptDataPermissionRespDTO dataPermission = new DeptDataPermissionRespDTO();
        dataPermission.setDeptIds(new LinkedHashSet<>(Collections.singletonList(10L)));
        when(permissionApi.getDeptDataPermission(eq(104L), eq("erp_warehouse"))).thenReturn(dataPermission);
        when(warehouseMapper.selectListByStatusIfPresent(eq(CommonStatusEnum.ENABLE.getStatus())))
                .thenReturn(Collections.singletonList(
                        new ErpWarehouseDO().setId(11L).setDeptId(10L).setSaleEnabled(true)));
        when(warehouseMapper.selectListByStatusIfPresent(eq(null))).thenReturn(Collections.singletonList(
                new ErpWarehouseDO().setId(11L).setDeptId(10L).setSaleEnabled(true)));
        when(userWarehousePermissionMapper.selectListByUserId(eq(104L))).thenReturn(Collections.emptyList());
        when(warehouseSaleDeptPermissionMapper.selectListByDeptIds(eq(new LinkedHashSet<>(Collections.singletonList(10L)))))
                .thenReturn(Arrays.asList(
                        ErpWarehouseSaleDeptPermissionDO.builder().warehouseId(11L).deptId(10L).build(),
                        ErpWarehouseSaleDeptPermissionDO.builder().warehouseId(12L).deptId(10L).build(),
                        ErpWarehouseSaleDeptPermissionDO.builder().warehouseId(13L).deptId(10L).build()));
        when(warehouseMapper.selectListByStatusAndIds(eq(CommonStatusEnum.ENABLE.getStatus()), any()))
                .thenAnswer(invocation -> {
                    Collection<Long> ids = invocation.getArgument(1);
                    if (ids.contains(12L) || ids.contains(13L)) {
                        return Arrays.asList(
                                new ErpWarehouseDO().setId(12L).setDeptId(20L).setSaleEnabled(true),
                                new ErpWarehouseDO().setId(13L).setDeptId(20L).setSaleEnabled(false));
                    }
                    return Collections.singletonList(
                            new ErpWarehouseDO().setId(11L).setDeptId(10L).setSaleEnabled(true));
                });

        try (MockedStatic<SecurityFrameworkUtils> mock = mockStatic(SecurityFrameworkUtils.class)) {
            mock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(104L);
            mock.when(SecurityFrameworkUtils::getLoginUserDeptId).thenReturn(null);

            List<ErpWarehouseDO> result = warehouseService.getCurrentUserVisibleSaleWarehouseList();

            assertEquals(Arrays.asList(11L, 12L), result.stream().map(ErpWarehouseDO::getId)
                    .collect(java.util.stream.Collectors.toList()));
        }
    }

    @Test
    public void testGetCurrentUserStockVisibleWarehouseList_onlyUsesOwnedDepartmentWarehouses() {
        DeptDataPermissionRespDTO dataPermission = new DeptDataPermissionRespDTO();
        dataPermission.setDeptIds(new LinkedHashSet<>(Collections.singletonList(20L)));
        when(permissionApi.getDeptDataPermission(eq(104L), eq("erp_stock"))).thenReturn(dataPermission);
        when(warehouseMapper.selectListByStatusIfPresent(eq(CommonStatusEnum.ENABLE.getStatus())))
                .thenReturn(Arrays.asList(
                        new ErpWarehouseDO().setId(11L).setDeptId(20L),
                        new ErpWarehouseDO().setId(12L).setDeptId(10L),
                        new ErpWarehouseDO().setId(14L).setDeptId(20L).setName("直发仓")));
        when(warehouseMapper.selectListByStatusAndIds(eq(CommonStatusEnum.ENABLE.getStatus()),
                eq(new LinkedHashSet<>(Arrays.asList(11L, 14L)))))
                .thenReturn(Arrays.asList(
                        new ErpWarehouseDO().setId(11L).setDeptId(20L),
                        new ErpWarehouseDO().setId(14L).setDeptId(20L).setName("直发仓")));

        try (MockedStatic<SecurityFrameworkUtils> mock = mockStatic(SecurityFrameworkUtils.class)) {
            mock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(104L);
            mock.when(SecurityFrameworkUtils::getLoginUserDeptId).thenReturn(20L);

            List<ErpWarehouseDO> result = warehouseService.getCurrentUserStockVisibleWarehouseList();

            assertEquals(Arrays.asList(11L, 14L), result.stream().map(ErpWarehouseDO::getId)
                    .collect(java.util.stream.Collectors.toList()));
            verify(warehouseSaleDeptPermissionMapper, never()).selectListByDeptIds(any());
            verify(userWarehousePermissionMapper, never()).selectListByUserId(any());
        }
    }

    @Test
    public void testValidateCurrentUserStockWarehousePermission_allowsOnlyStockVisibleWarehouses() {
        ErpWarehouseServiceImpl service = spy(warehouseService);
        doReturn(new ErpProductStockPermissionScope(false,
                new LinkedHashSet<>(Arrays.asList(11L, 12L)), Collections.emptySet(), 104L))
                .when(service).getCurrentUserProductStockPermissionScope();

        service.validateCurrentUserStockWarehousePermission(Collections.singleton(12L));
        assertThrows(IllegalArgumentException.class,
                () -> service.validateCurrentUserStockWarehousePermission(Collections.singleton(13L)));
    }

    @Test
    public void testValidateCurrentUserStockPermission_selfScopeRequiresCurrentUserCreator() {
        ErpWarehouseServiceImpl service = spy(warehouseService);
        doReturn(new ErpProductStockPermissionScope(false, Collections.emptySet(),
                new LinkedHashSet<>(Collections.singletonList(14L)), 104L))
                .when(service).getCurrentUserProductStockPermissionScope();

        ErpStockDO ownStock = new ErpStockDO().setId(1L).setWarehouseId(14L);
        ownStock.setCreator("104");
        ErpStockDO otherCreatorStock = new ErpStockDO().setId(2L).setWarehouseId(14L);
        otherCreatorStock.setCreator("105");
        ErpStockDO externalWarehouseStock = new ErpStockDO().setId(3L).setWarehouseId(12L);
        externalWarehouseStock.setCreator("104");

        service.validateCurrentUserStockPermission(Collections.singletonList(ownStock));
        assertThrows(IllegalArgumentException.class, () -> service.validateCurrentUserStockPermission(
                Collections.singletonList(otherCreatorStock)));
        assertThrows(IllegalArgumentException.class, () -> service.validateCurrentUserStockPermission(
                Collections.singletonList(externalWarehouseStock)));
    }

    @Test
    public void testGetCurrentUserStockMoveFromWarehouseList_mergesDistributedWarehouseWithoutSaleEnabledFilter() {
        when(permissionApi.hasAnyRoles(eq(104L), eq(RoleCodeEnum.SUPER_ADMIN.getCode()))).thenReturn(false);
        DeptDataPermissionRespDTO dataPermission = new DeptDataPermissionRespDTO();
        dataPermission.setDeptIds(new LinkedHashSet<>(Collections.singletonList(10L)));
        when(permissionApi.getDeptDataPermission(eq(104L), eq("erp_warehouse"))).thenReturn(dataPermission);
        when(warehouseMapper.selectListByStatusIfPresent(eq(CommonStatusEnum.ENABLE.getStatus())))
                .thenReturn(Collections.singletonList(new ErpWarehouseDO().setId(11L).setDeptId(10L)));
        when(warehouseMapper.selectListByStatusIfPresent(eq(null)))
                .thenReturn(Collections.singletonList(new ErpWarehouseDO().setId(11L).setDeptId(10L)));
        when(userWarehousePermissionMapper.selectListByUserId(eq(104L))).thenReturn(Collections.emptyList());
        when(warehouseSaleDeptPermissionMapper.selectListByDeptIds(eq(new LinkedHashSet<>(Collections.singletonList(10L)))))
                .thenReturn(Arrays.asList(
                        ErpWarehouseSaleDeptPermissionDO.builder().warehouseId(11L).deptId(10L).build(),
                        ErpWarehouseSaleDeptPermissionDO.builder().warehouseId(12L).deptId(10L).build()));
        when(warehouseMapper.selectListByStatusAndIds(eq(CommonStatusEnum.ENABLE.getStatus()), any()))
                .thenAnswer(invocation -> {
                    Collection<Long> ids = invocation.getArgument(1);
                    if (ids.contains(12L)) {
                        return Collections.singletonList(
                                new ErpWarehouseDO().setId(12L).setDeptId(20L).setSaleEnabled(false));
                    }
                    return Collections.singletonList(new ErpWarehouseDO().setId(11L).setDeptId(10L));
                });

        try (MockedStatic<SecurityFrameworkUtils> mock = mockStatic(SecurityFrameworkUtils.class)) {
            mock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(104L);
            mock.when(SecurityFrameworkUtils::getLoginUserDeptId).thenReturn(null);

            List<ErpWarehouseDO> result = warehouseService.getCurrentUserStockMoveFromWarehouseList();

            assertEquals(Arrays.asList(11L, 12L), result.stream().map(ErpWarehouseDO::getId)
                    .collect(java.util.stream.Collectors.toList()));
        }
    }

    @Test
    public void testValidateCurrentUserStockMoveFromWarehousePermission_allowsDistributedWarehouse() {
        ErpWarehouseDO distributedWarehouse = new ErpWarehouseDO().setId(12L).setDeptId(20L);
        when(permissionApi.hasAnyRoles(eq(104L), eq(RoleCodeEnum.SUPER_ADMIN.getCode()))).thenReturn(false);
        DeptDataPermissionRespDTO dataPermission = new DeptDataPermissionRespDTO();
        dataPermission.setDeptIds(new LinkedHashSet<>(Collections.singletonList(10L)));
        when(permissionApi.getDeptDataPermission(eq(104L), eq("erp_warehouse"))).thenReturn(dataPermission);
        when(warehouseMapper.selectListByStatusIfPresent(eq(CommonStatusEnum.ENABLE.getStatus())))
                .thenReturn(Collections.emptyList());
        when(warehouseMapper.selectListByStatusIfPresent(eq(null))).thenReturn(Collections.emptyList());
        when(userWarehousePermissionMapper.selectListByUserId(eq(104L))).thenReturn(Collections.emptyList());
        when(warehouseSaleDeptPermissionMapper.selectListByDeptIds(eq(new LinkedHashSet<>(Collections.singletonList(10L)))))
                .thenReturn(Collections.singletonList(
                        ErpWarehouseSaleDeptPermissionDO.builder().warehouseId(12L).deptId(10L).build()));
        when(warehouseMapper.selectListByStatusAndIds(eq(CommonStatusEnum.ENABLE.getStatus()), any()))
                .thenReturn(Collections.singletonList(distributedWarehouse));

        try (MockedStatic<SecurityFrameworkUtils> mock = mockStatic(SecurityFrameworkUtils.class)) {
            mock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(104L);
            mock.when(SecurityFrameworkUtils::getLoginUserDeptId).thenReturn(null);

            warehouseService.validateCurrentUserStockMoveFromWarehousePermission(Collections.singletonList(12L));
        }
    }

    @Test
    public void testValidateCurrentUserWarehousePermission_stillRejectsDistributedWarehouse() {
        when(permissionApi.hasAnyRoles(eq(104L), eq(RoleCodeEnum.SUPER_ADMIN.getCode()))).thenReturn(false);
        DeptDataPermissionRespDTO dataPermission = new DeptDataPermissionRespDTO();
        dataPermission.setDeptIds(new LinkedHashSet<>(Collections.singletonList(10L)));
        when(permissionApi.getDeptDataPermission(eq(104L), eq("erp_warehouse"))).thenReturn(dataPermission);
        when(warehouseMapper.selectListByStatusIfPresent(eq(CommonStatusEnum.ENABLE.getStatus())))
                .thenReturn(Collections.emptyList());
        when(userWarehousePermissionMapper.selectListByUserId(eq(104L))).thenReturn(Collections.emptyList());

        try (MockedStatic<SecurityFrameworkUtils> mock = mockStatic(SecurityFrameworkUtils.class)) {
            mock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(104L);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> warehouseService.validateCurrentUserWarehousePermission(Collections.singletonList(12L)));

            assertTrue(ex.getMessage().contains("No warehouse permission"));
        }
    }

    @Test
    public void testValidSaleWarehouseList_allowsSaleDistributedWarehouseWithoutBasePermission() {
        ErpWarehouseDO distributedWarehouse = new ErpWarehouseDO().setId(12L)
                .setName("Qionglai").setStatus(CommonStatusEnum.ENABLE.getStatus()).setDeptId(20L).setSaleEnabled(true);
        when(warehouseMapper.selectByIds(eq(Collections.singletonList(12L))))
                .thenAnswer(invocation -> {
                    if (DataPermissionContextHolder.get() == null
                            || DataPermissionContextHolder.get().enable()) {
                        return Collections.emptyList();
                    }
                    return Collections.singletonList(distributedWarehouse);
                });
        when(permissionApi.hasAnyRoles(eq(104L), eq(RoleCodeEnum.SUPER_ADMIN.getCode()))).thenReturn(false);
        DeptDataPermissionRespDTO dataPermission = new DeptDataPermissionRespDTO();
        dataPermission.setDeptIds(new LinkedHashSet<>(Collections.singletonList(10L)));
        when(permissionApi.getDeptDataPermission(eq(104L), eq("erp_warehouse"))).thenReturn(dataPermission);
        when(warehouseMapper.selectListByStatusIfPresent(eq(CommonStatusEnum.ENABLE.getStatus())))
                .thenReturn(Collections.emptyList());
        when(warehouseMapper.selectListByStatusIfPresent(eq(null))).thenReturn(Collections.emptyList());
        when(userWarehousePermissionMapper.selectListByUserId(eq(104L))).thenReturn(Collections.emptyList());
        when(warehouseSaleDeptPermissionMapper.selectListByDeptIds(eq(new LinkedHashSet<>(Collections.singletonList(10L)))))
                .thenReturn(Collections.singletonList(
                        ErpWarehouseSaleDeptPermissionDO.builder().warehouseId(12L).deptId(10L).build()));
        when(warehouseMapper.selectListByStatusAndIds(eq(CommonStatusEnum.ENABLE.getStatus()), any()))
                .thenReturn(Collections.singletonList(distributedWarehouse));

        try (MockedStatic<SecurityFrameworkUtils> mock = mockStatic(SecurityFrameworkUtils.class)) {
            mock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(104L);
            mock.when(SecurityFrameworkUtils::getLoginUserDeptId).thenReturn(null);

            List<ErpWarehouseDO> result = warehouseService.validSaleWarehouseList(Collections.singletonList(12L));

            assertEquals(Collections.singletonList(12L), result.stream().map(ErpWarehouseDO::getId)
                    .collect(java.util.stream.Collectors.toList()));
        }
    }

    @Test
    public void testValidSaleWarehouseList_rejectsWarehouseOutsideAuthorizedAndDistributedScope() {
        ErpWarehouseDO deniedWarehouse = new ErpWarehouseDO().setId(14L)
                .setName("Denied").setStatus(CommonStatusEnum.ENABLE.getStatus()).setDeptId(30L).setSaleEnabled(true);
        ErpWarehouseDO distributedWarehouse = new ErpWarehouseDO().setId(12L)
                .setName("Qionglai").setStatus(CommonStatusEnum.ENABLE.getStatus()).setDeptId(20L).setSaleEnabled(true);
        when(warehouseMapper.selectByIds(eq(Collections.singletonList(14L))))
                .thenReturn(Collections.singletonList(deniedWarehouse));
        when(permissionApi.hasAnyRoles(eq(104L), eq(RoleCodeEnum.SUPER_ADMIN.getCode()))).thenReturn(false);
        DeptDataPermissionRespDTO dataPermission = new DeptDataPermissionRespDTO();
        dataPermission.setDeptIds(new LinkedHashSet<>(Collections.singletonList(10L)));
        when(permissionApi.getDeptDataPermission(eq(104L), eq("erp_warehouse"))).thenReturn(dataPermission);
        when(warehouseMapper.selectListByStatusIfPresent(eq(CommonStatusEnum.ENABLE.getStatus())))
                .thenReturn(Collections.emptyList());
        when(warehouseMapper.selectListByStatusIfPresent(eq(null))).thenReturn(Collections.emptyList());
        when(userWarehousePermissionMapper.selectListByUserId(eq(104L))).thenReturn(Collections.emptyList());
        when(warehouseSaleDeptPermissionMapper.selectListByDeptIds(eq(new LinkedHashSet<>(Collections.singletonList(10L)))))
                .thenReturn(Collections.singletonList(
                        ErpWarehouseSaleDeptPermissionDO.builder().warehouseId(12L).deptId(10L).build()));
        when(warehouseMapper.selectListByStatusAndIds(eq(CommonStatusEnum.ENABLE.getStatus()), any()))
                .thenReturn(Collections.singletonList(distributedWarehouse));

        try (MockedStatic<SecurityFrameworkUtils> mock = mockStatic(SecurityFrameworkUtils.class)) {
            mock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(104L);
            mock.when(SecurityFrameworkUtils::getLoginUserDeptId).thenReturn(null);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> warehouseService.validSaleWarehouseList(Collections.singletonList(14L)));

            assertTrue(ex.getMessage().contains("No warehouse sale permission"));
        }
    }

    @Test
    public void testValidSaleWarehouseListForDept_allowsDistributedWarehouseWithoutLoginContext() {
        ErpWarehouseDO distributedWarehouse = new ErpWarehouseDO().setId(12L)
                .setName("DayiDirect").setStatus(CommonStatusEnum.ENABLE.getStatus())
                .setDeptId(20L).setSaleEnabled(true);
        when(warehouseMapper.selectByIds(eq(Collections.singletonList(12L))))
                .thenReturn(Collections.singletonList(distributedWarehouse));
        when(warehouseSaleDeptPermissionMapper.selectCountByWarehouseIdAndDeptId(12L, 10L)).thenReturn(1L);

        List<ErpWarehouseDO> result = warehouseService.validSaleWarehouseListForDept(
                Collections.singletonList(12L), 10L);

        assertEquals(Collections.singletonList(12L), result.stream().map(ErpWarehouseDO::getId)
                .collect(java.util.stream.Collectors.toList()));
        verify(permissionApi, never()).getDeptDataPermission(any(), any());
        verify(userWarehousePermissionMapper, never()).selectListByUserId(any());
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
    public void testDeleteWarehouse_recordWarehouseDeleteLog() {
        ErpWarehouseDO warehouse = new ErpWarehouseDO().setId(1L).setName("删除仓").setWarehouseCode("WH001");
        when(warehouseMapper.selectById(eq(1L))).thenReturn(warehouse);

        warehouseService.deleteWarehouse(1L);

        ArgumentCaptor<String> actionCaptor = ArgumentCaptor.forClass(String.class);
        verify(operateLogService).record(eq("仓库信息"), eq("删除"), eq(1L),
                actionCaptor.capture(), eq("WH001"));
        assertEquals("数据库编号：1\n名称：删除仓\n编码：WH001", actionCaptor.getValue());
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
    public void testImportWarehouseList_createWithLocationFields() {
        ErpWarehouseImportExcelVO row = new ErpWarehouseImportExcelVO();
        row.setName("新增仓");
        row.setWarehouseCode("WH001");
        row.setAddress("成都市高新区");
        row.setMapName("兴宇路通仓库");
        row.setLongitude(new BigDecimal("104.066801"));
        row.setLatitude(new BigDecimal("30.572269"));

        ErpWarehouseImportRespVO result = warehouseService.importWarehouseList(Collections.singletonList(row));

        assertEquals(1, result.getSuccessCount(), result.getFailureDetails().toString());
        assertEquals(1, result.getCreateCount());
        ArgumentCaptor<ErpWarehouseDO> insertCaptor = ArgumentCaptor.forClass(ErpWarehouseDO.class);
        verify(warehouseMapper).insert(insertCaptor.capture());
        assertEquals("成都市高新区", insertCaptor.getValue().getAddress());
        assertEquals("兴宇路通仓库", insertCaptor.getValue().getMapName());
        assertEquals(0, new BigDecimal("104.066801").compareTo(insertCaptor.getValue().getLongitude()));
        assertEquals(0, new BigDecimal("30.572269").compareTo(insertCaptor.getValue().getLatitude()));
    }

    @Test
    public void testImportWarehouseList_updateWithLocationFields() {
        ErpWarehouseImportExcelVO row = new ErpWarehouseImportExcelVO();
        row.setName("更新仓");
        row.setWarehouseCode("WH002");
        row.setAddress("新地址");
        row.setMapName("新地图");
        row.setLongitude(new BigDecimal("104.066801"));
        row.setLatitude(new BigDecimal("30.572269"));
        ErpWarehouseDO existing = new ErpWarehouseDO().setId(2L).setName("旧仓").setWarehouseCode("WH002").setDeptId(10L);
        when(warehouseMapper.selectByWarehouseCode(eq("WH002"))).thenReturn(existing);

        ErpWarehouseImportRespVO result = warehouseService.importWarehouseList(Collections.singletonList(row));

        assertEquals(1, result.getSuccessCount(), result.getFailureDetails().toString());
        assertEquals(1, result.getUpdateCount());
        ArgumentCaptor<ErpWarehouseDO> updateCaptor = ArgumentCaptor.forClass(ErpWarehouseDO.class);
        verify(warehouseMapper).updateById(updateCaptor.capture());
        assertEquals("新地址", updateCaptor.getValue().getAddress());
        assertEquals("新地图", updateCaptor.getValue().getMapName());
        assertEquals(0, new BigDecimal("104.066801").compareTo(updateCaptor.getValue().getLongitude()));
        assertEquals(0, new BigDecimal("30.572269").compareTo(updateCaptor.getValue().getLatitude()));
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
    public void testImportWarehouseList_createBlankCode_autoGenerate() {
        ErpWarehouseImportExcelVO row = new ErpWarehouseImportExcelVO();
        row.setName("新增仓");
        when(noRedisDAO.generatePlain(eq(ErpNoRedisDAO.WAREHOUSE_CODE_PREFIX))).thenReturn("WH000001");

        ErpWarehouseImportRespVO result = warehouseService.importWarehouseList(Collections.singletonList(row));

        assertEquals(1, result.getSuccessCount(), result.getFailureDetails().toString());
        assertEquals(1, result.getCreateCount());
        assertEquals(0, result.getFailureCount());
        ArgumentCaptor<ErpWarehouseDO> insertCaptor = ArgumentCaptor.forClass(ErpWarehouseDO.class);
        verify(warehouseMapper).insert(insertCaptor.capture());
        assertEquals("WH000001", insertCaptor.getValue().getWarehouseCode());
    }

    @Test
    public void testImportWarehouseList_updateByName_keepExistingCode() {
        ErpWarehouseImportExcelVO row = new ErpWarehouseImportExcelVO();
        row.setName("已有仓");
        row.setWarehouseCode("WH999");
        ErpWarehouseDO existing = new ErpWarehouseDO().setId(2L).setName("已有仓").setWarehouseCode("WH001").setDeptId(10L);
        when(warehouseMapper.selectByWarehouseCode(eq("WH999"))).thenReturn(null);
        when(warehouseMapper.selectByName(eq("已有仓"))).thenReturn(existing);

        ErpWarehouseImportRespVO result = warehouseService.importWarehouseList(Collections.singletonList(row));

        assertEquals(1, result.getSuccessCount(), result.getFailureDetails().toString());
        assertEquals(1, result.getUpdateCount());
        ArgumentCaptor<ErpWarehouseDO> updateCaptor = ArgumentCaptor.forClass(ErpWarehouseDO.class);
        verify(warehouseMapper).updateById(updateCaptor.capture());
        assertEquals("WH001", updateCaptor.getValue().getWarehouseCode());
        verify(warehouseMapper, never()).insert(any(ErpWarehouseDO.class));
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
    public void testResolveDirectWarehouseId_prefersSameDeptWarehouse() {
        List<ErpWarehouseDO> warehouses = Collections.singletonList(
                new ErpWarehouseDO().setId(12L).setName("直发仓").setDeptId(20L));
        when(warehouseMapper.selectListByNameAndDeptIdAndStatus(eq("直发仓"), eq(20L),
                eq(CommonStatusEnum.ENABLE.getStatus()))).thenReturn(warehouses);

        Long result = warehouseService.resolveDirectWarehouseId(20L);

        assertEquals(12L, result);
    }

    @Test
    public void testResolveDirectWarehouseId_otherDeptWarehouseExists_createsOwnWarehouse() {
        when(warehouseMapper.selectListByNameAndDeptIdAndStatus(eq("直发仓"), eq(20L),
                eq(CommonStatusEnum.ENABLE.getStatus()))).thenReturn(Collections.emptyList());
        when(noRedisDAO.generatePlain(eq(ErpNoRedisDAO.WAREHOUSE_CODE_PREFIX))).thenReturn("WH000001");
        when(warehouseMapper.insert(any(ErpWarehouseDO.class))).thenAnswer(invocation -> {
            ErpWarehouseDO warehouse = invocation.getArgument(0);
            warehouse.setId(12L);
            return 1;
        });

        Long result = warehouseService.resolveDirectWarehouseId(20L);

        assertEquals(12L, result);
        ArgumentCaptor<ErpWarehouseDO> captor = ArgumentCaptor.forClass(ErpWarehouseDO.class);
        verify(warehouseMapper).insert(captor.capture());
        assertEquals("直发仓", captor.getValue().getName());
        assertEquals(20L, captor.getValue().getDeptId());
        assertEquals(CommonStatusEnum.ENABLE.getStatus(), captor.getValue().getStatus());
        assertEquals(Boolean.TRUE, captor.getValue().getSaleEnabled());
        assertEquals(Boolean.FALSE, captor.getValue().getStockBillEnabled());
        assertEquals("系统自动创建：销售手推车跨部门调拨专用直发仓", captor.getValue().getRemark());
        verify(directWarehouseCreateLock).lock();
        verify(warehouseMapper, times(2)).selectListByNameAndDeptIdAndStatus(
                "直发仓", 20L, CommonStatusEnum.ENABLE.getStatus());
    }

    @Test
    public void testResolveDirectWarehouseId_createdByConcurrentRequest_reusesAfterLock() {
        ErpWarehouseDO concurrentCreated = new ErpWarehouseDO().setId(13L).setName("直发仓").setDeptId(20L);
        when(warehouseMapper.selectListByNameAndDeptIdAndStatus(eq("直发仓"), eq(20L),
                eq(CommonStatusEnum.ENABLE.getStatus())))
                .thenReturn(Collections.emptyList(), Collections.singletonList(concurrentCreated));

        Long result = warehouseService.resolveDirectWarehouseId(20L);

        assertEquals(13L, result);
        verify(directWarehouseCreateLock).lock();
        verify(warehouseMapper, never()).insert(any(ErpWarehouseDO.class));
    }

    @Test
    public void testResolveDirectWarehouseId_nullDeptThrows() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> warehouseService.resolveDirectWarehouseId(null));

        assertEquals(WAREHOUSE_DIRECT_NOT_CONFIGURED.getCode(), ex.getCode());
    }

    @Test
    public void testResolveDirectWarehouseId_multipleSameDeptThrows() {
        List<ErpWarehouseDO> warehouses = Arrays.asList(
                new ErpWarehouseDO().setId(11L).setName("直发仓").setDeptId(20L),
                new ErpWarehouseDO().setId(12L).setName("直发仓").setDeptId(20L));
        when(warehouseMapper.selectListByNameAndDeptIdAndStatus(eq("直发仓"), eq(20L),
                eq(CommonStatusEnum.ENABLE.getStatus()))).thenReturn(warehouses);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> warehouseService.resolveDirectWarehouseId(20L));

        assertEquals(WAREHOUSE_DIRECT_MULTIPLE.getCode(), ex.getCode());
    }

    @Test
    public void testGetWarehousePage() {
        ErpWarehousePageReqVO reqVO = new ErpWarehousePageReqVO();
        when(permissionApi.hasAnyRoles(eq(104L), eq(RoleCodeEnum.SUPER_ADMIN.getCode()))).thenReturn(false);
        DeptDataPermissionRespDTO dataPermission = new DeptDataPermissionRespDTO();
        dataPermission.setDeptIds(new LinkedHashSet<>(Collections.singletonList(10L)));
        when(permissionApi.getDeptDataPermission(eq(104L), eq("erp_warehouse"))).thenReturn(dataPermission);
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
            assertEquals(Collections.singletonList(11L), captor.getValue().stream().collect(java.util.stream.Collectors.toList()));
        }
    }

    @Test
    public void testGetWarehousePage_includesSaleDistributedWarehouseForArchiveInfoOnly() {
        ErpWarehousePageReqVO reqVO = new ErpWarehousePageReqVO();
        when(permissionApi.hasAnyRoles(eq(104L), eq(RoleCodeEnum.SUPER_ADMIN.getCode()))).thenReturn(false);
        DeptDataPermissionRespDTO dataPermission = new DeptDataPermissionRespDTO();
        dataPermission.setDeptIds(new LinkedHashSet<>(Collections.singletonList(10L)));
        when(permissionApi.getDeptDataPermission(eq(104L), eq("erp_warehouse"))).thenReturn(dataPermission);
        when(warehouseMapper.selectListByStatusIfPresent(eq(null))).thenReturn(Collections.emptyList());
        when(userWarehousePermissionMapper.selectListByUserId(eq(104L))).thenReturn(Collections.emptyList());
        when(warehouseSaleDeptPermissionMapper.selectListByDeptIds(eq(new LinkedHashSet<>(Collections.singletonList(10L)))))
                .thenReturn(Collections.singletonList(
                        ErpWarehouseSaleDeptPermissionDO.builder().warehouseId(12L).deptId(10L).build()));
        PageResult<ErpWarehouseDO> pageResult = new PageResult<>(
                Collections.singletonList(new ErpWarehouseDO().setId(12L)), 1L);
        when(warehouseMapper.selectPageByIds(eq(reqVO), any())).thenReturn(pageResult);

        try (MockedStatic<SecurityFrameworkUtils> mock = mockStatic(SecurityFrameworkUtils.class)) {
            mock.when(SecurityFrameworkUtils::getLoginUserId).thenReturn(104L);
            mock.when(SecurityFrameworkUtils::getLoginUserDeptId).thenReturn(null);

            assertSame(pageResult, warehouseService.getWarehousePage(reqVO));
            ArgumentCaptor<Collection<Long>> captor = ArgumentCaptor.forClass(Collection.class);
            verify(warehouseMapper).selectPageByIds(eq(reqVO), captor.capture());
            assertEquals(Collections.singletonList(12L), captor.getValue().stream()
                    .collect(java.util.stream.Collectors.toList()));
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
