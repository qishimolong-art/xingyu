package cn.iocoder.yudao.module.member.controller.app.auth.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Schema(description = "用户 APP - 微信小程序静默登录 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppAuthWeixinMiniAppSilentLoginRespVO {

    @Schema(description = "是否需要手机号授权", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean needPhoneAuth;

    @Schema(description = "用户编号", example = "1024")
    private Long userId;

    @Schema(description = "访问令牌", example = "happy")
    private String accessToken;

    @Schema(description = "刷新令牌", example = "nice")
    private String refreshToken;

    @Schema(description = "过期时间")
    private LocalDateTime expiresTime;

    @Schema(description = "社交用户 openid", example = "qq768")
    private String openid;

    public static AppAuthWeixinMiniAppSilentLoginRespVO needPhoneAuth() {
        return AppAuthWeixinMiniAppSilentLoginRespVO.builder().needPhoneAuth(true).build();
    }

    public static AppAuthWeixinMiniAppSilentLoginRespVO loginSuccess(AppAuthLoginRespVO loginRespVO) {
        return AppAuthWeixinMiniAppSilentLoginRespVO.builder()
                .needPhoneAuth(false)
                .userId(loginRespVO.getUserId())
                .accessToken(loginRespVO.getAccessToken())
                .refreshToken(loginRespVO.getRefreshToken())
                .expiresTime(loginRespVO.getExpiresTime())
                .openid(loginRespVO.getOpenid())
                .build();
    }

}
