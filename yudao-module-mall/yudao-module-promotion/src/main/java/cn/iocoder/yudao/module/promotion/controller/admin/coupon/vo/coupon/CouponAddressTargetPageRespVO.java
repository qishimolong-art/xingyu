package cn.iocoder.yudao.module.promotion.controller.admin.coupon.vo.coupon;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 按收货地区预览优惠券发放对象 Response VO")
@Data
public class CouponAddressTargetPageRespVO {

    private Long addressMatchedCount;
    private Long enabledMatchedCount;
    private PageResult<CouponAddressTargetItemRespVO> pageResult;

}
