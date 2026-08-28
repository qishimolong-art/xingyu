package cn.iocoder.yudao.module.promotion.controller.admin.diyhomebanner;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.promotion.controller.admin.diyhomebanner.vo.DiyHomeBannerCreateReqVO;
import cn.iocoder.yudao.module.promotion.controller.admin.diyhomebanner.vo.DiyHomeBannerPageReqVO;
import cn.iocoder.yudao.module.promotion.controller.admin.diyhomebanner.vo.DiyHomeBannerRespVO;
import cn.iocoder.yudao.module.promotion.controller.admin.diyhomebanner.vo.DiyHomeBannerUpdateReqVO;
import cn.iocoder.yudao.module.promotion.convert.diyhomebanner.DiyHomeBannerConvert;
import cn.iocoder.yudao.module.promotion.dal.dataobject.diyhomebanner.DiyHomeBannerDO;
import cn.iocoder.yudao.module.promotion.service.diyhomebanner.DiyHomeBannerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 首页轮播图")
@RestController
@RequestMapping("/promotion/diy-home-banner")
@Validated
public class DiyHomeBannerController {

    @Resource
    private DiyHomeBannerService diyHomeBannerService;

    @PostMapping("/create")
    @Operation(summary = "创建首页轮播图")
    @PreAuthorize("@ss.hasPermission('promotion:diy-home-banner:create')")
    public CommonResult<Long> createDiyHomeBanner(@Valid @RequestBody DiyHomeBannerCreateReqVO createReqVO) {
        return success(diyHomeBannerService.createDiyHomeBanner(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新首页轮播图")
    @PreAuthorize("@ss.hasPermission('promotion:diy-home-banner:update')")
    public CommonResult<Boolean> updateDiyHomeBanner(@Valid @RequestBody DiyHomeBannerUpdateReqVO updateReqVO) {
        diyHomeBannerService.updateDiyHomeBanner(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除首页轮播图")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('promotion:diy-home-banner:delete')")
    public CommonResult<Boolean> deleteDiyHomeBanner(@RequestParam("id") Long id) {
        diyHomeBannerService.deleteDiyHomeBanner(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得首页轮播图")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('promotion:diy-home-banner:query')")
    public CommonResult<DiyHomeBannerRespVO> getDiyHomeBanner(@RequestParam("id") Long id) {
        DiyHomeBannerDO banner = diyHomeBannerService.getDiyHomeBanner(id);
        return success(DiyHomeBannerConvert.INSTANCE.convert(banner));
    }

    @GetMapping("/page")
    @Operation(summary = "获得首页轮播图分页")
    @PreAuthorize("@ss.hasPermission('promotion:diy-home-banner:query')")
    public CommonResult<PageResult<DiyHomeBannerRespVO>> getDiyHomeBannerPage(@Valid DiyHomeBannerPageReqVO pageVO) {
        PageResult<DiyHomeBannerDO> pageResult = diyHomeBannerService.getDiyHomeBannerPage(pageVO);
        return success(DiyHomeBannerConvert.INSTANCE.convertPage(pageResult));
    }

}
