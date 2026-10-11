package cn.iocoder.yudao.module.erp.service.assistant;

import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.junit.jupiter.api.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;

class AssistantSecurityTest {
    AssistantQueryService service;
    PermissionApi permission;
    @BeforeEach void setup() {
        service=new AssistantQueryService();permission=mock(PermissionApi.class);ReflectionTestUtils.setField(service,"permissions",permission);
        LoginUser user=new LoginUser();user.setId(101L);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user,"unused",Collections.emptyList()));
        TenantContextHolder.setTenantId(1L);
        when(permission.hasAnyPermissions(anyLong(),anyString())).thenReturn(true);
        when(permission.getCurrentUserHiddenFields(anyString())).thenReturn(Collections.emptyList());
        when(permission.getCurrentUserHiddenFields(anyString(),nullable(Long.class),eq(false))).thenReturn(Collections.emptyList());
    }
    @AfterEach void clear() {TenantContextHolder.clear();SecurityContextHolder.clearContext();}
    @Test void missingIdentityOrTenantFailsClosed() {
        TenantContextHolder.clear();assertThrows(AssistantFailure.class,()->service.authorize(AssistantPlan.Metric.STOCK));
        TenantContextHolder.setTenantId(1L);SecurityContextHolder.clearContext();assertThrows(AssistantFailure.class,()->service.authorize(AssistantPlan.Metric.SALE));
    }
    @Test void entryPermissionDoesNotGrantBusinessPermission() {
        when(permission.hasAnyPermissions(101L,"erp:sale-return:query")).thenReturn(false);
        assertThrows(AssistantFailure.class,()->service.authorize(AssistantPlan.Metric.SALE));
    }
    @Test void hiddenAmountCannotLeakByAggregationOrRanking() {
        when(permission.getCurrentUserHiddenFields("erp_sale_out",null,false)).thenReturn(Collections.singletonList("col_totalPrice"));
        assertThrows(AssistantFailure.class,()->service.authorize(AssistantPlan.Metric.SALE));
        assertDoesNotThrow(()->service.authorize(AssistantPlan.Metric.STOCK));
    }
    @Test void salesReportUsesRecordedAmountPermissionAndKeepsExplicitSourceRestrictions() {
        when(permission.getCurrentUserHiddenFields("erp_product")).thenReturn(Collections.singletonList("col_costPrice"));
        when(permission.getCurrentUserHiddenFields("erp_sale_out")).thenReturn(Collections.singletonList("col_totalPrice"));
        assertDoesNotThrow(()->service.authorize(AssistantPlan.Metric.SALE));
        assertDoesNotThrow(()->service.authorize(AssistantPlan.Metric.RECEIVABLE));
        verify(permission,times(2)).getCurrentUserHiddenFields("erp_sale_out",null,false);
        verify(permission,never()).getCurrentUserHiddenFields("erp_sale_out");
        when(permission.getCurrentUserHiddenFields("erp_sale_return",null,false)).thenReturn(Collections.singletonList("col_totalPrice"));
        assertThrows(AssistantFailure.class,()->service.authorize(AssistantPlan.Metric.SALE));
        when(permission.getCurrentUserHiddenFields("erp_sale_return",null,false)).thenReturn(Collections.emptyList());
        when(permission.getCurrentUserHiddenFields("erp_sale_out",null,false)).thenReturn(Collections.singletonList("col_totalPrice"));
        assertThrows(AssistantFailure.class,()->service.authorize(AssistantPlan.Metric.RECEIVABLE));
        when(permission.getCurrentUserHiddenFields("erp_sale_out",null,false)).thenReturn(Collections.emptyList());
        when(permission.getCurrentUserHiddenFields("erp_product")).thenReturn(Collections.singletonList("col_salePrice"));
        assertDoesNotThrow(()->service.authorize(AssistantPlan.Metric.SALE));
        assertDoesNotThrow(()->service.authorize(AssistantPlan.Metric.RECEIVABLE));
    }
    @Test void unsupportedDimensionAndHistoryFailBeforeQuery() {
        AssistantPlan plan=new AssistantPlan();plan.setMetric(AssistantPlan.Metric.STOCK);plan.setPeriod("LAST_MONTH");
        assertThrows(IllegalArgumentException.class,plan::validate);
        plan.setMetric(AssistantPlan.Metric.RECEIPT);plan.setPeriod("THIS_WEEK");plan.setGroup(AssistantPlan.Group.PRODUCT);
        assertThrows(IllegalArgumentException.class,plan::validate);
    }
    @Test void shanghaiMondayAndMonthBoundaries() {
        AssistantPlan plan=new AssistantPlan();plan.setMetric(AssistantPlan.Metric.SALE);plan.setPeriod("THIS_WEEK");
        Clock clock=Clock.fixed(Instant.parse("2026-09-24T02:00:00Z"),ZoneOffset.UTC);
        assertArrayEquals(new LocalDateTime[]{LocalDateTime.of(2026,9,21,0,0),LocalDateTime.of(2026,9,24,10,0)},plan.range(clock));
        plan.setPeriod("LAST_MONTH");assertArrayEquals(new LocalDateTime[]{LocalDateTime.of(2026,8,1,0,0),LocalDateTime.of(2026,9,1,0,0)},plan.range(clock));
    }
    @Test void shanghaiCurrentAndLastYearBoundaries() {
        AssistantPlan plan=new AssistantPlan();plan.setMetric(AssistantPlan.Metric.SALE);plan.setPeriod("THIS_YEAR");
        Clock clock=Clock.fixed(Instant.parse("2026-09-29T07:30:00Z"),ZoneOffset.UTC);
        assertArrayEquals(new LocalDateTime[]{LocalDateTime.of(2026,1,1,0,0),LocalDateTime.of(2026,9,29,15,30)},plan.range(clock));
        plan.setPeriod("LAST_YEAR");
        assertArrayEquals(new LocalDateTime[]{LocalDateTime.of(2025,1,1,0,0),LocalDateTime.of(2026,1,1,0,0)},plan.range(clock));
    }
    @Test void missingKeyNeverMakesNetworkRequest() {
        AssistantModelClient client=new AssistantModelClient();ReflectionTestUtils.setField(client,"properties",new AssistantProperties());
        AssistantFailure error=assertThrows(AssistantFailure.class,()->client.parse("本月销售额",null,Collections.emptyList()));
        assertEquals("MODEL_NOT_CONFIGURED",error.getCode());
    }
    @Test void knowledgeIsNotExecutableWithoutDeploymentPublication() throws Exception {
        AssistantKnowledge knowledge=new AssistantKnowledge();ReflectionTestUtils.setField(knowledge,"properties",new AssistantProperties());knowledge.load();
        assertEquals(8,knowledge.catalog().size());assertThrows(AssistantFailure.class,()->knowledge.requirePublished(AssistantPlan.Metric.SALE));
    }
    @Test void departmentSpecificHiddenFieldsBlockBeforeAmountsAreRead() {
        cn.iocoder.yudao.module.erp.dal.mysql.assistant.AssistantQueryMapper mapper=mock(cn.iocoder.yudao.module.erp.dal.mysql.assistant.AssistantQueryMapper.class);
        cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService warehouses=mock(cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService.class);
        AssistantReadOnly reader=mock(AssistantReadOnly.class);
        when(reader.snapshot(any())).thenAnswer(call -> ((java.util.function.Supplier<?>)call.getArgument(0)).get());
        when(reader.select(anyMap())).thenAnswer(call -> mapper.select(call.getArgument(0)));
        ReflectionTestUtils.setField(service,"reader",reader);ReflectionTestUtils.setField(service,"warehouses",warehouses);ReflectionTestUtils.setField(service,"knowledge",mock(AssistantKnowledge.class));
        when(warehouses.getCurrentUserProductStockPermissionScope()).thenReturn(new cn.iocoder.yudao.module.erp.service.stock.bo.ErpProductStockPermissionScope(true,Collections.emptySet(),Collections.emptySet(),101L));
        when(permission.getCurrentUserHiddenFields(anyString(),eq(20L))).thenReturn(Collections.singletonList("col_count"));
        when(mapper.select(anyMap())).thenAnswer(invocation->{Map<?,?> args=invocation.getArgument(0);assertEquals("permissionDepartments",args.get("mode"));return Collections.singletonList(Collections.singletonMap("deptId",20L));});
        AssistantPlan plan=new AssistantPlan();plan.setMetric(AssistantPlan.Metric.STOCK);plan.setPeriod("CURRENT");
        assertThrows(AssistantFailure.class,()->service.execute(plan));verify(mapper,times(1)).select(anyMap());
    }
    @Test void hiddenProductPriceFieldsAreRemovedFromProductToolResult() {
        AssistantReadOnly reader=mock(AssistantReadOnly.class);
        cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService warehouses=mock(cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService.class);
        when(reader.snapshot(any())).thenAnswer(call->((java.util.function.Supplier<?>)call.getArgument(0)).get());
        when(warehouses.getCurrentUserProductStockPermissionScope()).thenReturn(new cn.iocoder.yudao.module.erp.service.stock.bo.ErpProductStockPermissionScope(true,Collections.emptySet(),Collections.emptySet(),101L));
        ReflectionTestUtils.setField(service,"reader",reader);ReflectionTestUtils.setField(service,"warehouses",warehouses);
        ReflectionTestUtils.setField(service,"knowledge",mock(AssistantKnowledge.class));
        when(permission.getCurrentUserHiddenFields("erp_product")).thenReturn(Collections.singletonList("col_sharePrice"));
        when(reader.select(anyMap())).thenAnswer(call->{
            String mode=(String)((Map<?,?>)call.getArgument(0)).get("mode");
            if("permissionDepartments".equals(mode)) return Collections.emptyList();
            if("summary".equals(mode)) {
                Map<String,Object> row=new LinkedHashMap<>();row.put("label","合计");row.put("unit","桶");row.put("amount",3);
                row.put("sharePrice",88);row.put("retailPrice",128);return Collections.singletonList(row);
            }
            return Collections.emptyList();
        });
        AssistantPlan plan=new AssistantPlan();plan.setMetric(AssistantPlan.Metric.STOCK);plan.setPeriod("CURRENT");plan.setIncludeProductPrices(true);

        Map<String,Object> result=service.execute(plan);

        Map<?,?> row=(Map<?,?>)((List<?>)result.get("summary")).get(0);
        assertFalse(row.containsKey("sharePrice"));
        assertEquals(128,row.get("retailPrice"));
        assertTrue(String.valueOf(result.get("priceFields")).contains("零售价"));
        assertFalse(String.valueOf(result.get("priceFields")).contains("股份价"));
        assertEquals("PARTIAL",result.get("priceStatus"));
        assertTrue(String.valueOf(result.get("priceNotice")).contains("以下价格暂无数据"));
        assertTrue(String.valueOf(result.get("priceNotice")).contains("部分价格字段当前账号不可见"));
    }
    @Test void permittedButNullPricesAreReportedAsNotMaintainedInsteadOfNoPermission() {
        Map<String,Object> result=executeProductPrice(Collections.emptyMap(),Collections.emptyList(),AssistantPlan.PriceQueryMode.ALL,null);

        assertEquals("NOT_MAINTAINED",result.get("priceStatus"));
        assertEquals("已找到该产品，但尚未完善价格信息",result.get("priceNotice"));
        assertEquals(Collections.emptyList(),result.get("priceFields"));
        assertEquals(8,((List<?>)result.get("missingPriceFields")).size());
        assertFalse(String.valueOf(result.get("priceNotice")).contains("没有可见"));
    }
    @Test void zeroPriceIsMaintainedAndPartialResultListsOnlyNullPrices() {
        Map<String,Object> values=new LinkedHashMap<>();values.put("retailPrice",0);values.put("sharePrice",88);
        Map<String,Object> result=executeProductPrice(values,Collections.emptyList(),AssistantPlan.PriceQueryMode.ALL,null);

        assertEquals("PARTIAL",result.get("priceStatus"));
        assertTrue(String.valueOf(result.get("priceFields")).contains("零售价"));
        assertTrue(String.valueOf(result.get("priceFields")).contains("股份价"));
        assertTrue(String.valueOf(result.get("priceNotice")).contains("采购价"));
        Map<?,?> row=(Map<?,?>)((List<?>)result.get("summary")).get(0);
        assertEquals(0,row.get("retailPrice"));
    }
    @Test void specificMissingPriceOnlyAnswersRequestedField() {
        Map<String,Object> values=new LinkedHashMap<>();values.put("retailPrice",128);values.put("sharePrice",null);
        Map<String,Object> result=executeProductPrice(values,Collections.emptyList(),AssistantPlan.PriceQueryMode.SPECIFIC,"sharePrice");

        assertEquals("NOT_MAINTAINED",result.get("priceStatus"));
        assertEquals("已找到该产品，但尚未维护股份价",result.get("priceNotice"));
        assertEquals(1,((List<?>)result.get("missingPriceFields")).size());
        Map<?,?> row=(Map<?,?>)((List<?>)result.get("summary")).get(0);
        assertFalse(row.containsKey("retailPrice"));
        assertFalse(row.containsKey("sharePrice"));
    }
    @Test void missingLastPurchasePriceExplainsNoPurchaseInboundPrice() {
        Map<String,Object> result=executeProductPrice(Collections.emptyMap(),Collections.emptyList(),AssistantPlan.PriceQueryMode.SPECIFIC,"lastPurchasePrice");

        assertEquals("NOT_MAINTAINED",result.get("priceStatus"));
        assertEquals("已找到该产品，但暂无最后采购价，尚未形成采购入库价格",result.get("priceNotice"));
    }
    @Test void hiddenRequestedPriceIsNoPermissionRatherThanNotMaintained() {
        AssistantFailure failure=assertThrows(AssistantFailure.class,()->executeProductPrice(
                Collections.singletonMap("sharePrice",88),Collections.singletonList("col_sharePrice"),
                AssistantPlan.PriceQueryMode.SPECIFIC,"sharePrice"));

        assertEquals("NO_PERMISSION",failure.getCode());
        assertTrue(failure.getMessage().contains("股份价"));
    }
    @Test void allHiddenGenericPricesAreNoPermission() {
        List<String> hidden=Arrays.asList("purchasePrice","salePrice","minPrice","referencePrice","retailPrice","wholesalePrice","sharePrice","lastPurchasePrice");
        AssistantFailure failure=assertThrows(AssistantFailure.class,()->executeProductPrice(Collections.emptyMap(),hidden,AssistantPlan.PriceQueryMode.ALL,null));

        assertEquals("NO_PERMISSION",failure.getCode());
        assertEquals("当前账号没有可见的产品价格字段",failure.getMessage());
    }
    @Test void allHiddenPricesDoNotBlockAnImplicitProductStockQuery() {
        List<String> hidden=Arrays.asList("purchasePrice","salePrice","minPrice","referencePrice","retailPrice","wholesalePrice","sharePrice","lastPurchasePrice");
        AssistantReadOnly reader=mock(AssistantReadOnly.class);
        cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService warehouses=mock(cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService.class);
        when(reader.snapshot(any())).thenAnswer(call->((java.util.function.Supplier<?>)call.getArgument(0)).get());
        when(warehouses.getCurrentUserProductStockPermissionScope()).thenReturn(
                new cn.iocoder.yudao.module.erp.service.stock.bo.ErpProductStockPermissionScope(true,Collections.emptySet(),Collections.emptySet(),101L));
        ReflectionTestUtils.setField(service,"reader",reader);ReflectionTestUtils.setField(service,"warehouses",warehouses);
        ReflectionTestUtils.setField(service,"knowledge",mock(AssistantKnowledge.class));
        when(permission.getCurrentUserHiddenFields("erp_product")).thenReturn(hidden);
        when(reader.select(anyMap())).thenAnswer(call->{
            String mode=String.valueOf(((Map<?,?>)call.getArgument(0)).get("mode"));
            if("permissionDepartments".equals(mode)) return Collections.emptyList();
            if("summary".equals(mode)) {Map<String,Object> row=new LinkedHashMap<>();row.put("label","合计");row.put("unit","桶");row.put("amount",5);return Collections.singletonList(row);}
            if("productResults".equals(mode)) return Collections.singletonList(productStockRow(1L,"产品A",11L,"仓A",5,"sharePrice",369));
            return Collections.emptyList();
        });
        AssistantPlan plan=new AssistantPlan();plan.setMetric(AssistantPlan.Metric.STOCK);plan.setPeriod("CURRENT");
        plan.setIncludeProductPrices(true);plan.setPriceQueryMode(AssistantPlan.PriceQueryMode.NONE);
        plan.setProductResultTable(true);plan.setProductIds(Collections.singletonList(1L));

        Map<String,Object> result=service.execute(plan);

        assertEquals("NO_PERMISSION",result.get("priceStatus"));
        Map<?,?> product=(Map<?,?>)((List<?>)result.get("productResults")).get(0);
        assertEquals(Collections.emptyList(),product.get("prices"));
        assertEquals("当前账号没有可见的产品价格字段",product.get("priceDisplayText"));
        assertEquals(1,((List<?>)product.get("warehouseStocks")).size());
    }
    @Test void productResultTableAggregatesWarehouseRowsKeepsNegativesAndUsesNoStockState() {
        AssistantReadOnly reader=mock(AssistantReadOnly.class);
        cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService warehouses=mock(cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService.class);
        when(reader.snapshot(any())).thenAnswer(call->((java.util.function.Supplier<?>)call.getArgument(0)).get());
        when(warehouses.getCurrentUserProductStockPermissionScope()).thenReturn(
                new cn.iocoder.yudao.module.erp.service.stock.bo.ErpProductStockPermissionScope(true,Collections.emptySet(),Collections.emptySet(),101L));
        ReflectionTestUtils.setField(service,"reader",reader);ReflectionTestUtils.setField(service,"warehouses",warehouses);
        ReflectionTestUtils.setField(service,"knowledge",mock(AssistantKnowledge.class));
        when(reader.select(anyMap())).thenAnswer(call->{
            String mode=String.valueOf(((Map<?,?>)call.getArgument(0)).get("mode"));
            if("permissionDepartments".equals(mode)) return Collections.emptyList();
            if("summary".equals(mode)) {Map<String,Object> row=new LinkedHashMap<>();row.put("label","合计");row.put("unit","桶");row.put("amount",0);return Collections.singletonList(row);}
            if("productResults".equals(mode)) {
                List<Map<String,Object>> values=new ArrayList<>();
                values.add(productStockRow(1L,"产品A",11L,"仓A",3,"sharePrice",369));
                values.add(productStockRow(1L,"产品A",11L,"仓A",2,"sharePrice",369));
                values.add(productStockRow(1L,"产品A",12L,"仓B",-1,"sharePrice",369));
                values.add(productStockRow(1L,"产品A",13L,"仓C",0,"sharePrice",369));
                values.add(productStockRow(2L,"产品B",null,null,0,"retailPrice",399));
                return values;
            }
            return Collections.emptyList();
        });
        AssistantPlan plan=new AssistantPlan();plan.setMetric(AssistantPlan.Metric.STOCK);plan.setPeriod("CURRENT");
        plan.setIncludeProductPrices(true);plan.setPriceQueryMode(AssistantPlan.PriceQueryMode.ALL);
        plan.setProductResultTable(true);plan.setProductIds(Arrays.asList(1L,2L));plan.setProductMatchTruncated(true);

        Map<String,Object> result=service.execute(plan);

        List<?> products=(List<?>)result.get("productResults");assertEquals(2,products.size());
        Map<?,?> first=(Map<?,?>)products.get(0);List<?> firstStocks=(List<?>)first.get("warehouseStocks");
        assertEquals(2,firstStocks.size());assertEquals(new java.math.BigDecimal("5"),((Map<?,?>)firstStocks.get(0)).get("amount"));
        assertEquals(new java.math.BigDecimal("-1"),((Map<?,?>)firstStocks.get(1)).get("amount"));
        assertEquals(Collections.emptyList(),((Map<?,?>)products.get(1)).get("warehouseStocks"));
        assertEquals(true,result.get("productMatchTruncated"));assertEquals(10,result.get("productMatchLimit"));
    }
    @Test void rankingSkipsUnitSummaryAndUsesRowsAsSuccessfulResult() {
        AssistantReadOnly reader=mock(AssistantReadOnly.class);
        cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService warehouses=mock(cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService.class);
        when(reader.snapshot(any())).thenAnswer(call->((java.util.function.Supplier<?>)call.getArgument(0)).get());
        when(warehouses.getCurrentUserProductStockPermissionScope()).thenReturn(new cn.iocoder.yudao.module.erp.service.stock.bo.ErpProductStockPermissionScope(true,Collections.emptySet(),Collections.emptySet(),101L));
        ReflectionTestUtils.setField(service,"reader",reader);ReflectionTestUtils.setField(service,"warehouses",warehouses);
        ReflectionTestUtils.setField(service,"knowledge",mock(AssistantKnowledge.class));
        List<String> modes=new ArrayList<>();
        when(reader.select(anyMap())).thenAnswer(call->{
            String mode=String.valueOf(((Map<?,?>)call.getArgument(0)).get("mode"));modes.add(mode);
            if("permissionDepartments".equals(mode)) return Collections.emptyList();
            if("groups".equals(mode)) {
                Map<String,Object> row=new LinkedHashMap<>();row.put("label","产品A");row.put("amount",20);row.put("unit","件");row.put("unitRank",1);
                return Collections.singletonList(row);
            }
            fail("ranking must not execute unrelated "+mode+" query");return Collections.emptyList();
        });
        AssistantPlan plan=new AssistantPlan();plan.setMetric(AssistantPlan.Metric.STOCK);plan.setPeriod("CURRENT");
        plan.setGroup(AssistantPlan.Group.PRODUCT);plan.setLimit(1);AssistantPresentation.prepare("哪个产品库存最多",plan);

        Map<String,Object> result=service.execute(plan);

        assertEquals(Arrays.asList("permissionDepartments","groups"),modes);
        assertEquals(Collections.emptyList(),result.get("summary"));assertEquals("SUCCESS",result.get("status"));
        assertEquals("RANKING",((Map<?,?>)result.get("presentation")).get("mode"));
    }
    @Test void exactZeroAccountTotalKeepsNonzeroDepartmentOffsetsAndSortsThem() {
        AssistantReadOnly reader=accountReader();
        when(reader.select(anyMap())).thenAnswer(call->{
            Map<?,?> args=call.getArgument(0);String mode=String.valueOf(args.get("mode"));
            if("permissionDepartments".equals(mode)) return Collections.emptyList();
            if("summary".equals(mode)) {
                Object req=args.get("reqVO");
                assertTrue(req instanceof cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableAccountPageReqVO);
                assertEquals(Boolean.TRUE,((cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.account.ErpReceivableAccountPageReqVO)req).getShowZeroBalance());
                Map<String,Object> row=new LinkedHashMap<>();row.put("label","合计");row.put("amount",java.math.BigDecimal.ZERO);row.put("unit","元");
                return Collections.singletonList(row);
            }
            if("accountParties".equals(mode)) return Collections.singletonList(accountRow(1L,"客户A",null,null,"0"));
            if("accountResults".equals(mode)) return Arrays.asList(
                    accountRow(1L,"客户A",12L,"兴宇路通","-10000"),
                    accountRow(1L,"客户A",11L,"甘孜分公司","10000"));
            return Collections.emptyList();
        });
        AssistantPlan plan=new AssistantPlan();plan.setMetric(AssistantPlan.Metric.RECEIVABLE);plan.setPeriod("CURRENT");plan.setPartyId(1L);

        Map<String,Object> result=service.execute(plan);

        assertEquals("SUCCESS",result.get("status"));assertEquals("应收余额合计",result.get("summaryLabel"));
        List<?> rows=(List<?>)result.get("accountResults");assertEquals(2,rows.size());
        assertEquals("甘孜分公司",((Map<?,?>)rows.get(0)).get("deptName"));
        assertEquals("兴宇路通",((Map<?,?>)rows.get(1)).get("deptName"));
        assertEquals(false,result.get("accountPartyTruncated"));assertEquals(10,result.get("accountPartyLimit"));
    }
    @Test void genericAccountsSelectTenPartiesBeforeExpandingDepartments() {
        AssistantReadOnly reader=accountReader();
        when(reader.select(anyMap())).thenAnswer(call->{
            Map<?,?> args=call.getArgument(0);String mode=String.valueOf(args.get("mode"));
            if("permissionDepartments".equals(mode) || "summary".equals(mode)) return Collections.emptyList();
            if("accountParties".equals(mode)) {
                List<Map<String,Object>> parties=new ArrayList<>();
                for(long id=1;id<=11;id++) parties.add(accountRow(id,"供应商"+id,null,null,String.valueOf(100-id)));
                return parties;
            }
            if("accountResults".equals(mode)) {
                assertEquals(Arrays.asList(1L,2L,3L,4L,5L,6L,7L,8L,9L,10L),args.get("accountPartyIds"));
                return Collections.singletonList(accountRow(1L,"供应商1",null,"未归属部门","99"));
            }
            return Collections.emptyList();
        });
        AssistantPlan plan=new AssistantPlan();plan.setMetric(AssistantPlan.Metric.PAYABLE);plan.setPeriod("CURRENT");

        Map<String,Object> result=service.execute(plan);

        assertEquals(true,result.get("accountPartyTruncated"));assertEquals(1,((List<?>)result.get("accountResults")).size());
        assertEquals("应付余额合计",result.get("summaryLabel"));
    }
    @Test void publishedMetricCannotUseAnUnverifiedGrouping() {
        AssistantProperties properties=new AssistantProperties();properties.setPublishedMetrics(Collections.singletonList("SALE"));
        Map<String,Object> definition=new HashMap<>();definition.put("status","VERIFIED");definition.put("groups",Collections.singletonList("NONE"));
        AssistantKnowledge knowledge=new AssistantKnowledge();ReflectionTestUtils.setField(knowledge,"properties",properties);
        ReflectionTestUtils.setField(knowledge,"entries",Collections.singletonMap("SALE",definition));
        AssistantPlan plan=new AssistantPlan();plan.setMetric(AssistantPlan.Metric.SALE);plan.setPeriod("THIS_MONTH");
        assertDoesNotThrow(()->knowledge.requirePublished(plan));
        plan.setGroup(AssistantPlan.Group.PARTY);
        assertThrows(IllegalArgumentException.class,()->knowledge.requirePublished(plan));
        plan.setGroup(AssistantPlan.Group.DAY);
        assertThrows(IllegalArgumentException.class,()->knowledge.requirePublished(plan));
    }
    @Test void sameNamedCandidatesClarifyAndRevokedChoiceCannotReachSummary() {
        AssistantReadOnly reader=mock(AssistantReadOnly.class);
        when(reader.snapshot(any())).thenAnswer(call->((java.util.function.Supplier<?>)call.getArgument(0)).get());
        cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService warehouses=mock(cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService.class);
        when(warehouses.getCurrentUserProductStockPermissionScope()).thenReturn(new cn.iocoder.yudao.module.erp.service.stock.bo.ErpProductStockPermissionScope(true,Collections.emptySet(),Collections.emptySet(),101L));
        ReflectionTestUtils.setField(service,"reader",reader);ReflectionTestUtils.setField(service,"warehouses",warehouses);ReflectionTestUtils.setField(service,"knowledge",mock(AssistantKnowledge.class));
        List<Map<String,Object>> candidates=new ArrayList<>();
        for(long id:new long[]{8,9}) {Map<String,Object> row=new HashMap<>();row.put("id",id);row.put("name","同名仓");candidates.add(row);}
        when(reader.select(anyMap())).thenAnswer(call->{
            String mode=(String)((Map<?,?>)call.getArgument(0)).get("mode");
            if("permissionDepartments".equals(mode)) return Collections.emptyList();
            assertEquals("candidates",mode,"Must never read amounts before resolving an authorized candidate");return candidates;
        });
        AssistantPlan plan=new AssistantPlan();plan.setMetric(AssistantPlan.Metric.STOCK);plan.setPeriod("CURRENT");plan.setWarehouse("同名仓");
        assertThrows(AssistantEntityChoice.class,()->service.execute(plan));assertEquals(Arrays.asList(8L,9L),plan.getPendingIds());
        AssistantQueryService.assignChoice(plan,"warehouse",8L);candidates.remove(0);
        assertEquals("FORBIDDEN",assertThrows(AssistantFailure.class,()->service.execute(plan)).getCode());
        plan.setWarehouseId(null);candidates.clear();
        AssistantFailure failure=assertThrows(AssistantFailure.class,()->service.execute(plan));
        assertEquals("ENTITY_NOT_FOUND",failure.getCode());
        assertEquals("没有找到匹配的仓库，请检查仓库名称或编码",failure.getMessage());
    }
    private static Map<String,Object> productStockRow(long productId,String productName,Long warehouseId,
                                                       String warehouseName,int amount,String priceKey,int price) {
        Map<String,Object> row=new LinkedHashMap<>();row.put("productId",productId);row.put("productName",productName);
        row.put("unit","桶");row.put("warehouseId",warehouseId);row.put("warehouseName",warehouseName);
        row.put("amount",new java.math.BigDecimal(amount));row.put(priceKey,new java.math.BigDecimal(price));return row;
    }
    private AssistantReadOnly accountReader() {
        AssistantReadOnly reader=mock(AssistantReadOnly.class);
        when(reader.snapshot(any())).thenAnswer(call->((java.util.function.Supplier<?>)call.getArgument(0)).get());
        ReflectionTestUtils.setField(service,"reader",reader);ReflectionTestUtils.setField(service,"knowledge",mock(AssistantKnowledge.class));
        cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO scope=
                mock(cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO.class);
        when(scope.getAll()).thenReturn(true);when(scope.getDeptIds()).thenReturn(Collections.emptySet());when(scope.getSelf()).thenReturn(false);
        when(permission.getDeptDataPermission(anyLong(),anyString())).thenReturn(scope);
        return reader;
    }
    private static Map<String,Object> accountRow(long partyId,String partyName,Long deptId,String deptName,String amount) {
        Map<String,Object> row=new LinkedHashMap<>();row.put("partyId",partyId);row.put("partyName",partyName);
        row.put("deptId",deptId);row.put("deptName",deptName);row.put("amount",new java.math.BigDecimal(amount));row.put("unit","元");return row;
    }
    private Map<String,Object> executeProductPrice(Map<String,Object> values,List<String> hidden,
                                                   AssistantPlan.PriceQueryMode mode,String priceField) {
        AssistantReadOnly reader=mock(AssistantReadOnly.class);
        cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService warehouses=mock(cn.iocoder.yudao.module.erp.service.stock.ErpWarehouseService.class);
        when(reader.snapshot(any())).thenAnswer(call->((java.util.function.Supplier<?>)call.getArgument(0)).get());
        when(warehouses.getCurrentUserProductStockPermissionScope()).thenReturn(
                new cn.iocoder.yudao.module.erp.service.stock.bo.ErpProductStockPermissionScope(true,Collections.emptySet(),Collections.emptySet(),101L));
        ReflectionTestUtils.setField(service,"reader",reader);ReflectionTestUtils.setField(service,"warehouses",warehouses);
        ReflectionTestUtils.setField(service,"knowledge",mock(AssistantKnowledge.class));
        when(permission.getCurrentUserHiddenFields("erp_product")).thenReturn(hidden);
        when(reader.select(anyMap())).thenAnswer(call->{
            String queryMode=String.valueOf(((Map<?,?>)call.getArgument(0)).get("mode"));
            if("permissionDepartments".equals(queryMode)) return Collections.emptyList();
            if("summary".equals(queryMode)) {
                Map<String,Object> row=new LinkedHashMap<>();row.put("label","测试产品");row.put("unit","桶");row.put("amount",0);
                row.putAll(values);return Collections.singletonList(row);
            }
            return Collections.emptyList();
        });
        AssistantPlan plan=new AssistantPlan();plan.setMetric(AssistantPlan.Metric.STOCK);plan.setPeriod("CURRENT");
        plan.setIncludeProductPrices(true);plan.setPriceQueryMode(mode);plan.setPriceField(priceField);
        return service.execute(plan);
    }
}
