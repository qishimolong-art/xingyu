package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.businesslicense;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Schema(description = "管理后台 - ERP 客户营业执照识别 Request VO")
@Data
public class ErpCustomerBusinessLicenseOcrReqVO {

    @Schema(description = "营业执照图片 Base64 或图片 URL", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "营业执照图片不能为空")
    @Size(max = 20 * 1024 * 1024, message = "营业执照图片内容不能超过 20MB")
    private String image;

}
