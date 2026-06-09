package cn.iocoder.yudao.module.erp.controller.admin.config.vo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 搜索字段配置 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpSearchFieldConfigRespVO {

    @Schema(description = "配置编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("配置编号")
    private Long id;

    @Schema(description = "模块标识", requiredMode = Schema.RequiredMode.REQUIRED, example = "purchase_order")
    @ExcelProperty("模块标识")
    private String moduleKey;

    @Schema(description = "字段名", requiredMode = Schema.RequiredMode.REQUIRED, example = "supplierId")
    @ExcelProperty("字段名")
    private String fieldName;

    @Schema(description = "字段中文名", example = "供应商")
    @ExcelProperty("字段中文名")
    private String fieldLabel;

    @Schema(description = "前端组件类型", example = "ApiSelect")
    @ExcelProperty("前端组件类型")
    private String component;

    @Schema(description = "是否启用", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    @ExcelProperty("是否启用")
    private Boolean enabled;

    @Schema(description = "排序", example = "1")
    @ExcelProperty("排序")
    private Integer sort;

    @Schema(description = "创建者", example = "1024")
    @ExcelProperty("创建者")
    private String creator;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

}
