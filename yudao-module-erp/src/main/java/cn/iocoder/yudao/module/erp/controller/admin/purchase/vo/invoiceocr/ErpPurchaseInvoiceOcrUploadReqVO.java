package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoiceocr;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "管理后台 - ERP 采购发票 OCR 上传 Request VO")
@Data
public class ErpPurchaseInvoiceOcrUploadReqVO {

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "是否上传后自动识别并匹配", example = "true")
    private Boolean autoProcess;

    @Schema(description = "发票文件列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    @NotEmpty(message = "发票文件不能为空")
    private List<File> files;

    @Data
    public static class File {

        @Schema(description = "文件名", requiredMode = Schema.RequiredMode.REQUIRED, example = "invoice.pdf")
        @NotBlank(message = "文件名不能为空")
        private String fileName;

        @Schema(description = "文件 URL", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "文件地址不能为空")
        private String fileUrl;

        @Schema(description = "文件类型", example = "application/pdf")
        private String fileType;

        @Schema(description = "文件大小")
        private Long fileSize;

    }

}
