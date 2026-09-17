package cn.iocoder.yudao.module.erp.controller.admin.report.trade;

import cn.iocoder.yudao.framework.jackson.config.YudaoJacksonAutoConfiguration;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.report.trade.ErpTradeReportModels.*;
import cn.iocoder.yudao.module.erp.service.report.trade.ErpTradeReportService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.*;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.LocalDateTime;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Real method-security proxies and HTTP date binding; production Jackson serializer configuration. */
class ErpTradeReportControllerTest {
    AnnotationConfigApplicationContext context;
    ErpSaleReportV2Controller sale;
    ErpPurchaseReportV2Controller purchase;
    ErpTradeReportService service;
    Gate gate;
    MockMvc mvc;
    @Configuration @EnableGlobalMethodSecurity(prePostEnabled=true,proxyTargetClass=true)
    static class Config {
        @Bean ErpSaleReportV2Controller sale(){return new ErpSaleReportV2Controller();}
        @Bean ErpPurchaseReportV2Controller purchase(){return new ErpPurchaseReportV2Controller();}
        @Bean(name="ss") Gate gate(){return new Gate();}
    }
    public static class Gate {final Set<String> allowed=new HashSet<>();public boolean hasPermission(String name){return allowed.contains(name);}}
    @BeforeEach void setup(){
        context=new AnnotationConfigApplicationContext();context.getBeanFactory().registerSingleton("reportService",mock(ErpTradeReportService.class));context.register(Config.class);context.refresh();
        sale=context.getBean(ErpSaleReportV2Controller.class);purchase=context.getBean(ErpPurchaseReportV2Controller.class);service=context.getBean(ErpTradeReportService.class);gate=context.getBean(Gate.class);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("user","unused",Collections.emptyList()));
        Jackson2ObjectMapperBuilder builder=new Jackson2ObjectMapperBuilder();YudaoJacksonAutoConfiguration jackson=new YudaoJacksonAutoConfiguration();jackson.ldtEpochMillisCustomizer().customize(builder);ObjectMapper mapper=builder.build();mapper.registerModule(jackson.timestampSupportModuleBean());
        mvc=MockMvcBuilders.standaloneSetup(sale,purchase).setMessageConverters(new MappingJackson2HttpMessageConverter(mapper)).build();
    }
    @AfterEach void cleanup(){SecurityContextHolder.clearContext();context.close();}
    @Test void noPermissionBlocksEveryReadEndpointBeforeService(){Filter f=new Filter();assertThrows(AccessDeniedException.class,()->sale.status());assertThrows(AccessDeniedException.class,()->sale.items(f));assertThrows(AccessDeniedException.class,()->sale.groups(f));assertThrows(AccessDeniedException.class,()->sale.summary(f));assertThrows(AccessDeniedException.class,()->sale.options(f,"PRODUCT",null));assertThrows(AccessDeniedException.class,()->purchase.status());assertThrows(AccessDeniedException.class,()->purchase.items(f));assertThrows(AccessDeniedException.class,()->purchase.groups(f));assertThrows(AccessDeniedException.class,()->purchase.summary(f));assertThrows(AccessDeniedException.class,()->purchase.options(f,"PRODUCT",null));verifyNoInteractions(service);}
    @Test void oldQueryPermissionAndOtherModulePermissionNeverGrantNewReport(){gate.allowed.add("erp:sale-report:query");gate.allowed.add("erp:purchase-report-v2:query");assertThrows(AccessDeniedException.class,()->sale.items(new Filter()));purchase.items(new Filter());verify(service).page(eq(false),any(Filter.class),eq(false));}
    @Test void queryAloneCannotExportAndExportAloneCannotReadOrExport(){Filter f=new Filter();gate.allowed.add("erp:sale-report-v2:query");assertThrows(AccessDeniedException.class,()->sale.export(f,"DETAIL",new MockHttpServletResponse()));gate.allowed.clear();gate.allowed.add("erp:sale-report-v2:export");assertThrows(AccessDeniedException.class,()->sale.items(f));assertThrows(AccessDeniedException.class,()->sale.export(f,"DETAIL",new MockHttpServletResponse()));verifyNoInteractions(service);}
    @Test void exportWithBothPermissionsPassesExactlyTheSameFilterAndModule()throws Exception {gate.allowed.add("erp:purchase-report-v2:query");gate.allowed.add("erp:purchase-report-v2:export");Filter f=new Filter();f.setSupplierId(501L);MockHttpServletResponse response=new MockHttpServletResponse();purchase.export(f,"GROUP",response);verify(service).export(eq(false),same(f),eq("GROUP"),same(response));}
    @Test void saleSummaryAndOptionsUseOnlyNewSaleQueryPermissionAndModule(){gate.allowed.add("erp:sale-report-v2:query");Filter f=new Filter();f.setCustomerId(701L);sale.summary(f);sale.options(f,"CUSTOMER","汽配");verify(service).summary(eq(true),same(f));verify(service).options(eq(true),same(f),eq("CUSTOMER"),eq("汽配"));}
    @Test void actualHttpBindsIsoMicrosecondsWithoutEpochOrRounding()throws Exception {gate.allowed.add("erp:sale-report-v2:query");mvc.perform(get("/erp/business-report/sale-v2/item-page").param("postedFrom","2026-01-02T03:04:05.123456").param("postedTo","2026-01-03T03:04:05.654321")).andExpect(status().isOk());ArgumentCaptor<Filter> captured=ArgumentCaptor.forClass(Filter.class);verify(service).page(eq(true),captured.capture(),eq(false));assertEquals(LocalDateTime.of(2026,1,2,3,4,5,123456000),captured.getValue().getPostedFrom());assertEquals(654321000,captured.getValue().getPostedTo().getNano());}
    @Test void actualHttpAcceptsExplicitSpaceSecondsAndRejectsInvalidTime()throws Exception {gate.allowed.add("erp:purchase-report-v2:query");mvc.perform(get("/erp/business-report/purchase-v2/item-page").param("postedFrom","2026-01-02 03:04:05").param("postedTo","2026-01-03 03:04:05")).andExpect(status().isOk());ArgumentCaptor<Filter> captured=ArgumentCaptor.forClass(Filter.class);verify(service).page(eq(false),captured.capture(),eq(false));assertEquals(LocalDateTime.of(2026,1,2,3,4,5),captured.getValue().getPostedFrom());clearInvocations(service);mvc.perform(get("/erp/business-report/purchase-v2/item-page").param("postedFrom","not-a-date").param("postedTo","2026-01-03 03:04:05")).andExpect(status().isBadRequest());verifyNoInteractions(service);}
    @Test void productionJacksonPreservesIntegerCountsAndStringDecimalHttpContract()throws Exception {gate.allowed.add("erp:sale-report-v2:query");Bundle b=new Bundle();b.setPage(new PageResult<>(Collections.emptyList(),0L));Map<String,Object> summary=new LinkedHashMap<>();summary.put("rowCount",3L);summary.put("missingSnapshotCount",1L);summary.put("grossAmount","123456789012345.123456");summary.put("netAmount",null);b.setSummary(summary);when(service.page(eq(true),any(Filter.class),eq(false))).thenReturn(b);mvc.perform(get("/erp/business-report/sale-v2/item-page").param("postedFrom","2026-01-02T03:04:05").param("postedTo","2026-01-03T03:04:05")).andExpect(status().isOk()).andExpect(jsonPath("$.data.summary.rowCount").value(3)).andExpect(jsonPath("$.data.summary.missingSnapshotCount").value(1)).andExpect(jsonPath("$.data.summary.grossAmount").value("123456789012345.123456"));}
}
