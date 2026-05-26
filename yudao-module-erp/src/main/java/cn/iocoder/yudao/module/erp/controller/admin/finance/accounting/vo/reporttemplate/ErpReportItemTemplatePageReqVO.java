package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.reporttemplate;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - ERP 报表项目模板分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpReportItemTemplatePageReqVO extends PageParam {

    @Schema(description = "报表类型：1=资产负债表 2=利润表 3=现金流量表", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "报表类型不能为空")
    private Integer reportType;

    @Schema(description = "区域：1=左侧 2=右侧", example = "1")
    private Integer side;

    @Schema(description = "项目名称", example = "货币资金")
    private String itemName;

    @Schema(description = "是否启用", example = "true")
    private Boolean enable;

}
