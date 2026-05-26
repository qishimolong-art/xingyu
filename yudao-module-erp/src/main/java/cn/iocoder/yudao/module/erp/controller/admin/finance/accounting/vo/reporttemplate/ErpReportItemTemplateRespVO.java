package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.reporttemplate;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 报表项目模板 Response VO")
@Data
public class ErpReportItemTemplateRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "报表类型：1=资产负债表 2=利润表 3=现金流量表", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer reportType;

    @Schema(description = "区域：1=左侧 2=右侧", example = "1")
    private Integer side;

    @Schema(description = "项目名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "货币资金")
    private String itemName;

    @Schema(description = "行次", example = "1")
    private Integer rowNo;

    @Schema(description = "取数公式", example = "1001+1002")
    private String formula;

    @Schema(description = "父项编号", example = "0")
    private Long parentId;

    @Schema(description = "排序", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer sort;

    @Schema(description = "是否启用", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean enable;

    @Schema(description = "备注", example = "示例")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
