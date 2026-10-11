package cn.iocoder.yudao.module.erp.service.assistant;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;

/** V2 能力注册项；执行器只能引用已登记的能力。 */
@Data
@Builder
public class AssistantCapability {
    private String id;
    private String domain;
    private AssistantSemanticRequest.QueryMode queryMode;
    private String executor;
    private String requiredPermission;
    private int rowLimit;
    private int timeoutMillis;
    private boolean auditable;
    private boolean virtual;
    @Singular private List<String> outputFields;
}
