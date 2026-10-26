package cn.iocoder.yudao.module.erp.enums.purchase;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErpSupplierImportTaskStageEnum {

    QUEUED("QUEUED", "等待处理"),
    PARSING("PARSING", "解析文件"),
    IMPORTING("IMPORTING", "导入数据"),
    FINALIZING("FINALIZING", "保存导入结果"),
    FINISHED("FINISHED", "处理完成");

    private final String stage;
    private final String name;

    public static String getName(String stage) {
        for (ErpSupplierImportTaskStageEnum value : values()) {
            if (value.stage.equals(stage)) {
                return value.name;
            }
        }
        return "处理中";
    }

}
