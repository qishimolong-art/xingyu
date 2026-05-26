package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.config.ErpSaleConfigPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.config.ErpSaleConfigSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleConfigDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpSaleConfigMapper;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_CONFIG_CODE_DUPLICATE;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.SALE_CONFIG_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

public class ErpSaleConfigServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpSaleConfigServiceImpl saleConfigService;

    @Mock
    private ErpSaleConfigMapper saleConfigMapper;

    // ==================== create ====================

    @Test
    public void testCreate_sameTypeAndCodeDuplicate_throwException() {
        ErpSaleConfigSaveReqVO reqVO = buildReq("CONTRACT_TYPE", "annual");
        when(saleConfigMapper.selectByTypeAndCode(eq("CONTRACT_TYPE"), eq("annual")))
                .thenReturn(new ErpSaleConfigDO().setId(1L));

        assertServiceException(() -> saleConfigService.createSaleConfig(reqVO),
                SALE_CONFIG_CODE_DUPLICATE, "CONTRACT_TYPE", "annual");
        verify(saleConfigMapper, never()).insert(any(ErpSaleConfigDO.class));
    }

    @Test
    public void testCreate_differentTypeCanUseSameCode() {
        ErpSaleConfigSaveReqVO reqVO = buildReq("ROUTE", "annual");
        when(saleConfigMapper.selectByTypeAndCode(eq("ROUTE"), eq("annual"))).thenReturn(null);
        doAnswer(invocation -> {
            ErpSaleConfigDO config = invocation.getArgument(0);
            config.setId(99L);
            return 1;
        }).when(saleConfigMapper).insert(any(ErpSaleConfigDO.class));

        Long id = saleConfigService.createSaleConfig(reqVO);

        assertEquals(99L, id);
        verify(saleConfigMapper).insert(argThat((ErpSaleConfigDO config) ->
                "ROUTE".equals(config.getConfigType()) && "annual".equals(config.getCode())));
    }

    @Test
    public void testCreate_normalCase_success() {
        ErpSaleConfigSaveReqVO reqVO = buildReq("FREIGHT_EXPLAIN", "free_shipping");
        reqVO.setConfigValue("满100包邮");
        reqVO.setRemark("测试备注");
        when(saleConfigMapper.selectByTypeAndCode(eq("FREIGHT_EXPLAIN"), eq("free_shipping"))).thenReturn(null);
        doAnswer(invocation -> {
            ErpSaleConfigDO config = invocation.getArgument(0);
            config.setId(100L);
            return 1;
        }).when(saleConfigMapper).insert(any(ErpSaleConfigDO.class));

        Long id = saleConfigService.createSaleConfig(reqVO);

        assertEquals(100L, id);
        verify(saleConfigMapper).insert(argThat((ErpSaleConfigDO config) ->
                "FREIGHT_EXPLAIN".equals(config.getConfigType())
                        && "free_shipping".equals(config.getCode())
                        && "满100包邮".equals(config.getConfigValue())
                        && "测试配置".equals(config.getName())
                        && Integer.valueOf(0).equals(config.getStatus())
                        && Integer.valueOf(1).equals(config.getSort())));
    }

    // ==================== update ====================

    @Test
    public void testUpdate_normalCase_success() {
        ErpSaleConfigSaveReqVO reqVO = buildReq("CONTRACT_TYPE", "monthly");
        reqVO.setId(10L);
        reqVO.setName("月度合同");
        // mock: exists
        when(saleConfigMapper.selectById(eq(10L))).thenReturn(new ErpSaleConfigDO().setId(10L));
        // mock: code unique (same record)
        when(saleConfigMapper.selectByTypeAndCode(eq("CONTRACT_TYPE"), eq("monthly")))
                .thenReturn(new ErpSaleConfigDO().setId(10L));

        saleConfigService.updateSaleConfig(reqVO);

        verify(saleConfigMapper).updateById(argThat((ErpSaleConfigDO config) ->
                Long.valueOf(10L).equals(config.getId())
                        && "月度合同".equals(config.getName())
                        && "monthly".equals(config.getCode())));
    }

    @Test
    public void testUpdate_notExists_throwException() {
        ErpSaleConfigSaveReqVO reqVO = buildReq("CONTRACT_TYPE", "annual");
        reqVO.setId(999L);
        when(saleConfigMapper.selectById(eq(999L))).thenReturn(null);

        assertServiceException(() -> saleConfigService.updateSaleConfig(reqVO), SALE_CONFIG_NOT_EXISTS);
        verify(saleConfigMapper, never()).updateById(any(ErpSaleConfigDO.class));
    }

    @Test
    public void testUpdate_codeDuplicateWithOtherRecord_throwException() {
        ErpSaleConfigSaveReqVO reqVO = buildReq("CONTRACT_TYPE", "annual");
        reqVO.setId(10L);
        // mock: exists
        when(saleConfigMapper.selectById(eq(10L))).thenReturn(new ErpSaleConfigDO().setId(10L));
        // mock: code belongs to another record (id=20)
        when(saleConfigMapper.selectByTypeAndCode(eq("CONTRACT_TYPE"), eq("annual")))
                .thenReturn(new ErpSaleConfigDO().setId(20L));

        assertServiceException(() -> saleConfigService.updateSaleConfig(reqVO),
                SALE_CONFIG_CODE_DUPLICATE, "CONTRACT_TYPE", "annual");
        verify(saleConfigMapper, never()).updateById(any(ErpSaleConfigDO.class));
    }

    // ==================== delete ====================

    @Test
    public void testDelete_normalCase_success() {
        when(saleConfigMapper.selectById(eq(5L))).thenReturn(new ErpSaleConfigDO().setId(5L));

        saleConfigService.deleteSaleConfig(Collections.singletonList(5L));

        verify(saleConfigMapper).deleteByIds(eq(Collections.singletonList(5L)));
    }

    @Test
    public void testDelete_notExists_throwException() {
        when(saleConfigMapper.selectById(eq(999L))).thenReturn(null);

        assertServiceException(() -> saleConfigService.deleteSaleConfig(Collections.singletonList(999L)),
                SALE_CONFIG_NOT_EXISTS);
        verify(saleConfigMapper, never()).deleteByIds(any());
    }

    @Test
    public void testDelete_emptyList_doNothing() {
        saleConfigService.deleteSaleConfig(Collections.emptyList());

        verify(saleConfigMapper, never()).selectById(any());
        verify(saleConfigMapper, never()).deleteByIds(any());
    }

    // ==================== get ====================

    @Test
    public void testGetSaleConfig_normalCase_returnDO() {
        ErpSaleConfigDO expected = new ErpSaleConfigDO().setId(7L).setConfigType("ROUTE").setCode("r1").setName("线路1");
        when(saleConfigMapper.selectById(eq(7L))).thenReturn(expected);

        ErpSaleConfigDO result = saleConfigService.getSaleConfig(7L);

        assertNotNull(result);
        assertEquals(7L, result.getId());
        assertEquals("ROUTE", result.getConfigType());
    }

    @Test
    public void testGetSaleConfig_notExists_returnNull() {
        when(saleConfigMapper.selectById(eq(888L))).thenReturn(null);

        ErpSaleConfigDO result = saleConfigService.getSaleConfig(888L);

        assertNull(result);
    }

    // ==================== page ====================

    @Test
    public void testGetSaleConfigPage_normalCase_returnPage() {
        ErpSaleConfigPageReqVO pageReqVO = new ErpSaleConfigPageReqVO();
        pageReqVO.setConfigType("CONTRACT_TYPE");
        PageResult<ErpSaleConfigDO> expected = new PageResult<>(
                Collections.singletonList(new ErpSaleConfigDO().setId(1L)), 1L);
        when(saleConfigMapper.selectPage(any(ErpSaleConfigPageReqVO.class))).thenReturn(expected);

        PageResult<ErpSaleConfigDO> result = saleConfigService.getSaleConfigPage(pageReqVO);

        assertNotNull(result);
        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getList().size());
    }

    // ==================== simpleList ====================

    @Test
    public void testGetSaleConfigSimpleList_byType_returnList() {
        List<ErpSaleConfigDO> expected = Arrays.asList(
                new ErpSaleConfigDO().setId(1L).setConfigType("ROUTE").setCode("r1"),
                new ErpSaleConfigDO().setId(2L).setConfigType("ROUTE").setCode("r2")
        );
        when(saleConfigMapper.selectListByTypeAndStatus(eq("ROUTE"), eq(0))).thenReturn(expected);

        List<ErpSaleConfigDO> result = saleConfigService.getSaleConfigSimpleList("ROUTE", 0);

        assertNotNull(result);
        assertEquals(2, result.size());
        verify(saleConfigMapper).selectListByTypeAndStatus(eq("ROUTE"), eq(0));
    }

    // ==================== helper ====================

    private ErpSaleConfigSaveReqVO buildReq(String type, String code) {
        ErpSaleConfigSaveReqVO reqVO = new ErpSaleConfigSaveReqVO();
        reqVO.setConfigType(type);
        reqVO.setCode(code);
        reqVO.setName("测试配置");
        reqVO.setStatus(0);
        reqVO.setSort(1);
        return reqVO;
    }

}
