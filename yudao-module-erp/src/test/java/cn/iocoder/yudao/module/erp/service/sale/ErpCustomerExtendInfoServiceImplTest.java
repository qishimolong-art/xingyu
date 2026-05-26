package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customer.extend.ErpCustomerExtendInfoSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerExtendInfoDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpCustomerExtendInfoMapper;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * {@link ErpCustomerExtendInfoServiceImpl} 的单元测试
 *
 * 注意：该 Service 是 upsert 模式（按 customerId 唯一），不是常规 CRUD。
 */
public class ErpCustomerExtendInfoServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpCustomerExtendInfoServiceImpl extendInfoService;

    @Mock
    private ErpCustomerExtendInfoMapper extendInfoMapper;
    @Mock
    private ErpCustomerService customerService;

    // ==================== saveExtendInfo - insert branch ====================

    @Test
    public void testSaveExtendInfo_newRecord_insert() {
        ErpCustomerExtendInfoSaveReqVO reqVO = buildSaveReq(100L);
        // mock: customer exists
        // mock: no existing record for this customer
        when(extendInfoMapper.selectByCustomerId(eq(100L))).thenReturn(null);
        // 用 ArgumentCaptor 在 insert 被调用瞬间抓取参数（doAnswer 会回写 id）
        org.mockito.ArgumentCaptor<ErpCustomerExtendInfoDO> captor =
                org.mockito.ArgumentCaptor.forClass(ErpCustomerExtendInfoDO.class);
        doAnswer(invocation -> {
            ErpCustomerExtendInfoDO info = invocation.getArgument(0);
            // 校验：service 在 insert 前已将 id 设为 null（即使 reqVO 没传 id 也不影响）
            assertNull(info.getId());
            info.setId(999L);
            return 1;
        }).when(extendInfoMapper).insert(any(ErpCustomerExtendInfoDO.class));

        Long id = extendInfoService.saveExtendInfo(reqVO);

        assertEquals(999L, id);
        verify(customerService).validateCustomer(eq(100L));
        verify(extendInfoMapper).insert(captor.capture());
        ErpCustomerExtendInfoDO captured = captor.getValue();
        assertEquals(100L, captured.getCustomerId());
        assertEquals(0, new BigDecimal("5000.00").compareTo(captured.getAdvanceAmount()));
        assertEquals("张老板", captured.getBoss());
        verify(extendInfoMapper, never()).updateById(any(ErpCustomerExtendInfoDO.class));
    }

    // ==================== saveExtendInfo - update branch ====================

    @Test
    public void testSaveExtendInfo_existingRecord_update() {
        ErpCustomerExtendInfoSaveReqVO reqVO = buildSaveReq(100L);
        reqVO.setBoss("李老板（修改）");
        // mock: existing record
        ErpCustomerExtendInfoDO existing = new ErpCustomerExtendInfoDO()
                .setId(50L).setCustomerId(100L).setBoss("张老板");
        when(extendInfoMapper.selectByCustomerId(eq(100L))).thenReturn(existing);

        Long id = extendInfoService.saveExtendInfo(reqVO);

        assertEquals(50L, id);
        verify(customerService).validateCustomer(eq(100L));
        verify(extendInfoMapper).updateById(argThat((ErpCustomerExtendInfoDO info) ->
                Long.valueOf(50L).equals(info.getId())
                        && "李老板（修改）".equals(info.getBoss())));
        verify(extendInfoMapper, never()).insert(any(ErpCustomerExtendInfoDO.class));
    }

    // ==================== saveExtendInfo - invalid customer ====================

    @Test
    public void testSaveExtendInfo_invalidCustomer_throwException() {
        ErpCustomerExtendInfoSaveReqVO reqVO = buildSaveReq(999L);
        doThrow(new ServiceException(CUSTOMER_NOT_EXISTS))
                .when(customerService).validateCustomer(eq(999L));

        assertServiceException(() -> extendInfoService.saveExtendInfo(reqVO), CUSTOMER_NOT_EXISTS);
        verify(extendInfoMapper, never()).selectByCustomerId(any());
        verify(extendInfoMapper, never()).insert(any(ErpCustomerExtendInfoDO.class));
        verify(extendInfoMapper, never()).updateById(any(ErpCustomerExtendInfoDO.class));
    }

    // ==================== saveExtendInfo - with id in reqVO (upsert still uses customerId) ====================

    @Test
    public void testSaveExtendInfo_reqVOHasId_butStillUsesCustomerIdLookup() {
        // Even if reqVO has an id, the service uses selectByCustomerId to decide insert vs update
        ErpCustomerExtendInfoSaveReqVO reqVO = buildSaveReq(200L);
        reqVO.setId(77L); // this id is ignored for lookup
        when(extendInfoMapper.selectByCustomerId(eq(200L))).thenReturn(null);
        doAnswer(invocation -> {
            ErpCustomerExtendInfoDO info = invocation.getArgument(0);
            // service sets id to null before insert
            assertNull(info.getId());
            info.setId(1000L);
            return 1;
        }).when(extendInfoMapper).insert(any(ErpCustomerExtendInfoDO.class));

        Long id = extendInfoService.saveExtendInfo(reqVO);

        assertEquals(1000L, id);
        verify(extendInfoMapper).insert(any(ErpCustomerExtendInfoDO.class));
    }

    // ==================== getByCustomerId ====================

    @Test
    public void testGetByCustomerId_exists_returnDO() {
        ErpCustomerExtendInfoDO expected = new ErpCustomerExtendInfoDO()
                .setId(10L).setCustomerId(100L)
                .setAdvanceAmount(new BigDecimal("3000.00"))
                .setBoss("王老板");
        when(extendInfoMapper.selectByCustomerId(eq(100L))).thenReturn(expected);

        ErpCustomerExtendInfoDO result = extendInfoService.getByCustomerId(100L);

        assertNotNull(result);
        assertEquals(10L, result.getId());
        assertEquals(100L, result.getCustomerId());
        assertEquals("王老板", result.getBoss());
    }

    @Test
    public void testGetByCustomerId_notExists_returnNull() {
        when(extendInfoMapper.selectByCustomerId(eq(888L))).thenReturn(null);

        ErpCustomerExtendInfoDO result = extendInfoService.getByCustomerId(888L);

        assertNull(result);
    }

    // ==================== helper ====================

    private ErpCustomerExtendInfoSaveReqVO buildSaveReq(Long customerId) {
        ErpCustomerExtendInfoSaveReqVO reqVO = new ErpCustomerExtendInfoSaveReqVO();
        reqVO.setCustomerId(customerId);
        reqVO.setAdvanceAmount(new BigDecimal("5000.00"));
        reqVO.setBaseAmount(new BigDecimal("2000.00"));
        reqVO.setSettleDay(15);
        reqVO.setBoss("张老板");
        reqVO.setBossPhone("13800138000");
        reqVO.setLegalPerson("张三");
        reqVO.setBusinessScope("汽配销售");
        reqVO.setGenerateOutBill(true);
        reqVO.setGenerateInBill(false);
        return reqVO;
    }

}
