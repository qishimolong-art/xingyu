package cn.iocoder.yudao.module.erp.enums.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErpAsyncExportTaskStageEnum {

    QUEUED("QUEUED", "排队中"),
    COUNTING("COUNTING", "统计数据"),
    EXPORTING("EXPORTING", "生成文件"),
    FINALIZING("FINALIZING", "文件收尾"),
    FINISHED("FINISHED", "导出完成");

    private final String stage;
    private final String name;

    public static String getName(String stage) {
        for (ErpAsyncExportTaskStageEnum value : values()) {
            if (value.stage.equals(stage)) {
                return value.name;
            }
        }
        return stage;
    }

}
