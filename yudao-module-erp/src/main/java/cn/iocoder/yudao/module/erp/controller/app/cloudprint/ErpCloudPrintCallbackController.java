package cn.iocoder.yudao.module.erp.controller.app.cloudprint;

import cn.iocoder.yudao.framework.tenant.core.aop.TenantIgnore;
import cn.iocoder.yudao.module.erp.service.cloudprint.ErpCloudPrintCallbackService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import java.util.Collections;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/erp/print/callback")
public class ErpCloudPrintCallbackController {

    @Resource
    private ErpCloudPrintCallbackService callbackService;

    @PostMapping(value = "/{pathToken}", consumes = MediaType.APPLICATION_JSON_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    @PermitAll
    @TenantIgnore
    public ResponseEntity<Map<String, String>> onCallback(@PathVariable("pathToken") String pathToken,
                                                           @RequestBody(required = false) String rawBody) {
        try {
            callbackService.receiveCallback(pathToken, rawBody);
            return ResponseEntity.ok(Collections.singletonMap("message", "OK"));
        } catch (ResponseStatusException ex) {
            return ResponseEntity.status(ex.getStatus())
                    .body(Collections.singletonMap("message", ex.getStatus().getReasonPhrase()));
        } catch (Exception ex) {
            log.error("[onCallback][云打印回调接收失败 exception({})]", ex.getClass().getSimpleName());
            return ResponseEntity.status(500).body(Collections.singletonMap("message", "ERROR"));
        }
    }

}
