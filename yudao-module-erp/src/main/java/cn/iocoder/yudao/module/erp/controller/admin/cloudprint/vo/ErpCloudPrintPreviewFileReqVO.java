package cn.iocoder.yudao.module.erp.controller.admin.cloudprint.vo;

import lombok.Data;

import javax.validation.constraints.NotBlank;

@Data
public class ErpCloudPrintPreviewFileReqVO {

    @NotBlank(message = "预览令牌不能为空")
    private String previewToken;

    @NotBlank(message = "预览项编号不能为空")
    private String previewItemId;
}
