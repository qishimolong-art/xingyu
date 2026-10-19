package cn.iocoder.yudao.module.erp.service.assistant.customer;

import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.erp.service.assistant.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class CustomerAssistantExecutionService {
    private static final String SYSTEM="你是面向已登录客户的ERP客服路由器。只能选择给定工具，且只能查询当前绑定客户。价格问题选择query_customer_price，库存问题选择query_mall_stock，客户自己的订单、发货、配送、退货和未完成数量选择query_customer_orders；缺少关键商品或意图时选择clarify_customer_intent。禁止采购、供应商、成本、利润、财务、应收收款、公司知识、其他客户和任意SQL。不要回答问题，只调用一个工具；参数只能来自客户原话或服务端候选项。";
    private static final java.util.regex.Pattern FORBIDDEN=java.util.regex.Pattern.compile("采购|供应商|成本|利润|毛利|应收|收款|财务|公司知识|经营分析|text[- ]?to[- ]?sql|sql|其他客户|别的客户",java.util.regex.Pattern.CASE_INSENSITIVE);
    @Resource private CustomerAssistantContextService contextService;
    @Resource private CustomerAssistantStore store;
    @Resource private CustomerAssistantToolRegistry tools;
    @Resource private CustomerAssistantQueryService queries;
    @Resource private AssistantModelClient model;

    private final ExecutorService workers=new ThreadPoolExecutor(2,4,60,TimeUnit.SECONDS,new ArrayBlockingQueue<>(16),new ThreadPoolExecutor.AbortPolicy());
    private final ScheduledExecutorService deadlines=Executors.newSingleThreadScheduledExecutor();
    private final Map<String,Running> active=new ConcurrentHashMap<>();
    private final Set<String> busy=ConcurrentHashMap.newKeySet();

    @Data public static class Request {private String conversationId;private String question;private String choiceField;private Long choiceId;}
    public interface EventSink {void emit(String event,Object data)throws Exception;void complete();}
    @AllArgsConstructor public class Handle {private final String requestId;public String getRequestId(){return requestId;}public void stop(boolean timeout){stopInternal(requestId,timeout);}}
    private static class Running {final String conversationId;final CustomerAssistantPrincipal principal;final AtomicBoolean stopped=new AtomicBoolean();volatile Future<?> future;Running(String id,CustomerAssistantPrincipal p){conversationId=id;principal=p;}}

    public Handle execute(Request request,EventSink sink){CustomerAssistantPrincipal principal=contextService.current();store.owner(request.getConversationId(),principal);if(!busy.add(request.getConversationId()))throw new AssistantFailure("BUSY","当前会话正在查询，请稍后重试");
        SecurityContext security=SecurityContextHolder.createEmptyContext();security.setAuthentication(SecurityContextHolder.getContext().getAuthentication());String requestId=UUID.randomUUID().toString();Running running=new Running(request.getConversationId(),principal);active.put(requestId,running);
        ScheduledFuture<?> deadline=deadlines.schedule(()->stopInternal(requestId,true),60,TimeUnit.SECONDS);
        try{running.future=workers.submit(()->run(request,sink,requestId,running,security,deadline));}catch(RejectedExecutionException e){deadline.cancel(false);active.remove(requestId);busy.remove(request.getConversationId());throw new AssistantFailure("BUSY","当前查询较多，请稍后重试");}return new Handle(requestId);}
    public boolean cancel(String requestId){Running r=active.get(requestId);if(r==null)return false;CustomerAssistantPrincipal current=contextService.current();store.owner(r.conversationId,current);if(!current.getCustomerId().equals(r.principal.getCustomerId())||!current.getMemberUserId().equals(r.principal.getMemberUserId()))return false;stopInternal(requestId,false);return true;}
    public boolean isConversationBusy(String id){return busy.contains(id);}

    private void run(Request request,EventSink sink,String requestId,Running running,SecurityContext security,ScheduledFuture<?> deadline){SecurityContextHolder.setContext(security);TenantContextHolder.setTenantId(running.principal.getTenantId());TenantContextHolder.setIgnore(false);long start=System.currentTimeMillis();String messageId=null,status="FAILED",capability=null;Object saved=null;model.beginRequest();
        try{CustomerAssistantPrincipal current=contextService.current();
            if(!Objects.equals(current.getMemberUserId(),running.principal.getMemberUserId())||!Objects.equals(current.getCustomerId(),running.principal.getCustomerId()))
                throw new AssistantFailure("CUSTOMER_BINDING_CHANGED","客户绑定已变化，请重新进入智能客服");
            store.owner(request.getConversationId(),current);checkStopped(running,false);messageId=store.begin(request.getConversationId(),AssistantModelClient.redact(request.getQuestion()),current);emit(sink,"started",Collections.singletonMap("requestId",requestId),running);emit(sink,"progress",Collections.singletonMap("text","正在理解问题"),running);
            if(FORBIDDEN.matcher(request.getQuestion()).find())throw new AssistantFailure("CAPABILITY_FORBIDDEN","该问题不在客户智能客服开放范围内，请转人工客服");
            AssistantToolCall call=model.chooseTool(request.getQuestion(),Collections.singletonMap("channel","CUSTOMER_APP"),tools.definitions(),SYSTEM);capability=call.getName();Map<String,Object> args=new LinkedHashMap<>(call.getArguments());if(request.getChoiceId()!=null)args.put("productId",request.getChoiceId());
            if(!tools.isAllowed(call.getName()))throw new AssistantFailure("CAPABILITY_FORBIDDEN","该能力未向客户开放");
            emit(sink,"progress",Collections.singletonMap("text","正在核对客户范围并查询"),running);Map<String,Object> result;
            if(CustomerAssistantToolRegistry.PRICE.equals(call.getName()))result=queries.price(current,args);else if(CustomerAssistantToolRegistry.STOCK.equals(call.getName()))result=queries.stock(current,args);else if(CustomerAssistantToolRegistry.ORDER.equals(call.getName()))result=queries.orders(current,args);else if(CustomerAssistantToolRegistry.CLARIFY.equals(call.getName())){Map<String,Object> clarification=AssistantClarification.missing("MISSING_PARAMETER",string(args.get("question"),"请补充要查询的商品或订单信息"),Collections.singletonList("queryCondition"),Arrays.asList("查询客户价","查询可售库存","查询我的订单"));saved=AssistantEntryProtocol.clarification(clarification);status="CLARIFY";emit(sink,"clarification",saved,running);emit(sink,"done",AssistantEntryProtocol.done(status),running);sink.complete();return;}else throw new AssistantFailure("CAPABILITY_FORBIDDEN","该能力未向客户开放");
            saved=AssistantEntryProtocol.result(result,null,messageId);status=String.valueOf(result.get("status"));emit(sink,"result",saved,running);emit(sink,"done",AssistantEntryProtocol.done(status),running);sink.complete();
        }catch(CustomerAssistantQueryService.ProductChoice choice){status="CLARIFY";saved=AssistantEntryProtocol.clarification(choice.getResponse());safeEmit(sink,"clarification",saved);safeEmit(sink,"done",AssistantEntryProtocol.done(status));sink.complete();
        }catch(Exception e){boolean timeout=running.stopped.get()&&System.currentTimeMillis()-start>=59_000;status=timeout?"TIMEOUT":running.stopped.get()?"CANCELLED":e instanceof AssistantFailure?((AssistantFailure)e).getCode():"QUERY_FAILED";String text=timeout?"查询超时，请缩小范围后重试":running.stopped.get()?"查询已停止":e instanceof AssistantFailure?e.getMessage():"查询未完成，请稍后重试";saved=AssistantEntryProtocol.error(status,text);safeEmit(sink,"error",saved);sink.complete();
        }finally{try{if(messageId!=null)store.finish(messageId,running.principal,status,saved,System.currentTimeMillis()-start,model.takeTokens(),capability);}catch(Exception ignored){}deadline.cancel(false);active.remove(requestId);busy.remove(request.getConversationId());TenantContextHolder.clear();SecurityContextHolder.clearContext();model.takeTokens();}}
    private void emit(EventSink sink,String event,Object data,Running running)throws Exception{checkStopped(running,false);sink.emit(event,data);}
    private void safeEmit(EventSink sink,String event,Object data){try{sink.emit(event,data);}catch(Exception ignored){}}
    private void checkStopped(Running r,boolean timeout){if(r.stopped.get())throw new AssistantFailure(timeout?"TIMEOUT":"CANCELLED",timeout?"查询超时":"查询已停止");}
    private void stopInternal(String id,boolean timeout){Running r=active.get(id);if(r==null)return;r.stopped.set(true);Future<?> f=r.future;if(f!=null)f.cancel(true);}
    private String string(Object value,String fallback){return value==null||String.valueOf(value).trim().isEmpty()?fallback:String.valueOf(value);}
    @PreDestroy public void shutdown(){active.keySet().forEach(id->stopInternal(id,false));workers.shutdownNow();deadlines.shutdownNow();}
}
