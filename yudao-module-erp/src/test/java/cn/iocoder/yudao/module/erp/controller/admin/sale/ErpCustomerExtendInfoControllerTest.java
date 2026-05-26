package cn.iocoder.yudao.module.erp.controller.admin.sale;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.extend.ErpCustomerExtendInfoRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.extend.ErpCustomerExtendInfoSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerExtendInfoDO;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerExtendInfoService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ErpCustomerExtendInfoController} 的单元测试
 */
public class ErpCustomerExtendInfoControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpCustomerExtendInfoController controller;

    @Mock
    private ErpCustomerExtendInfoService extendInfoService;

    @Test
    public void testGetByCustomer_paramPassThrough() {
        ErpCustomerExtendInfoDO extendInfo = new ErpCustomerExtendInfoDO();
        extendInfo.setId(10L);
        extendInfo.setCustomerId(1L);
        when(extendInfoService.getByCustomerId(eq(1L))).thenReturn(extendInfo);

        CommonResult<ErpCustomerExtendInfoRespVO> result = controller.getByCustomer(1L);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(10L, result.getData().getId());
        verify(extendInfoService).getByCustomerId(eq(1L));
    }

    @Test
    public void testGetByCustomer_returnsNull() {
        when(extendInfoService.getByCustomerId(eq(999L))).thenReturn(null);

        CommonResult<ErpCustomerExtendInfoRespVO> result = controller.getByCustomer(999L);

        assertEquals(0, result.getCode());
        assertNull(result.getData());
        verify(extendInfoService).getByCustomerId(eq(999L));
    }

    @Test
    public void testGetByCustomer_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerExtendInfoController.class.getMethod("getByCustomer", Long.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:query"));
    }

    @Test
    public void testSave_paramPassThrough() {
        ErpCustomerExtendInfoSaveReqVO reqVO = new ErpCustomerExtendInfoSaveReqVO();
        reqVO.setCustomerId(1L);
        reqVO.setLegalPerson("张三");
        when(extendInfoService.saveExtendInfo(any())).thenReturn(88L);

        CommonResult<Long> result = controller.save(reqVO);

        assertEquals(0, result.getCode());
        assertEquals(88L, result.getData());
        verify(extendInfoService).saveExtendInfo(eq(reqVO));
    }

    @Test
    public void testSave_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpCustomerExtendInfoController.class.getMethod("save", ErpCustomerExtendInfoSaveReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:customer:update"));
    }

}
