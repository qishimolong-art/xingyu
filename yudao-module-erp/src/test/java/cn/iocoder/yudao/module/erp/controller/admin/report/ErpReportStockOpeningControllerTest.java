package cn.iocoder.yudao.module.erp.controller.admin.report;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.jackson.config.YudaoJacksonAutoConfiguration;
import cn.iocoder.yudao.framework.common.util.json.JsonUtils;
import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpStockDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.stock.ErpWarehouseDO;
import cn.iocoder.yudao.module.erp.dal.mysql.stock.ErpWarehouseMapper;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService;
import cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseServiceImpl;
import cn.iocoder.yudao.module.erp.service.stock.cost.ErpDualCostPostingService;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Exercises the real method-security proxy and warehouse gate, rather than reading annotations. */
class ErpReportStockOpeningControllerTest {
    private AnnotationConfigApplicationContext context;
    private ErpReportStockOpeningController controller;
    private ErpWarehouseService warehouse;
    private ErpDualCostPostingService posting;
    private PermissionGate gate;
    private ErpStockService stocks;
    private ErpStockDO stock;

    @Configuration
    @EnableGlobalMethodSecurity(prePostEnabled = true, proxyTargetClass = true)
    static class Config {
        @Bean public ErpReportStockOpeningController controller() { return new ErpReportStockOpeningController(); }
        @Bean(name = "ss") public PermissionGate permissionGate() { return new PermissionGate(); }
    }

    public static class PermissionGate {
        boolean allowed;
        String requested;
        public boolean hasPermission(String permission) {
            requested = permission;
            return allowed;
        }
    }

