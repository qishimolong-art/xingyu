package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.reporttemplate;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - ERP 报表项目模板新增/修改 Request VO")
@Data
public class ErpReportItemTemplateSaveReqVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "报表类型：1=资产负债表 2=利润表 3=现金流量表", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "报表类型不能为空")
    private Integer reportType;

    @Schema(description = "区域：1=左侧 2=右侧", example = "1")
    private Integer side;

    @Schema(description = "项目名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "货币资金")
    @NotEmpty(message = "项目名称不能为空")
    private String itemName;

    @Schema(description = "行次", example = "1")
    private Integer rowNo;

    @Schema(description = "取数公式", example = "1001+1002")
    private String formula;

    @Schema(description = "父项编号", example = "0")
    private Long parentId;

    @Schema(description = "排序", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "排序不能为空")
    private Integer sort;

    @Schema(description = "是否启用", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    @NotNull(message = "是否启用不能为空")
    private Boolean enable;

    @Schema(description = "备注", example = "示例")
    private String remark;

}
