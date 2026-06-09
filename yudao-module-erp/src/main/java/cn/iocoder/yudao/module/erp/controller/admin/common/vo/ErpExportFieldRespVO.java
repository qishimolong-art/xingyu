package cn.iocoder.yudao.module.erp.controller.admin.common.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "Admin - ERP export field Response VO")
@Data
public class ErpExportFieldRespVO {

    @Schema(description = "Export VO field name", example = "no")
    private String field;

    @Schema(description = "Excel header label", example = "入库单号")
    private String label;

    @Schema(description = "Field group key", example = "main")
    private String group;

    @Schema(description = "Field group label", example = "单据信息")
    private String groupLabel;

    @Schema(description = "Whether the field is required", example = "false")
    private Boolean required;

    @Schema(description = "Display sort", example = "10")
    private Integer sort;

}
