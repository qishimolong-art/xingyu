package cn.iocoder.yudao.module.promotion.controller.admin.coupon.vo.coupon;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 按收货地区发放优惠券的会员预览 Response VO")
@Data
public class CouponAddressTargetItemRespVO {

    private Long userId;
    private String nickname;
    private String mobile;
    private List<String> matchedAreaNames;
    private Boolean sendable;
    private String skipReason;

}
