package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.config;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - ERP 销售配置新增/修改 Request VO")
@Data
public class ErpSaleConfigSaveReqVO {

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "配置类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "CONTRACT_TYPE")
    @NotBlank(message = "配置类型不能为空")
    private String configType;

    @Schema(description = "配置编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "annual")
    @NotBlank(message = "配置编码不能为空")
    private String code;

    @Schema(description = "配置名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "年度合同")
    @NotBlank(message = "配置名称不能为空")
    private String name;

    @Schema(description = "配置值")
    private String configValue;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "状态不能为空")
    private Integer status;

    @Schema(description = "排序", example = "1")
    private Integer sort;

    @Schema(description = "备注")
    private String remark;

}
