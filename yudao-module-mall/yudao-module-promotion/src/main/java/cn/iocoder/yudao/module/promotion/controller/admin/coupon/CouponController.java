package cn.iocoder.yudao.module.promotion.controller.admin.coupon;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.collection.MapUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.idempotent.core.annotation.Idempotent;
import cn.iocoder.yudao.framework.idempotent.core.keyresolver.impl.ExpressionIdempotentKeyResolver;
import cn.iocoder.yudao.module.member.api.user.MemberUserApi;
import cn.iocoder.yudao.module.member.api.user.dto.MemberUserRespDTO;
import cn.iocoder.yudao.module.promotion.controller.admin.coupon.vo.coupon.*;
import cn.iocoder.yudao.module.promotion.convert.coupon.CouponConvert;
import cn.iocoder.yudao.module.promotion.dal.dataobject.coupon.CouponDO;
import cn.iocoder.yudao.module.promotion.service.coupon.CouponService;
import cn.iocoder.yudao.module.promotion.service.coupon.bo.CouponAddressSendResultBO;
import cn.iocoder.yudao.module.promotion.service.coupon.bo.CouponAddressTargetPageResultBO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.common.util.collection.CollectionUtils.convertSet;

@Tag(name = "管理后台 - 优惠劵")
@RestController
@RequestMapping("/promotion/coupon")
@Validated
public class CouponController {

    @Resource
    private CouponService couponService;
    @Resource
    private MemberUserApi memberUserApi;

    @DeleteMapping("/delete")
    @Operation(summary = "回收优惠劵")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('promotion:coupon:delete')")
    public CommonResult<Boolean> deleteCoupon(@RequestParam("id") Long id) {
        couponService.deleteCoupon(id);
        return success(true);
    }

    @GetMapping("/page")
    @Operation(summary = "获得优惠劵分页")
    @PreAuthorize("@ss.hasPermission('promotion:coupon:query')")
    public CommonResult<PageResult<CouponPageItemRespVO>> getCouponPage(@Valid CouponPageReqVO pageVO) {
        PageResult<CouponDO> pageResult = couponService.getCouponPage(pageVO);
        PageResult<CouponPageItemRespVO> pageResulVO = CouponConvert.INSTANCE.convertPage(pageResult);
        if (CollUtil.isEmpty(pageResulVO.getList())) {
            return success(pageResulVO);
        }

        // 读取用户信息，进行拼接
        Map<Long, MemberUserRespDTO> userMap = memberUserApi.getUserMap(convertSet(pageResult.getList(), CouponDO::getUserId));
        pageResulVO.getList().forEach(itemRespVO -> MapUtils.findAndThen(userMap, itemRespVO.getUserId(),
                userRespDTO -> itemRespVO.setNickname(userRespDTO.getNickname())));
        return success(pageResulVO);
    }

    @PostMapping("/send")
    @Operation(summary = "发送优惠劵")
    @PreAuthorize("@ss.hasPermission('promotion:coupon:send')")
    public CommonResult<Boolean> sendCoupon(@Valid @RequestBody CouponSendReqVO reqVO) {
        couponService.takeCouponByAdmin(reqVO.getTemplateId(), reqVO.getUserIds());
        return success(true);
    }

    @PostMapping("/address-target/page")
    @Operation(summary = "按收货地区预览优惠券发放对象")
    @PreAuthorize("@ss.hasPermission('promotion:coupon:send')")
    public CommonResult<CouponAddressTargetPageRespVO> getAddressTargetPage(
            @Valid @RequestBody CouponAddressTargetPageReqVO reqVO) {
        CouponAddressTargetPageResultBO result = couponService.getAddressTargetPage(reqVO);
        CouponAddressTargetPageRespVO respVO = BeanUtils.toBean(result, CouponAddressTargetPageRespVO.class);
        respVO.setPageResult(new PageResult<>(
                BeanUtils.toBean(result.getPageResult().getList(), CouponAddressTargetItemRespVO.class),
                result.getPageResult().getTotal()));
        return success(respVO);
    }

    @PostMapping("/send-by-address")
    @Operation(summary = "按收货地区发送优惠券")
    @PreAuthorize("@ss.hasPermission('promotion:coupon:send')")
    @Idempotent(timeout = 1, timeUnit = TimeUnit.DAYS,
            keyResolver = ExpressionIdempotentKeyResolver.class,
            keyArg = "#reqVO.requestId.toString()")
    public CommonResult<CouponAddressSendRespVO> sendCouponByAddress(
            @Valid @RequestBody CouponAddressSendReqVO reqVO) {
        CouponAddressSendResultBO result = couponService.takeCouponByAddress(
                reqVO.getTemplateId(), reqVO.getAreaIds(), reqVO.getExcludedUserIds());
        return success(BeanUtils.toBean(result, CouponAddressSendRespVO.class));
    }

}
