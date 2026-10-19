package cn.iocoder.yudao.module.erp.service.report.purchase;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseInvoiceDetailReportModels.Filter;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseInvoiceDetailReportModels.Row;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceVisibleScope;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ErpPurchaseInvoiceDetailReportRepositoryTest extends BaseMockitoUnitTest {

    @InjectMocks
    private ErpPurchaseInvoiceDetailReportRepository repository;

    @Mock
    private NamedParameterJdbcTemplate jdbc;

    @Test
    void sourceDocIdIsResponseOnlyAndDoesNotEnterExcel() throws Exception {
        assertTrue(Row.class.isAnnotationPresent(ExcelIgnoreUnannotated.class));
        assertFalse(Row.class.getDeclaredField("sourceDocId").isAnnotationPresent(ExcelProperty.class));
    }

    @Test
    void countUsesApprovedTenantSafeSourceAndSettlementQuery() {
        when(jdbc.queryForObject(anyString(), anyMap(), eq(Long.class))).thenReturn(2L);
        Filter filter = new Filter();
        filter.setDocumentNo("CGPJ");
        ErpFinanceVisibleScope visibleScope = new ErpFinanceVisibleScope(false, Arrays.asList(7L, 8L), 9L);

        long result = repository.count(filter,
                new ErpPurchaseInvoiceDetailReportRepository.Scope(1L, visibleScope));

        assertEquals(2L, result);
        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, Object>> params = ArgumentCaptor.forClass(Map.class);
        verify(jdbc).queryForObject(sql.capture(), params.capture(), eq(Long.class));
        String value = sql.getValue();
        assertTrue(value.contains("inv.status=20 AND inv.approve_time IS NOT NULL"));
        assertTrue(value.contains("ii.tenant_id=:tenant AND ii.deleted=b'0'"));
        assertTrue(value.contains("pin.tenant_id=ii.tenant_id AND pin.deleted=b'0'"));
        assertTrue(value.contains("ii.source_type,ii.source_doc_id"));
        assertTrue(value.contains("in_settlement AS"));
        assertTrue(value.contains("adjust_payment_agg AS"));
        assertTrue(value.contains("WHEN 30 THEN pr.refund_price"));
        assertTrue(value.contains("inv.dept_id IN (:scopeDeptIds) OR inv.handler_id=:scopeUserId"));
        assertEquals(1L, params.getValue().get("tenant"));
        assertEquals("%CGPJ%", params.getValue().get("documentNo"));
    }

    @Test
    void pageRejectsUnknownOrderExpressionByFallingBackToDefaultOrder() {
        doReturn(Collections.emptyList()).when(jdbc).query(anyString(), anyMap(),
                org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<Object>>any());
        Filter filter = new Filter();
        filter.setOrderField("approve_date desc; drop table erp_purchase_invoice");
        filter.setOrderDirection("asc");

        repository.page(filter, new ErpPurchaseInvoiceDetailReportRepository.Scope(1L,
                new ErpFinanceVisibleScope(true, Collections.emptyList(), null)), 0, 10);

        ArgumentCaptor<String> sql = ArgumentCaptor.forClass(String.class);
        verify(jdbc).query(sql.capture(), anyMap(),
                org.mockito.ArgumentMatchers.<org.springframework.jdbc.core.RowMapper<Object>>any());
        assertTrue(sql.getValue().contains(
                "ORDER BY report.approve_date DESC,report.document_no DESC,report.item_id ASC"));
        assertTrue(!sql.getValue().contains("drop table"));
    }
}
