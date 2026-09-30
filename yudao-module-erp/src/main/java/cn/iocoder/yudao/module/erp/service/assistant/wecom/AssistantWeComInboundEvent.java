package cn.iocoder.yudao.module.erp.service.assistant.wecom;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class AssistantWeComInboundEvent {
    private final JsonNode frame;
}
