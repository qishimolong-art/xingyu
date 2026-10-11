package cn.iocoder.yudao.module.promotion.controller.admin.coupon.vo.coupon;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;

@Schema(description = "管理后台 - 按收货地区发放优惠券 Request VO")
@Data
public class CouponAddressSendReqVO {

    @Schema(description = "单次操作编号，用于防重复提交", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "操作编号不能为空")
    private UUID requestId;

    @Schema(description = "优惠券模板编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "优惠券模板编号不能为空")
    private Long templateId;

    @Schema(description = "选中的省、市、区县编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "收货地区不能为空")
    private Set<Integer> areaIds;

    @Schema(description = "人工排除的会员编号")
    private Set<Long> excludedUserIds = Collections.emptySet();

}
