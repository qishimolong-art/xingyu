package cn.iocoder.yudao.module.erp.service.common;

import cn.iocoder.yudao.framework.security.core.service.SecurityFrameworkService;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.print.ErpPrintFieldRespVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableExpenseDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable.ErpPayableExpenseItemDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpCustomerDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleOutDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.sale.ErpSaleQuoteDO;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.sale.ErpSaleBizSourceTypeEnum;
import cn.iocoder.yudao.module.erp.service.finance.payable.ErpPayableExpenseService;
import cn.iocoder.yudao.module.erp.service.sale.ErpCustomerService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleOutService;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleQuoteService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import cn.iocoder.yudao.module.system.api.permission.dto.FieldDefinitionRespDTO;
import cn.iocoder.yudao.module.system.api.user.AdminUserApi;
import cn.iocoder.yudao.module.system.api.user.dto.AdminUserRespDTO;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    @Mock
    private PermissionApi permissionApi;
    @Mock
    private AdminUserApi adminUserApi;
    @Mock
    private ErpCustomerService customerService;
    @Mock
    private ErpSaleOutService saleOutService;
    @Mock
    private ErpSaleQuoteService saleQuoteService;

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

    @Test
    void testGetFields_saleOutReadableStatusCodes() {
        when(securityFrameworkService.hasPermission("erp:sale-out:print-template")).thenReturn(true);
        when(permissionApi.getFieldDefinitions("erp_sale_out")).thenReturn(List.of(
                definition("status", "状态", 34),
                definition("settleStatus", "结算状态", 35),
                definition("customerCode", "客户编码", 36)));

        ErpPrintFieldRespVO result = printService.getFields("sale_out");

        assertEquals("sale_out", result.getModuleKey());
        assertEquals("document.statusName", fieldCode(result, "状态"));
        assertEquals("document.settleStatusName", fieldCode(result, "结算状态"));
        assertEquals("document.customerCode", fieldCode(result, "客户编码"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void testGetPrintData_saleOutEnrichesDetailFields() {
        Long businessId = 1001L;
        Long customerId = 2001L;
        Long sourceId = 3001L;
        Long sourceCreatorId = 4001L;
        ErpSaleOutDO saleOut = new ErpSaleOutDO()
                .setId(businessId)
                .setNo("XS-001")
                .setStatus(ErpAuditStatus.APPROVE.getStatus())
                .setSettleStatus(1)
                .setCustomerId(customerId)
                .setSourceType(ErpSaleBizSourceTypeEnum.QUOTE.getType())
                .setSourceId(sourceId)
                .setDiscountPrice(new BigDecimal("8.00"))
                .setOtherPrice(new BigDecimal("3.50"))
                .setTotalPrice(new BigDecimal("100.00"));
        ErpCustomerDO customer = new ErpCustomerDO().setId(customerId).setName("星宇客户").setCode("CUST-001");
        ErpSaleQuoteDO quote = new ErpSaleQuoteDO().setId(sourceId).setFreightType("到付");
        quote.setCreator(String.valueOf(sourceCreatorId));
        quote.setCreateTime(LocalDateTime.of(2026, 9, 3, 10, 30));
        AdminUserRespDTO sourceCreator = new AdminUserRespDTO();
        sourceCreator.setId(sourceCreatorId);
        sourceCreator.setNickname("来源制单人");
        when(securityFrameworkService.hasPermission("erp:sale-out:print")).thenReturn(true);
        when(saleOutService.getSaleOut(businessId)).thenReturn(saleOut);
        when(saleOutService.getSaleOutItemListByOutId(businessId)).thenReturn(List.of());
        when(customerService.getCustomerMap(Set.of(customerId))).thenReturn(Map.of(customerId, customer));
        when(customerService.getCustomer(customerId)).thenReturn(customer);
        when(saleQuoteService.getSaleQuote(sourceId)).thenReturn(quote);
        when(adminUserApi.getUser(sourceCreatorId)).thenReturn(sourceCreator);

        Map<String, Object> result = printService.getPrintData("sale_out", businessId);

        Map<String, Object> document = (Map<String, Object>) result.get("document");
        assertEquals("CUST-001", document.get("customerCode"));
        assertEquals("已审核", document.get("statusName"));
        assertEquals("部分结算", document.get("settleStatusName"));
        assertEquals("3.5", document.get("feeAmount"));
        assertEquals("8", document.get("reductionAmount"));
        assertEquals("100", document.get("afterReductionAmount"));
        assertEquals("2026-09-03", document.get("sourceCreateTime"));
        assertEquals("来源制单人", document.get("sourceCreatorName"));
        assertEquals("到付", document.get("freightType"));
        Map<String, Object> main = (Map<String, Object>) result.get("main");
        assertEquals("CUST-001", main.get("document.customerCode"));
        assertEquals("已审核", main.get("document.statusName"));
        assertEquals("部分结算", main.get("document.settleStatusName"));
        assertEquals("来源制单人", main.get("document.sourceCreatorName"));
        verify(saleOutService).getSaleOut(businessId);
        verify(saleOutService).getSaleOutItemListByOutId(businessId);
        verify(saleQuoteService).getSaleQuote(sourceId);
    }

    private FieldDefinitionRespDTO definition(String key, String label, Integer sort) {
        FieldDefinitionRespDTO definition = new FieldDefinitionRespDTO();
        definition.setFieldKey(key);
        definition.setFieldLabel(label);
        definition.setSort(sort);
        return definition;
    }

    private String fieldCode(ErpPrintFieldRespVO fields, String name) {
        return fields.getGroups().get(0).getFields().stream()
                .filter(field -> name.equals(field.getName()))
                .findFirst()
                .orElseThrow()
                .getCode();
    }

}
