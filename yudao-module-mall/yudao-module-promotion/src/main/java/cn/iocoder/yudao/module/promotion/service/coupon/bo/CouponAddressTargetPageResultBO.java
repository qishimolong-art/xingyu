package cn.iocoder.yudao.module.promotion.service.coupon.bo;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import lombok.Data;

@Data
public class CouponAddressTargetPageResultBO {

    private Long addressMatchedCount;
    private Long enabledMatchedCount;
    private PageResult<CouponAddressTargetItemBO> pageResult;

}
