package cn.iocoder.yudao.module.erp.enums.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErpSimpleArchiveImportTaskStageEnum {

    QUEUED("QUEUED", "排队中"),
    PARSING("PARSING", "解析文件"),
    IMPORTING("IMPORTING", "导入数据"),
    FINALIZING("FINALIZING", "整理结果"),
    FINISHED("FINISHED", "已完成");

    private final String stage;
    private final String name;

    public static String getName(String stage) {
        for (ErpSimpleArchiveImportTaskStageEnum value : values()) {
            if (value.stage.equals(stage)) {
                return value.name;
            }
        }
        return stage;
    }

}
