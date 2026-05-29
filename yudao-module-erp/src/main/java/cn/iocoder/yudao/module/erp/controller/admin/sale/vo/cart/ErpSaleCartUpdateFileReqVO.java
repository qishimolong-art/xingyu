package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - ERP 销售手推车更新快递单 Request VO")
@Data
public class ErpSaleCartUpdateFileReqVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "编号不能为空")
    private Long id;

    @Schema(description = "快递单图片地址", example = "https://example.com/express.jpg")
    private String fileUrl;

}
