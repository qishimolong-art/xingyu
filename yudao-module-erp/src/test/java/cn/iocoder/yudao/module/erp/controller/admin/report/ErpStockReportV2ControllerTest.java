package cn.iocoder.yudao.module.erp.controller.admin.report;

import cn.iocoder.yudao.module.erp.controller.admin.report.vo.ErpStockReportV2Models.Filter;
import cn.iocoder.yudao.module.erp.service.stock.report.ErpStockReportV2Service;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Independent authorization verification through the real Spring method-security proxy. */
class ErpStockReportV2ControllerTest {
    AnnotationConfigApplicationContext context;
    ErpStockReportV2Controller controller;
    ErpStockReportV2Service service;
    Gate gate;
    @Configuration
    @EnableGlobalMethodSecurity(prePostEnabled=true, proxyTargetClass=true)
    static class Config {
        @Bean ErpStockReportV2Controller controller() { return new ErpStockReportV2Controller(); }
        @Bean(name="ss") Gate gate() { return new Gate(); }
    }
    public static class Gate {
        final Set<String> allowed = new HashSet<>();
        public boolean hasPermission(String code) { return allowed.contains(code); }
    }
    @BeforeEach void setup() {
        context = new AnnotationConfigApplicationContext();
        context.getBeanFactory().registerSingleton("reportService", mock(ErpStockReportV2Service.class));
        context.register(Config.class); context.refresh();
        controller=context.getBean(ErpStockReportV2Controller.class);
        service=context.getBean(ErpStockReportV2Service.class);
        gate=context.getBean(Gate.class);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("user", "unused", Collections.emptyList()));
    }
    @AfterEach void cleanup() { SecurityContextHolder.clearContext(); context.close(); }
    @Test void allReadOperationsRequireQueryPermissionBeforeAccessingData() {
        Filter f = new Filter();
        assertThrows(AccessDeniedException.class, () -> controller.status());
        assertThrows(AccessDeniedException.class, () -> controller.reportOptions());
        assertThrows(AccessDeniedException.class, () -> controller.movementPage(f));
        assertThrows(AccessDeniedException.class, () -> controller.movementSummary(f));
        assertThrows(AccessDeniedException.class, () -> controller.balancePage(f));
        assertThrows(AccessDeniedException.class, () -> controller.balanceSummary(f));
        assertThrows(AccessDeniedException.class, () -> controller.productOptions("", null, 1, 20));
        verifyNoInteractions(service);
    }
    @Test void queryPermissionDoesNotGrantExport() {
        gate.allowed.add("erp:stock-record:query");
        Filter f = new Filter();
        controller.balancePage(f);
        controller.reportOptions();
        verify(service).balancePage(f);
        verify(service).reportOptions();
        clearInvocations(service);
        assertThrows(AccessDeniedException.class, () -> controller.export(f,"BALANCE",new MockHttpServletResponse()));
        verifyNoInteractions(service);
    }
    @Test void openingConfirmationOnlyCanResolveSafeProductOptionsButCannotReadAmounts() {
        gate.allowed.add("erp:report-stock-opening:confirm");
        controller.productOptions("part", 201L, 2, 10);
        verify(service).productOptions("part",201L,2,10);
        clearInvocations(service);
        assertThrows(AccessDeniedException.class, () -> controller.status());
        assertThrows(AccessDeniedException.class, () -> controller.reportOptions());
        assertThrows(AccessDeniedException.class, () -> controller.balancePage(new Filter()));
        assertThrows(AccessDeniedException.class, () -> controller.export(new Filter(),"BALANCE",new MockHttpServletResponse()));
        verifyNoInteractions(service);
    }
    @Test void exportPermissionUsesSameServiceFilterWithoutInventingQueryPermission() throws Exception {
        gate.allowed.add("erp:stock-record:export");
        Filter f = new Filter(); f.setProductId(101L);
        MockHttpServletResponse response = new MockHttpServletResponse();
        controller.export(f,"MOVEMENT",response);
        verify(service).export(same(f),eq("MOVEMENT"),same(response));
        assertThrows(AccessDeniedException.class, () -> controller.movementPage(f));
    }
}
