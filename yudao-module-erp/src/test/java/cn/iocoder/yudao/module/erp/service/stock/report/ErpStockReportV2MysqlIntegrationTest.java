package cn.iocoder.yudao.module.erp.service.stock.report;

import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.jackson.config.YudaoJacksonAutoConfiguration;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.controller.admin.report.vo.ErpStockReportV2Models.*;
import cn.iocoder.yudao.module.erp.service.stock.report.ErpStockReportV2Repository.Scope;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.apache.poi.ss.usermodel.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Independent numerical and permission fixtures against an explicitly isolated MySQL instance. */
@EnabledIfEnvironmentVariable(named = "ERP_STOCK_REPORT_MYSQL_TEST_URL", matches = ".+")
class ErpStockReportV2MysqlIntegrationTest {
    private JdbcTemplate jdbc;
    private ErpStockReportV2Repository repository;
    private ErpStockReportV2Service service;
    private PermissionApi permissions;
    private DeptDataPermissionRespDTO recordPermission;
    private final LocalDateTime cutover = LocalDateTime.of(2026, 1, 1, 0, 0);

    @BeforeEach
    void fixture() throws Exception {
        String url = System.getenv("ERP_STOCK_REPORT_MYSQL_TEST_URL");
        assertTrue(url.matches("jdbc:mysql://127\\.0\\.0\\.1:33379/report_stock_v2_test(?:\\?.*)?"));
        jdbc = new JdbcTemplate(new DriverManagerDataSource(url, "root", ""));
        assertTrue(jdbc.queryForObject("SELECT @@datadir", String.class).replace('\\', '/')
                .contains("/Temp/xingyu-report-mysql-"));
        String ddl = new String(Files.readAllBytes(Paths.get("../sql/mysql/erp_report_dual_cost_foundation_20260909.sql")), StandardCharsets.UTF_8);
        for (String statement : ddl.split(";")) if (!statement.trim().isEmpty()) jdbc.execute(statement);
        String originDdl = new String(Files.readAllBytes(Paths.get("../sql/mysql/erp_stock_new_dimension_origin_20260909.sql")), StandardCharsets.UTF_8);
        for (String statement : originDdl.split(";")) if (!statement.trim().isEmpty()) jdbc.execute(statement);
        jdbc.execute("DELETE FROM erp_stock_dual_cost_origin");
        jdbc.execute("CREATE TABLE IF NOT EXISTS erp_stock (id BIGINT PRIMARY KEY,tenant_id BIGINT,product_id BIGINT,warehouse_id BIGINT,dept_id BIGINT,count DECIMAL(24,6),cost_price DECIMAL(24,6),cost_amount DECIMAL(24,6),creator VARCHAR(64),deleted BIT DEFAULT 0) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE IF NOT EXISTS erp_stock_record (id BIGINT PRIMARY KEY,tenant_id BIGINT,product_id BIGINT,warehouse_id BIGINT,creator VARCHAR(64),deleted BIT DEFAULT 0) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE IF NOT EXISTS erp_product (id BIGINT PRIMARY KEY,tenant_id BIGINT,code VARCHAR(64),name VARCHAR(255),deleted BIT DEFAULT 0) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE IF NOT EXISTS erp_warehouse (id BIGINT PRIMARY KEY,tenant_id BIGINT,dept_id BIGINT,name VARCHAR(255),status INT,deleted BIT DEFAULT 0) ENGINE=InnoDB");
        jdbc.execute("CREATE TABLE IF NOT EXISTS system_dept (id BIGINT PRIMARY KEY,tenant_id BIGINT,name VARCHAR(255),status INT,deleted BIT DEFAULT 0) ENGINE=InnoDB");
        for (String table : Arrays.asList("erp_stock_dual_cost_posting", "erp_stock_dual_cost_balance", "erp_stock_record", "erp_stock", "erp_product", "erp_warehouse", "system_dept")) jdbc.execute("DELETE FROM " + table);
        jdbc.update("INSERT INTO system_dept VALUES (301,1,'Department A',0,0),(901,2,'Other tenant department',0,0)");
        jdbc.update("INSERT INTO erp_product VALUES (101,1,'P101','Active part',0),(102,1,'P102','No movement part',0),(901,2,'P901','Other tenant part',0)");
        jdbc.update("INSERT INTO erp_warehouse VALUES (201,1,301,'Warehouse A',0,0),(901,2,901,'Other tenant warehouse',0,0)");
        jdbc.update("INSERT INTO erp_stock VALUES (11,1,101,201,301,11,110,1210,'99',0),(12,1,102,201,301,2,50,100,'88',0),(91,2,901,901,901,5,10,50,'99',0)");
        jdbc.update("INSERT INTO erp_stock_record VALUES (1,1,101,201,'88',0),(2,1,101,201,'99',0)");
        opening(1, 11, "11", "1210", "1430", "10", "1000", "1200", 2, "110", "1210", 301);
        opening(1, 12, "2", "100", "150", "2", "100", "150", 0, "50", "100", 301);
        opening(2, 91, "5", "50", "60", "5", "50", "60", 0, "10", "50", 901);
        event(1, 70, "5", "650", "750", "15", "1650", "1950", cutover.plusDays(1), 88);
        event(2, 50, "-4", "-440", "-520", "11", "1210", "1430", cutover.plusDays(2), 99);
        repository = new ErpStockReportV2Repository();
        ReflectionTestUtils.setField(repository, "jdbcTemplate", jdbc);
        service = new ErpStockReportV2Service();
        permissions = mock(PermissionApi.class);
        recordPermission = new DeptDataPermissionRespDTO();
        recordPermission.setAll(true);
        DeptDataPermissionRespDTO stockPermission = new DeptDataPermissionRespDTO();
        stockPermission.setAll(true);
        when(permissions.getDeptDataPermission(99L, "erp_stock_record")).thenReturn(recordPermission);
        when(permissions.getDeptDataPermission(99L, "erp_stock")).thenReturn(stockPermission);
        ReflectionTestUtils.setField(service, "repository", repository);
        ReflectionTestUtils.setField(service, "permissionApi", permissions);
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "cutover", cutover.toString());
        LoginUser user = new LoginUser();
        user.setId(99L);
        user.setInfo(Collections.singletonMap(LoginUser.INFO_KEY_DEPT_ID, "301"));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, "unused", Collections.emptyList()));
        TenantContextHolder.setTenantId(1L);
    }

    @AfterEach
    void clearContext() {
        TenantContextHolder.clear();
        SecurityContextHolder.clearContext();
    }

    private void opening(long tenant, long stock, String quantity, String financial, String settlement,
                         String initialQuantity, String initialFinancial, String initialSettlement,
                         long legacy, String legacyPrice, String legacyAmount, long dept) {
        jdbc.update("INSERT INTO erp_stock_dual_cost_balance (tenant_id,stock_id,quantity,financial_amount,settlement_amount,opening_quantity,opening_financial_amount,opening_settlement_amount,cutover_at,confirmed_by,confirmed_at,evidence,updated_at,legacy_record_id,legacy_cost_price,legacy_cost_amount,stock_dept_id) VALUES (?,?,?,?,?,?,?,?,?,99,?,'independent fixture',?,?,?,?,?)",
                tenant, stock, quantity, financial, settlement, initialQuantity, initialFinancial, initialSettlement,
                cutover, cutover, cutover, legacy, legacyPrice, legacyAmount, dept);
    }

    private void event(long id, int type, String quantity, String financial, String settlement,
                       String balanceQuantity, String balanceFinancial, String balanceSettlement,
                       LocalDateTime posted, long by) {
        jdbc.update("INSERT INTO erp_stock_dual_cost_posting (id,tenant_id,action_key,request_hash,stock_id,product_id,warehouse_id,stock_dept_id,accounting_dept_id,biz_type,biz_id,biz_item_id,biz_no,biz_date,posted_at,quantity,financial_movement,settlement_movement,balance_quantity,financial_balance,settlement_balance,rule_version,batch_no,posted_by) VALUES (?,1,?,'fixture',11,101,201,301,501,?,?,?,'FIXTURE',?,?,?,?,?,?,?,?,'V1','BATCH-A',?)",
                id, "event-" + id, type, 400 + id, 500 + id, cutover.minusMonths(1), posted,
                quantity, financial, settlement, balanceQuantity, balanceFinancial, balanceSettlement, by);
    }

    private Scope scope() {
        Scope scope = new Scope();
        scope.setTenantId(1);
        scope.setUserId(99);
        scope.setAll(true);
        scope.setWarehouseIds(Collections.singleton(201L));
        scope.setWholeWarehouseIds(Collections.singleton(201L));
        return scope;
    }

    private Filter filter() {
        Filter filter = new Filter();
        filter.setPostedFrom(cutover);
        filter.setPostedTo(cutover.plusDays(3));
        filter.setPageNo(1);
        filter.setPageSize(1);
        return filter;
    }

    private void decimal(String expected, BigDecimal actual) {
        assertNotNull(actual);
        assertEquals(0, new BigDecimal(expected).compareTo(actual));
    }

    @Test
    void exactReceivingIssuingBalanceAndNoMovementStockAreReconciled() {
        List<BalanceRow> rows = repository.balancePage(filter(), scope(), cutover, 0, 10);
        assertEquals(2, rows.size());
        BalanceRow active = rows.stream().filter(row -> row.getStockId() == 11).findFirst().get();
        assertEquals("READY", active.getDataStatus());
        decimal("10", active.getOpeningQuantity());
        decimal("5", active.getInQuantity());
        decimal("4", active.getOutQuantity());
        decimal("11", active.getClosingQuantity());
        decimal("1000", active.getOpeningFinancialAmount());
        decimal("650", active.getFinancialInAmount());
        decimal("440", active.getFinancialOutAmount());
        decimal("1210", active.getClosingFinancialAmount());
        decimal("1200", active.getOpeningSettlementAmount());
        decimal("750", active.getSettlementInAmount());
        decimal("520", active.getSettlementOutAmount());
        decimal("1430", active.getClosingSettlementAmount());
        BalanceRow quiet = rows.stream().filter(row -> row.getStockId() == 12).findFirst().get();
        decimal("2", quiet.getOpeningQuantity());
        decimal("2", quiet.getClosingQuantity());
        decimal("100", quiet.getClosingFinancialAmount());
        decimal("150", quiet.getClosingSettlementAmount());
    }

    @Test
    void exclusiveEndAndNextPeriodOpeningUsePostingTimeNotSourceDate() {
        Filter first = filter();
        first.setProductId(101L);
        first.setPostedTo(cutover.plusDays(2));
        BalanceRow beforeSale = repository.balancePage(first, scope(), cutover, 0, 10).get(0);
        decimal("15", beforeSale.getClosingQuantity());
        decimal("1650", beforeSale.getClosingFinancialAmount());
        assertEquals(1, repository.movementCount(first, scope()));
        Filter second = filter();
        second.setProductId(101L);
        second.setPostedFrom(cutover.plusDays(2));
        BalanceRow salePeriod = repository.balancePage(second, scope(), cutover, 0, 10).get(0);
        decimal("15", salePeriod.getOpeningQuantity());
        decimal("1650", salePeriod.getOpeningFinancialAmount());
        decimal("1950", salePeriod.getOpeningSettlementAmount());
        decimal("11", salePeriod.getClosingQuantity());
        assertEquals(1, repository.movementCount(second, scope()));
    }

    @Test
    void entireSummaryDoesNotDependOnRequestedPage() {
        Filter filter = filter();
        List<BalanceRow> first = repository.balancePage(filter, scope(), cutover, 0, 1);
        List<BalanceRow> second = repository.balancePage(filter, scope(), cutover, 1, 1);
        assertNotEquals(first.get(0).getStockId(), second.get(0).getStockId());
        Summary total = repository.balanceSummary(filter, scope(), cutover, false);
        assertEquals(2L, total.getRowCount());
        decimal("13", total.getClosingQuantity());
        decimal("1310", total.getClosingFinancialAmount());
        decimal("1580", total.getClosingSettlementAmount());
        assertEquals(2L, repository.movementSummary(filter, scope()).getRowCount());
    }

    @Test
    void stockCreatorAndPostingCreatorAreSeparatePermissionAxes() {
        Scope stockSelf = scope();
        stockSelf.setWholeWarehouseIds(Collections.emptySet());
        stockSelf.setSelfWarehouseIds(Collections.singleton(201L));
        assertEquals(Collections.singletonList(11L), repository.balancePage(filter(), stockSelf, cutover, 0, 10)
                .stream().map(BalanceRow::getStockId).collect(Collectors.toList()));
        assertEquals(Collections.singletonList(101L), repository.productOptions("", stockSelf, 0, 10)
                .stream().map(ProductOption::getId).collect(Collectors.toList()));
        assertEquals(1L, repository.productOptionCount("", stockSelf));
        stockSelf.setAll(false);
        stockSelf.setSelf(true);
        List<MovementRow> ownEvents = repository.movementPage(filter(), stockSelf, 0, 10);
        assertEquals(1, ownEvents.size());
        assertEquals(2L, ownEvents.get(0).getPostingId());
    }

    @Test
    void departmentWarehouseAndTenantRestrictionsApplyToAllQueries() {
        Scope deniedDepartment = scope();
        deniedDepartment.setAll(false);
        deniedDepartment.setDeptIds(Collections.singleton(999L));
        assertEquals(0L, repository.movementCount(filter(), deniedDepartment));
        assertEquals(0L, repository.balanceSummary(filter(), deniedDepartment, cutover, false).getRowCount());
        Scope deniedWarehouse = scope();
        deniedWarehouse.setWarehouseIds(Collections.emptySet());
        deniedWarehouse.setWholeWarehouseIds(Collections.emptySet());
        assertTrue(repository.balancePage(filter(), deniedWarehouse, cutover, 0, 10).isEmpty());
        assertTrue(repository.movementPage(filter(), deniedWarehouse, 0, 10).isEmpty());
        assertEquals(2L, repository.balanceSummary(filter(), scope(), cutover, false).getRowCount());
    }

    @Test
    void selfEventsCannotExposeWholeInventoryBalances() {
        recordPermission.setAll(false);
        recordPermission.setSelf(true);
        assertTrue(assertThrows(ServiceException.class, () -> service.balancePage(filter())).getMessage().contains("仅本人"));
        assertEquals(409, assertThrows(ServiceException.class, () -> service.balanceSummary(filter())).getCode());
        List<MovementRow> rows = service.movementPage(filter()).getList();
        assertEquals(1, rows.size());
        MovementRow own = rows.get(0);
        assertEquals(2L, own.getPostingId());
        assertTrue(own.getBalanceMasked());
        assertNull(own.getBalanceQuantity());
        assertNull(own.getFinancialBalance());
        assertNull(own.getSettlementBalance());
        decimal("-440", own.getFinancialMovement());
    }

    @Test
    void departmentSensitiveFieldsAreMaskedIdenticallyInPageSummaryAndExcel() throws Exception {
        when(permissions.getCurrentUserHiddenFields("erp_product", 301L)).thenReturn(Collections.singletonList("financialAmount"));
        Filter f = filter();
        f.setProductId(101L);
        BalanceRow row = service.balancePage(f).getList().get(0);
        assertTrue(row.getFinancialMasked());
        assertNull(row.getOpeningFinancialAmount());
        assertNull(row.getClosingFinancialAmount());
        decimal("1430", row.getClosingSettlementAmount());
        Summary summary = service.balanceSummary(f);
        assertNull(summary.getClosingFinancialAmount());
        decimal("1430", summary.getClosingSettlementAmount());
        MockHttpServletResponse response = new MockHttpServletResponse();
        service.export(f, "BALANCE", response);
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            Sheet sheet = workbook.getSheetAt(0);
            assertEquals(1, sheet.getLastRowNum());
            assertEquals("", cellText(sheet, 1, "期末财务成本"));
            decimal("1430", new BigDecimal(cellText(sheet, 1, "期末结算成本")));
        }
    }

    @Test
    void mixedDepartmentDenyAlsoMasksEntireTotalsAndMovementSnapshots() {
        when(permissions.getCurrentUserHiddenFields("erp_product", 501L)).thenReturn(Collections.singletonList("settlementAmount"));
        MovementRow movement = service.movementPage(filter()).getList().get(0);
        assertTrue(movement.getSettlementMasked());
        assertNull(movement.getSettlementMovement());
        assertNull(movement.getSettlementBalance());
        assertNotNull(movement.getFinancialMovement());
        assertNull(service.movementSummary(filter()).getSettlementInAmount());
    }

    @Test
    void missingOpeningAndStaleCostsNeverBecomeZeroValuedBalances() {
        jdbc.update("INSERT INTO erp_stock VALUES (13,1,103,201,301,3,40,120,'99',0)");
        Filter missing = filter();
        missing.setProductId(103L);
        BalanceRow unknown = service.balancePage(missing).getList().get(0);
        assertEquals("MISSING_OPENING", unknown.getDataStatus());
        assertNull(unknown.getOpeningQuantity());
        assertNull(unknown.getInQuantity());
        assertNull(unknown.getClosingFinancialAmount());
        Summary total = service.balanceSummary(filter());
        assertEquals("INCOMPLETE", total.getDataStatus());
        assertEquals(1L, total.getMissingOpeningCount());
        assertNull(total.getClosingQuantity());
        jdbc.update("UPDATE erp_stock SET cost_amount=1211 WHERE id=11");
        Filter stale = filter();
        stale.setProductId(101L);
        BalanceRow changed = service.balancePage(stale).getList().get(0);
        assertEquals("STALE_OPENING", changed.getDataStatus());
        assertNull(changed.getClosingQuantity());
        assertEquals("INCOMPLETE", service.balanceSummary(stale).getDataStatus());
    }

    @Test
    void periodBeforeCutoverIsExplicitlyIncompleteAndPartialFlowFilterIsRejected() {
        Filter tooEarly = filter();
        tooEarly.setPostedFrom(cutover.minusDays(1));
        BalanceRow row = service.balancePage(tooEarly).getList().get(0);
        assertEquals("BEFORE_CUTOVER", row.getDataStatus());
        assertNull(row.getOpeningFinancialAmount());
        assertEquals("INCOMPLETE", service.balanceSummary(tooEarly).getDataStatus());
        Filter subset = filter();
        subset.setBizTypes(Collections.singletonList(70));
        assertTrue(assertThrows(ServiceException.class, () -> service.balancePage(subset)).getMessage().contains("局部发生额"));
    }

    @Test
    void partiallyMigratedSchemaReturnsExplicitUnavailableStatus() {
        jdbc.execute("ALTER TABLE erp_stock_dual_cost_balance RENAME COLUMN legacy_cost_price TO old_legacy_cost_price");
        try {
            assertEquals("SCHEMA_MISSING", service.status().getStatus());
            assertEquals(409, assertThrows(ServiceException.class, () -> service.balancePage(filter())).getCode());
        } finally {
            jdbc.execute("ALTER TABLE erp_stock_dual_cost_balance RENAME COLUMN old_legacy_cost_price TO legacy_cost_price");
        }
    }

    @Test
    void largeDecimalIsExactInJsonAndExcelAndExportUsesFullFilteredSet() throws Exception {
        String exact = "123456789012345.123456";
        jdbc.update("UPDATE erp_stock_dual_cost_balance SET opening_financial_amount=?,financial_amount=? WHERE stock_id=12", exact, exact);
        Filter f = filter();
        f.setProductId(102L);
        BalanceRow row = service.balancePage(f).getList().get(0);
        YudaoJacksonAutoConfiguration jackson = new YudaoJacksonAutoConfiguration();
        Jackson2ObjectMapperBuilder builder = Jackson2ObjectMapperBuilder.json();
        jackson.ldtEpochMillisCustomizer().customize(builder);
        ObjectMapper mapper = builder.build().registerModule(jackson.timestampSupportModuleBean());
        JsonNode json = mapper.valueToTree(row);
        assertTrue(json.get("closingFinancialAmount").isTextual(), "JSON must preserve decimal as a string");
        assertEquals(exact, json.get("closingFinancialAmount").asText());
        assertTrue(json.get("cutoverAt").isTextual());
        assertEquals(cutover, LocalDateTime.parse(json.get("cutoverAt").asText()));
        JsonNode movementJson = mapper.valueToTree(service.movementPage(filter()).getList().get(0));
        assertTrue(movementJson.get("postedAt").isTextual());
        assertEquals(cutover.plusDays(1), LocalDateTime.parse(movementJson.get("postedAt").asText()));
        MockHttpServletResponse response = new MockHttpServletResponse();
        service.export(f, "BALANCE", response);
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            Sheet sheet = workbook.getSheetAt(0);
            int index = column(sheet, "期末财务成本");
            assertEquals(CellType.STRING, sheet.getRow(1).getCell(index).getCellType());
            assertEquals(exact, sheet.getRow(1).getCell(index).getStringCellValue());
            assertEquals(1, sheet.getLastRowNum());
        }
        MockHttpServletResponse allResponse = new MockHttpServletResponse();
        service.export(filter(), "BALANCE", allResponse);
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(allResponse.getContentAsByteArray()))) {
            assertEquals(2, workbook.getSheetAt(0).getLastRowNum(), "Export must include both rows despite pageSize=1");
        }
    }

    @Test
    void businessTypesPreserveSignedZeroQuantityAdjustmentsAndUnknownTypes() throws Exception {
        event(3, 999, "0", "12.123456", "-2.123456", "11", "1222.123456", "1427.876544", cutover.plusDays(2).plusSeconds(1), 99);
        Filter f=filter(); f.setPageSize(20);
        BalanceRow row=service.balancePage(f).getList().stream().filter(r->r.getStockId()==11).findFirst().get();
        Map<Integer,BizTypeAmount> types=row.getByBizType().stream().collect(Collectors.toMap(BizTypeAmount::getBizType, x->x));
        assertEquals(new HashSet<>(Arrays.asList(50,70,999)),types.keySet());
        decimal("-4",types.get(50).getQuantity()); decimal("650",types.get(70).getFinancialMovement());
        decimal("0",types.get(999).getQuantity()); decimal("12.123456",types.get(999).getFinancialMovement());
        decimal("-2.123456",types.get(999).getSettlementMovement());
        Summary sum=service.balanceSummary(filter());
        assertEquals(3,sum.getByBizType().size());
        decimal("222.123456",sum.getByBizType().stream().map(BizTypeAmount::getFinancialMovement).reduce(BigDecimal.ZERO,BigDecimal::add));
        MockHttpServletResponse response=new MockHttpServletResponse(); service.export(f,"BALANCE",response);
        try(Workbook workbook=WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            Sheet sheet=workbook.getSheet("业务类型发生额"); assertNotNull(sheet); assertEquals(3,sheet.getLastRowNum());
            assertEquals("12.123456",cellText(sheet,3,"财务成本净发生"));
            assertEquals(CellType.STRING,sheet.getRow(3).getCell(column(sheet,"财务成本净发生")).getCellType());
        }
        when(permissions.getCurrentUserHiddenFields("erp_product",301L)).thenReturn(Collections.singletonList("financialAmount"));
        assertTrue(service.balancePage(f).getList().get(0).getByBizType().stream().allMatch(x->x.getFinancialMovement()==null));
        assertTrue(service.balanceSummary(f).getByBizType().stream().allMatch(x->x.getFinancialMovement()==null));
        response=new MockHttpServletResponse();service.export(f,"BALANCE",response);
        try(Workbook workbook=WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            assertEquals("",cellText(workbook.getSheet("业务类型发生额"),1,"财务成本净发生"));
        }
    }

    @Test
    void allDecimalExcelFieldsIncludingInheritedAndBusinessTypeFieldsRemainExactText() throws Exception {
        String exact="123456789012345.123456";
        for(Class<?> type:Arrays.asList(BalanceRow.class,MovementRow.class,BizTypeAmount.class)) {
            Object row=type.getDeclaredConstructor().newInstance();
            List<String> titles=new ArrayList<>();
            for(Class<?> current=type;current!=Object.class;current=current.getSuperclass())
                for(java.lang.reflect.Field field:current.getDeclaredFields()) if(field.getType()==BigDecimal.class) {
                    field.setAccessible(true);field.set(row,new BigDecimal(exact));
                    cn.idev.excel.annotation.ExcelProperty annotation=field.getAnnotation(cn.idev.excel.annotation.ExcelProperty.class);
                    assertNotNull(annotation,field.getName());titles.add(annotation.value()[0]);
                }
            java.io.ByteArrayOutputStream bytes=new java.io.ByteArrayOutputStream();
            cn.idev.excel.FastExcelFactory.write(bytes,type).sheet(0,"fields").doWrite(Collections.singletonList(row));
            try(Workbook workbook=WorkbookFactory.create(new ByteArrayInputStream(bytes.toByteArray()))) {
                for(String title:titles) {
                    Cell cell=workbook.getSheetAt(0).getRow(1).getCell(column(workbook.getSheetAt(0),title));
                    assertEquals(CellType.STRING,cell.getCellType(),type.getSimpleName()+title);
                    assertEquals(exact,cell.getStringCellValue(),type.getSimpleName()+title);
                }
            }
        }
    }

    @Test
    void historicalDisabledWarehouseAndDepartmentOptionsRemainSelectableWithinScope() {
        jdbc.update("UPDATE erp_warehouse SET status=1 WHERE id=201");
        jdbc.update("UPDATE system_dept SET status=1 WHERE id=301");
        ReportOptions options=service.reportOptions();
        assertEquals(Collections.singletonList(201L),options.getWarehouses().stream().map(HistoryOption::getId).collect(Collectors.toList()));
        assertEquals(Integer.valueOf(1),options.getWarehouses().get(0).getStatus());
        assertEquals(Collections.singletonList(301L),options.getStockDepartments().stream().map(HistoryOption::getId).collect(Collectors.toList()));
        assertEquals(Integer.valueOf(1),options.getStockDepartments().get(0).getStatus());
        Scope restricted=scope();restricted.setAll(false);restricted.setDeptIds(Collections.singleton(999L));
        assertTrue(repository.reportOptions(restricted).getStockDepartments().isEmpty());
        restricted.setWarehouseIds(Collections.emptySet());restricted.setWholeWarehouseIds(Collections.emptySet());
        assertTrue(repository.reportOptions(restricted).getWarehouses().isEmpty());
    }

    @Test
    void historyOptionsWorkBeforeNewLedgerTablesExist() {
        jdbc.execute("RENAME TABLE erp_stock_dual_cost_balance TO fixture_saved_balance, erp_stock_dual_cost_posting TO fixture_saved_posting");
        try {
            ReflectionTestUtils.setField(service,"enabled",false);
            ReportOptions options=service.reportOptions();
            assertEquals(1,options.getWarehouses().size());
            assertEquals(1,options.getStockDepartments().size());
        } finally {
            jdbc.execute("RENAME TABLE fixture_saved_balance TO erp_stock_dual_cost_balance, fixture_saved_posting TO erp_stock_dual_cost_posting");
        }
    }

    @Test
    void exactMiddayCutoverAndSecondPrecisionBoundsDoNotIncludeEarlierEvents() {
        LocalDateTime midday=cutover.withHour(12).withMinute(34).withSecond(56);
        jdbc.update("UPDATE erp_stock_dual_cost_balance SET cutover_at=? WHERE tenant_id=1",midday);
        ReflectionTestUtils.setField(service,"cutover",midday.toString());
        Filter f=filter();f.setPostedFrom(midday);
        f.setPostedTo(cutover.plusDays(1));
        decimal("10",service.balancePage(f).getList().get(0).getClosingQuantity());
        f.setPostedTo(cutover.plusDays(1).plusSeconds(1));
        decimal("15",service.balancePage(f).getList().get(0).getClosingQuantity());
        f.setPostedFrom(midday.minusSeconds(1));
        assertEquals("BEFORE_CUTOVER",service.balancePage(f).getList().get(0).getDataStatus());
    }

    @Test
    void newbornStockIsExcludedBeforeItsMicrosecondBirthAndCrossingRangeRetainsOrigin() throws Exception {
        LocalDateTime birth=cutover.plusDays(1).withHour(12).withMinute(34).withSecond(56).withNano(123456000);
        jdbc.update("UPDATE erp_stock SET count=0,cost_price=0,cost_amount=0 WHERE id=12");
        jdbc.update("UPDATE erp_stock_dual_cost_balance SET quantity=0,financial_amount=0,settlement_amount=0,opening_quantity=0,opening_financial_amount=0,opening_settlement_amount=0,legacy_cost_price=0,legacy_cost_amount=0 WHERE stock_id=12");
        jdbc.update("INSERT INTO erp_stock_dual_cost_origin(tenant_id,stock_id,product_id,warehouse_id,origin_kind,available_from,created_by,action_key,history_check_version,created_at) VALUES(1,12,102,201,'NEW_DIMENSION',?,99,'newborn','TEST',?)",birth,birth);
        Filter f=filter();f.setProductId(102L);f.setPostedTo(birth);
        assertEquals(0L,service.balancePage(f).getTotal());assertEquals(0L,service.balanceSummary(f).getRowCount());
        f.setPostedTo(birth.plusNanos(1000));
        BalanceRow row=service.balancePage(f).getList().get(0);
        assertEquals("NEW_DIMENSION",row.getOriginKind());assertEquals(birth,row.getAvailableFrom());assertEquals("READY",row.getDataStatus());decimal("0",row.getOpeningQuantity());decimal("0",row.getClosingFinancialAmount());
        assertEquals(1L,service.balanceSummary(f).getRowCount());
        MockHttpServletResponse response=new MockHttpServletResponse();service.export(f,"BALANCE",response);
        try(Workbook workbook=WorkbookFactory.create(new ByteArrayInputStream(response.getContentAsByteArray()))) {
            Sheet sheet=workbook.getSheetAt(0);assertEquals("NEW_DIMENSION",cellText(sheet,1,"核算起点类型"));
            assertTrue(cellText(sheet,1,"可计算起点").contains("2026-01-02"));
            assertTrue(cellText(sheet,1,"可计算起点").contains("12:34:56.123456"),"export must preserve the actual microsecond boundary: "+cellText(sheet,1,"可计算起点"));
        }
        when(permissions.getCurrentUserHiddenFields("erp_product",301L)).thenReturn(Collections.singletonList("costAmount"));
        assertNull(service.balancePage(f).getList().get(0).getClosingFinancialAmount());assertNull(service.balanceSummary(f).getClosingFinancialAmount());
        Scope denied=scope();denied.setWholeWarehouseIds(Collections.emptySet());denied.setSelfWarehouseIds(Collections.singleton(201L));
        assertTrue(repository.balancePage(f,denied,cutover,0,10).isEmpty());
    }

    @Test
    void manualBalanceMetadataRemainsCutoverAndMissingOriginSchemaIsExplicit() {
        BalanceRow row=service.balancePage(filter()).getList().get(0);assertEquals("MANUAL_OPENING",row.getOriginKind());assertEquals(cutover,row.getAvailableFrom());
        jdbc.execute("RENAME TABLE erp_stock_dual_cost_origin TO saved_origin_schema");
        try{assertEquals("SCHEMA_MISSING",service.status().getStatus());}
        finally{jdbc.execute("RENAME TABLE saved_origin_schema TO erp_stock_dual_cost_origin");}
    }

    private int column(Sheet sheet, String title) {
        for (Cell cell : sheet.getRow(0)) if (title.equals(cell.getStringCellValue())) return cell.getColumnIndex();
        throw new AssertionError("Missing Excel header " + title);
    }

    private String cellText(Sheet sheet, int row, String title) {
        return new DataFormatter(Locale.ROOT).formatCellValue(sheet.getRow(row).getCell(column(sheet, title)));
    }
}
