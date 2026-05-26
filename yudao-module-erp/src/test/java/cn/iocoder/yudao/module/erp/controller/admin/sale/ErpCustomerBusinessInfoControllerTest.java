package cn.iocoder.yudao.module.erp.controller.admin.sale;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerbusinessinfo.ErpCustomerBusinessInfoPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerbusinessinfo.ErpCustomerBusinessInfoRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerbusinessinfo.ErpCustomerBusinessInfoSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerBusinessInfoDO;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerBusinessInfoService;
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
 * {@link ErpCustomerBusinessInfoController} 的单元测试
 */
public class ErpCustomerBusinessInfoControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpCustomerBusinessInfoController controller;

    @Mock
    private ErpCustomerBusinessInfoService businessInfoService;

    @Test
    public void testCreateBusinessInfo_paramPassThrough() {
        ErpCustomerBusinessInfoSaveReqVO reqVO = new ErpCustomerBusinessInfoSaveReqVO();
        reqVO.setCustomerId(1L);
        reqVO.setCreditCode("91110000000000000X");
        when(businessInfoService.createBusinessInfo(any())).thenReturn(99L);

        CommonResult<Long> result = controller.createBusinessInfo(reqVO);

        assertEquals(0, result.getCode());
        assertEquals(99L, result.getData());
        verify(businessInfoService).createBusinessInfo(eq(reqVO));
    }

    @Test
    public void testCreateBusinessInfo_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerBusinessInfoController.class.getMethod("createBusinessInfo", ErpCustomerBusinessInfoSaveReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:update"));
    }

    @Test
    public void testUpdateBusinessInfo_paramPassThrough() {
        ErpCustomerBusinessInfoSaveReqVO reqVO = new ErpCustomerBusinessInfoSaveReqVO();
        reqVO.setId(10L);
        reqVO.setCustomerId(1L);

        CommonResult<Boolean> result = controller.updateBusinessInfo(reqVO);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(businessInfoService).updateBusinessInfo(eq(reqVO));
    }

    @Test
    public void testUpdateBusinessInfo_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerBusinessInfoController.class.getMethod("updateBusinessInfo", ErpCustomerBusinessInfoSaveReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:update"));
    }

    @Test
    public void testDeleteBusinessInfo_paramPassThrough() {
        CommonResult<Boolean> result = controller.deleteBusinessInfo(77L);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(businessInfoService).deleteBusinessInfo(eq(77L));
    }

    @Test
    public void testDeleteBusinessInfo_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerBusinessInfoController.class.getMethod("deleteBusinessInfo", Long.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:update"));
    }

    @Test
    public void testGetBusinessInfo_paramPassThrough() {
        ErpCustomerBusinessInfoDO info = new ErpCustomerBusinessInfoDO();
        info.setId(55L);
        info.setCustomerId(1L);
        info.setCreditCode("91110000000000000X");
        when(businessInfoService.getBusinessInfo(eq(55L))).thenReturn(info);

        CommonResult<ErpCustomerBusinessInfoRespVO> result = controller.getBusinessInfo(55L);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(55L, result.getData().getId());
        verify(businessInfoService).getBusinessInfo(eq(55L));
    }

    @Test
    public void testGetBusinessInfo_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerBusinessInfoController.class.getMethod("getBusinessInfo", Long.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:query"));
    }

    @Test
    public void testGetBusinessInfoPage_paramPassThrough() {
        ErpCustomerBusinessInfoPageReqVO pageReqVO = new ErpCustomerBusinessInfoPageReqVO();
        pageReqVO.setCustomerId(1L);
        PageResult<ErpCustomerBusinessInfoDO> pageResult = new PageResult<>(Collections.emptyList(), 0L);
        when(businessInfoService.getBusinessInfoPage(eq(pageReqVO))).thenReturn(pageResult);

        CommonResult<PageResult<ErpCustomerBusinessInfoRespVO>> result = controller.getBusinessInfoPage(pageReqVO);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(0L, result.getData().getTotal());
        verify(businessInfoService).getBusinessInfoPage(eq(pageReqVO));
    }

    @Test
    public void testGetBusinessInfoPage_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerBusinessInfoController.class.getMethod("getBusinessInfoPage", ErpCustomerBusinessInfoPageReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:query"));
    }

    @Test
    public void testGetBusinessInfoListByCustomer_paramPassThrough() {
        List<ErpCustomerBusinessInfoDO> list = Collections.singletonList(new ErpCustomerBusinessInfoDO());
        when(businessInfoService.getBusinessInfoListByCustomerId(eq(1L))).thenReturn(list);

        CommonResult<List<ErpCustomerBusinessInfoRespVO>> result = controller.getBusinessInfoListByCustomer(1L);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
        verify(businessInfoService).getBusinessInfoListByCustomerId(eq(1L));
    }

    @Test
    public void testGetBusinessInfoListByCustomer_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerBusinessInfoController.class.getMethod("getBusinessInfoListByCustomer", Long.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:query"));
    }

}
