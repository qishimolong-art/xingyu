package cn.iocoder.yudao.module.erp.controller.admin.sale.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Schema(description = "管理后台 - ERP 销售单据修改备注 Request VO")
@Data
public class ErpSaleUpdateRemarkReqVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "17386")
    @NotNull(message = "编号不能为空")
    private Long id;

    @Schema(description = "备注", example = "审批后补充说明")
    @NotNull(message = "备注不能为 null，清空备注请传空字符串")
    @Size(max = 500, message = "备注长度不能超过 500 个字符")
    private String remark;

}
