package cn.iocoder.yudao.module.erp.controller.admin.attachment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 扫码上传附件会话 Response VO")
@Data
@Accessors(chain = true)
public class ErpAttachmentScanSessionRespVO {

    @Schema(description = "会话编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long sessionId;

    @Schema(description = "标题")
    private String title;

    @Schema(description = "业务类型预留字段")
    private String bizType;

    @Schema(description = "业务单号预留字段")
    private String bizNo;

    @Schema(description = "过期时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime expireTime;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;

    @Schema(description = "已上传数量", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer uploadedCount;

    @Schema(description = "最大附件数", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer maxFileCount;

}
