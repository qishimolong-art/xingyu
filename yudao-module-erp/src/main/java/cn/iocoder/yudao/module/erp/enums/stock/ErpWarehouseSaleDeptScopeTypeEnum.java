package cn.iocoder.yudao.module.erp.enums.stock;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * ERP 仓库销售部门授权范围枚举。
 */
@Getter
@AllArgsConstructor
public enum ErpWarehouseSaleDeptScopeTypeEnum {

    ALL("ALL", "全部部门可用"),
    SPECIFIED("SPECIFIED", "指定部门可用");

    private final String type;
    private final String name;

    public static boolean contains(String type) {
        return Arrays.stream(values()).anyMatch(item -> item.type.equals(type));
    }

}
