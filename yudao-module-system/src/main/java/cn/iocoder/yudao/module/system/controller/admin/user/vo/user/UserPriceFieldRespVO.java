package cn.iocoder.yudao.module.system.controller.admin.user.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 用户价格字段配置 - 响应 VO
 */
@Schema(description = "用户价格字段配置 Response VO")
@Data
public class UserPriceFieldRespVO {

    @Schema(description = "主键")
    private Long id;

    @Schema(description = "用户 ID")
    private Long userId;

    @Schema(description = "价格字段编码")
    private String priceFieldCode;

    @Schema(description = "是否可见")
    private Boolean visible;

}
