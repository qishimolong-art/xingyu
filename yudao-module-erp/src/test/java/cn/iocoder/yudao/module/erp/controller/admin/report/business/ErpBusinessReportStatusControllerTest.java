package cn.iocoder.yudao.module.erp.controller.admin.report.business;

import cn.iocoder.yudao.module.erp.controller.admin.report.vo.business.ErpBusinessReportStatusRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.business.ErpBusinessReportStatusRespVO.ModuleStatus;
import cn.iocoder.yudao.module.erp.service.report.business.ErpBusinessReportStatusService;
import cn.iocoder.yudao.module.erp.service.report.business.ErpBusinessReportStatusServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ErpBusinessReportStatusControllerTest {

    private AnnotationConfigApplicationContext context;
    private ErpBusinessReportStatusController controller;
    private Gate gate;

    @Configuration
    @EnableGlobalMethodSecurity(prePostEnabled = true, proxyTargetClass = true)
    static class Config {

        @Bean
        ErpBusinessReportStatusController businessReportStatusController() {
            return new ErpBusinessReportStatusController();
        }

        @Bean
        ErpBusinessReportStatusService businessReportStatusService() {
            return new ErpBusinessReportStatusServiceImpl();
        }

        @Bean(name = "ss")
        Gate gate() {
            return new Gate();
        }
    }

    public static class Gate {

        private final Set<String> allowed = new HashSet<>();

        public boolean hasAnyPermissions(String... permissions) {
            for (String permission : permissions) {
                if (allowed.contains(permission)) {
                    return true;
                }
            }
            return false;
        }
    }

    @BeforeEach
    void setup() {
        context = new AnnotationConfigApplicationContext(Config.class);
        controller = context.getBean(ErpBusinessReportStatusController.class);
        gate = context.getBean(Gate.class);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("user", "unused", new java.util.ArrayList<>()));
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        context.close();
    }

    @Test
    void noBusinessReportPermissionBlocksStatus() {
        assertThrows(AccessDeniedException.class, () -> controller.getStatus());
    }

    @Test
    void anyExistingBusinessReportQueryPermissionCanReadStatus() {
        gate.allowed.add("erp:purchase-report-v2:query");

        ErpBusinessReportStatusRespVO data = controller.getStatus().getData();

        assertEquals("统一经营报表", data.getReportName());
        assertEquals(13, data.getModules().size());
        assertEquals(13, data.getModules().stream().map(ModuleStatus::getCode).collect(Collectors.toSet()).size());
        assertTrue(data.getModules().stream().anyMatch(module -> "00".equals(module.getCode())));
        assertTrue(data.getModules().stream().anyMatch(module -> "12".equals(module.getCode())));
        assertTrue(data.getGlobalNotes().stream().anyMatch(note -> note.contains("毛利率")
                && note.contains("财务公式未确认前不能作为正式结果")));
        assertTrue(data.getGlobalNotes().stream().anyMatch(note -> note.contains("双成本开关默认关闭")));
        assertTrue(findModule(data, "03").getDependencySql().stream()
                .anyMatch(sql -> sql.contains("erp_stock_record_cursor_20260909.sql")
                        && sql.contains("仅双成本准备")));
        assertTrue(findModule(data, "10").getRiskNotes().stream()
                .anyMatch(note -> note.contains("三大报表金额列必须保持不可用边界")));
        ModuleStatus auditTraceModule = findModule(data, "12");
        assertTrue(auditTraceModule.getDependencyPermissions().contains("erp:stock-record:query"));
        assertTrue(auditTraceModule.getDependencyPermissions().contains("system:operate-log:query"));
        assertTrue(auditTraceModule.getDependencyPermissions().contains("erp:purchase-invoice-ocr:query"));
        assertTrue(auditTraceModule.getDependencyPermissions().contains("erp:import-record:query"));
        assertTrue(auditTraceModule.getDependencyPermissions().contains("erp:export-record:query"));
        assertTrue(auditTraceModule.getDependencyPermissions().stream()
                .noneMatch(permission -> "infra:api-access-log:query".equals(permission)
                        || "erp:import-export-record:query".equals(permission)));
    }

    private static ModuleStatus findModule(ErpBusinessReportStatusRespVO data, String code) {
        return data.getModules().stream()
                .filter(module -> code.equals(module.getCode()))
                .findFirst()
                .orElseThrow(IllegalStateException::new);
    }

}

