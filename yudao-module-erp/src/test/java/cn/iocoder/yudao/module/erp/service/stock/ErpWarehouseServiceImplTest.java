package cn.iocoder.yudao.module.erp.service.stock;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehousePageReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseBranchMapper;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseMapper;
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
    private ErpWarehouseBranchMapper warehouseBranchMapper;

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

}
