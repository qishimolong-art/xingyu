package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Schema(description = "管理后台 - ERP 销售单更新快递单 Request VO")
@Data
public class ErpSaleOutUpdateExpressFileReqVO {

    @Schema(description = "销售单编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "销售单编号不能为空")
    private Long id;

    @Schema(description = "快递单图片地址", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "https://example.com/express.jpg")
    @NotBlank(message = "快递单图片地址不能为空")
    @Size(max = 512, message = "快递单图片地址不能超过 512 个字符")
    private String expressFileUrl;

}
