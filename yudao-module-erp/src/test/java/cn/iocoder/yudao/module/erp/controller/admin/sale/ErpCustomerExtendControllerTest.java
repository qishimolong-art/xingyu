package cn.iocoder.yudao.module.erp.controller.admin.sale;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerextend.ErpCustomerExtendPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerextend.ErpCustomerExtendRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerextend.ErpCustomerExtendSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerExtendDO;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerExtendService;
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
 * {@link ErpCustomerExtendController} 的单元测试
 */
public class ErpCustomerExtendControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpCustomerExtendController controller;

    @Mock
    private ErpCustomerExtendService extendService;

    @Test
    public void testCreateExtend_paramPassThrough() {
        ErpCustomerExtendSaveReqVO reqVO = new ErpCustomerExtendSaveReqVO();
        reqVO.setCustomerId(1L);
        reqVO.setExtendKey("key1");
        when(extendService.createExtend(any())).thenReturn(99L);

        CommonResult<Long> result = controller.createExtend(reqVO);

        assertEquals(0, result.getCode());
        assertEquals(99L, result.getData());
        verify(extendService).createExtend(eq(reqVO));
    }

    @Test
    public void testCreateExtend_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerExtendController.class.getMethod("createExtend", ErpCustomerExtendSaveReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:update"));
    }

    @Test
    public void testUpdateExtend_paramPassThrough() {
        ErpCustomerExtendSaveReqVO reqVO = new ErpCustomerExtendSaveReqVO();
        reqVO.setId(10L);
        reqVO.setCustomerId(1L);
        reqVO.setExtendKey("key1");

        CommonResult<Boolean> result = controller.updateExtend(reqVO);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(extendService).updateExtend(eq(reqVO));
    }

    @Test
    public void testUpdateExtend_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerExtendController.class.getMethod("updateExtend", ErpCustomerExtendSaveReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:update"));
    }

    @Test
    public void testDeleteExtend_paramPassThrough() {
        CommonResult<Boolean> result = controller.deleteExtend(77L);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(extendService).deleteExtend(eq(77L));
    }

    @Test
    public void testDeleteExtend_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerExtendController.class.getMethod("deleteExtend", Long.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:update"));
    }

    @Test
    public void testGetExtend_paramPassThrough() {
        ErpCustomerExtendDO extend = new ErpCustomerExtendDO();
        extend.setId(55L);
        extend.setCustomerId(1L);
        extend.setExtendKey("key1");
        when(extendService.getExtend(eq(55L))).thenReturn(extend);

        CommonResult<ErpCustomerExtendRespVO> result = controller.getExtend(55L);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(55L, result.getData().getId());
        verify(extendService).getExtend(eq(55L));
    }

    @Test
    public void testGetExtend_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerExtendController.class.getMethod("getExtend", Long.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:query"));
    }

    @Test
    public void testGetExtendPage_paramPassThrough() {
        ErpCustomerExtendPageReqVO pageReqVO = new ErpCustomerExtendPageReqVO();
        pageReqVO.setCustomerId(1L);
        PageResult<ErpCustomerExtendDO> pageResult = new PageResult<>(Collections.emptyList(), 0L);
        when(extendService.getExtendPage(eq(pageReqVO))).thenReturn(pageResult);

        CommonResult<PageResult<ErpCustomerExtendRespVO>> result = controller.getExtendPage(pageReqVO);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(0L, result.getData().getTotal());
        verify(extendService).getExtendPage(eq(pageReqVO));
    }

    @Test
    public void testGetExtendPage_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerExtendController.class.getMethod("getExtendPage", ErpCustomerExtendPageReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:query"));
    }

    @Test
    public void testGetExtendListByCustomer_paramPassThrough() {
        List<ErpCustomerExtendDO> list = Collections.singletonList(new ErpCustomerExtendDO());
        when(extendService.getExtendListByCustomerId(eq(1L))).thenReturn(list);

        CommonResult<List<ErpCustomerExtendRespVO>> result = controller.getExtendListByCustomer(1L);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
        verify(extendService).getExtendListByCustomerId(eq(1L));
    }

    @Test
    public void testGetExtendListByCustomer_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerExtendController.class.getMethod("getExtendListByCustomer", Long.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:query"));
    }

}
