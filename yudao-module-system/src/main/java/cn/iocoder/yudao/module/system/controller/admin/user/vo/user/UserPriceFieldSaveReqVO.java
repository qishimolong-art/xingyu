package cn.iocoder.yudao.module.system.controller.admin.user.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 用户价格字段配置 - 保存请求 VO
 */
@Schema(description = "用户价格字段配置保存 Request VO")
@Data
public class UserPriceFieldSaveReqVO {

    @Schema(description = "用户 ID", required = true)
    @NotNull(message = "用户 ID 不能为空")
    private Long userId;

    @Schema(description = "设为可见的价格字段编码列表")
    private List<String> fieldCodes;

}
