package cn.iocoder.yudao.module.erp.enums.purchase;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErpPurchaseOrderDetailImportTaskStageEnum {
    QUEUED("QUEUED", "排队中"),
    PARSING("PARSING", "解析文件"),
    MATCHING("MATCHING", "匹配配件"),
    FINALIZING("FINALIZING", "整理结果"),
    FINISHED("FINISHED", "处理完成");

    private final String stage;
    private final String name;

    public static String getName(String stage) {
        for (ErpPurchaseOrderDetailImportTaskStageEnum value : values()) {
            if (value.stage.equals(stage)) return value.name;
        }
        return stage;
    }
}
