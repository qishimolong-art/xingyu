package cn.iocoder.yudao.module.erp.service.assistant.wecom;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AssistantWeComBoundEvent {
    private Long tenantId;
    private Long userId;
    private String wecomUserId;
    private String question;
}
