package cn.iocoder.yudao.module.erp.controller.app.sale.vo.customermember;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "用户 App - ERP 客户小程序授权状态 Response VO")
@Data
public class AppErpCustomerMemberAuthRespVO {

    @Schema(description = "是否已授权", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean authorized;

    @Schema(description = "ERP 客户编号", example = "1001")
    private Long customerId;

    @Schema(description = "ERP 客户名称", example = "某某客户")
    private String customerName;

    @Schema(description = "会员用户编号", example = "286")
    private Long memberUserId;

    @Schema(description = "手机号快照", example = "13800000000")
    private String mobile;

    @Schema(description = "是否允许查看价格", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean priceVisible;

    @Schema(description = "是否允许下订", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean orderEnabled;

}
