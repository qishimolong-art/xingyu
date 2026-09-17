package cn.iocoder.yudao.module.erp.controller.admin.attachment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 扫码上传附件明细 Response VO")
@Data
@Accessors(chain = true)
public class ErpAttachmentScanFileRespVO {

    @Schema(description = "附件明细编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long fileId;

    @Schema(description = "文件名", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fileName;

    @Schema(description = "文件 URL", requiredMode = Schema.RequiredMode.REQUIRED)
    private String fileUrl;

    @Schema(description = "文件类型")
    private String fileType;

    @Schema(description = "文件大小", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long fileSize;

    @Schema(description = "上传时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime uploadTime;

}