    @BeforeEach
    void setup() {
        context = new AnnotationConfigApplicationContext();
        context.getBeanFactory().registerSingleton("warehouseService", mock(ErpWarehouseServiceImpl.class));
        context.getBeanFactory().registerSingleton("postingService", mock(ErpDualCostPostingService.class));
        context.getBeanFactory().registerSingleton("stockService", mock(ErpStockService.class));
        context.register(Config.class);
        context.refresh();
        controller = context.getBean(ErpReportStockOpeningController.class);
        warehouse = context.getBean(ErpWarehouseService.class);
        posting = context.getBean(ErpDualCostPostingService.class);
        gate = context.getBean(PermissionGate.class);
        stocks = context.getBean(ErpStockService.class);
        stock = new ErpStockDO();
        stock.setId(11L);
        stock.setProductId(101L);
        stock.setWarehouseId(201L);
        stock.setCreator("99");
        when(stocks.getStock(101L, 201L)).thenReturn(stock);
        LoginUser user = new LoginUser();
        user.setId(99L);
        user.setInfo(Collections.singletonMap(LoginUser.INFO_KEY_DEPT_ID, "301"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, "unused", Collections.emptyList()));
    }

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        context.close();
    }

    private ErpReportStockOpeningController.OpeningRequest request() {
        ErpReportStockOpeningController.OpeningRequest request = new ErpReportStockOpeningController.OpeningRequest();
        request.setProductId(101L);
        request.setWarehouseId(201L);
        request.setQuantity(new BigDecimal("10"));
        request.setFinancialAmount(new BigDecimal("1000"));
        request.setSettlementAmount(new BigDecimal("1200"));
        request.setCutoverAt(LocalDateTime.of(2026, 1, 1, 0, 0));
        request.setEvidence("approved inventory reconciliation");
        return request;
    }

    @Test
    void parentMenuPermissionAloneCannotConfirmOpening() {
        gate.allowed = false;
        assertThrows(AccessDeniedException.class, () -> controller.confirm(request()));
        assertEquals("erp:report-stock-opening:confirm", gate.requested);
        verifyNoInteractions(warehouse, posting);
    }

    @Test
    void warehousePermissionFailurePreventsLedgerWrite() {
        gate.allowed = true;
        doThrow(new AccessDeniedException("warehouse not allowed")).when(warehouse)
                .validateCurrentUserStockWarehousePermission(Collections.singleton(201L));
        assertThrows(AccessDeniedException.class, () -> controller.confirm(request()));
        verifyNoInteractions(posting);
    }

    @Test
    void missingLoginIdentityCannotConfirmOpening() {
        gate.allowed = true;
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("unknown principal", "unused", Collections.emptyList()));
        assertThrows(IllegalStateException.class, () -> controller.confirm(request()));
        verifyNoInteractions(posting);
    }

    @Test
    void operatorComesFromLoginContextDespiteSpoofedJsonField() throws Exception {
        gate.allowed = true;
        ObjectMapper mapper = productionMapper();
        String json = "{\"productId\":101,\"warehouseId\":201,\"quantity\":10,\"financialAmount\":1000,"
                + "\"settlementAmount\":1200,\"cutoverAt\":\"2026-01-01 00:00:00\","
                + "\"evidence\":\"approved inventory reconciliation\",\"confirmedBy\":123456,\"operator\":123456}";
        ErpReportStockOpeningController.OpeningRequest decoded = mapper.readValue(json,
                ErpReportStockOpeningController.OpeningRequest.class);
        assertTrue(controller.confirm(decoded).getData());
        verify(warehouse).validateCurrentUserStockWarehousePermission(Collections.singleton(201L));
        verify(posting).confirmOpening(eq(101L), eq(201L), eq(new BigDecimal("10")),
                eq(new BigDecimal("1000")), eq(new BigDecimal("1200")), eq(request().getCutoverAt()),
                eq(request().getEvidence()), eq(99L));
    }

    private ObjectMapper productionMapper() {
        YudaoJacksonAutoConfiguration config = new YudaoJacksonAutoConfiguration();
        Jackson2ObjectMapperBuilder builder = Jackson2ObjectMapperBuilder.json();
        config.ldtEpochMillisCustomizer().customize(builder);
        return builder.build().registerModule(config.timestampSupportModuleBean())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    @Test
    void productionJsonUtilsPreservesCutoverTextAndRejectsInvalidDate() {
        ObjectMapper previous = (ObjectMapper) ReflectionTestUtils.getField(JsonUtils.class, "objectMapper");
        try {
            JsonUtils.init(productionMapper());
            ErpReportStockOpeningController.OpeningRequest parsed = JsonUtils.parseObject(
                    "{\"cutoverAt\":\"2026-09-09 09:08:07\"}", ErpReportStockOpeningController.OpeningRequest.class);
            assertEquals(LocalDateTime.of(2026, 9, 9, 9, 8, 7), parsed.getCutoverAt());
            assertThrows(RuntimeException.class, () -> JsonUtils.parseObject(
                    "{\"cutoverAt\":\"not-a-date\"}", ErpReportStockOpeningController.OpeningRequest.class));
            assertThrows(RuntimeException.class, () -> JsonUtils.parseObject(
                    "{\"cutoverAt\":\"2026-02-30 10:00:00\"}", ErpReportStockOpeningController.OpeningRequest.class));
        } finally {
            JsonUtils.init(previous);
        }
    }

    private void enableActualSelfStockPermission() {
        gate.allowed = true;
        PermissionApi permissions = mock(PermissionApi.class);
        ErpWarehouseMapper mapper = mock(ErpWarehouseMapper.class);
        DeptDataPermissionRespDTO scope = new DeptDataPermissionRespDTO();
        scope.setSelf(true);
        when(permissions.getDeptDataPermission(99L, "erp_stock")).thenReturn(scope);
        ErpWarehouseDO allowedWarehouse = new ErpWarehouseDO();
        allowedWarehouse.setId(201L);
        allowedWarehouse.setDeptId(301L);
        when(mapper.selectListByStatusIfPresent(0)).thenReturn(Collections.singletonList(allowedWarehouse));
        ReflectionTestUtils.setField(warehouse, "permissionApi", permissions);
        ReflectionTestUtils.setField(warehouse, "warehouseMapper", mapper);
        when(warehouse.getCurrentUserProductStockPermissionScope()).thenCallRealMethod();
        doCallRealMethod().when(warehouse).validateCurrentUserStockWarehousePermission(anyCollection());
        doCallRealMethod().when(warehouse).validateCurrentUserStockPermission(anyCollection());
    }

    @Test
    void actualSelfScopeRejectsOtherCreatorsStockEvenInVisibleWarehouse() {
        enableActualSelfStockPermission();
        stock.setCreator("88");
        assertTrue(warehouse.getCurrentUserProductStockPermissionScope().canAccessWarehouse(201L));
        assertThrows(IllegalArgumentException.class, () -> controller.confirm(request()));
        verifyNoInteractions(posting);
    }

    @Test
    void actualSelfScopeAllowsOwnStockInOwnDepartmentWarehouse() {
        enableActualSelfStockPermission();
        assertTrue(controller.confirm(request()).getData());
        verify(posting).confirmOpening(eq(101L), eq(201L), any(), any(), any(), any(), anyString(), eq(99L));
    }
}
