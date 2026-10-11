package cn.iocoder.yudao.module.promotion.controller.admin.coupon.vo.coupon;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.Set;

@Schema(description = "管理后台 - 按收货地区预览优惠券发放对象 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class CouponAddressTargetPageReqVO extends PageParam {

    @Schema(description = "优惠券模板编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "优惠券模板编号不能为空")
    private Long templateId;

    @Schema(description = "选中的省、市、区县编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "收货地区不能为空")
    private Set<Integer> areaIds;

}
