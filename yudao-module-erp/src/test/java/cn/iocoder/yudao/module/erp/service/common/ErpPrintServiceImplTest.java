package cn.iocoder.yudao.module.erp.service.common;

import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableExpenseDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableExpenseItemDO;
import cn.iocoder.yudao.module.erp.service.finance.payable.ErpPayableExpenseService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpPrintServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpPrintServiceImpl printService;

    @Mock
    private SecurityFrameworkService securityFrameworkService;
    @Mock
    private ErpPayableExpenseService payableExpenseService;

    @Test
    @SuppressWarnings("unchecked")
    void testGetPrintData_payableExpense() {
        Long businessId = 1001L;
        ErpPayableExpenseDO expense = new ErpPayableExpenseDO()
                .setId(businessId)
                .setNo("FY-001")
                .setBizTime(LocalDate.of(2026, 8, 17))
                .setExpenseBizType("一般费用")
                .setTotalAmount(new BigDecimal("120.50"));
        ErpPayableExpenseItemDO item = new ErpPayableExpenseItemDO()
                .setId(2001L)
                .setExpenseId(businessId)
                .setItemName("办公用品")
                .setAmount(new BigDecimal("120.50"));
        when(securityFrameworkService.hasPermission("erp:payable-expense:print")).thenReturn(true);
        when(payableExpenseService.getPayableExpense(businessId)).thenReturn(expense);
        when(payableExpenseService.getPayableExpenseItemListByExpenseId(businessId)).thenReturn(List.of(item));

        Map<String, Object> result = printService.getPrintData("payable_expense", businessId);

        assertEquals("payable_expense", result.get("moduleKey"));
        assertEquals("费用支付", result.get("moduleName"));
        Map<String, Object> document = (Map<String, Object>) result.get("document");
        assertEquals("FY-001", document.get("no"));
        assertEquals("2026-08-17", document.get("bizTime"));
        assertEquals("一般费用", document.get("expenseBizType"));
        assertEquals("120.5", document.get("totalAmount"));
        Map<String, Object> main = (Map<String, Object>) result.get("main");
        assertEquals("120.50", main.get("document.totalAmountUpper"));
        List<Map<String, Object>> items = (List<Map<String, Object>>) result.get("items");
        assertEquals(1, items.size());
        assertEquals(1, items.get(0).get("items.seq"));
        assertEquals("办公用品", items.get(0).get("items.itemName"));
        assertEquals("120.5", items.get(0).get("items.amount"));
        verify(payableExpenseService).getPayableExpense(businessId);
        verify(payableExpenseService).getPayableExpenseItemListByExpenseId(businessId);
    }

}
