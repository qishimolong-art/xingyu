package cn.iocoder.yudao.module.erp.service.report.purchase;

import cn.iocoder.yudao.module.erp.controller.admin.report.vo.purchase.ErpPurchaseOrderDetailReportModels.Filter;
import cn.iocoder.yudao.module.erp.service.finance.ErpFinanceVisibleScope;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpPurchaseOrderDetailReportRepositoryTest {

    private final ErpPurchaseOrderDetailReportRepository repository =
            new ErpPurchaseOrderDetailReportRepository();

    @Test
    void orderDateUsesPurchaseOrderCreateTimeForDisplayFilterAndSort() throws Exception {
        Method selectFieldsMethod = ErpPurchaseOrderDetailReportRepository.class
                .getDeclaredMethod("selectFields");
        selectFieldsMethod.setAccessible(true);
        String selectFields = (String) selectFieldsMethod.invoke(repository);
        assertTrue(selectFields.contains("po.create_time AS order_date"));
        assertFalse(selectFields.contains("po.order_date"));

        Filter filter = new Filter();
        filter.setOrderDate(new LocalDateTime[]{
                LocalDateTime.of(2026, 10, 1, 0, 0),
                LocalDateTime.of(2026, 10, 14, 23, 59, 59)
        });
        filter.setOrderField("orderDate");
        filter.setOrderDirection("asc");

        ErpFinanceVisibleScope allScope = new ErpFinanceVisibleScope(true, Collections.emptyList(), null);
        ErpPurchaseOrderDetailReportRepository.Scope scope =
                new ErpPurchaseOrderDetailReportRepository.Scope(1L, allScope, allScope);
        Method baseMethod = ErpPurchaseOrderDetailReportRepository.class
                .getDeclaredMethod("base", Filter.class, ErpPurchaseOrderDetailReportRepository.Scope.class);
        baseMethod.setAccessible(true);
        Object sql = baseMethod.invoke(repository, filter, scope);
        Field textField = sql.getClass().getDeclaredField("text");
        textField.setAccessible(true);
        String whereSql = (String) textField.get(sql);
        assertTrue(whereSql.contains("po.create_time>=:dateFrom"));
        assertTrue(whereSql.contains("po.create_time<=:dateTo"));
        assertFalse(whereSql.contains("po.order_date"));

        Method orderByMethod = ErpPurchaseOrderDetailReportRepository.class
                .getDeclaredMethod("orderBy", Filter.class);
        orderByMethod.setAccessible(true);
        String orderBy = (String) orderByMethod.invoke(repository, filter);
        assertTrue(orderBy.startsWith(" ORDER BY po.create_time ASC"));
        assertFalse(orderBy.contains("po.order_date"));

        String defaultOrderBy = (String) orderByMethod.invoke(repository, new Filter());
        assertTrue(defaultOrderBy.startsWith(" ORDER BY po.create_time DESC"));
        assertFalse(defaultOrderBy.contains("po.order_date"));
    }
}
