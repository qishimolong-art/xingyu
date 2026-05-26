package cn.iocoder.yudao.module.erp.controller.admin.sale;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerBatchUpdateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.ErpCustomerSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleOutMapper;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ErpCustomerControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpCustomerController controller;

    @Mock
    private ErpCustomerService customerService;

    @Mock
    private ErpSaleOutMapper saleOutMapper;

    // ==================== createCustomer ====================

    @Test
    public void testCreateCustomer_paramPassThrough() {
        ErpCustomerSaveReqVO reqVO = new ErpCustomerSaveReqVO();
        when(customerService.createCustomer(any())).thenReturn(99L);

        CommonResult<Long> result = controller.createCustomer(reqVO);

        assertEquals(0, result.getCode());
        assertEquals(99L, result.getData());
        verify(customerService).createCustomer(eq(reqVO));
    }

    @Test
    public void testCreateCustomer_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerController.class.getMethod("createCustomer", ErpCustomerSaveReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:create"));
    }

    // ==================== updateCustomer ====================

    @Test
    public void testUpdateCustomer_paramPassThrough() {
        ErpCustomerSaveReqVO reqVO = new ErpCustomerSaveReqVO();

        CommonResult<Boolean> result = controller.updateCustomer(reqVO);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(customerService).updateCustomer(eq(reqVO));
    }

    @Test
    public void testUpdateCustomer_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerController.class.getMethod("updateCustomer", ErpCustomerSaveReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:update"));
    }

    // ==================== deleteCustomer ====================

    @Test
    public void testDeleteCustomer_paramPassThrough() {
        CommonResult<Boolean> result = controller.deleteCustomer(1L);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(customerService).deleteCustomer(eq(1L));
    }

    @Test
    public void testDeleteCustomer_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerController.class.getMethod("deleteCustomer", Long.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:delete"));
    }

    // ==================== getCustomer ====================

    @Test
    public void testGetCustomer_paramPassThrough() {
        ErpCustomerDO customer = new ErpCustomerDO();
        customer.setId(1L);
        customer.setName("test");
        when(customerService.getCustomer(eq(1L))).thenReturn(customer);

        CommonResult<ErpCustomerRespVO> result = controller.getCustomer(1L);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getId());
        assertEquals("test", result.getData().getName());
    }

    @Test
    public void testGetCustomer_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerController.class.getMethod("getCustomer", Long.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:query"));
    }

    // ==================== getCustomerPage ====================

    @Test
    public void testGetCustomerPage_paramPassThrough() {
        ErpCustomerPageReqVO pageReqVO = new ErpCustomerPageReqVO();
        ErpCustomerDO customer = new ErpCustomerDO();
        customer.setId(1L);
        customer.setName("test");
        PageResult<ErpCustomerDO> pageResult = new PageResult<>(Arrays.asList(customer), 1L);
        when(customerService.getCustomerPage(any())).thenReturn(pageResult);
        when(saleOutMapper.selectSaleStatsByCustomerIds(any())).thenReturn(Collections.emptyList());

        CommonResult<PageResult<ErpCustomerRespVO>> result = controller.getCustomerPage(pageReqVO);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1L, result.getData().getTotal());
        assertEquals(1, result.getData().getList().size());
    }

    @Test
    public void testGetCustomerPage_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerController.class.getMethod("getCustomerPage", ErpCustomerPageReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:query"));
    }

    // ==================== getCustomerSimpleList ====================

    @Test
    public void testGetCustomerSimpleList_paramPassThrough() {
        ErpCustomerDO customer = new ErpCustomerDO();
        customer.setId(1L);
        customer.setName("test");
        customer.setContact("contact1");
        customer.setMobile("13800138000");
        when(customerService.getCustomerListByStatus(any())).thenReturn(Arrays.asList(customer));

        CommonResult<List<ErpCustomerRespVO>> result = controller.getCustomerSimpleList();

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
        assertEquals("test", result.getData().get(0).getName());
    }

    // ==================== exportCustomerExcel ====================

    @Test
    public void testExportCustomerExcel_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerController.class.getMethod("exportCustomerExcel", ErpCustomerPageReqVO.class,
                javax.servlet.http.HttpServletResponse.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:export"));
    }

    // ==================== importTemplate ====================

    @Test
    public void testImportTemplate_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerController.class.getMethod("importTemplate", javax.servlet.http.HttpServletResponse.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:import"));
    }

    // ==================== importCustomer ====================

    @Test
    public void testImportCustomer_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerController.class.getMethod("importCustomer",
                org.springframework.web.multipart.MultipartFile.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:import"));
    }

    // ==================== batchUpdateCustomer ====================

    @Test
    public void testBatchUpdateCustomer_paramPassThrough() {
        ErpCustomerBatchUpdateReqVO reqVO = new ErpCustomerBatchUpdateReqVO();

        CommonResult<Boolean> result = controller.batchUpdateCustomer(reqVO);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(customerService).batchUpdateCustomer(eq(reqVO));
    }

    @Test
    public void testBatchUpdateCustomer_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerController.class.getMethod("batchUpdateCustomer", ErpCustomerBatchUpdateReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:update"));
    }

}
