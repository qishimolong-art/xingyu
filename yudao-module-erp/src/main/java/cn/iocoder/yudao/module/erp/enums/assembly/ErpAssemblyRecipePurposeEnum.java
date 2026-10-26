package cn.iocoder.yudao.module.erp.enums.assembly;

import lombok.AllArgsConstructor;
import lombok.Getter;

/** 组装配方用途。 */
@Getter
@AllArgsConstructor
public enum ErpAssemblyRecipePurposeEnum {

    ASSEMBLY(1, "装配");

    private final Integer purpose;
    private final String name;

    public static boolean isValid(Integer purpose) {
        return ASSEMBLY.purpose.equals(purpose);
    }

}
