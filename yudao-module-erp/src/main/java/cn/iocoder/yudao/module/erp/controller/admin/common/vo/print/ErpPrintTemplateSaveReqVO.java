package cn.iocoder.yudao.module.erp.controller.admin.common.vo.print;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;

@Schema(description = "管理后台 - ERP 打印模板保存 Request VO")
@Data
public class ErpPrintTemplateSaveReqVO {

    private Long id;

    @NotBlank(message = "模块不能为空")
    private String moduleKey;

    @NotBlank(message = "模板名称不能为空")
    private String name;

    private Boolean defaulted;
    private Integer status;

    @NotBlank(message = "模板内容不能为空")
    private String templateJson;

    private String paperConfig;

}
