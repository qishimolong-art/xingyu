package cn.iocoder.yudao.module.erp.controller.admin.report;

import cn.iocoder.yudao.module.erp.controller.admin.report.vo.ErpPurchaseCostConfirmationModels.*;
import cn.iocoder.yudao.module.erp.service.purchase.cost.ErpPurchaseCostConfirmationService;
import org.junit.jupiter.api.*;
import org.springframework.context.annotation.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Real security proxy: the new permissions are independent of purchase approval and each other. */
class ErpPurchaseCostConfirmationControllerTest {
    AnnotationConfigApplicationContext context;ErpPurchaseCostConfirmationController controller;
    ErpPurchaseCostConfirmationService service;Gate gate;
    @Configuration @EnableGlobalMethodSecurity(prePostEnabled=true,proxyTargetClass=true)
    static class Config {
        @Bean ErpPurchaseCostConfirmationController controller(){return new ErpPurchaseCostConfirmationController();}
        @Bean(name="ss") Gate gate(){return new Gate();}
    }
    public static class Gate {final Set<String> allowed=new HashSet<>();public boolean hasPermission(String code){return allowed.contains(code);}}
    @BeforeEach void setup(){context=new AnnotationConfigApplicationContext();context.getBeanFactory().registerSingleton("costService",mock(ErpPurchaseCostConfirmationService.class));context.register(Config.class);context.refresh();controller=context.getBean(ErpPurchaseCostConfirmationController.class);service=context.getBean(ErpPurchaseCostConfirmationService.class);gate=context.getBean(Gate.class);SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken("user","unused",Collections.emptyList()));}
    @AfterEach void cleanup(){SecurityContextHolder.clearContext();context.close();}
    @Test void purchaseApprovalPermissionDoesNotGrantCostReadOrConfirmation(){gate.allowed.add("erp:purchase-in:update-status");assertThrows(AccessDeniedException.class,()->controller.preview(401L));assertThrows(AccessDeniedException.class,()->controller.itemPage(401L,"signature",1,20));assertThrows(AccessDeniedException.class,()->controller.confirm(new ConfirmRequest()));verifyNoInteractions(service);}
    @Test void queryAllowsPagedReadButNotWrite(){gate.allowed.add("erp:purchase-cost-confirmation:query");controller.preview(401L);controller.itemPage(401L,"signature",3,20);verify(service).preview(401L);verify(service).itemPage(401L,"signature",3,20);clearInvocations(service);assertThrows(AccessDeniedException.class,()->controller.confirm(new ConfirmRequest()));verifyNoInteractions(service);}
    @Test void confirmPermissionAloneCannotWriteWithoutReadPermission(){gate.allowed.add("erp:purchase-cost-confirmation:confirm");assertThrows(AccessDeniedException.class,()->controller.confirm(new ConfirmRequest()));verifyNoInteractions(service);}
    @Test void bothPermissionsForwardExactWholeDocumentRequest(){gate.allowed.add("erp:purchase-cost-confirmation:confirm");gate.allowed.add("erp:purchase-cost-confirmation:query");ConfirmRequest request=new ConfirmRequest();request.setPurchaseInId(401L);controller.confirm(request);verify(service).confirm(same(request));}
}
