package cn.iocoder.yudao.module.erp.enums.purchase;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErpPurchaseOrderImportTaskStageEnum {
    QUEUED("QUEUED", "排队中"),
    PARSING("PARSING", "解析文件"),
    VALIDATING("VALIDATING", "校验数据"),
    IMPORTING("IMPORTING", "创建订单"),
    FINALIZING("FINALIZING", "整理结果"),
    FINISHED("FINISHED", "处理完成");

    private final String stage;
    private final String name;

    public static String getName(String stage) {
        for (ErpPurchaseOrderImportTaskStageEnum value : values()) {
            if (value.stage.equals(stage)) return value.name;
        }
        return stage;
    }
}
