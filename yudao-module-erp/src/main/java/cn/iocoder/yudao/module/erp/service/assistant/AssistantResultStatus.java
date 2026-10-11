package cn.iocoder.yudao.module.erp.service.assistant;

import java.util.Locale;

/** V2 对外统一结果状态。 */
public enum AssistantResultStatus {
    SUCCESS,
    PARTIAL_SUCCESS,
    NO_DATA,
    ENTITY_NOT_FOUND,
    CHOICE_REQUIRED,
    CLARIFY,
    NO_PERMISSION,
    CAPABILITY_UNAVAILABLE,
    KNOWLEDGE_NOT_FOUND,
    UNVERIFIED_KNOWLEDGE,
    POLICY_REJECTED,
    SYSTEM_ERROR;

    public static AssistantResultStatus from(String value) {
        if (value == null) return SYSTEM_ERROR;
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if ("EMPTY".equals(normalized)) return NO_DATA;
        if ("FORBIDDEN".equals(normalized) || "FIELD_FORBIDDEN".equals(normalized)) return NO_PERMISSION;
        if ("CAPABILITY_UNVERIFIED".equals(normalized) || "TEXT_TO_SQL_DISABLED".equals(normalized)) {
            return CAPABILITY_UNAVAILABLE;
        }
        if ("MODEL_INVALID".equals(normalized) || "INVALID_CHOICE".equals(normalized)) return CLARIFY;
        try {
            return valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            return SYSTEM_ERROR;
        }
    }
}
