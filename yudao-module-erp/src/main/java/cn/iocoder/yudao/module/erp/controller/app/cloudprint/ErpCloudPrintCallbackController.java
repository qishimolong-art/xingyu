package cn.iocoder.yudao.module.erp.controller.app.cloudprint;

import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.module.erp.service.cloudprint.ErpCloudPrintService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import java.util.Collections;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/erp/print/callback")
public class ErpCloudPrintCallbackController {

    @Resource
    private ErpCloudPrintService cloudPrintService;

    @PostMapping("/{pathToken}")
    @PermitAll
    @TenantIgnore
    public Map<String, String> onCallback(@PathVariable("pathToken") String pathToken,
                                          @RequestBody String rawBody) {
        try {
            cloudPrintService.handleCallback(pathToken, rawBody);
        } catch (Exception ex) {
            log.error("[onCallback][云打印回调处理失败 rawBody({})]", rawBody, ex);
        }
        return Collections.singletonMap("message", "OK");
    }

}
