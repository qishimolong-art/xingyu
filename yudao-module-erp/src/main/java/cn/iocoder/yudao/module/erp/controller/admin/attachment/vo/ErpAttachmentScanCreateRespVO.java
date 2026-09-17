package cn.iocoder.yudao.module.erp.controller.admin.attachment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 扫码上传附件会话创建 Response VO")
@Data
@Accessors(chain = true)
public class ErpAttachmentScanCreateRespVO {

    @Schema(description = "会话编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long sessionId;

    @Schema(description = "临时票据", requiredMode = Schema.RequiredMode.REQUIRED)
    private String ticket;

    @Schema(description = "二维码 URL", requiredMode = Schema.RequiredMode.REQUIRED)
    private String qrUrl;

    @Schema(description = "过期时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime expireTime;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;

}
