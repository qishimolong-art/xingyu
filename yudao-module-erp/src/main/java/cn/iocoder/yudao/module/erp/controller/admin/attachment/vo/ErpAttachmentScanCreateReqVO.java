package cn.iocoder.yudao.module.erp.controller.admin.attachment.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Size;

@Schema(description = "管理后台 - 扫码上传附件会话创建 Request VO")
@Data
public class ErpAttachmentScanCreateReqVO {

    @Schema(description = "业务类型预留字段", example = "purchase-order")
    @Size(max = 64, message = "业务类型长度不能超过 64 个字符")
    private String bizType;

    @Schema(description = "业务编号预留字段", example = "100")
    private Long bizId;

    @Schema(description = "业务单号预留字段", example = "CGD202609110001")
    @Size(max = 64, message = "业务单号长度不能超过 64 个字符")
    private String bizNo;

    @Schema(description = "上传页面标题", example = "采购订单附件")
    @Size(max = 100, message = "标题长度不能超过 100 个字符")
    private String title;

    @Schema(description = "最大附件数", example = "9")
    @Min(value = 1, message = "最大附件数不能小于 1")
    @Max(value = 9, message = "最大附件数不能大于 9")
    private Integer maxFileCount;

}
