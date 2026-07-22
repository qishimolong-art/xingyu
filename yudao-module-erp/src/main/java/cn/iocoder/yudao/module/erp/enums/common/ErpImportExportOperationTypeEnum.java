package cn.iocoder.yudao.module.erp.enums.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * ERP import/export operation type.
 */
@Getter
@AllArgsConstructor
public enum ErpImportExportOperationTypeEnum {

    IMPORT("IMPORT", "导入"),
    EXPORT("EXPORT", "导出");

    private final String type;
    private final String name;

}
