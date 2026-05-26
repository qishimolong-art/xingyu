package cn.iocoder.yudao.module.erp.controller.admin.sale;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerarea.ErpCustomerAreaPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerarea.ErpCustomerAreaRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerarea.ErpCustomerAreaSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerAreaDO;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerAreaService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ErpCustomerAreaControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpCustomerAreaController controller;

    @Mock
    private ErpCustomerAreaService areaService;

    // ==================== createArea ====================

    @Test
    public void testCreateArea_paramPassThrough() {
        ErpCustomerAreaSaveReqVO reqVO = new ErpCustomerAreaSaveReqVO();
        when(areaService.createArea(any())).thenReturn(99L);

        CommonResult<Long> result = controller.createArea(reqVO);

        assertEquals(0, result.getCode());
        assertEquals(99L, result.getData());
        verify(areaService).createArea(eq(reqVO));
    }

    @Test
    public void testCreateArea_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerAreaController.class.getMethod("createArea", ErpCustomerAreaSaveReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:update"));
    }

    // ==================== updateArea ====================

    @Test
    public void testUpdateArea_paramPassThrough() {
        ErpCustomerAreaSaveReqVO reqVO = new ErpCustomerAreaSaveReqVO();

        CommonResult<Boolean> result = controller.updateArea(reqVO);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(areaService).updateArea(eq(reqVO));
    }

    @Test
    public void testUpdateArea_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerAreaController.class.getMethod("updateArea", ErpCustomerAreaSaveReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:update"));
    }

    // ==================== deleteArea ====================

    @Test
    public void testDeleteArea_paramPassThrough() {
        CommonResult<Boolean> result = controller.deleteArea(1L);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(areaService).deleteArea(eq(1L));
    }

    @Test
    public void testDeleteArea_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerAreaController.class.getMethod("deleteArea", Long.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:update"));
    }

    // ==================== getArea ====================

    @Test
    public void testGetArea_paramPassThrough() {
        ErpCustomerAreaDO area = new ErpCustomerAreaDO();
        area.setId(1L);
        area.setCustomerId(100L);
        area.setDetailAddress("test address");
        when(areaService.getArea(eq(1L))).thenReturn(area);

        CommonResult<ErpCustomerAreaRespVO> result = controller.getArea(1L);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getId());
        assertEquals("test address", result.getData().getDetailAddress());
    }

    @Test
    public void testGetArea_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerAreaController.class.getMethod("getArea", Long.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:query"));
    }

    // ==================== getAreaPage ====================

    @Test
    public void testGetAreaPage_paramPassThrough() {
        ErpCustomerAreaPageReqVO pageReqVO = new ErpCustomerAreaPageReqVO();
        ErpCustomerAreaDO area = new ErpCustomerAreaDO();
        area.setId(1L);
        PageResult<ErpCustomerAreaDO> pageResult = new PageResult<>(Arrays.asList(area), 1L);
        when(areaService.getAreaPage(any())).thenReturn(pageResult);

        CommonResult<PageResult<ErpCustomerAreaRespVO>> result = controller.getAreaPage(pageReqVO);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getTotal());
        assertEquals(1, result.getData().getList().size());
    }

    @Test
    public void testGetAreaPage_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerAreaController.class.getMethod("getAreaPage", ErpCustomerAreaPageReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:query"));
    }

    // ==================== getAreaListByCustomer ====================

    @Test
    public void testGetAreaListByCustomer_paramPassThrough() {
        ErpCustomerAreaDO area = new ErpCustomerAreaDO();
        area.setId(1L);
        area.setCustomerId(100L);
        when(areaService.getAreaListByCustomerId(eq(100L))).thenReturn(Arrays.asList(area));

        CommonResult<List<ErpCustomerAreaRespVO>> result = controller.getAreaListByCustomer(100L);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
        assertEquals(100L, result.getData().get(0).getCustomerId());
    }

    @Test
    public void testGetAreaListByCustomer_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerAreaController.class.getMethod("getAreaListByCustomer", Long.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:query"));
    }

}
