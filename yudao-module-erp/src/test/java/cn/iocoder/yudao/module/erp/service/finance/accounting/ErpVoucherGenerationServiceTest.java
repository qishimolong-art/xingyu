package cn.iocoder.yudao.module.erp.service.finance.accounting;
import cn.iocoder.yudao.module.erp.service.finance.accounting.rule.*;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.*;
import cn.iocoder.yudao.module.erp.dal.mysql.finance.accounting.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static cn.iocoder.yudao.module.erp.service.finance.accounting.rule.ErpVoucherRuleModels.*;

@ExtendWith(MockitoExtension.class) @MockitoSettings(strictness=Strictness.LENIENT)
class ErpVoucherGenerationServiceTest {
    @InjectMocks ErpVoucherGenerationService service;
    @Mock ErpVoucherSourceReader reader; @Mock ErpVoucherRuleStore store; @Mock ErpVoucherRuleCalculator calculator;
    @Mock ErpVoucherOriginLookup originLookup;
    @Mock ErpVoucherMapper vouchers;@Mock ErpVoucherAttributionMapper attributions;
    @Mock ErpVoucherService voucherService;@Mock ErpBookOpenService bookOpenService;
    Config config;Context context;Request request;Batch batch;
    @BeforeEach void setup(){
        ErpVoucherSourceReader.Source s=ErpVoucherRuleCalculatorTest.source(6,"100","0");
        when(reader.read(6,1L)).thenReturn(s);config=new Config();config.setVersion(1L);when(store.config()).thenReturn(config);
        context=new Context();context.setScenario("RECEIPT");context.setVersion(1L);context.setSourceVersion(s.version());when(store.context(6,1L)).thenReturn(context);
        when(originLookup.find(6,1L)).thenReturn(Collections.emptyList());
        when(bookOpenService.isVoucherTypeEnabled(any(),anyInt())).thenReturn(true);
        when(calculator.calculate(any(),any(),any(),any())).thenReturn(Collections.emptyList());
        request=new Request();request.setBizType(6);request.setBizId(1L);batch=new Batch();batch.getItems().add(request);
    }
    @Test void previewDoesNotCreateOrModifyAnything(){assertEquals("READY",service.preview(batch).get(0).getStatus());verifyNoInteractions(voucherService,attributions);verify(store,never()).lockConfig();}
    @Test void changedRuleInvalidatesPreviewBeforeAnyInsert(){request.setPreviewToken(service.previewOne(request).getPreviewToken());config.setVersion(2L);assertThrows(RuntimeException.class,()->service.generate(batch));verifyNoInteractions(voucherService);}
    @Test void existingVoucherIsReturnedWithoutReplacement(){when(originLookup.find(6,1L)).thenReturn(Collections.singletonList(new ErpVoucherDO().setId(99L)));assertEquals(Collections.singletonList(99L),service.generate(batch));verifyNoInteractions(voucherService);}
    @Test void changedSourceRequiresNewAccountingConfirmation(){context.setSourceVersion("stale");assertEquals("INCOMPLETE",service.previewOne(request).getStatus());}
    @Test void mismatchedDateAndPeriodCannotGenerate(){request.setVoucherDate(java.time.LocalDate.of(2026,9,5));request.setAttributionMonth(8);assertNotEquals("READY",service.previewOne(request).getStatus());}
    @Test void blockedSourceCannotReachVoucherCreation(){when(reader.read(6,1L)).thenThrow(ErpVoucherSourceReader.problem("无访问权限"));assertThrows(RuntimeException.class,()->service.generate(batch));verifyNoInteractions(voucherService);}
    @Test void entireBatchRollsBackWhenLaterSourceFails() {
        org.h2.jdbcx.JdbcDataSource ds=new org.h2.jdbcx.JdbcDataSource();ds.setURL("jdbc:h2:mem:voucher_"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1");
        org.springframework.jdbc.core.JdbcTemplate jdbc=new org.springframework.jdbc.core.JdbcTemplate(ds);
        jdbc.execute("create table test_voucher(id bigint primary key)");
        ErpVoucherGenerationService target=spy(service);
        doAnswer(inv->{Request r=inv.getArgument(0);Preview p=new Preview();p.setStatus("READY");p.setPreviewToken("confirmed");p.setBizNo("SK"+r.getBizId());p.setSourceAmount(new java.math.BigDecimal("100"));p.setVoucherDate(java.time.LocalDate.of(2026,9,9));return p;}).when(target).previewOne(any());
        when(reader.read(eq(6),anyLong())).thenAnswer(inv->ErpVoucherRuleCalculatorTest.source(6,"100","0"));
        java.util.concurrent.atomic.AtomicInteger count=new java.util.concurrent.atomic.AtomicInteger();
        when(voucherService.createVoucherFromBiz(anyInt(),anyLong(),anyString(),any(),any(),anyString(),anyList())).thenAnswer(inv->{
            if(count.incrementAndGet()==2)throw ErpVoucherSourceReader.problem("第二笔生成失败");jdbc.update("insert into test_voucher values(1)");return 1L;
        });
        request.setPreviewToken("confirmed");Request second=new Request();second.setBizType(6);second.setBizId(2L);second.setPreviewToken("confirmed");batch.getItems().add(second);
        org.springframework.aop.framework.ProxyFactory factory=new org.springframework.aop.framework.ProxyFactory(target);
        factory.setProxyTargetClass(true);
        factory.addAdvice(new org.springframework.transaction.interceptor.TransactionInterceptor(new org.springframework.jdbc.datasource.DataSourceTransactionManager(ds),new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource()));
        ErpVoucherGenerationService proxy=(ErpVoucherGenerationService)factory.getProxy();
        assertThrows(RuntimeException.class,()->proxy.generate(batch));assertEquals(2,count.get());
        assertEquals(0,jdbc.queryForObject("select count(*) from test_voucher",Integer.class));
    }
    @Test void concurrentSubmissionsCreateOnlyOneVoucher() throws Exception {
        org.h2.jdbcx.JdbcDataSource ds=new org.h2.jdbcx.JdbcDataSource();ds.setURL("jdbc:h2:mem:concurrent_"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=5000");
        org.springframework.jdbc.core.JdbcTemplate jdbc=new org.springframework.jdbc.core.JdbcTemplate(ds);
        jdbc.execute("create table rule_mutex(id int primary key)");jdbc.update("insert into rule_mutex values(1)");
        jdbc.execute("create table created_voucher(id bigint primary key)");
        when(store.lockConfig()).thenAnswer(inv->{jdbc.queryForObject("select id from rule_mutex where id=1 for update",Integer.class);return new ErpVoucherRuleStateDO();});
        when(originLookup.find(6,1L)).thenAnswer(inv->jdbc.query("select id from created_voucher",(rs,n)->new ErpVoucherDO().setId(rs.getLong(1))));
        when(voucherService.createVoucherFromBiz(anyInt(),anyLong(),anyString(),any(),any(),anyString(),anyList())).thenAnswer(inv->{jdbc.update("insert into created_voucher values(1)");return 1L;});
        request.setPreviewToken(service.previewOne(request).getPreviewToken());
        org.springframework.aop.framework.ProxyFactory factory=new org.springframework.aop.framework.ProxyFactory(service);factory.setProxyTargetClass(true);
        factory.addAdvice(new org.springframework.transaction.interceptor.TransactionInterceptor(new org.springframework.jdbc.datasource.DataSourceTransactionManager(ds),new org.springframework.transaction.annotation.AnnotationTransactionAttributeSource()));
        ErpVoucherGenerationService proxy=(ErpVoucherGenerationService)factory.getProxy();
        java.util.concurrent.ExecutorService executor=java.util.concurrent.Executors.newFixedThreadPool(2);
        java.util.concurrent.CountDownLatch start=new java.util.concurrent.CountDownLatch(1);
        try {
            java.util.concurrent.Callable<List<Long>> submit=()->{start.await();return proxy.generate(batch);};
            java.util.concurrent.Future<List<Long>> first=executor.submit(submit), second=executor.submit(submit);start.countDown();
            assertEquals(Collections.singletonList(1L),first.get(10,java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(Collections.singletonList(1L),second.get(10,java.util.concurrent.TimeUnit.SECONDS));
            assertEquals(1,jdbc.queryForObject("select count(*) from created_voucher",Integer.class));
            verify(voucherService,times(1)).createVoucherFromBiz(anyInt(),anyLong(),anyString(),any(),any(),anyString(),anyList());
        } finally {executor.shutdownNow();}
    }
}
