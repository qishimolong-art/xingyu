package cn.iocoder.yudao.module.erp.controller.app.assistant;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.erp.service.assistant.AssistantFailure;
import cn.iocoder.yudao.module.erp.service.assistant.AssistantProperties;
import cn.iocoder.yudao.module.erp.service.assistant.customer.*;
import lombok.Data;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import javax.annotation.Resource;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.io.IOException;
import java.util.*;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/erp/customer-assistant")
@Validated
public class AppCustomerAssistantController {
    @Resource private CustomerAssistantContextService contextService;
    @Resource private CustomerAssistantStore store;
    @Resource private CustomerAssistantExecutionService executionService;
    @Resource private AssistantProperties properties;

    @Data public static class Ask {@NotBlank @Size(max=36) private String conversationId;@NotBlank @Size(max=2000) private String question;@Size(max=32) private String choiceField;private Long choiceId;}

    @GetMapping("/readiness") public CommonResult<Map<String,Object>> readiness(){CustomerAssistantPrincipal p=contextService.current();Map<String,Object> v=new LinkedHashMap<>();v.put("enabled",true);v.put("customerName",p.getCustomerName());v.put("priceVisible",p.isPriceVisible());v.put("orderEnabled",p.isOrderEnabled());v.put("humanServiceAvailable",true);v.put("examples",Arrays.asList("这个配件我的价格是多少？","这个商品现在还有多少可售库存？","我的订单发货到哪里了？"));v.put("capabilities",properties.getCustomerApp().getAllowedCapabilities());return success(v);}
    @PostMapping("/conversation") public CommonResult<String> create(){return success(store.create(contextService.current()));}
    @GetMapping("/conversations") public CommonResult<List<Map<String,Object>>> conversations(@RequestParam(defaultValue="1")int page,@RequestParam(defaultValue="20")int pageSize){return success(store.list(contextService.current(),page,pageSize));}
    @GetMapping("/messages") public CommonResult<List<Map<String,Object>>> messages(@RequestParam String conversationId,@RequestParam(defaultValue="1")int page){return success(store.messages(conversationId,contextService.current(),page));}
    @DeleteMapping("/conversation") public CommonResult<Boolean> delete(@RequestParam String id){if(executionService.isConversationBusy(id))throw new AssistantFailure("BUSY","请先停止当前查询");store.delete(id,contextService.current());return success(true);}
    @PostMapping("/requests/{id}/cancel") public CommonResult<Boolean> cancel(@PathVariable String id){return success(executionService.cancel(id));}
    @PostMapping(value="/send",produces="text/event-stream")
    @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(requestEnable=false,responseEnable=false)
    public SseEmitter send(@Valid @RequestBody Ask ask){SseEmitter emitter=new SseEmitter(65_000L);CustomerAssistantExecutionService.Request request=new CustomerAssistantExecutionService.Request();request.setConversationId(ask.getConversationId());request.setQuestion(ask.getQuestion());request.setChoiceField(ask.getChoiceField());request.setChoiceId(ask.getChoiceId());CustomerAssistantExecutionService.Handle handle=executionService.execute(request,new CustomerAssistantExecutionService.EventSink(){public void emit(String event,Object data)throws IOException{emitter.send(SseEmitter.event().name(event).data(data));}public void complete(){emitter.complete();}});emitter.onCompletion(()->handle.stop(false));emitter.onTimeout(()->handle.stop(true));emitter.onError(e->handle.stop(false));return emitter;}
    @ExceptionHandler(AssistantFailure.class) public CommonResult<Object> failure(AssistantFailure e){return CommonResult.error(1_030_990_001,e.getMessage());}
}
