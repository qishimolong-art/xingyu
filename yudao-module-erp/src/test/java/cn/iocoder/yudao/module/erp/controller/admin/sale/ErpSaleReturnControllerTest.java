package cn.iocoder.yudao.module.erp.controller.admin.sale;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns.ErpSaleReturnSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleReturnDO;
import cn.iocoder.yudao.module.erp.service.config.ErpFieldConfigService;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleReturnService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ErpSaleReturnController} 的单元测试
 */
public class ErpSaleReturnControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpSaleReturnController controller;

    @Mock
    private ErpSaleReturnService saleReturnService;
    @Mock
    private ErpStockService stockService;
    @Mock
    private ErpProductService productService;
    @Mock
    private ErpCustomerService customerService;
    @Mock
    private ErpWarehouseService warehouseService;
    @Mock
    private ErpSaleFieldPermissionMasker fieldPermissionMasker;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private DeptApi deptApi;
    @Mock
    private ErpFieldConfigService fieldConfigService;

    // ========== createSaleReturn ==========

    @Test
    public void testCreateSaleReturn_paramPassThrough() {
        ErpSaleReturnSaveReqVO reqVO = new ErpSaleReturnSaveReqVO();
        when(saleReturnService.createSaleReturn(any())).thenReturn(202L);

        CommonResult<Long> result = controller.createSaleReturn(reqVO);

        assertEquals(0, result.getCode());
        assertEquals(202L, result.getData());
        verify(saleReturnService).createSaleReturn(eq(reqVO));
    }

    @Test
    public void testCreateSaleReturn_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleReturnController.class.getMethod("createSaleReturn", ErpSaleReturnSaveReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-return:create"));
    }

    // ========== updateSaleReturn ==========

    @Test
    public void testUpdateSaleReturn_paramPassThrough() {
        ErpSaleReturnSaveReqVO reqVO = new ErpSaleReturnSaveReqVO();
        reqVO.setId(33L);

        CommonResult<Boolean> result = controller.updateSaleReturn(reqVO);

        assertEquals(0, result.getCode());
        assertEquals(Boolean.TRUE, result.getData());
        verify(saleReturnService).updateSaleReturn(eq(reqVO));
    }

    @Test
    public void testUpdateSaleReturn_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleReturnController.class.getMethod("updateSaleReturn", ErpSaleReturnSaveReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-return:update"));
    }

    // ========== updateSaleReturnStatus ==========

    @Test
    public void testUpdateSaleReturnStatus_paramPassThrough() {
        CommonResult<Boolean> result = controller.updateSaleReturnStatus(33L, 20);

        assertEquals(0, result.getCode());
        assertEquals(Boolean.TRUE, result.getData());
        verify(saleReturnService).updateSaleReturnStatus(eq(33L), eq(20));
    }

    @Test
    public void testUpdateSaleReturnStatus_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleReturnController.class.getMethod("updateSaleReturnStatus", Long.class, Integer.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-return:update-status"));
    }

    // ========== deleteSaleReturn ==========

    @Test
    public void testDeleteSaleReturn_paramPassThrough() {
        List<Long> ids = Arrays.asList(7L, 8L, 9L);

        CommonResult<Boolean> result = controller.deleteSaleReturn(ids);

        assertEquals(0, result.getCode());
        assertEquals(Boolean.TRUE, result.getData());
        verify(saleReturnService).deleteSaleReturn(eq(ids));
    }

    @Test
    public void testDeleteSaleReturn_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleReturnController.class.getMethod("deleteSaleReturn", List.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-return:delete"));
    }

    // ========== getSaleReturn ==========

    @Test
    public void testGetSaleReturn_nullShortCircuit() {
        when(saleReturnService.getSaleReturn(eq(1024L))).thenReturn(null);

        CommonResult<ErpSaleReturnRespVO> result = controller.getSaleReturn(1024L);

        assertEquals(0, result.getCode());
        assertNull(result.getData());
        verify(saleReturnService).getSaleReturn(eq(1024L));
    }

    @Test
    public void testGetSaleReturn_withoutItems_returnsEmptyItems() {
        ErpSaleReturnDO saleReturn = new ErpSaleReturnDO();
        saleReturn.setId(1025L);
        saleReturn.setNo("XSTH1025");
        when(saleReturnService.getSaleReturn(eq(1025L))).thenReturn(saleReturn);
        when(saleReturnService.getSaleReturnItemListByReturnId(eq(1025L))).thenReturn(null);

        CommonResult<ErpSaleReturnRespVO> result = controller.getSaleReturn(1025L);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertNotNull(result.getData().getItems());
        assertTrue(result.getData().getItems().isEmpty());
    }

    @Test
    public void testGetSaleReturn_customerNameFilledByCustomerId() {
        ErpSaleReturnDO saleReturn = new ErpSaleReturnDO();
        saleReturn.setId(1026L);
        saleReturn.setCustomerId(31L);
        when(saleReturnService.getSaleReturn(eq(1026L))).thenReturn(saleReturn);
        when(saleReturnService.getSaleReturnItemListByReturnId(eq(1026L))).thenReturn(Collections.emptyList());
        when(customerService.getCustomer(eq(31L)))
                .thenReturn(new ErpCustomerDO().setId(31L).setName("客户乙"));

        CommonResult<ErpSaleReturnRespVO> result = controller.getSaleReturn(1026L);

        assertEquals(0, result.getCode());
        assertEquals("客户乙", result.getData().getCustomerName());
        verify(customerService).getCustomer(eq(31L));
    }

    @Test
    public void testGetSaleReturn_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleReturnController.class.getMethod("getSaleReturn", Long.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-return:query"));
    }

    // ========== getSaleReturnPage ==========

    @Test
    public void testGetSaleReturnPage_emptyShortCircuit() {
        ErpSaleReturnPageReqVO reqVO = new ErpSaleReturnPageReqVO();
        PageResult<ErpSaleReturnDO> emptyPage = new PageResult<>(new ArrayList<>(), 0L);
        when(saleReturnService.getSaleReturnPage(eq(reqVO))).thenReturn(emptyPage);

        CommonResult<PageResult<ErpSaleReturnRespVO>> result = controller.getSaleReturnPage(reqVO);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(0L, result.getData().getTotal());
        assertTrue(result.getData().getList().isEmpty());
        verify(saleReturnService).getSaleReturnPage(eq(reqVO));
    }

    @Test
    public void testGetSaleReturnPage_withoutItems_returnsEmptyItems() {
        ErpSaleReturnPageReqVO reqVO = new ErpSaleReturnPageReqVO();
        ErpSaleReturnDO saleReturn = new ErpSaleReturnDO();
        saleReturn.setId(1030L);
        saleReturn.setNo("XSTH1030");
        PageResult<ErpSaleReturnDO> pageResult = new PageResult<>(Collections.singletonList(saleReturn), 1L);
        when(saleReturnService.getSaleReturnPage(eq(reqVO))).thenReturn(pageResult);
        when(saleReturnService.getSaleReturnItemListByReturnIds(any())).thenReturn(null);
        when(customerService.getCustomerMap(any())).thenReturn(null);

        CommonResult<PageResult<ErpSaleReturnRespVO>> result = controller.getSaleReturnPage(reqVO);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getTotal());
        assertNotNull(result.getData().getList().get(0).getItems());
        assertTrue(result.getData().getList().get(0).getItems().isEmpty());
    }

    @Test
    public void testGetSaleReturnPage_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleReturnController.class.getMethod("getSaleReturnPage", ErpSaleReturnPageReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-return:query"));
    }

    // ========== exportSaleReturnExcel ==========

    @Test
    public void testExportSaleReturnExcel_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleReturnController.class.getMethod("exportSaleReturnExcel",
                ErpSaleReturnPageReqVO.class, String.class, javax.servlet.http.HttpServletResponse.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-return:export"));
    }

}
