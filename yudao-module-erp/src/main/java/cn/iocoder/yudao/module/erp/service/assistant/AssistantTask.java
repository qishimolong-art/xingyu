package cn.iocoder.yudao.module.erp.service.assistant;

import okhttp3.Call;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicBoolean;

/** One request budget, also propagated to blocking model and JDBC operations. */
public final class AssistantTask {
    private static final ThreadLocal<AssistantTask> CURRENT = new ThreadLocal<>();
    private final long deadline = System.nanoTime() + 60_000_000_000L;
    private final AtomicBoolean cancelled = new AtomicBoolean();
    private volatile Call call;
    private volatile Statement statement;
    private volatile boolean timedOut;
    public static void attach(AssistantTask task) { CURRENT.set(task); }
    public static void clear() { CURRENT.remove(); }
    public static AssistantTask current() { return CURRENT.get(); }
    public static void checkCurrent() { if(current()!=null) current().check(); }
    public long remainingMillis() { check(); return Math.max(1,(deadline-System.nanoTime())/1_000_000); }
    public void check() {
        if(System.nanoTime()>=deadline) { timedOut=true; cancel(); }
        if(cancelled.get()) throw new AssistantFailure(timedOut?"TIMEOUT":"CANCELLED",timedOut?"查询超时，未返回不完整总额":"本次查询已停止");
    }
    public void timeout() { timedOut=true; cancel(); }
    public void cancel() {
        cancelled.set(true);
        Call active=call; if(active!=null) active.cancel();
        Statement sql=statement; if(sql!=null) try { sql.cancel(); } catch(Exception ignored) { /* JDBC timeout remains enforced. */ }
    }
    public void call(Call value) { call=value; if(value!=null) { if(cancelled.get()) value.cancel(); check(); } }
    public void statement(Statement value) { statement=value; if(value!=null) { if(cancelled.get()) try { value.cancel(); } catch(Exception ignored) { } check(); } }
}
