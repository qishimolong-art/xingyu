package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoiceocr;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Schema(description = "管理后台 - ERP 采购发票 OCR 修改厂家单号 Request VO")
@Data
public class ErpPurchaseInvoiceOcrUpdateFactoryOrderNoReqVO {

    @Schema(description = "厂家单号", requiredMode = Schema.RequiredMode.REQUIRED, example = "S250801001")
    @NotBlank(message = "厂家单号不能为空")
    @Size(max = 64, message = "厂家单号长度不能超过 64 个字符")
    private String factoryOrderNo;

}
