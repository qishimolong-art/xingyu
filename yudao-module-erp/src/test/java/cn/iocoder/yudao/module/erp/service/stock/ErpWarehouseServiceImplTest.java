package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseImportExcelVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseImportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehousePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpStockMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseBranchMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseMapper;
import cn.iocoder.yudao.module.erp.service.base.ErpBaseArchiveReferenceService;
import cn.iocoder.yudao.module.erp.service.common.ErpOperateLogService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.WAREHOUSE_NOT_ENABLE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.WAREHOUSE_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
    private ErpStockFieldPermissionMasker fieldPermissionMasker;
    @Mock
    private ErpOperateLogService operateLogService;
    @Mock
    private ErpBaseArchiveReferenceService baseArchiveReferenceService;
    @Mock
    private DeptApi deptApi;

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
        when(warehouseMapper.selectPage(eq(reqVO))).thenReturn(null);

        assertSame(null, warehouseService.getWarehousePage(reqVO));
    }

    private DeptRespDTO dept(Long id, String name) {
        DeptRespDTO dept = new DeptRespDTO();
        dept.setId(id);
        dept.setName(name);
        dept.setStatus(CommonStatusEnum.ENABLE.getStatus());
        return dept;
    }

}
