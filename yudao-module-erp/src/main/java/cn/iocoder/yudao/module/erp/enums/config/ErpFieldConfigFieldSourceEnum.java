package cn.iocoder.yudao.module.erp.enums.config;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErpFieldConfigFieldSourceEnum {

    SYSTEM("SYSTEM"),
    CUSTOM("CUSTOM");

    private final String source;

}
