package cn.iocoder.yudao.module.erp.service.report.finance;

import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpPayableExpenseDetailReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.finance.ErpPayableExpenseDetailReportModels.Row;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

class ErpPayableExpenseDetailReportServiceTest extends BaseMockitoUnitTest {

    @InjectMocks private ErpPayableExpenseDetailReportService service;
    @Mock private ErpPayableExpenseDetailReportRepository repository;
    @Mock private PermissionApi permissionApi;

    @Test
    void shouldMaskMainAndItemFieldsAndKeepReservedColumnsEmpty() throws Exception {
        when(permissionApi.getCurrentUserHiddenFields("erp_finance_payable_expense")).thenReturn(Arrays.asList(
                "accountId", "no", "item_itemName", "item_amount", "voucherNo", "remark", "handlerId", "party",
                "deptId", "item_deptId", "item_handlerId"));
        Row row = fullRow();

        Method method = ErpPayableExpenseDetailReportService.class
                .getDeclaredMethod("applyVisibility", List.class);
        method.setAccessible(true);
        method.invoke(service, Collections.singletonList(row));

        assertThat(row.getApproveDate()).isEqualTo("2026-10-14");
        assertThat(row.getAccountName()).isNull();
        assertThat(row.getDocumentNo()).isNull();
        assertThat(row.getItemName()).isNull();
        assertThat(row.getAmount()).isNull();
        assertThat(row.getVoucherNo()).isNull();
        assertThat(row.getSummary()).isNull();
        assertThat(row.getHandlerName()).isNull();
        assertThat(row.getParty()).isNull();
        assertThat(row.getBillingDeptName()).isNull();
        assertThat(row.getDeptName()).isNull();
        assertThat(row.getBusinessUserName()).isNull();
        assertThat(row.getAmountVisible()).isFalse();
        assertThat(row.getType()).isNull();
        assertThat(row.getInvoice()).isNull();
        assertThat(row.getOccurrenceDate()).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void shouldExportExactFifteenColumnsAndRemoveOnlyAmountWhenHidden() throws Exception {
        Method method = ErpPayableExpenseDetailReportService.class
                .getDeclaredMethod("exportFields", boolean.class);
        method.setAccessible(true);
        Set<String> visible = (Set<String>) method.invoke(service, true);
        Set<String> hidden = (Set<String>) method.invoke(service, false);

        assertThat(visible).containsExactly("approveDate", "accountName", "documentNo", "itemName", "amount",
                "voucherNo", "summary", "handlerName", "party", "type", "billingDeptName", "deptName",
                "businessUserName", "invoice", "occurrenceDate");
        assertThat(hidden).containsExactly("approveDate", "accountName", "documentNo", "itemName", "voucherNo",
                "summary", "handlerName", "party", "type", "billingDeptName", "deptName", "businessUserName",
                "invoice", "occurrenceDate");
    }

    @Test
    void shouldRejectInvalidAmountRangeBeforeQuery() {
        Filter filter = new Filter();
        filter.setPageNo(1);
        filter.setPageSize(10);
        filter.setAmountMin(new BigDecimal("10"));
        filter.setAmountMax(new BigDecimal("9.99"));

        assertThatThrownBy(() -> service.getPage(filter))
                .hasMessage("金额区间不正确");
    }

    private Row fullRow() {
        return new Row().setApproveDate("2026-10-14").setAccountName("现金账户").setDocumentNo("FYZF1")
                .setItemName("差旅费用").setAmount(new BigDecimal("88.60")).setVoucherNo("PZ1")
                .setSummary("摘要").setHandlerName("经办人").setParty("对象").setBillingDeptName("开单部门")
                .setDeptName("明细部门").setBusinessUserName("业务员");
    }
}
