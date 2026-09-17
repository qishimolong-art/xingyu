package cn.iocoder.yudao.module.erp.controller.admin.attachment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import java.util.List;

@Schema(description = "管理后台 - 扫码上传附件状态 Response VO")
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class ErpAttachmentScanStatusRespVO extends ErpAttachmentScanSessionRespVO {

    @Schema(description = "附件列表", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<ErpAttachmentScanFileRespVO> files;

}
