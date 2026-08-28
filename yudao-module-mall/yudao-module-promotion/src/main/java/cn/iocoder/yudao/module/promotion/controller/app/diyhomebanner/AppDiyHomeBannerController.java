package cn.iocoder.yudao.module.promotion.controller.app.diyhomebanner;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.promotion.controller.app.diyhomebanner.vo.AppDiyHomeBannerRespVO;
import cn.iocoder.yudao.module.promotion.convert.diyhomebanner.DiyHomeBannerConvert;
import cn.iocoder.yudao.module.promotion.dal.dataobject.diyhomebanner.DiyHomeBannerDO;
import cn.iocoder.yudao.module.promotion.service.diyhomebanner.DiyHomeBannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/promotion/diy-home-banner")
@Tag(name = "用户 APP - 首页轮播图")
@Validated
public class AppDiyHomeBannerController {

    @Resource
    private DiyHomeBannerService diyHomeBannerService;

    @GetMapping("/list")
    @Operation(summary = "获得首页轮播图列表")
    @PermitAll
    public CommonResult<List<AppDiyHomeBannerRespVO>> getDiyHomeBannerList() {
        List<DiyHomeBannerDO> bannerList = diyHomeBannerService.getEnabledDiyHomeBannerList();
        return success(DiyHomeBannerConvert.INSTANCE.convertAppList(bannerList));
    }

}
