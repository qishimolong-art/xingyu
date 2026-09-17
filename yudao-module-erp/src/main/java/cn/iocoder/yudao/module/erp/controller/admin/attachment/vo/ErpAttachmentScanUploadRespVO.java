package cn.iocoder.yudao.module.erp.controller.admin.attachment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

@Schema(description = "管理后台 - 扫码上传附件上传 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class ErpAttachmentScanUploadRespVO extends ErpAttachmentScanFileRespVO {

    @Schema(description = "已上传数量", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer uploadedCount;

    @Schema(description = "会话状态", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;

}
