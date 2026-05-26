package cn.iocoder.yudao.module.erp.controller.admin.sale;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerimage.ErpCustomerImagePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerimage.ErpCustomerImageRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerimage.ErpCustomerImageSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerImageDO;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerImageService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ErpCustomerImageController} 的单元测试
 */
public class ErpCustomerImageControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpCustomerImageController controller;

    @Mock
    private ErpCustomerImageService imageService;

    @Test
    public void testCreateImage_paramPassThrough() {
        ErpCustomerImageSaveReqVO reqVO = new ErpCustomerImageSaveReqVO();
        reqVO.setCustomerId(1L);
        reqVO.setImageUrl("http://x/a.jpg");
        when(imageService.createImage(any())).thenReturn(99L);

        CommonResult<Long> result = controller.createImage(reqVO);

        assertEquals(0, result.getCode());
        assertEquals(99L, result.getData());
        verify(imageService).createImage(eq(reqVO));
    }

    @Test
    public void testCreateImage_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerImageController.class.getMethod("createImage", ErpCustomerImageSaveReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:update"));
    }

    @Test
    public void testUpdateImage_paramPassThrough() {
        ErpCustomerImageSaveReqVO reqVO = new ErpCustomerImageSaveReqVO();
        reqVO.setId(10L);
        reqVO.setCustomerId(1L);
        reqVO.setImageUrl("http://x/a.jpg");

        CommonResult<Boolean> result = controller.updateImage(reqVO);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(imageService).updateImage(eq(reqVO));
    }

    @Test
    public void testUpdateImage_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerImageController.class.getMethod("updateImage", ErpCustomerImageSaveReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:update"));
    }

    @Test
    public void testDeleteImage_paramPassThrough() {
        CommonResult<Boolean> result = controller.deleteImage(77L);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(imageService).deleteImage(eq(77L));
    }

    @Test
    public void testDeleteImage_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerImageController.class.getMethod("deleteImage", Long.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:update"));
    }

    @Test
    public void testGetImage_paramPassThrough() {
        ErpCustomerImageDO image = new ErpCustomerImageDO();
        image.setId(55L);
        image.setCustomerId(1L);
        image.setImageUrl("http://x/a.jpg");
        when(imageService.getImage(eq(55L))).thenReturn(image);

        CommonResult<ErpCustomerImageRespVO> result = controller.getImage(55L);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(55L, result.getData().getId());
        verify(imageService).getImage(eq(55L));
    }

    @Test
    public void testGetImage_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerImageController.class.getMethod("getImage", Long.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:query"));
    }

    @Test
    public void testGetImagePage_paramPassThrough() {
        ErpCustomerImagePageReqVO pageReqVO = new ErpCustomerImagePageReqVO();
        pageReqVO.setCustomerId(1L);
        PageResult<ErpCustomerImageDO> pageResult = new PageResult<>(Collections.emptyList(), 0L);
        when(imageService.getImagePage(eq(pageReqVO))).thenReturn(pageResult);

        CommonResult<PageResult<ErpCustomerImageRespVO>> result = controller.getImagePage(pageReqVO);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(0L, result.getData().getTotal());
        verify(imageService).getImagePage(eq(pageReqVO));
    }

    @Test
    public void testGetImagePage_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerImageController.class.getMethod("getImagePage", ErpCustomerImagePageReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:query"));
    }

    @Test
    public void testGetImageListByCustomer_paramPassThrough() {
        List<ErpCustomerImageDO> list = Collections.singletonList(new ErpCustomerImageDO());
        when(imageService.getImageListByCustomerId(eq(1L))).thenReturn(list);

        CommonResult<List<ErpCustomerImageRespVO>> result = controller.getImageListByCustomer(1L);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
        verify(imageService).getImageListByCustomerId(eq(1L));
    }

    @Test
    public void testGetImageListByCustomer_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerImageController.class.getMethod("getImageListByCustomer", Long.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:query"));
    }

}
