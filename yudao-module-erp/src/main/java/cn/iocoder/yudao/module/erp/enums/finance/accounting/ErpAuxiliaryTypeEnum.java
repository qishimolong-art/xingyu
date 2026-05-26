package cn.iocoder.yudao.module.erp.enums.finance.accounting;

import cn.iocoder.yudao.framework.common.core.ArrayValuable;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

/**
 * ERP 辅助核算类型枚举
 *
 * 用于会计科目挂的辅助核算维度，凭证录入时按所选科目反查决定弹出哪几个核算项下拉。
 */
@RequiredArgsConstructor
@Getter
public enum ErpAuxiliaryTypeEnum implements ArrayValuable<String> {

    SUPPLIER("supplier", "供应商"),
    CUSTOMER("customer", "客户"),
    PROJECT("project", "项目"),
    DEPT("dept", "部门"),
    PERSON("person", "个人"),
    ;

    public static final String[] ARRAYS = Arrays.stream(values())
            .map(ErpAuxiliaryTypeEnum::getType).toArray(String[]::new);

    private final String type;
    private final String name;

    @Override
    public String[] array() {
        return ARRAYS;
    }
}
