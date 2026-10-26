package cn.iocoder.yudao.module.erp.enums.stock;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErpStockExportTaskStageEnum {

    QUEUED("QUEUED", "排队中"),
    COUNTING("COUNTING", "正在统计数据"),
    EXPORTING("EXPORTING", "正在生成文件"),
    FINALIZING("FINALIZING", "正在完成文件"),
    FINISHED("FINISHED", "已完成");

    private final String stage;
    private final String name;

    public static String getName(String stage) {
        for (ErpStockExportTaskStageEnum value : values()) {
            if (value.stage.equals(stage)) {
                return value.name;
            }
        }
        return stage;
    }

}
