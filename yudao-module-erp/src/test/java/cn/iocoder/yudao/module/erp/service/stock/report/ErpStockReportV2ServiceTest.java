package cn.iocoder.yudao.module.erp.service.stock.report;

import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.ErpStockReportV2Models.*;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ErpStockReportV2ServiceTest {
    private ErpStockReportV2Service service;
    private ErpStockReportV2Repository repository;
    private PermissionApi permission;

    @BeforeEach
    void setup() {
        service=new ErpStockReportV2Service(); repository=mock(ErpStockReportV2Repository.class); permission=mock(PermissionApi.class);
        ReflectionTestUtils.setField(service,"repository",repository); ReflectionTestUtils.setField(service,"permissionApi",permission);
        ReflectionTestUtils.setField(service,"cutover","2026-01-01T00:00:00");
        LoginUser user=new LoginUser();user.setId(9L);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user,"",Collections.emptyList()));
        TenantContextHolder.setTenantId(1L);
    }

    @AfterEach void cleanup() { SecurityContextHolder.clearContext();TenantContextHolder.clear(); }

    private Filter filter() { return new Filter().setPostedFrom(LocalDateTime.of(2026,1,1,0,0)).setPostedTo(LocalDateTime.of(2026,2,1,0,0)); }
    private void enable() {
        ReflectionTestUtils.setField(service,"enabled",true);
        when(permission.getDeptDataPermission(9L,"erp_stock_record")).thenReturn(new DeptDataPermissionRespDTO().setAll(true));
        when(permission.getDeptDataPermission(9L,"erp_stock")).thenReturn(new DeptDataPermissionRespDTO().setAll(true));
        when(repository.warehouseIds(eq(1L),anyCollection(),eq(true))).thenReturn(Collections.singleton(20L));
        when(repository.permissionDepartments(any(),any(),anyBoolean())).thenReturn(new HashSet<>(Arrays.asList(100L,200L)));
    }

    @Test void disabledIsExplicitAndNeverReadsUnmigratedTables() {
        assertEquals("DISABLED",service.status().getStatus());
        assertThrows(ServiceException.class,()->service.movementPage(filter()));
        verifyNoInteractions(repository,permission);
    }

    @Test void departmentCostDenyAppliesToWholeFilteredPageAndSummary() {
        enable();
        when(permission.getCurrentUserHiddenFields("erp_product",200L)).thenReturn(Collections.singletonList("col_lastPurchasePrice"));
        MovementRow row=new MovementRow().setStockDeptId(100L).setFinancialMovement(new BigDecimal("-100"))
                .setSettlementMovement(new BigDecimal("-120")).setFinancialBalance(new BigDecimal("500"));
        when(repository.movementPage(any(),any(),anyLong(),anyInt())).thenReturn(Collections.singletonList(row));
        MovementRow visible=service.movementPage(filter()).getList().get(0);
        assertTrue(visible.getFinancialMasked());assertTrue(visible.getSettlementMasked());
        assertNull(visible.getFinancialMovement());assertNull(visible.getSettlementMovement());assertNull(visible.getFinancialBalance());
        Summary result=new Summary();result.setFinancialInAmount(new BigDecimal("1000"));result.setSettlementInAmount(new BigDecimal("1200"));
        when(repository.movementSummary(any(),any())).thenReturn(result);
        when(repository.balanceSummary(any(),any(),any(),eq(true))).thenReturn(new Summary().setMissingOpeningCount(0L).setOutsideCoverageCount(0L).setStaleCount(0L));
        Summary total=service.movementSummary(filter());
        assertEquals("COMPLETE",total.getDataStatus());assertNull(total.getFinancialInAmount());assertNull(total.getSettlementInAmount());
    }

    @Test void ownMovementCannotRevealBalancesBuiltFromOtherUsers() {
        enable();
        when(permission.getDeptDataPermission(9L,"erp_stock_record")).thenReturn(new DeptDataPermissionRespDTO().setSelf(true));
        MovementRow row=new MovementRow().setStockDeptId(100L).setFinancialMovement(new BigDecimal("-100"))
                .setBalanceQuantity(new BigDecimal("500")).setFinancialBalance(new BigDecimal("10000"));
        when(repository.movementPage(any(),any(),anyLong(),anyInt())).thenReturn(Collections.singletonList(row));
        MovementRow visible=service.movementPage(filter()).getList().get(0);
        assertEquals("SELF_MOVEMENT_ONLY",visible.getDataStatus());assertTrue(visible.getBalanceMasked());
        assertNull(visible.getBalanceQuantity());assertNull(visible.getFinancialBalance());assertNotNull(visible.getFinancialMovement());
        assertThrows(ServiceException.class,()->service.balancePage(filter()));
    }

    @Test void incompleteOpeningDoesNotBecomeZeroOrPartialCompanyTotal() {
        enable();
        Summary partial=new Summary().setRowCount(2L).setMissingOpeningCount(1L).setOutsideCoverageCount(0L).setStaleCount(0L);
        partial.setClosingQuantity(new BigDecimal("10")).setClosingFinancialAmount(new BigDecimal("1000"));
        when(repository.balanceSummary(any(),any(),any(),eq(false))).thenReturn(partial);
        Summary visible=service.balanceSummary(filter());
        assertEquals("INCOMPLETE",visible.getDataStatus());assertNull(visible.getClosingQuantity());assertNull(visible.getClosingFinancialAmount());
    }

    @Test void balanceCannotUsePartialMovementFiltersOrSensitiveSort() {
        assertThrows(ServiceException.class,()->ErpStockReportV2Service.validateFilter(filter().setBizTypes(Collections.singletonList(50)),false));
        assertThrows(ServiceException.class,()->ErpStockReportV2Service.validateFilter(filter().setBatchNo("batch1"),false));
        assertThrows(ServiceException.class,()->ErpStockReportV2Service.validateFilter(filter().setOrderField("financialMovement"),true));
        assertThrows(ServiceException.class,()->ErpStockReportV2Service.validateFilter(filter().setOrderField("postedAt;DROP TABLE erp_stock"),true));
    }

    @Test void emptyOrReversedRangeIsRejectedBeforeDataAccess() {
        Filter f=filter();f.setPostedTo(f.getPostedFrom());
        assertThrows(ServiceException.class,()->service.balancePage(f));verifyNoInteractions(repository,permission);
    }
}
