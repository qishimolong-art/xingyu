package cn.iocoder.yudao.module.erp.service.assistant;

import cn.iocoder.yudao.framework.common.biz.system.permission.dto.DeptDataPermissionRespDTO;
import cn.iocoder.yudao.framework.security.core.LoginUser;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.transferledger.ErpStockTransferLedgerPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.transferledger.ErpStockTransferLedgerSummaryRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.stock.vo.transferledger.ErpStockTransferLedgerTotalRespVO;
import cn.iocoder.yudao.module.erp.dal.mysql.assistant.AssistantSql;
import cn.iocoder.yudao.module.erp.service.stock.ErpStockTransferLedgerService;
import cn.iocoder.yudao.module.system.api.dept.DeptApi;
import cn.iocoder.yudao.module.system.api.permission.PermissionApi;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AssistantStage5BusinessQueryTest {

    @AfterEach void clear() { TenantContextHolder.clear(); SecurityContextHolder.clearContext(); }

    @Test void detectsEveryNewStage5BusinessFamily() {
        assertEquals(Collections.singletonList("product_category"), ids(AssistantStage5BusinessQuery.MASTER_TOOL,"列出当前启用的商品分类"));
        assertEquals(Collections.singletonList("base_data"), ids(AssistantStage5BusinessQuery.MASTER_TOOL,"分别列出采购类型、发票类型和配送方式"));
        assertEquals(Collections.singletonList("product_price_history"),
                ids(AssistantStage5BusinessQuery.STOCK_TOOL,"本月配件价格调整历史"));
        assertEquals(new LinkedHashSet<>(Arrays.asList("stock_in","stock_out","stock_in_bill","stock_out_bill")),
                new LinkedHashSet<>(ids(AssistantStage5BusinessQuery.STOCK_TOOL,"本月其他入库、其他出库、入库单和出库单各有多少张")));
        assertEquals(new LinkedHashSet<>(Arrays.asList("sale_quote","sale_cart")),
                new LinkedHashSet<>(ids(AssistantStage5BusinessQuery.SALE_TOOL,"本月已审核销售报价和销售开单分别有多少张")));
        assertEquals(new LinkedHashSet<>(Arrays.asList("sale_out","sale_return")),
                new LinkedHashSet<>(ids(AssistantStage5BusinessQuery.SALE_TOOL,"本月销售出库和退货分别有多少张")));
        assertEquals(new LinkedHashSet<>(Arrays.asList("sale_pick","sale_delivery")),
                new LinkedHashSet<>(ids(AssistantStage5BusinessQuery.SALE_TOOL,"本月拣货配送任务有多少张")));
        assertEquals(Collections.singletonList("stock_transfer_out"),
                ids(AssistantStage5BusinessQuery.STOCK_TOOL,"本月调拨出库有多少张"));
        assertEquals(Collections.singletonList("stock_transfer_in"),
                ids(AssistantStage5BusinessQuery.STOCK_TOOL,"本月调拨入库有多少张"));
        assertEquals(Collections.singletonList("stock_transfer_ledger"),
                ids(AssistantStage5BusinessQuery.STOCK_TOOL,"本月调拨台账异常记录"));
        assertEquals(new LinkedHashSet<>(Arrays.asList("purchase_invoice","purchase_invoice_ocr")),
                new LinkedHashSet<>(ids(AssistantStage5BusinessQuery.PURCHASE_TOOL,"本月采购发票和发票识别批次分别有多少条")));
        assertEquals(new LinkedHashSet<>(Arrays.asList("purchase_price_adjust","product_price_history")),
                new LinkedHashSet<>(ids(AssistantStage5BusinessQuery.PURCHASE_TOOL,"分别查询配件价格调整和采购调价业务单据")));
        assertEquals(new LinkedHashSet<>(Arrays.asList("purchase_in","purchase_return")),
                new LinkedHashSet<>(ids(AssistantStage5BusinessQuery.PURCHASE_TOOL,"本月采购入库和退货分别有多少张")));
        assertTrue(ids(AssistantStage5BusinessQuery.FINANCE_TOOL,"列出本月已审核应收调账、应付调账和核销记录")
                .containsAll(Arrays.asList("receivable_adjust","payable_adjust","receipt_writeoff","payment_writeoff")));
        assertEquals(Collections.singletonList("receivable_adjust"),
                ids(AssistantStage5BusinessQuery.FINANCE_TOOL,"列出本月应收调账记录"));
        assertEquals(Collections.singletonList("receipt_writeoff"),
                ids(AssistantStage5BusinessQuery.FINANCE_TOOL,"列出本月收款核销记录"));
    }

    @Test void executesOnlyServerOwnedTenantAndScopeBoundSql() {
        TenantContextHolder.setTenantId(7L);
        AssistantStage5BusinessQuery service=new AssistantStage5BusinessQuery();
        AssistantSemanticReadOnly reader=mock(AssistantSemanticReadOnly.class);
        AssistantToolPermissionService permissions=mock(AssistantToolPermissionService.class);
        doAnswer(invocation->{
            Map<String,Object> scope=invocation.getArgument(0);String prefix=invocation.getArgument(1);
            scope.put(prefix+"All",true);scope.put(prefix+"DeptIds",Collections.emptySet());scope.put(prefix+"SelfUserId",null);return null;
        }).when(permissions).documentScope(anyMap(),anyString(),anyString(),anyBoolean());
        when(reader.query(contains("COUNT(DISTINCT"),anyList(),eq(1))).thenReturn(Collections.singletonList(row("record_count",1L)));
        when(reader.query(argThat(sql->sql!=null&&!sql.contains("COUNT(DISTINCT")),anyList(),anyInt()))
                .thenReturn(Collections.singletonList(row("label","XSTJ-1")));
        ReflectionTestUtils.setField(service,"reader",reader);ReflectionTestUtils.setField(service,"toolPermissions",permissions);

        Map<String,Object> result=service.execute(AssistantStage5BusinessQuery.SALE_TOOL,"本月已审核销售调价单有多少张",new AssistantExecutionEnvelope());
        assertEquals("SUCCESS",result.get("status"));
        ArgumentCaptor<String> sql=ArgumentCaptor.forClass(String.class);
        verify(reader,times(2)).query(sql.capture(),anyList(),anyInt());
        String combined=String.join("\n",sql.getAllValues()).toLowerCase(Locale.ROOT);
        assertTrue(combined.contains("erp_sale_price_adjust"));
        assertTrue(combined.contains("tenant_id=?"));
        assertTrue(combined.contains("deleted=0"));
        assertFalse(combined.contains("password"));assertFalse(combined.contains("mobile"));assertFalse(combined.contains("bank_account"));
    }

    @Test void baseDataCapabilityCoversAllTwentyThreeConfiguredDictionaryTypes() {
        TenantContextHolder.setTenantId(7L);
        AssistantStage5BusinessQuery service=new AssistantStage5BusinessQuery();
        AssistantSemanticReadOnly reader=mock(AssistantSemanticReadOnly.class);
        AssistantToolPermissionService permissions=mock(AssistantToolPermissionService.class);
        doAnswer(invocation->{Map<String,Object> scope=invocation.getArgument(0);String prefix=invocation.getArgument(1);
            scope.put(prefix+"All",true);scope.put(prefix+"DeptIds",Collections.emptySet());scope.put(prefix+"SelfUserId",null);return null;
        }).when(permissions).documentScope(anyMap(),anyString(),eq("erp_base_data"),eq(true));
        when(reader.query(anyString(),anyList(),anyInt())).thenReturn(Collections.emptyList());
        ReflectionTestUtils.setField(service,"reader",reader);ReflectionTestUtils.setField(service,"toolPermissions",permissions);

        service.execute(AssistantStage5BusinessQuery.MASTER_TOOL,
                "供应商往来类别、客户往来类别、供应商类型、客户类型、普通区域、销售区域、价格级别、结算方式、送货方式、运费类型、物流公司、开票类型、线路、运费说明、采购方式、订货公司、收货地址、仓储中心、仓储仓库、费用支付类型、支出类型、费用单据类型、费用项目",
                new AssistantExecutionEnvelope());

        ArgumentCaptor<List> parameters=ArgumentCaptor.forClass(List.class);
        verify(reader,times(2)).query(anyString(),parameters.capture(),anyInt());
        Set<Object> actual=new LinkedHashSet<>(parameters.getAllValues().get(0));actual.remove(7L);
        assertEquals(new LinkedHashSet<>(Arrays.asList("supplier_category","customer_category","supplier_type","customer_type",
                "region","sales_area","price_level","settle_method","delivery_method","freight_type","logistics_company",
                "invoice_type","route","freight_explain","purchase_type","order_company","receive_address","storage_center",
                "storage_warehouse","payable_expense_biz_type","payable_expense_type","payable_expense_doc_type",
                "bank_transfer_fee_expense_category")),actual);
    }

    @Test void verifiedSaleAndPurchaseMetricsUseBusinessSourcesAndIndependentScopes() {
        assertEquals(Arrays.asList("erp:sale-out:query","erp:sale-return:query"),
                AssistantQueryService.requiredPermissions(AssistantPlan.Metric.SALE));
        assertEquals(Arrays.asList("erp:purchase-in:query","erp:purchase-return:query"),
                AssistantQueryService.requiredPermissions(AssistantPlan.Metric.PURCHASE));
        AssistantPlan sale=new AssistantPlan();sale.setMetric(AssistantPlan.Metric.SALE);sale.setPeriod("THIS_MONTH");
        String saleSql=AssistantSql.source(sale);
        assertTrue(saleSql.contains("erp_sale_out"));assertTrue(saleSql.contains("erp_sale_return"));
        assertTrue(saleSql.contains("saleOutAll"));assertTrue(saleSql.contains("saleReturnAll"));
        assertFalse(saleSql.contains("erp_sale_report"));
        AssistantPlan purchase=new AssistantPlan();purchase.setMetric(AssistantPlan.Metric.PURCHASE);purchase.setPeriod("THIS_MONTH");
        String purchaseSql=AssistantSql.source(purchase);
        assertTrue(purchaseSql.contains("erp_purchase_in"));assertTrue(purchaseSql.contains("erp_purchase_return"));
        assertTrue(purchaseSql.contains("purchaseInAll"));assertTrue(purchaseSql.contains("purchaseReturnAll"));
        assertFalse(purchaseSql.contains("erp_purchase_report"));
    }

    @Test void purchaseInboundBindsResolvedPurchaserAsExactStringIdAndSupportsGrouping() {
        TenantContextHolder.setTenantId(7L);
        AssistantStage5BusinessQuery service=new AssistantStage5BusinessQuery();
        AssistantSemanticReadOnly reader=mock(AssistantSemanticReadOnly.class);
        AssistantToolPermissionService permissions=mock(AssistantToolPermissionService.class);
        doAnswer(invocation->{Map<String,Object> scope=invocation.getArgument(0);String prefix=invocation.getArgument(1);
            scope.put(prefix+"All",true);scope.put(prefix+"DeptIds",Collections.emptySet());scope.put(prefix+"SelfUserId",null);return null;
        }).when(permissions).documentScope(anyMap(),anyString(),eq("erp_purchase_in"),eq(true));
        when(reader.query(anyString(),anyList(),anyInt())).thenReturn(Collections.emptyList());
        ReflectionTestUtils.setField(service,"reader",reader);ReflectionTestUtils.setField(service,"toolPermissions",permissions);
        AssistantExecutionEnvelope envelope=new AssistantExecutionEnvelope();
        Map<String,Object> purchaser=new LinkedHashMap<>();purchaser.put("entityType","PURCHASER");purchaser.put("id",42L);
        envelope.getArguments().put("resolvedEntity",purchaser);

        service.execute(AssistantStage5BusinessQuery.PURCHASE_TOOL,"各采购员本月采购入库金额排名",envelope);

        ArgumentCaptor<String> sql=ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<List> parameters=ArgumentCaptor.forClass(List.class);
        verify(reader).query(sql.capture(),parameters.capture(),eq(50));
        assertTrue(sql.getValue().contains("CAST(pu.id AS CHAR)=t.purchaser"));
        assertTrue(sql.getValue().contains("t.purchaser=?"));
        assertTrue(sql.getValue().contains("GROUP BY t.purchaser,pu.nickname"));
        assertTrue(parameters.getValue().contains("42"));
        assertFalse(parameters.getValue().contains(42L));
    }

    @Test void unsupportedPurchaseDocumentDoesNotDropPurchaserCondition() {
        TenantContextHolder.setTenantId(7L);
        AssistantStage5BusinessQuery service=new AssistantStage5BusinessQuery();
        AssistantToolPermissionService permissions=mock(AssistantToolPermissionService.class);
        doAnswer(invocation->{Map<String,Object> scope=invocation.getArgument(0);String prefix=invocation.getArgument(1);
            scope.put(prefix+"All",true);scope.put(prefix+"DeptIds",Collections.emptySet());scope.put(prefix+"SelfUserId",null);return null;
        }).when(permissions).documentScope(anyMap(),anyString(),eq("erp_purchase_return"),eq(true));
        ReflectionTestUtils.setField(service,"reader",mock(AssistantSemanticReadOnly.class));ReflectionTestUtils.setField(service,"toolPermissions",permissions);
        AssistantExecutionEnvelope envelope=new AssistantExecutionEnvelope();
        Map<String,Object> purchaser=new LinkedHashMap<>();purchaser.put("entityType","PURCHASER");purchaser.put("id",42L);
        envelope.getArguments().put("resolvedEntity",purchaser);

        AssistantFailure failure=assertThrows(AssistantFailure.class,()->service.execute(
                AssistantStage5BusinessQuery.PURCHASE_TOOL,"李四的采购退货单",envelope));
        assertEquals("DATASET_UNPUBLISHED",failure.getCode());
        assertTrue(failure.getMessage().contains("未执行删除采购员条件"));
    }

    @Test void writeoffUsesParentDocumentForPartyAndDataScope() {
        TenantContextHolder.setTenantId(7L);
        AssistantStage5BusinessQuery service=new AssistantStage5BusinessQuery();
        AssistantSemanticReadOnly reader=mock(AssistantSemanticReadOnly.class);
        AssistantToolPermissionService permissions=mock(AssistantToolPermissionService.class);
        doAnswer(invocation->{
            Map<String,Object> scope=invocation.getArgument(0);String prefix=invocation.getArgument(1);
            scope.put(prefix+"All",false);scope.put(prefix+"DeptIds",Collections.singleton(31L));scope.put(prefix+"SelfUserId","9");return null;
        }).when(permissions).documentScope(anyMap(),anyString(),eq("erp_finance_receipt"),eq(true));
        when(reader.query(anyString(),anyList(),anyInt())).thenReturn(Collections.emptyList());
        ReflectionTestUtils.setField(service,"reader",reader);ReflectionTestUtils.setField(service,"toolPermissions",permissions);
        AssistantExecutionEnvelope envelope=new AssistantExecutionEnvelope();
        Map<String,Object> customer=new LinkedHashMap<>();customer.put("entityType","CUSTOMER");customer.put("id",88L);
        envelope.getArguments().put("resolvedEntity",customer);

        service.execute(AssistantStage5BusinessQuery.FINANCE_TOOL,"本月收款核销记录",envelope);

        ArgumentCaptor<String> sql=ArgumentCaptor.forClass(String.class);
        verify(reader,times(2)).query(sql.capture(),anyList(),anyInt());
        String combined=String.join("\n",sql.getAllValues()).toLowerCase(Locale.ROOT);
        assertTrue(combined.contains("join erp_finance_receipt p"));
        assertTrue(combined.contains("p.dept_id in (?)"));
        assertTrue(combined.contains("p.creator=?"));
        assertTrue(combined.contains("p.customer_id=?"));
        assertTrue(combined.contains("t.write_off_status=1"));
        assertFalse(combined.contains("t.dept_id"));
    }

    @Test void pickUsesPickPermissionDocumentScopeAndWarehouseScope() {
        TenantContextHolder.setTenantId(7L);
        AssistantStage5BusinessQuery service=new AssistantStage5BusinessQuery();
        AssistantSemanticReadOnly reader=mock(AssistantSemanticReadOnly.class);
        AssistantToolPermissionService permissions=mock(AssistantToolPermissionService.class);
        doAnswer(invocation->{
            Map<String,Object> scope=invocation.getArgument(0);String prefix=invocation.getArgument(1);
            scope.put(prefix+"All",false);scope.put(prefix+"DeptIds",Collections.singleton(31L));scope.put(prefix+"SelfUserId",null);return null;
        }).when(permissions).documentScope(anyMap(),anyString(),eq("erp_sale_out"),eq(true));
        doAnswer(invocation->{Map<String,Object> scope=invocation.getArgument(0);scope.put("stockAll",false);
            scope.put("stockDeptIds",Collections.singleton(41L));scope.put("stockSelfIds",Collections.singleton(42L));return null;
        }).when(permissions).stockScope(anyMap());
        when(reader.query(anyString(),anyList(),anyInt())).thenReturn(Collections.emptyList());
        ReflectionTestUtils.setField(service,"reader",reader);ReflectionTestUtils.setField(service,"toolPermissions",permissions);

        service.execute(AssistantStage5BusinessQuery.SALE_TOOL,"本月待拣货任务",new AssistantExecutionEnvelope());

        verify(permissions).authorize(eq("erp:sale-pick:query"),eq("erp_sale_out"),anyCollection());
        ArgumentCaptor<String> sql=ArgumentCaptor.forClass(String.class);
        verify(reader,times(2)).query(sql.capture(),anyList(),anyInt());
        String combined=String.join("\n",sql.getAllValues()).toLowerCase(Locale.ROOT);
        assertTrue(combined.contains("erp_sale_pick_delivery_pick_task"));
        assertTrue(combined.contains("join erp_sale_pick_delivery_order p"));
        assertTrue(combined.contains("p.dept_id in (?)"));
        assertTrue(combined.contains("t.warehouse_id in (?,?)"));
        assertTrue(combined.contains("t.status=10"));
    }

    @Test void transferLedgerReusesPermissionAwareBusinessPairingService() {
        TenantContextHolder.setTenantId(7L);
        AssistantStage5BusinessQuery service=new AssistantStage5BusinessQuery();
        AssistantToolPermissionService permissions=mock(AssistantToolPermissionService.class);
        ErpStockTransferLedgerService ledger=mock(ErpStockTransferLedgerService.class);
        ErpStockTransferLedgerSummaryRespVO day=new ErpStockTransferLedgerSummaryRespVO()
                .setBusinessDate(LocalDate.of(2026,10,1)).setTransferOutDocumentCount(2L)
                .setTransferOutCount(new BigDecimal("8")).setTransferInDocumentCount(2L)
                .setTransferInCount(new BigDecimal("8")).setDifferenceCount(BigDecimal.ZERO)
                .setAbnormalGroupCount(0L).setLedgerStatus("NORMAL");
        when(ledger.getSummaryPage(any())).thenReturn(new PageResult<>(Collections.singletonList(day),1L));
        when(ledger.getTotal(any())).thenReturn(new ErpStockTransferLedgerTotalRespVO()
                .setTransferOutCount(new BigDecimal("8")).setTransferInCount(new BigDecimal("8"))
                .setDifferenceCount(BigDecimal.ZERO).setAbnormalGroupCount(0L));
        ReflectionTestUtils.setField(service,"toolPermissions",permissions);
        ReflectionTestUtils.setField(service,"transferLedger",ledger);

        Map<String,Object> result=service.execute(AssistantStage5BusinessQuery.STOCK_TOOL,
                "本月正常调拨台账",new AssistantExecutionEnvelope());

        verify(permissions).authorize(eq("erp:stock-transfer-ledger:query"),eq("erp_stock_transfer_ledger"),anyCollection());
        ArgumentCaptor<ErpStockTransferLedgerPageReqVO> request=ArgumentCaptor.forClass(ErpStockTransferLedgerPageReqVO.class);
        verify(ledger).getSummaryPage(request.capture());verify(ledger).getTotal(any());
        assertEquals("NORMAL",request.getValue().getMatchStatus());
        assertNotNull(request.getValue().getMoveTime());assertEquals(2,request.getValue().getMoveTime().length);
        assertEquals("SUCCESS",result.get("status"));
        assertEquals("5B",result.get("capabilityBatch"));
        assertEquals(1,((List<?>)result.get("rows")).size());
    }

    @Test void userListRequiresOnlyUserPermissionAndDoesNotExposeDepartments() {
        AssistantStage5BusinessQuery service=organizationService(true,false,Collections.emptySet(),Collections.emptySet());
        AssistantSemanticReadOnly reader=(AssistantSemanticReadOnly)ReflectionTestUtils.getField(service,"reader");
        AssistantToolPermissionService toolPermissions=(AssistantToolPermissionService)ReflectionTestUtils.getField(service,"toolPermissions");
        when(reader.query(anyString(),anyList(),anyInt())).thenReturn(Collections.emptyList());

        Map<String,Object> result=service.execute(AssistantStage5BusinessQuery.ORGANIZATION_TOOL,
                "列出所有用户",new AssistantExecutionEnvelope());

        verify(toolPermissions).authorize(eq("system:user:query"),eq("system_users"),anyCollection());
        verify(toolPermissions,never()).authorize(eq("system:dept:query"),anyString(),anyCollection());
        ArgumentCaptor<String> sql=ArgumentCaptor.forClass(String.class);
        verify(reader,times(2)).query(sql.capture(),anyList(),anyInt());
        String combined=String.join("\n",sql.getAllValues()).toLowerCase(Locale.ROOT);
        assertTrue(combined.contains("from system_users u"));
        assertFalse(combined.contains("system_user_dept"));
        assertFalse(combined.contains("system_dept"));
        assertFalse(combined.contains("main_department"));
        assertFalse(combined.contains("related_departments"));
        assertEquals(Collections.singletonList("系统用户（system:user:query）"),result.get("basis"));
    }

    @Test void userDepartmentRelationRequiresBothPermissionsAndSelfScopeDoesNotExpandToCoworkers() {
        AssistantStage5BusinessQuery service=organizationService(false,true,Collections.emptySet(),
                new LinkedHashSet<>(Arrays.asList(31L,32L)));
        AssistantSemanticReadOnly reader=(AssistantSemanticReadOnly)ReflectionTestUtils.getField(service,"reader");
        AssistantToolPermissionService toolPermissions=(AssistantToolPermissionService)ReflectionTestUtils.getField(service,"toolPermissions");
        when(reader.query(anyString(),anyList(),anyInt())).thenReturn(Collections.emptyList());

        Map<String,Object> result=service.execute(AssistantStage5BusinessQuery.ORGANIZATION_TOOL,
                "列出用户与部门关系",new AssistantExecutionEnvelope());

        verify(toolPermissions).authorize(eq("system:user:query"),eq("system_users"),anyCollection());
        verify(toolPermissions).authorize(eq("system:dept:query"),eq("system_dept"),anyCollection());
        ArgumentCaptor<String> sql=ArgumentCaptor.forClass(String.class);
        verify(reader,times(2)).query(sql.capture(),anyList(),anyInt());
        String combined=String.join("\n",sql.getAllValues()).toLowerCase(Locale.ROOT);
        assertTrue(combined.contains("count(distinct u.id)"));
        assertTrue(combined.contains("group_concat(distinct ad.name"));
        assertTrue(combined.contains("left join system_user_dept"));
        assertTrue(combined.contains("u.id=?"));
        assertFalse(combined.contains("u.dept_id in ("));
        assertFalse(combined.contains("visible_ud"));
        assertEquals(Arrays.asList("系统用户（system:user:query）","系统部门（system:dept:query）"),result.get("basis"));
    }

    @Test void departmentListRequiresOnlyDepartmentPermissionAndUsesVisibleDepartmentScope() {
        AssistantStage5BusinessQuery service=organizationService(false,true,Collections.emptySet(),
                new LinkedHashSet<>(Arrays.asList(31L,32L)));
        AssistantSemanticReadOnly reader=(AssistantSemanticReadOnly)ReflectionTestUtils.getField(service,"reader");
        AssistantToolPermissionService toolPermissions=(AssistantToolPermissionService)ReflectionTestUtils.getField(service,"toolPermissions");
        when(reader.query(anyString(),anyList(),anyInt())).thenReturn(Collections.emptyList());

        Map<String,Object> result=service.execute(AssistantStage5BusinessQuery.ORGANIZATION_TOOL,
                "列出所有部门",new AssistantExecutionEnvelope());

        verify(toolPermissions).authorize(eq("system:dept:query"),eq("system_dept"),anyCollection());
        verify(toolPermissions,never()).authorize(eq("system:user:query"),anyString(),anyCollection());
        ArgumentCaptor<String> sql=ArgumentCaptor.forClass(String.class);
        verify(reader).query(sql.capture(),anyList(),eq(100));
        assertTrue(sql.getValue().toLowerCase(Locale.ROOT).contains("d.id in (?,?)"));
        assertEquals(Collections.singletonList("系统部门（system:dept:query）"),result.get("basis"));
    }

    @Test void realtimeDepartmentResponsibleQuestionStopsAtExplicitCapabilityBoundary() {
        AssistantStage5BusinessQuery service=organizationService(true,false,Collections.emptySet(),Collections.emptySet());
        AssistantSemanticReadOnly reader=(AssistantSemanticReadOnly)ReflectionTestUtils.getField(service,"reader");
        AssistantToolPermissionService toolPermissions=(AssistantToolPermissionService)ReflectionTestUtils.getField(service,"toolPermissions");

        AssistantFailure failure=assertThrows(AssistantFailure.class,()->service.execute(
                AssistantStage5BusinessQuery.ORGANIZATION_TOOL,"当前总经办负责人是谁",new AssistantExecutionEnvelope()));

        assertEquals("CAPABILITY_UNVERIFIED",failure.getCode());
        assertTrue(failure.getMessage().contains("实时查询尚未开放"));
        assertTrue(failure.getMessage().contains("未使用公司知识库"));
        verifyNoInteractions(reader,toolPermissions);
    }

    @Test void realtimeCompanyLeadershipQuestionStopsAtExplicitCapabilityBoundary() {
        AssistantStage5BusinessQuery service=organizationService(true,false,Collections.emptySet(),Collections.emptySet());
        AssistantSemanticReadOnly reader=(AssistantSemanticReadOnly)ReflectionTestUtils.getField(service,"reader");
        AssistantToolPermissionService toolPermissions=(AssistantToolPermissionService)ReflectionTestUtils.getField(service,"toolPermissions");

        for(String question:Arrays.asList("现任董事长是谁", "当前公司一把手是谁", "ERP中公司的老板是谁")) {
            AssistantFailure failure=assertThrows(AssistantFailure.class,()->service.execute(
                    AssistantStage5BusinessQuery.ORGANIZATION_TOOL,question,new AssistantExecutionEnvelope()),question);

            assertEquals("CAPABILITY_UNVERIFIED",failure.getCode(),question);
            assertTrue(failure.getMessage().contains("公司管理角色实时查询尚未开放"),question);
            assertTrue(failure.getMessage().contains("未使用公司知识库"),question);
        }
        verifyNoInteractions(reader,toolPermissions);
    }

    @Test void realtimeDepartmentDutyQuestionStopsAtExplicitCapabilityBoundary() {
        AssistantStage5BusinessQuery service=organizationService(true,false,Collections.emptySet(),Collections.emptySet());
        AssistantSemanticReadOnly reader=(AssistantSemanticReadOnly)ReflectionTestUtils.getField(service,"reader");
        AssistantToolPermissionService toolPermissions=(AssistantToolPermissionService)ReflectionTestUtils.getField(service,"toolPermissions");

        AssistantFailure failure=assertThrows(AssistantFailure.class,()->service.execute(
                AssistantStage5BusinessQuery.ORGANIZATION_TOOL,"当前财务部职责是什么",new AssistantExecutionEnvelope()));

        assertEquals("CAPABILITY_UNVERIFIED",failure.getCode());
        assertTrue(failure.getMessage().contains("部门职责实时查询尚未开放"));
        assertTrue(failure.getMessage().contains("未使用公司知识库"));
        verifyNoInteractions(reader,toolPermissions);
    }

    @Test void realtimeOrganizationWorkAndCollaborationQuestionsStopAtExplicitCapabilityBoundary() {
        AssistantStage5BusinessQuery service=organizationService(true,false,Collections.emptySet(),Collections.emptySet());
        AssistantSemanticReadOnly reader=(AssistantSemanticReadOnly)ReflectionTestUtils.getField(service,"reader");
        AssistantToolPermissionService toolPermissions=(AssistantToolPermissionService)ReflectionTestUtils.getField(service,"toolPermissions");

        for(String question:Arrays.asList("当前传媒工作由谁负责", "现在各部门如何协作", "ERP中汪凯虎负责什么",
                "当前总经办有哪些人", "ERP中总经办有那些人")) {
            AssistantFailure failure=assertThrows(AssistantFailure.class,()->service.execute(
                    AssistantStage5BusinessQuery.ORGANIZATION_TOOL,question,new AssistantExecutionEnvelope()),question);
            assertEquals("CAPABILITY_UNVERIFIED",failure.getCode(),question);
            assertTrue(failure.getMessage().contains("实时查询尚未开放"),question);
            assertTrue(failure.getMessage().contains("未使用公司知识库"),question);
        }
        verifyNoInteractions(reader,toolPermissions);
    }

    @Test void contextualRealtimePersonReferenceStopsAtExplicitCapabilityBoundary() {
        AssistantStage5BusinessQuery service=organizationService(true,false,Collections.emptySet(),Collections.emptySet());
        AssistantSemanticReadOnly reader=(AssistantSemanticReadOnly)ReflectionTestUtils.getField(service,"reader");
        AssistantToolPermissionService toolPermissions=(AssistantToolPermissionService)ReflectionTestUtils.getField(service,"toolPermissions");
        AssistantExecutionEnvelope envelope=new AssistantExecutionEnvelope();
        envelope.getArguments().put(AssistantKnowledgeContext.CONTEXTUAL_REALTIME_BOUNDARY_ARGUMENT,true);

        AssistantFailure failure=assertThrows(AssistantFailure.class,()->service.execute(
                AssistantStage5BusinessQuery.ORGANIZATION_TOOL,"他当前还负责什么",envelope));

        assertEquals("CAPABILITY_UNVERIFIED",failure.getCode());
        assertTrue(failure.getMessage().contains("实时查询尚未开放"));
        assertTrue(failure.getMessage().contains("未使用公司知识库"));
        verifyNoInteractions(reader,toolPermissions);
    }

    @Test void structuredDepartmentCountUsesAggregateAndCurrentPermissionRange() {
        AssistantStage5BusinessQuery service=organizationService(false,false,Collections.singleton(31L),Collections.emptySet());
        AssistantSemanticReadOnly reader=(AssistantSemanticReadOnly)ReflectionTestUtils.getField(service,"reader");
        when(reader.query(anyString(),anyList(),anyInt())).thenReturn(Collections.singletonList(row("record_count",204L)));
        AssistantExecutionEnvelope envelope=new AssistantExecutionEnvelope();
        AssistantUnderstandingResult.Subtask task=new AssistantUnderstandingResult.Subtask();task.setTitle("启用部门数量");
        task.getConditions().setOrganizationView("DEPARTMENTS");task.getConditions().setAggregation("COUNT");task.getConditions().setStatus("启用");
        envelope.setUnderstandingTask(task);
        Map<String,Object> result=service.execute(AssistantStage5BusinessQuery.ORGANIZATION_TOOL,"这一句不应该再被正则解析",envelope);
        ArgumentCaptor<String> sql=ArgumentCaptor.forClass(String.class);ArgumentCaptor<List> params=ArgumentCaptor.forClass(List.class);
        verify(reader).query(sql.capture(),params.capture(),eq(1));
        assertTrue(sql.getValue().contains("COUNT(DISTINCT d.id)"));assertTrue(sql.getValue().contains("d.tenant_id=?"));
        assertTrue(sql.getValue().contains("d.deleted=0"));assertTrue(sql.getValue().contains("d.id IN (?)"));assertTrue(sql.getValue().contains("d.status=0"));
        assertTrue(params.getValue().contains(31L));assertFalse(sql.getValue().contains("LIMIT 100"));
        assertEquals(204L,((Map<?,?>)((List<?>)result.get("summary")).get(0)).get("record_count"));assertTrue(((List<?>)result.get("rows")).isEmpty());
    }

    @Test void structuredBusinessSpecCannotDropUnknownOrUnsupportedObjectFilters() {
        TenantContextHolder.setTenantId(7L);AssistantStage5BusinessQuery service=new AssistantStage5BusinessQuery();
        AssistantSemanticReadOnly reader=mock(AssistantSemanticReadOnly.class);AssistantToolPermissionService permissions=mock(AssistantToolPermissionService.class);
        ReflectionTestUtils.setField(service,"reader",reader);ReflectionTestUtils.setField(service,"toolPermissions",permissions);
        doAnswer(i->{((Map<String,Object>)i.getArgument(0)).put("scopeAll",true);return null;}).when(permissions).documentScope(anyMap(),anyString(),anyString(),anyBoolean());
        AssistantExecutionEnvelope envelope=new AssistantExecutionEnvelope();AssistantUnderstandingResult.Subtask task=new AssistantUnderstandingResult.Subtask();task.setTitle("采购发票");
        task.getConditions().setBusinessTypes(Collections.singletonList("purchase_invoice"));envelope.setUnderstandingTask(task);
        Map<String,Object> product=new LinkedHashMap<>();product.put("id",7L);product.put("entityType","PRODUCT");envelope.getArguments().put("resolvedEntity",product);
        assertThrows(AssistantFailure.class,()->service.execute(AssistantStage5BusinessQuery.PURCHASE_TOOL,"采购发票",envelope));verifyNoInteractions(reader);
    }

    private static AssistantStage5BusinessQuery organizationService(boolean all,boolean self,
                                                                     Set<Long> dataDeptIds,Set<Long> ownDeptIds) {
        TenantContextHolder.setTenantId(7L);
        LoginUser login=new LoginUser();login.setId(101L);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(login,"unused",Collections.emptyList()));
        AssistantStage5BusinessQuery service=new AssistantStage5BusinessQuery();
        AssistantSemanticReadOnly reader=mock(AssistantSemanticReadOnly.class);
        AssistantToolPermissionService toolPermissions=mock(AssistantToolPermissionService.class);
        PermissionApi permissions=mock(PermissionApi.class);
        DeptApi departments=mock(DeptApi.class);
        DeptDataPermissionRespDTO scope=new DeptDataPermissionRespDTO();
        scope.setAll(all);scope.setSelf(self);scope.setDeptIds(new LinkedHashSet<>(dataDeptIds));
        when(permissions.getDeptDataPermission(101L,"system_users")).thenReturn(scope);
        when(permissions.getDeptIdsByUserId(101L)).thenReturn(new LinkedHashSet<>(ownDeptIds));
        ReflectionTestUtils.setField(service,"reader",reader);
        ReflectionTestUtils.setField(service,"toolPermissions",toolPermissions);
        ReflectionTestUtils.setField(service,"permissions",permissions);
        ReflectionTestUtils.setField(service,"departments",departments);
        return service;
    }

    private static List<String> ids(String tool,String question){return AssistantStage5BusinessQuery.detectCapabilityIds(tool,question);}
    private static Map<String,Object> row(String key,Object value){Map<String,Object> row=new LinkedHashMap<>();row.put(key,value);return row;}
}
