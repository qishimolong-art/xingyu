package cn.iocoder.yudao.module.promotion.service.coupon.bo;

import lombok.Data;

import java.util.List;

@Data
public class CouponAddressTargetItemBO {

    private Long userId;
    private String nickname;
    private String mobile;
    private List<String> matchedAreaNames;
    private Boolean sendable;
    private String skipReason;

}
