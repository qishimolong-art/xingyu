package cn.iocoder.yudao.module.erp.controller.admin.config.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;

@Schema(description = "管理后台 - ERP 自定义字段创建 Request VO")
@Data
public class ErpFieldConfigCreateCustomReqVO {

    @Schema(description = "模块标识", requiredMode = Schema.RequiredMode.REQUIRED, example = "erp_product")
    @NotEmpty(message = "模块标识不能为空")
    private String moduleKey;

    @Schema(description = "逻辑字段编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "hello")
    @NotEmpty(message = "字段编码不能为空")
    private String fieldName;

    @Schema(description = "字段名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "测试字段")
    @NotEmpty(message = "字段名称不能为空")
    private String fieldLabel;

    @Schema(description = "字段类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "TEXT")
    @NotEmpty(message = "字段类型不能为空")
    private String fieldType;

    @Schema(description = "字段分组", example = "base_info")
    private String fieldGroup;

    @Schema(description = "组件类型", example = "Input")
    private String componentType;

    @Schema(description = "最大长度", example = "255")
    private Integer maxLength;

    @Schema(description = "数值精度", example = "18")
    private Integer decimalPrecision;

    @Schema(description = "数值小数位", example = "2")
    private Integer decimalScale;

    @Schema(description = "默认值")
    private String defaultValue;

    @Schema(description = "是否必填", example = "false")
    private Boolean required;

    @Schema(description = "表单是否显示", example = "true")
    private Boolean visible;

    @Schema(description = "列表是否显示", example = "false")
    private Boolean listVisible;

    @Schema(description = "是否可搜索", example = "false")
    private Boolean searchable;

    @Schema(description = "是否只读", example = "false")
    private Boolean readonly;

}
