package cn.iocoder.yudao.module.system.controller.admin.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotEmpty;

@Schema(description = "管理后台 - 企业微信 H5 免登 Request VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthWeComLoginReqVO {

    @Schema(description = "企业微信网页授权返回的 code", requiredMode = Schema.RequiredMode.REQUIRED, example = "CODE")
    @NotEmpty(message = "授权码不能为空")
    private String code;

    @Schema(description = "企业微信网页授权返回的 state", requiredMode = Schema.RequiredMode.REQUIRED,
            example = "9b2ffbc1-7425-4155-9894-9d5c08541d62")
    @NotEmpty(message = "state 不能为空")
    private String state;

}
