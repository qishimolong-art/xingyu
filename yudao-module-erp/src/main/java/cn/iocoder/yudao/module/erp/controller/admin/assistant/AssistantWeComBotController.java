package cn.iocoder.yudao.module.erp.controller.admin.assistant;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.erp.service.assistant.AssistantFailure;
import cn.iocoder.yudao.module.erp.service.assistant.wecom.AssistantWeComBindingService;
import cn.iocoder.yudao.module.erp.service.assistant.wecom.AssistantWeComWebSocketClient;
import lombok.Data;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.LinkedHashMap;
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

/** 企业微信机器人一次性免登绑定公共接口。 */
@RestController
@RequestMapping("/erp/assistant/wecom-bot")
public class AssistantWeComBotController {

    @Resource private AssistantWeComBindingService bindingService;
    @Resource private AssistantWeComWebSocketClient webSocketClient;

    @GetMapping("/bind-authorize-url")
    @PermitAll
    public CommonResult<String> getBindAuthorizeUrl(@RequestParam String ticket) {
        return success(bindingService.getAuthorizeUrl(ticket));
    }

    @PostMapping("/bind")
    @PermitAll
    public CommonResult<Map<String, Object>> bind(@Valid @RequestBody BindReqVO request) {
        AssistantWeComBindingService.BindResult result = bindingService.bind(request.getCode(), request.getState());
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("nickname", result.getNickname());
        response.put("existing", result.isExisting());
        response.put("continued", webSocketClient.isAvailable());
        response.put("message", webSocketClient.isAvailable()
                ? "身份绑定成功，机器人将继续处理首次问题"
                : "身份绑定成功，但机器人连接暂不可用，请返回企业微信重新提问");
        return success(response);
    }

    @ExceptionHandler(AssistantFailure.class)
    public CommonResult<Object> failure(AssistantFailure failure) {
        return CommonResult.error(1_030_990_001, failure.getMessage());
    }

    @Data
    public static class BindReqVO {
        @NotBlank @Size(max = 1024) private String code;
        @NotBlank @Size(max = 128) private String state;
    }
}
