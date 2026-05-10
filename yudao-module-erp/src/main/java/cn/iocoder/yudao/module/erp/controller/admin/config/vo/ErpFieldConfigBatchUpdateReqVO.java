package cn.iocoder.yudao.module.erp.controller.admin.config.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - ERP 字段配置批量更新 Request VO")
@Data
public class ErpFieldConfigBatchUpdateReqVO {

    @Schema(description = "模块标识", requiredMode = Schema.RequiredMode.REQUIRED, example = "purchase_order")
    @NotEmpty(message = "模块标识不能为空")
    private String moduleKey;

    @Schema(description = "字段配置列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "字段配置列表不能为空")
    @Valid
    private List<Item> items;

    @Schema(description = "字段配置项")
    @Data
    public static class Item {

        @Schema(description = "字段名", requiredMode = Schema.RequiredMode.REQUIRED, example = "supplierId")
        @NotEmpty(message = "字段名不能为空")
        private String fieldName;

        @Schema(description = "字段中文名", example = "供应商")
        private String fieldLabel;

        @Schema(description = "是否必填", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
        @NotNull(message = "是否必填不能为空")
        private Boolean required;

        @Schema(description = "排序", example = "1")
        private Integer sort;

    }

}
