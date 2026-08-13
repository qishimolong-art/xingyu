package cn.iocoder.yudao.module.erp.controller.admin.common.vo.print;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - ERP 打印记录创建 Request VO")
@Data
public class ErpPrintRecordCreateReqVO {

    @NotBlank(message = "模块不能为空")
    private String moduleKey;

    @NotNull(message = "业务单据不能为空")
    private Long businessId;

    private String businessNo;
    private Long templateId;

}
