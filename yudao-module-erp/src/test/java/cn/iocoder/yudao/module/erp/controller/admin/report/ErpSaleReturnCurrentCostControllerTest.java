package cn.iocoder.yudao.module.erp.controller.admin.report;

import cn.iocoder.yudao.module.erp.controller.admin.report.vo.ErpSaleReturnCurrentCostModels.ConfirmRequest;
import cn.iocoder.yudao.module.erp.controller.admin.sale.ErpSaleReturnController;
import cn.iocoder.yudao.module.erp.service.sale.ErpSaleReturnService;
import cn.iocoder.yudao.module.erp.service.sale.returncost.ErpSaleReturnCurrentCostService;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import javax.annotation.Resource;
import java.lang.reflect.Field;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Actual method-security proxies for new cost endpoints and the existing approval endpoint. */
class ErpSaleReturnCurrentCostControllerTest {
    static final String QUERY="erp:sale-return-current-cost:query",CONFIRM="erp:sale-return-current-cost:confirm",AUDIT="erp:sale-return:update-status";
    AnnotationConfigApplicationContext context;
    ErpSaleReturnCurrentCostController controller;
    ErpSaleReturnController approval;
    ErpSaleReturnCurrentCostService service;
    ErpSaleReturnService approvals;
    Gate gate;
    @Configuration @EnableGlobalMethodSecurity(prePostEnabled=true,proxyTargetClass=true)
    static class Config {
        @Bean ErpSaleReturnCurrentCostController controller(){return new ErpSaleReturnCurrentCostController();}
        @Bean ErpSaleReturnController approvalController(){return new ErpSaleReturnController();}
        @Bean(name="ss") Gate gate(){return new Gate();}
    }
    public static class Gate {final Set<String> allowed=new HashSet<>();public boolean hasPermission(String code){return allowed.contains(code);}}
    @BeforeEach void setup(){
        context=new AnnotationConfigApplicationContext();
        for(Class<?> type:Arrays.asList(ErpSaleReturnCurrentCostController.class,ErpSaleReturnController.class))for(Field field:type.getDeclaredFields())if(field.isAnnotationPresent(Resource.class)&&!context.getBeanFactory().containsSingleton(field.getName()))context.getBeanFactory().registerSingleton(field.getName(),mock(field.getType()));
        context.register(Config.class);context.refresh();controller=context.getBean(ErpSaleReturnCurrentCostController.class);approval=context.getBean(ErpSaleReturnController.class);service=context.getBean(ErpSaleReturnCurrentCostService.class);approvals=context.getBean(ErpSaleReturnService.class);gate=context.getBean(Gate.class);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("signed-in-user","unused",Collections.emptyList()));
    }
    @AfterEach void cleanup(){SecurityContextHolder.clearContext();context.close();}
    void denyAll(){assertThrows(AccessDeniedException.class,()->controller.preview(701L));assertThrows(AccessDeniedException.class,()->controller.itemPage(701L,"source","basis",1,20));assertThrows(AccessDeniedException.class,()->controller.confirm(new ConfirmRequest()));assertThrows(AccessDeniedException.class,()->approval.updateSaleReturnStatus(701L,20,"basis"));verifyNoInteractions(service,approvals);}
    @Test void noCostOrAuditPermissionsDoNotReachEitherService(){denyAll();}
    @Test void confirmOnlyCannotReadConfirmOrApprove(){gate.allowed.add(CONFIRM);denyAll();}
    @Test void queryOnlyCanReadButCannotConfirmOrAudit(){
        gate.allowed.add(QUERY);controller.preview(701L);controller.itemPage(701L,"source","basis",3,20);verify(service).preview(701L);verify(service).itemPage(701L,"source","basis",3,20);clearInvocations(service);
        assertThrows(AccessDeniedException.class,()->controller.confirm(new ConfirmRequest()));assertThrows(AccessDeniedException.class,()->approval.updateSaleReturnStatus(701L,20,"basis"));verifyNoInteractions(service,approvals);
    }
    @Test void auditOnlyPreviewsAndApprovesExactSignatureWithoutManualConfirmation(){
        gate.allowed.add(AUDIT);controller.preview(701L);controller.itemPage(701L,"source","basis",2,20);approval.updateSaleReturnStatus(701L,20,"exact-basis");verify(approvals).updateSaleReturnStatusWithCostBasis(701L,20,"exact-basis");verify(service).preview(701L);verify(service).itemPage(701L,"source","basis",2,20);clearInvocations(service,approvals);
        assertThrows(AccessDeniedException.class,()->controller.confirm(new ConfirmRequest()));verifyNoInteractions(service,approvals);
    }
    @Test void queryAndConfirmCanConfirmButCannotApprove(){
        gate.allowed.addAll(Arrays.asList(QUERY,CONFIRM));ConfirmRequest request=new ConfirmRequest();request.setId(701L);controller.confirm(request);verify(service).confirm(same(request));assertThrows(AccessDeniedException.class,()->approval.updateSaleReturnStatus(701L,20,"basis"));verifyNoInteractions(approvals);
    }
    @Test void auditAndConfirmCanConfirmAndUseTheOriginalApprovalEndpoint(){
        gate.allowed.addAll(Arrays.asList(AUDIT,CONFIRM));ConfirmRequest request=new ConfirmRequest();request.setId(701L);controller.confirm(request);approval.updateSaleReturnStatus(701L,20,"basis");verify(service).confirm(same(request));verify(approvals).updateSaleReturnStatusWithCostBasis(701L,20,"basis");
    }
    @Test void invalidApprovalStatusIsRejectedBeforeEitherApprovalBranch(){
        gate.allowed.add(AUDIT);assertThrows(cn.iocoder.yudao.framework.common.exception.ServiceException.class,()->approval.updateSaleReturnStatus(701L,10,"basis"));verifyNoInteractions(approvals,service);
    }
}
