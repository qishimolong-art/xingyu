package cn.iocoder.yudao.module.erp.controller.admin.sale;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutSaveReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleOutUpdateExpressFileReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out.ErpSaleReturnableItemRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleReturnItemMapper;
import cn.iocoder.yudao.module.erp.service.product.ErpProductService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleFieldPermissionMasker;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleOutService;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_OUT_EXPRESS_FILE_SIZE_EXCEEDED;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ErpSaleOutController} 的单元测试
 */
public class ErpSaleOutControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpSaleOutController controller;

    @Mock
    private ErpSaleOutService saleOutService;
    @Mock
    private ErpStockService stockService;
    @Mock
    private ErpProductService productService;
    @Mock
    private ErpCustomerService customerService;
    @Mock
    private ErpWarehouseService warehouseService;
    @Mock
    private ErpSaleReturnItemMapper saleReturnItemMapper;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private DeptApi deptApi;
    @Mock
    private ErpSaleFieldPermissionMasker fieldPermissionMasker;

    // ========== createSaleOut ==========

    @Test
    public void testCreateSaleOut_paramPassThrough() {
        ErpSaleOutSaveReqVO reqVO = new ErpSaleOutSaveReqVO();
        when(saleOutService.createSaleOut(any())).thenReturn(101L);

        CommonResult<Long> result = controller.createSaleOut(reqVO);

        assertEquals(0, result.getCode());
        assertEquals(101L, result.getData());
        verify(saleOutService).createSaleOut(eq(reqVO));
    }

    @Test
    public void testCreateSaleOut_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleOutController.class.getMethod("createSaleOut", ErpSaleOutSaveReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-out:create"));
    }

    // ========== updateSaleOut ==========

    @Test
    public void testUpdateSaleOut_paramPassThrough() {
        ErpSaleOutSaveReqVO reqVO = new ErpSaleOutSaveReqVO();
        reqVO.setId(22L);

        CommonResult<Boolean> result = controller.updateSaleOut(reqVO);

        assertEquals(0, result.getCode());
        assertEquals(Boolean.TRUE, result.getData());
        verify(saleOutService).updateSaleOut(eq(reqVO));
    }

    @Test
    public void testUpdateSaleOut_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleOutController.class.getMethod("updateSaleOut", ErpSaleOutSaveReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-out:update"));
    }

    // ========== updateSaleOutExpressFile ==========

    @Test
    public void testUpdateSaleOutExpressFile_paramPassThrough() {
        ErpSaleOutUpdateExpressFileReqVO reqVO = new ErpSaleOutUpdateExpressFileReqVO();
        reqVO.setId(22L);
        reqVO.setExpressFileUrl("https://example.com/express.jpg");

        CommonResult<Boolean> result = controller.updateSaleOutExpressFile(reqVO);

        assertEquals(0, result.getCode());
        assertEquals(Boolean.TRUE, result.getData());
        verify(saleOutService).updateSaleOutExpressFile(eq(reqVO));
    }

    @Test
    public void testUpdateSaleOutExpressFile_hasDedicatedPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleOutController.class.getMethod(
                "updateSaleOutExpressFile", ErpSaleOutUpdateExpressFileReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-out:upload-express"));
    }

    // ========== uploadSaleOutExpressFile ==========

    @Test
    public void testUploadSaleOutExpressFile_paramPassThrough() throws Exception {
        byte[] content = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xD9};
        MockMultipartFile file = new MockMultipartFile(
                "file", "express.jpg", "image/jpeg", content);
        when(saleOutService.uploadSaleOutExpressFile(eq(22L), any(byte[].class), eq("express.jpg")))
                .thenReturn("https://example.com/express.jpg");

        CommonResult<String> result = controller.uploadSaleOutExpressFile(22L, file);

        assertEquals(0, result.getCode());
        assertEquals("https://example.com/express.jpg", result.getData());
        verify(saleOutService).uploadSaleOutExpressFile(eq(22L),
                argThat(bytes -> Arrays.equals(content, bytes)), eq("express.jpg"));
    }

    @Test
    public void testUploadSaleOutExpressFile_hasDedicatedMappingAndPermission() throws NoSuchMethodException {
        Method method = ErpSaleOutController.class.getMethod(
                "uploadSaleOutExpressFile", Long.class, MultipartFile.class);
        PostMapping mapping = method.getAnnotation(PostMapping.class);
        assertNotNull(mapping);
        assertTrue(Arrays.asList(mapping.value()).contains("/upload-express-file"));
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize);
        assertTrue(preAuthorize.value().contains("erp:sale-out:upload-express"));
    }

    @Test
    public void testUploadSaleOutExpressFile_rejectsOversizedFileBeforeReading() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "express.jpg", "image/jpeg", new byte[5 * 1024 * 1024 + 1]);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> controller.uploadSaleOutExpressFile(22L, file));

        assertEquals(SALE_OUT_EXPRESS_FILE_SIZE_EXCEEDED.getCode(), exception.getCode());
        verify(saleOutService, never()).uploadSaleOutExpressFile(any(), any(byte[].class), anyString());
    }

    // ========== updateSaleOutStatus ==========

    @Test
    public void testUpdateSaleOutStatus_paramPassThrough() {
        CommonResult<Boolean> result = controller.updateSaleOutStatus(22L, 20);

        assertEquals(0, result.getCode());
        assertEquals(Boolean.TRUE, result.getData());
        verify(saleOutService).updateSaleOutStatus(eq(22L), eq(20));
    }

    @Test
    public void testUpdateSaleOutStatus_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleOutController.class.getMethod("updateSaleOutStatus", Long.class, Integer.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-out:update-status"));
    }

    // ========== deleteSaleOut ==========

    @Test
    public void testDeleteSaleOut_paramPassThrough() {
        List<Long> ids = Arrays.asList(10L, 20L);

        CommonResult<Boolean> result = controller.deleteSaleOut(ids);

        assertEquals(0, result.getCode());
        assertEquals(Boolean.TRUE, result.getData());
        verify(saleOutService).deleteSaleOut(eq(ids));
    }

    @Test
    public void testDeleteSaleOut_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleOutController.class.getMethod("deleteSaleOut", List.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-out:delete"));
    }

    // ========== getSaleOut ==========

    @Test
    public void testGetSaleOut_nullShortCircuit() {
        when(saleOutService.getSaleOut(eq(1024L))).thenReturn(null);

        CommonResult<ErpSaleOutRespVO> result = controller.getSaleOut(1024L);

        assertEquals(0, result.getCode());
        assertNull(result.getData());
        verify(saleOutService).getSaleOut(eq(1024L));
    }

    @Test
    public void testGetSaleOut_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleOutController.class.getMethod("getSaleOut", Long.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-out:query"));
    }

    // ========== getSaleOutPage ==========

    @Test
    public void testGetSaleOutPage_emptyShortCircuit() {
        ErpSaleOutPageReqVO reqVO = new ErpSaleOutPageReqVO();
        PageResult<ErpSaleOutDO> emptyPage = new PageResult<>(new ArrayList<>(), 0L);
        when(saleOutService.getSaleOutPage(eq(reqVO))).thenReturn(emptyPage);

        CommonResult<PageResult<ErpSaleOutRespVO>> result = controller.getSaleOutPage(reqVO);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(0L, result.getData().getTotal());
        assertTrue(result.getData().getList().isEmpty());
        verify(saleOutService).getSaleOutPage(eq(reqVO));
    }

    @Test
    public void testGetSaleOutPage_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleOutController.class.getMethod("getSaleOutPage", ErpSaleOutPageReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-out:query"));
    }

    // ========== getReturnableItems ==========

    @Test
    public void testGetReturnableItems_paramPassThrough() {
        List<ErpSaleReturnableItemRespVO> mockItems = Collections.singletonList(new ErpSaleReturnableItemRespVO());
        when(saleOutService.getReturnableItemsByOutId(eq(17386L))).thenReturn(mockItems);

        CommonResult<List<ErpSaleReturnableItemRespVO>> result = controller.getReturnableItems(17386L);

        assertEquals(0, result.getCode());
        assertEquals(mockItems, result.getData());
        verify(saleOutService).getReturnableItemsByOutId(eq(17386L));
    }

    @Test
    public void testGetReturnableItems_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleOutController.class.getMethod("getReturnableItems", Long.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-return:create"));
    }

    // ========== exportSaleOutExcel ==========

    @Test
    public void testExportSaleOutExcel_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleOutController.class.getMethod("exportSaleOutExcel",
                ErpSaleOutPageReqVO.class, javax.servlet.http.HttpServletResponse.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-out:export"));
    }

}
