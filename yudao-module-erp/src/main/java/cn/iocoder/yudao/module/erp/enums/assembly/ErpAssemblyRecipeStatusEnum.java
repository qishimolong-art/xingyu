package cn.iocoder.yudao.module.erp.enums.assembly;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 组装配方状态。 */
@Getter
@AllArgsConstructor
public enum ErpAssemblyRecipeStatusEnum {

    DISABLED(0, "停用"),
    ENABLED(1, "启用");

    private final Integer status;
    private final String name;

    public static boolean isValid(Integer status) {
        return DISABLED.status.equals(status) || ENABLED.status.equals(status);
    }

}
