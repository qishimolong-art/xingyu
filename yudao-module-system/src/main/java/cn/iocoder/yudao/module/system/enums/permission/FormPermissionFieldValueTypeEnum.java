package cn.iocoder.yudao.module.system.enums.permission;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FormPermissionFieldValueTypeEnum {

    SINGLE_ID("single_id"),
    CSV_IDS("csv_ids"),
    JSON_IDS("json_ids");

    private final String code;

    public static boolean contains(String code) {
        for (FormPermissionFieldValueTypeEnum value : values()) {
            if (value.getCode().equals(code)) {
                return true;
            }
        }
        return false;
    }

}
