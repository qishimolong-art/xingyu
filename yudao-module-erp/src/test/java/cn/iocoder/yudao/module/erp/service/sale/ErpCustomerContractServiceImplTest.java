package cn.iocoder.yudao.module.erp.service.sale;

import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customercontract.ErpCustomerContractSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerContractDO;
import cn.iocoder.yudao.module.erp.dal.mysql.sale.ErpCustomerContractMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static cn.iocoder.yudao.framework.test.core.util.AssertUtils.assertServiceException;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_CONTRACT_NOT_EXISTS;
import static cn.iocoder.yudao.module.erp.enums.ErrorCodeConstants.CUSTOMER_NOT_EXISTS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link ErpCustomerContractServiceImpl} 的单元测试
 */
public class ErpCustomerContractServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpCustomerContractServiceImpl contractService;

    @Mock
    private ErpCustomerContractMapper contractMapper;
    @Mock
    private ErpCustomerService customerService;

    // ==================== createContract ====================

    @Test
    public void testCreateContract_normalCase_returnId() {
        // 准备
        ErpCustomerContractSaveReqVO reqVO = new ErpCustomerContractSaveReqVO();
        reqVO.setCustomerId(10L);
        reqVO.setContractNo("HT-001");
        reqVO.setContractDate(LocalDateTime.of(2026, 5, 20, 10, 0));
        reqVO.setContractType("销售合同");
        reqVO.setBaseAmount(new BigDecimal("10000.00"));
        reqVO.setMainContract(true);

        // mock：insert 时回填 id
        when(contractMapper.insert(ArgumentMatchers.<ErpCustomerContractDO>any())).thenAnswer(invocation -> {
            ErpCustomerContractDO contract = invocation.getArgument(0);
            contract.setId(100L);
            return 1;
        });

        // 执行
        Long resultId = contractService.createContract(reqVO);

        // 断言
        assertNotNull(resultId);
        assertEquals(100L, resultId);
        verify(customerService).validateCustomer(eq(10L));
        verify(contractMapper).insert(ArgumentMatchers.<ErpCustomerContractDO>argThat(contract ->
                Long.valueOf(10L).equals(contract.getCustomerId())
                        && "HT-001".equals(contract.getContractNo())
                        && "销售合同".equals(contract.getContractType())
                        && new BigDecimal("10000.00").compareTo(contract.getBaseAmount()) == 0
                        && Boolean.TRUE.equals(contract.getMainContract())));
    }

    @Test
    public void testCreateContract_invalidCustomer_throwException() {
        // 准备
        ErpCustomerContractSaveReqVO reqVO = new ErpCustomerContractSaveReqVO();
        reqVO.setCustomerId(11L);
        reqVO.setContractNo("HT-002");
        // mock：客户校验失败
        doThrow(new ServiceException(CUSTOMER_NOT_EXISTS))
                .when(customerService).validateCustomer(eq(11L));

        // 执行 & 断言
        assertServiceException(() -> contractService.createContract(reqVO), CUSTOMER_NOT_EXISTS);
        // 校验：未 insert
        verify(contractMapper, never()).insert(ArgumentMatchers.<ErpCustomerContractDO>any());
    }

    // ==================== updateContract ====================

    @Test
    public void testUpdateContract_normalCase_success() {
        Long contractId = 20L;
        // 准备：existing
        ErpCustomerContractDO existContract = new ErpCustomerContractDO()
                .setId(contractId)
                .setCustomerId(20L)
                .setContractNo("HT-OLD");
        when(contractMapper.selectById(eq(contractId))).thenReturn(existContract);
        // 准备：更新参数
        ErpCustomerContractSaveReqVO reqVO = new ErpCustomerContractSaveReqVO();
        reqVO.setId(contractId);
        reqVO.setCustomerId(20L);
        reqVO.setContractNo("HT-NEW");
        reqVO.setBaseAmount(new BigDecimal("20000.00"));

        // 执行
        contractService.updateContract(reqVO);

        // 断言
        verify(customerService).validateCustomer(eq(20L));
        verify(contractMapper).updateById(ArgumentMatchers.<ErpCustomerContractDO>argThat(update ->
                contractId.equals(update.getId())
                        && "HT-NEW".equals(update.getContractNo())
                        && new BigDecimal("20000.00").compareTo(update.getBaseAmount()) == 0));
    }

    @Test
    public void testUpdateContract_notExists_throwException() {
        Long contractId = 21L;
        when(contractMapper.selectById(eq(contractId))).thenReturn(null);
        ErpCustomerContractSaveReqVO reqVO = new ErpCustomerContractSaveReqVO();
        reqVO.setId(contractId);
        reqVO.setCustomerId(21L);

        // 执行 & 断言
        assertServiceException(() -> contractService.updateContract(reqVO), CUSTOMER_CONTRACT_NOT_EXISTS);
        // 校验：未发生 customer 校验和 update
        verify(customerService, never()).validateCustomer(any());
        verify(contractMapper, never()).updateById(ArgumentMatchers.<ErpCustomerContractDO>any());
    }

    // ==================== deleteContract ====================

    @Test
    public void testDeleteContract_normalCase_success() {
        Long contractId = 30L;
        ErpCustomerContractDO existContract = new ErpCustomerContractDO()
                .setId(contractId)
                .setCustomerId(30L)
                .setContractNo("HT-DELETE");
        when(contractMapper.selectById(eq(contractId))).thenReturn(existContract);

        // 执行
        contractService.deleteContract(contractId);

        // 断言
        verify(contractMapper).deleteById(eq(contractId));
    }

    // ==================== getContractListByCustomerId ====================

    @Test
    public void testGetContractListByCustomerId_returnList() {
        Long customerId = 40L;
        ErpCustomerContractDO contract1 = new ErpCustomerContractDO().setId(401L).setCustomerId(customerId).setContractNo("HT-A");
        ErpCustomerContractDO contract2 = new ErpCustomerContractDO().setId(402L).setCustomerId(customerId).setContractNo("HT-B");
        when(contractMapper.selectListByCustomerId(eq(customerId)))
                .thenReturn(Arrays.asList(contract1, contract2));

        // 执行
        List<ErpCustomerContractDO> result = contractService.getContractListByCustomerId(customerId);

        // 断言
        assertEquals(2, result.size());
        assertEquals(401L, result.get(0).getId());
        assertEquals(402L, result.get(1).getId());
        // 校验：先 validateCustomer 再查询
        verify(customerService).validateCustomer(eq(customerId));
        verify(contractMapper).selectListByCustomerId(eq(customerId));
    }

}
