package cn.iocoder.yudao.module.erp.service.assistant;

public class AssistantFailure extends RuntimeException {
    private final String code;
    private AssistantExecutionEnvelope envelope;
    public AssistantFailure(String code, String message) { super(message); this.code = code; }
    public String getCode() { return code; }
    public AssistantExecutionEnvelope getEnvelope() { return envelope; }
    public AssistantFailure attachEnvelope(AssistantExecutionEnvelope value) { this.envelope = value; return this; }
}
