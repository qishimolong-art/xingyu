package cn.iocoder.yudao.module.erp.controller.admin.sale;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.config.ErpSaleConfigPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.config.ErpSaleConfigRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.config.ErpSaleConfigSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleConfigDO;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleConfigService;
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

/**
 * {@link ErpSaleConfigController} 的单元测试
 */
public class ErpSaleConfigControllerTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpSaleConfigController controller;

    @Mock
    private ErpSaleConfigService saleConfigService;

    @Test
    public void testCreateSaleConfig_paramPassThrough() {
        ErpSaleConfigSaveReqVO reqVO = new ErpSaleConfigSaveReqVO();
        reqVO.setConfigType("CONTRACT_TYPE");
        reqVO.setCode("annual");
        reqVO.setName("年度合同");
        reqVO.setStatus(0);
        when(saleConfigService.createSaleConfig(any())).thenReturn(99L);

        CommonResult<Long> result = controller.createSaleConfig(reqVO);

        assertEquals(0, result.getCode());
        assertEquals(99L, result.getData());
        verify(saleConfigService).createSaleConfig(eq(reqVO));
    }

    @Test
    public void testCreateSaleConfig_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleConfigController.class.getMethod("createSaleConfig", ErpSaleConfigSaveReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-config:create"));
    }

    @Test
    public void testUpdateSaleConfig_paramPassThrough() {
        ErpSaleConfigSaveReqVO reqVO = new ErpSaleConfigSaveReqVO();
        reqVO.setId(10L);
        reqVO.setConfigType("CONTRACT_TYPE");
        reqVO.setCode("annual");
        reqVO.setName("年度合同");
        reqVO.setStatus(0);

        CommonResult<Boolean> result = controller.updateSaleConfig(reqVO);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(saleConfigService).updateSaleConfig(eq(reqVO));
    }

    @Test
    public void testUpdateSaleConfig_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleConfigController.class.getMethod("updateSaleConfig", ErpSaleConfigSaveReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-config:update"));
    }

    @Test
    public void testDeleteSaleConfig_paramPassThrough() {
        List<Long> ids = Arrays.asList(1L, 2L, 3L);

        CommonResult<Boolean> result = controller.deleteSaleConfig(ids);

        assertEquals(0, result.getCode());
        assertTrue(result.getData());
        verify(saleConfigService).deleteSaleConfig(eq(ids));
    }

    @Test
    public void testDeleteSaleConfig_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleConfigController.class.getMethod("deleteSaleConfig", List.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-config:delete"));
    }

    @Test
    public void testGetSaleConfig_paramPassThrough() {
        ErpSaleConfigDO config = new ErpSaleConfigDO();
        config.setId(55L);
        config.setConfigType("CONTRACT_TYPE");
        config.setCode("annual");
        when(saleConfigService.getSaleConfig(eq(55L))).thenReturn(config);

        CommonResult<ErpSaleConfigRespVO> result = controller.getSaleConfig(55L);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(55L, result.getData().getId());
        verify(saleConfigService).getSaleConfig(eq(55L));
    }

    @Test
    public void testGetSaleConfig_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleConfigController.class.getMethod("getSaleConfig", Long.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-config:query"));
    }

    @Test
    public void testGetSaleConfigPage_paramPassThrough() {
        ErpSaleConfigPageReqVO pageReqVO = new ErpSaleConfigPageReqVO();
        pageReqVO.setConfigType("CONTRACT_TYPE");
        PageResult<ErpSaleConfigDO> pageResult = new PageResult<>(Collections.emptyList(), 0L);
        when(saleConfigService.getSaleConfigPage(eq(pageReqVO))).thenReturn(pageResult);

        CommonResult<PageResult<ErpSaleConfigRespVO>> result = controller.getSaleConfigPage(pageReqVO);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(0L, result.getData().getTotal());
        verify(saleConfigService).getSaleConfigPage(eq(pageReqVO));
    }

    @Test
    public void testGetSaleConfigPage_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleConfigController.class.getMethod("getSaleConfigPage", ErpSaleConfigPageReqVO.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-config:query"));
    }

    @Test
    public void testGetSaleConfigSimpleList_paramPassThrough() {
        List<ErpSaleConfigDO> list = Collections.singletonList(new ErpSaleConfigDO());
        when(saleConfigService.getSaleConfigSimpleList(eq("CONTRACT_TYPE"), eq(0))).thenReturn(list);

        CommonResult<List<ErpSaleConfigDO>> result = controller.getSaleConfigSimpleList("CONTRACT_TYPE", 0);

        assertEquals(0, result.getCode());
        assertNotNull(result.getData());
        assertEquals(1, result.getData().size());
        verify(saleConfigService).getSaleConfigSimpleList(eq("CONTRACT_TYPE"), eq(0));
    }

    @Test
    public void testGetSaleConfigSimpleList_hasPreAuthorize() throws NoSuchMethodException {
        Method method = ErpSaleConfigController.class.getMethod("getSaleConfigSimpleList", String.class, Integer.class);
        PreAuthorize anno = method.getAnnotation(PreAuthorize.class);
        assertNotNull(anno);
        assertTrue(anno.value().contains("erp:sale-config:query"));
    }

}
