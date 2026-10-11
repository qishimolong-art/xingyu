package cn.iocoder.yudao.module.promotion.controller.admin.coupon.vo.coupon;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 按收货地区发放优惠券结果 Response VO")
@Data
public class CouponAddressSendRespVO {

    private Long addressMatchedCount;
    private Long disabledSkippedCount;
    private Long excludedCount;
    private Long limitSkippedCount;
    private Long successCount;

}
