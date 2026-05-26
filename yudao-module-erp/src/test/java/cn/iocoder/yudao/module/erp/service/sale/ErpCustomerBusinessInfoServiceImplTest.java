package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerbusinessinfo.ErpCustomerBusinessInfoPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerbusinessinfo.ErpCustomerBusinessInfoSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerBusinessInfoDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpCustomerBusinessInfoMapper;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_BUSINESS_INFO_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link ErpCustomerBusinessInfoServiceImpl} 的单元测试
 */
public class ErpCustomerBusinessInfoServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpCustomerBusinessInfoServiceImpl businessInfoService;

    @Mock
    private ErpCustomerBusinessInfoMapper businessInfoMapper;
    @Mock
    private ErpCustomerService customerService;

    // ==================== create ====================

    @Test
    public void testCreateBusinessInfo_normalCase_returnId() {
        ErpCustomerBusinessInfoSaveReqVO reqVO = buildSaveReq(100L);
        doAnswer(invocation -> {
            ErpCustomerBusinessInfoDO businessInfo = invocation.getArgument(0);
            businessInfo.setId(999L);
            return 1;
        }).when(businessInfoMapper).insert(any(ErpCustomerBusinessInfoDO.class));

        Long id = businessInfoService.createBusinessInfo(reqVO);

        assertEquals(999L, id);
        verify(customerService).validateCustomer(eq(100L));
        verify(businessInfoMapper).insert(argThat((ErpCustomerBusinessInfoDO info) ->
                Long.valueOf(100L).equals(info.getCustomerId())
                        && "91110000000000000X".equals(info.getCreditCode())
                        && "张三".equals(info.getLegalPerson())));
    }

    @Test
    public void testCreateBusinessInfo_invalidCustomer_throwException() {
        ErpCustomerBusinessInfoSaveReqVO reqVO = buildSaveReq(101L);
        doThrow(new ServiceException(CUSTOMER_NOT_EXISTS))
                .when(customerService).validateCustomer(eq(101L));

        assertServiceException(() -> businessInfoService.createBusinessInfo(reqVO), CUSTOMER_NOT_EXISTS);
        verify(businessInfoMapper, never()).insert(any(ErpCustomerBusinessInfoDO.class));
    }

    // ==================== update ====================

    @Test
    public void testUpdateBusinessInfo_normalCase_success() {
        ErpCustomerBusinessInfoSaveReqVO reqVO = buildSaveReq(100L);
        reqVO.setId(50L);
        reqVO.setLegalPerson("李四（修改）");
        when(businessInfoMapper.selectById(eq(50L)))
                .thenReturn(new ErpCustomerBusinessInfoDO().setId(50L).setCustomerId(100L));

        businessInfoService.updateBusinessInfo(reqVO);

        verify(customerService).validateCustomer(eq(100L));
        verify(businessInfoMapper).updateById(argThat((ErpCustomerBusinessInfoDO info) ->
                Long.valueOf(50L).equals(info.getId())
                        && "李四（修改）".equals(info.getLegalPerson())));
    }

    @Test
    public void testUpdateBusinessInfo_notExists_throwException() {
        ErpCustomerBusinessInfoSaveReqVO reqVO = buildSaveReq(100L);
        reqVO.setId(999L);
        when(businessInfoMapper.selectById(eq(999L))).thenReturn(null);

        assertServiceException(() -> businessInfoService.updateBusinessInfo(reqVO),
                CUSTOMER_BUSINESS_INFO_NOT_EXISTS);
        verify(customerService, never()).validateCustomer(any());
        verify(businessInfoMapper, never()).updateById(any(ErpCustomerBusinessInfoDO.class));
    }

    // ==================== delete ====================

    @Test
    public void testDeleteBusinessInfo_normalCase_success() {
        when(businessInfoMapper.selectById(eq(10L)))
                .thenReturn(new ErpCustomerBusinessInfoDO().setId(10L));

        businessInfoService.deleteBusinessInfo(10L);

        verify(businessInfoMapper).deleteById(eq(10L));
    }

    @Test
    public void testDeleteBusinessInfo_notExists_throwException() {
        when(businessInfoMapper.selectById(eq(999L))).thenReturn(null);

        assertServiceException(() -> businessInfoService.deleteBusinessInfo(999L),
                CUSTOMER_BUSINESS_INFO_NOT_EXISTS);
        verify(businessInfoMapper, never()).deleteById(anyLong());
    }

    // ==================== get ====================

    @Test
    public void testGetBusinessInfo_normalCase_returnDO() {
        ErpCustomerBusinessInfoDO expected = new ErpCustomerBusinessInfoDO()
                .setId(7L).setCustomerId(100L).setCreditCode("CC123");
        when(businessInfoMapper.selectById(eq(7L))).thenReturn(expected);

        ErpCustomerBusinessInfoDO result = businessInfoService.getBusinessInfo(7L);

        assertNotNull(result);
        assertEquals(7L, result.getId());
        assertEquals("CC123", result.getCreditCode());
    }

    @Test
    public void testGetBusinessInfo_notExists_returnNull() {
        when(businessInfoMapper.selectById(eq(888L))).thenReturn(null);

        ErpCustomerBusinessInfoDO result = businessInfoService.getBusinessInfo(888L);

        assertNull(result);
    }

    // ==================== page ====================

    @Test
    public void testGetBusinessInfoPage_normalCase_returnPage() {
        ErpCustomerBusinessInfoPageReqVO pageReqVO = new ErpCustomerBusinessInfoPageReqVO();
        pageReqVO.setCustomerId(100L);
        PageResult<ErpCustomerBusinessInfoDO> expected = new PageResult<>(
                Collections.singletonList(new ErpCustomerBusinessInfoDO().setId(1L).setCustomerId(100L)), 1L);
        when(businessInfoMapper.selectPage(any(ErpCustomerBusinessInfoPageReqVO.class))).thenReturn(expected);

        PageResult<ErpCustomerBusinessInfoDO> result = businessInfoService.getBusinessInfoPage(pageReqVO);

        assertNotNull(result);
        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getList().size());
    }

    // ==================== listByCustomerId ====================

    @Test
    public void testGetBusinessInfoListByCustomerId_normalCase_returnList() {
        List<ErpCustomerBusinessInfoDO> expected = Arrays.asList(
                new ErpCustomerBusinessInfoDO().setId(1L).setCustomerId(100L),
                new ErpCustomerBusinessInfoDO().setId(2L).setCustomerId(100L)
        );
        when(businessInfoMapper.selectListByCustomerId(eq(100L))).thenReturn(expected);

        List<ErpCustomerBusinessInfoDO> result = businessInfoService.getBusinessInfoListByCustomerId(100L);

        assertEquals(2, result.size());
        verify(customerService).validateCustomer(eq(100L));
        verify(businessInfoMapper).selectListByCustomerId(eq(100L));
    }

    @Test
    public void testGetBusinessInfoListByCustomerId_invalidCustomer_throwException() {
        doThrow(new ServiceException(CUSTOMER_NOT_EXISTS))
                .when(customerService).validateCustomer(eq(999L));

        assertServiceException(() -> businessInfoService.getBusinessInfoListByCustomerId(999L),
                CUSTOMER_NOT_EXISTS);
        verify(businessInfoMapper, never()).selectListByCustomerId(any());
    }

    // ==================== helper ====================

    private ErpCustomerBusinessInfoSaveReqVO buildSaveReq(Long customerId) {
        ErpCustomerBusinessInfoSaveReqVO reqVO = new ErpCustomerBusinessInfoSaveReqVO();
        reqVO.setCustomerId(customerId);
        reqVO.setCreditCode("91110000000000000X");
        reqVO.setLegalPerson("张三");
        reqVO.setRegisteredCapital("100万");
        reqVO.setEstablishDate("2020-01-01");
        reqVO.setBusinessStatus("存续");
        reqVO.setBusinessScope("软件开发");
        reqVO.setRemark("测试");
        return reqVO;
    }

}
