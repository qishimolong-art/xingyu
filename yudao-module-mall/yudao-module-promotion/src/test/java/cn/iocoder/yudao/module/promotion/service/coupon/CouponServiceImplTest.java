package cn.iocoder.yudao.module.promotion.service.coupon;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.exception.ServiceException;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.test.core.ut.BaseMockitoUnitTest;
import cn.iocoder.yudao.module.member.api.address.MemberAddressApi;
import cn.iocoder.yudao.module.member.api.address.dto.MemberAddressTargetRespDTO;
import cn.iocoder.yudao.module.member.api.user.MemberUserApi;
import cn.iocoder.yudao.module.promotion.controller.admin.coupon.vo.coupon.CouponAddressTargetPageReqVO;
import cn.iocoder.yudao.module.promotion.dal.dataobject.coupon.CouponDO;
import cn.iocoder.yudao.module.promotion.dal.dataobject.coupon.CouponTemplateDO;
import cn.iocoder.yudao.module.promotion.dal.mysql.coupon.CouponMapper;
import cn.iocoder.yudao.module.promotion.enums.common.PromotionDiscountTypeEnum;
import cn.iocoder.yudao.module.promotion.enums.common.PromotionProductScopeEnum;
import cn.iocoder.yudao.module.promotion.enums.coupon.CouponTakeTypeEnum;
import cn.iocoder.yudao.module.promotion.enums.coupon.CouponTemplateValidityTypeEnum;
import cn.iocoder.yudao.module.promotion.service.coupon.bo.CouponAddressSendResultBO;
import cn.iocoder.yudao.module.promotion.service.coupon.bo.CouponAddressTargetPageResultBO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CouponServiceImplTest extends BaseMockitoUnitTest {

    @InjectMocks
    private CouponServiceImpl couponService;
    @Mock
    private CouponTemplateService couponTemplateService;
    @Mock
    private CouponMapper couponMapper;
    @Mock
    private MemberUserApi memberUserApi;
    @Mock
    private MemberAddressApi memberAddressApi;

    @Test
    void takeCouponByAddress_filtersAndCreatesOneCouponPerEligibleUser() {
        CouponTemplateDO template = buildTemplate();
        when(couponTemplateService.getCouponTemplate(1L)).thenReturn(template);
        when(couponTemplateService.isTakeLimitCountUnlimited(1)).thenReturn(false);
        when(couponTemplateService.isTotalCountUnlimited(-1)).thenReturn(true);
        when(memberAddressApi.getAddressTargetUserList(anySet(), isNull())).thenReturn(Arrays.asList(
                target(1L, CommonStatusEnum.ENABLE.getStatus()),
                target(1L, CommonStatusEnum.ENABLE.getStatus()),
                target(2L, CommonStatusEnum.DISABLE.getStatus()),
                target(3L, CommonStatusEnum.ENABLE.getStatus()),
                target(4L, CommonStatusEnum.ENABLE.getStatus())));
        when(memberAddressApi.getAddressTargetCount(anySet())).thenReturn(5L);
        HashMap<Long, Integer> takeCounts = new HashMap<>();
        takeCounts.put(4L, 1);
        when(couponMapper.selectCountByTemplateIdAndUserIds(eq(1L), anyCollection())).thenReturn(takeCounts);

        CouponAddressSendResultBO result = couponService.takeCouponByAddress(
                1L, Collections.singleton(510000), Collections.singleton(3L));

        assertEquals(5L, result.getAddressMatchedCount());
        assertEquals(1L, result.getDisabledSkippedCount());
        assertEquals(1L, result.getExcludedCount());
        assertEquals(1L, result.getLimitSkippedCount());
        assertEquals(1L, result.getSuccessCount());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CouponDO>> couponCaptor = ArgumentCaptor.forClass(List.class);
        verify(couponMapper).insertBatch(couponCaptor.capture(), eq(500));
        assertEquals(Collections.singletonList(1L),
                Collections.singletonList(couponCaptor.getValue().get(0).getUserId()));
        verify(couponTemplateService).updateCouponTemplateTakeCount(1L, 1);
    }

    @Test
    void takeCouponByAddress_propagatesTechnicalFailureForTransactionRollback() {
        CouponTemplateDO template = buildTemplate();
        when(couponTemplateService.getCouponTemplate(1L)).thenReturn(template);
        when(couponTemplateService.isTakeLimitCountUnlimited(1)).thenReturn(false);
        when(couponTemplateService.isTotalCountUnlimited(-1)).thenReturn(true);
        when(memberAddressApi.getAddressTargetUserList(anySet(), isNull()))
                .thenReturn(Collections.singletonList(target(1L, CommonStatusEnum.ENABLE.getStatus())));
        when(couponMapper.selectCountByTemplateIdAndUserIds(eq(1L), anyCollection()))
                .thenReturn(Collections.emptyMap());
        doThrow(new IllegalStateException("database error"))
                .when(couponTemplateService).updateCouponTemplateTakeCount(1L, 1);

        assertThrows(IllegalStateException.class, () -> couponService.takeCouponByAddress(
                1L, Collections.singleton(510000), Collections.emptySet()));
        verify(couponMapper).insertBatch(anyCollection(), eq(500));
    }

    @Test
    void getAddressTargetPage_masksMobileAndMarksTakeLimit() {
        CouponTemplateDO template = buildTemplate();
        MemberAddressTargetRespDTO target = target(1L, CommonStatusEnum.ENABLE.getStatus());
        target.setNickname("会员一");
        target.setMobile("13812345678");
        target.setMatchedAreaNames(Collections.singletonList("四川省 成都市 武侯区"));
        when(couponTemplateService.getCouponTemplate(1L)).thenReturn(template);
        when(couponTemplateService.isTakeLimitCountUnlimited(1)).thenReturn(false);
        when(memberAddressApi.getAddressTargetUserPage(anySet(), eq(CommonStatusEnum.ENABLE.getStatus()), any()))
                .thenReturn(new PageResult<>(Collections.singletonList(target), 1L));
        when(memberAddressApi.getAddressTargetCount(anySet())).thenReturn(13L);
        when(couponMapper.selectCountByTemplateIdAndUserIds(eq(1L), anyCollection()))
                .thenReturn(Collections.singletonMap(1L, 1));
        CouponAddressTargetPageReqVO reqVO = new CouponAddressTargetPageReqVO();
        reqVO.setTemplateId(1L);
        reqVO.setAreaIds(Collections.singleton(510000));
        reqVO.setPageNo(1);
        reqVO.setPageSize(10);

        CouponAddressTargetPageResultBO result = couponService.getAddressTargetPage(reqVO);

        assertEquals(13L, result.getAddressMatchedCount());
        assertEquals(1L, result.getEnabledMatchedCount());
        assertEquals("138****5678", result.getPageResult().getList().get(0).getMobile());
        assertEquals(false, result.getPageResult().getList().get(0).getSendable());
        assertEquals("已达每人限领上限", result.getPageResult().getList().get(0).getSkipReason());
    }

    @Test
    void takeCouponByAddress_capsByTemplateRemainingCount() {
        CouponTemplateDO template = buildTemplate().setTotalCount(2).setTakeCount(1);
        when(couponTemplateService.getCouponTemplate(1L)).thenReturn(template);
        when(couponTemplateService.isTakeLimitCountUnlimited(1)).thenReturn(false);
        when(couponTemplateService.isTotalCountUnlimited(2)).thenReturn(false);
        when(memberAddressApi.getAddressTargetUserList(anySet(), isNull())).thenReturn(Arrays.asList(
                target(3L, CommonStatusEnum.ENABLE.getStatus()),
                target(1L, CommonStatusEnum.ENABLE.getStatus()),
                target(2L, CommonStatusEnum.ENABLE.getStatus())));
        when(memberAddressApi.getAddressTargetCount(anySet())).thenReturn(3L);
        when(couponMapper.selectCountByTemplateIdAndUserIds(eq(1L), anyCollection()))
                .thenReturn(Collections.emptyMap());

        CouponAddressSendResultBO result = couponService.takeCouponByAddress(
                1L, Collections.singleton(510000), Collections.emptySet());

        assertEquals(1L, result.getSuccessCount());
        assertEquals(2L, result.getLimitSkippedCount());
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CouponDO>> couponCaptor = ArgumentCaptor.forClass(List.class);
        verify(couponMapper).insertBatch(couponCaptor.capture(), eq(500));
        assertEquals(1L, couponCaptor.getValue().get(0).getUserId());
    }

    @Test
    void takeCouponByAddress_rejectsWrongTypeDisabledAndExpiredTemplates() {
        CouponTemplateDO template = buildTemplate().setTakeType(CouponTakeTypeEnum.USER.getType());
        when(couponTemplateService.getCouponTemplate(1L)).thenReturn(template);
        assertThrows(ServiceException.class, () -> couponService.takeCouponByAddress(
                1L, Collections.singleton(510000), Collections.emptySet()));

        template.setTakeType(CouponTakeTypeEnum.ADMIN.getType())
                .setStatus(CommonStatusEnum.DISABLE.getStatus());
        assertThrows(ServiceException.class, () -> couponService.takeCouponByAddress(
                1L, Collections.singleton(510000), Collections.emptySet()));

        template.setStatus(CommonStatusEnum.ENABLE.getStatus())
                .setValidityType(CouponTemplateValidityTypeEnum.DATE.getType())
                .setValidEndTime(LocalDateTime.now().minusMinutes(1));
        assertThrows(ServiceException.class, () -> couponService.takeCouponByAddress(
                1L, Collections.singleton(510000), Collections.emptySet()));
        verifyNoInteractions(memberAddressApi);
    }

    private static CouponTemplateDO buildTemplate() {
        return new CouponTemplateDO()
                .setId(1L).setName("定向券")
                .setStatus(CommonStatusEnum.ENABLE.getStatus())
                .setTakeType(CouponTakeTypeEnum.ADMIN.getType())
                .setTotalCount(CouponTemplateDO.TOTAL_COUNT_MAX).setTakeCount(0)
                .setTakeLimitCount(1)
                .setValidityType(CouponTemplateValidityTypeEnum.TERM.getType())
                .setFixedStartTerm(0).setFixedEndTerm(7)
                .setUsePrice(0)
                .setProductScope(PromotionProductScopeEnum.ALL.getScope())
                .setDiscountType(PromotionDiscountTypeEnum.PRICE.getType())
                .setDiscountPrice(100);
    }

    private static MemberAddressTargetRespDTO target(Long userId, Integer status) {
        MemberAddressTargetRespDTO target = new MemberAddressTargetRespDTO();
        target.setUserId(userId);
        target.setStatus(status);
        return target;
    }

}
