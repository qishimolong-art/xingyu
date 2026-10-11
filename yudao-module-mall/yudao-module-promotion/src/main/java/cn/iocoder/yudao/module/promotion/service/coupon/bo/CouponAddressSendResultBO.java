package cn.iocoder.yudao.module.promotion.service.coupon.bo;

import lombok.Data;

@Data
public class CouponAddressSendResultBO {

    private Long addressMatchedCount;
    private Long disabledSkippedCount;
    private Long excludedCount;
    private Long limitSkippedCount;
    private Long successCount;

}
