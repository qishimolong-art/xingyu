package cn.iocoder.yudao.module.promotion.controller.app.diyhomebanner.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户 APP - 首页轮播图 Response VO")
@Data
public class AppDiyHomeBannerRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "标题", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @Schema(description = "跳转链接", requiredMode = Schema.RequiredMode.REQUIRED)
    private String url;

    @Schema(description = "图片地址", requiredMode = Schema.RequiredMode.REQUIRED)
    private String picUrl;

}
