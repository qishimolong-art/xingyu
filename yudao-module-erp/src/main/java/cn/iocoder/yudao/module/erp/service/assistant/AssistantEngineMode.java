package cn.iocoder.yudao.module.erp.service.assistant;

import java.util.Locale;

/** 智能问数双引擎运行模式。 */
public enum AssistantEngineMode {
    V1,
    V2_SHADOW,
    V2_GRAY,
    V2;

    public static AssistantEngineMode parse(String value) {
        if (value == null || value.trim().isEmpty()) {
            return V1;
        }
        try {
            return valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return V1;
        }
    }
}
