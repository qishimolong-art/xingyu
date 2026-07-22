package cn.iocoder.yudao.module.erp.controller.admin.stock;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse.ErpWarehouseRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.dept.dto.DeptRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ErpWarehouseControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpWarehouseController controller;

    @Mock
    private ErpWarehouseService warehouseService;
    @Mock
    private ErpStockFieldPermissionMasker fieldPermissionMasker;
    @Mock
    private DeptApi deptApi;
    @Mock
    private AdminUserApi adminUserApi;

    @Test
    public void testGetWarehouseSimpleList_stockUsesProductStockVisibleWarehouses() {
        when(warehouseService.getCurrentUserStockVisibleWarehouseList()).thenReturn(Arrays.asList(
                new ErpWarehouseDO().setId(10L).setName("Dayi Warehouse").setDeptId(1L),
                new ErpWarehouseDO().setId(20L).setName("Qionglai Warehouse").setDeptId(2L)));
        when(deptApi.getDeptMap(any())).thenReturn(Collections.emptyMap());

        CommonResult<List<ErpWarehouseRespVO>> result = controller.getWarehouseSimpleList("stock", null);

        assertEquals(Arrays.asList(10L, 20L), result.getData().stream()
                .map(ErpWarehouseRespVO::getId)
                .collect(Collectors.toList()));
        verify(warehouseService).getCurrentUserStockVisibleWarehouseList();
    }

    @Test
    public void testGetWarehouseSimpleList_saleDeptFilterIntersectsCurrentUserVisibleWarehouses() {
        when(warehouseService.getSaleWarehouseListByDeptId(30L)).thenReturn(Arrays.asList(
                new ErpWarehouseDO().setId(10L).setName("WJ").setDeptId(1L),
                new ErpWarehouseDO().setId(20L).setName("HY").setDeptId(2L)));
        when(warehouseService.getCurrentUserVisibleSaleWarehouseList()).thenReturn(Collections.singletonList(
                new ErpWarehouseDO().setId(10L).setName("WJ").setDeptId(1L)));
        when(deptApi.getDeptMap(any())).thenReturn(Collections.emptyMap());

        CommonResult<List<ErpWarehouseRespVO>> result = controller.getWarehouseSimpleList("sale", 30L);

        assertEquals(Collections.singletonList(10L), result.getData().stream()
                .map(ErpWarehouseRespVO::getId)
                .collect(Collectors.toList()));
        verify(warehouseService).getSaleWarehouseListByDeptId(30L);
        verify(warehouseService).getCurrentUserVisibleSaleWarehouseList();
    }

    @Test
    public void testGetPurchaseWarehouseOwnerDeptSimpleList_usesAuthorizedPurchaseWarehouses() {
        when(warehouseService.getCurrentUserAuthorizedPurchaseWarehouseList()).thenReturn(Arrays.asList(
                new ErpWarehouseDO().setId(10L).setDeptId(1L),
                new ErpWarehouseDO().setId(20L).setDeptId(2L),
                new ErpWarehouseDO().setId(30L).setDeptId(1L)));
        Map<Long, DeptRespDTO> deptMap = new LinkedHashMap<>();
        deptMap.put(1L, new DeptRespDTO().setId(1L).setName("Dayi Branch"));
        deptMap.put(2L, new DeptRespDTO().setId(2L).setName("Qionglai Branch"));
        when(deptApi.getDeptMap(any())).thenReturn(deptMap);

        CommonResult<List<DeptRespDTO>> result = controller.getPurchaseWarehouseOwnerDeptSimpleList();

        assertEquals(Arrays.asList(1L, 2L), result.getData().stream()
                .map(DeptRespDTO::getId)
                .collect(Collectors.toList()));
        verify(warehouseService).getCurrentUserAuthorizedPurchaseWarehouseList();
    }
}
