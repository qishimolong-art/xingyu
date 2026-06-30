package cn.iocoder.yudao.module.erp.enums.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

@Getter
@AllArgsConstructor
public enum ErpFieldConfigFieldTypeEnum {

    TEXT("TEXT"),
    TEXTAREA("TEXTAREA"),
    INTEGER("INTEGER"),
    DECIMAL("DECIMAL"),
    DATE("DATE"),
    DATETIME("DATETIME"),
    BOOLEAN("BOOLEAN");

    private final String type;

    public static boolean isValid(String type) {
        return Arrays.stream(values()).anyMatch(item -> item.getType().equals(type));
    }

}
