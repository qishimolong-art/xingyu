package cn.iocoder.yudao.module.system.controller.app.appnotice;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.system.controller.app.appnotice.vo.AppNoticeRespVO;
import cn.iocoder.yudao.module.system.dal.dataobject.appnotice.AppNoticeDO;
import cn.iocoder.yudao.module.system.service.appnotice.AppNoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.annotation.security.PermitAll;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "用户 App - 小程序公告")
@RestController("appAppNoticeController")
@RequestMapping("/system/app-notice")
@Validated
public class AppNoticeController {

    @Resource
    private AppNoticeService appNoticeService;

    @GetMapping("/home")
    @Operation(summary = "获取首页公告")
    @PermitAll
    public CommonResult<AppNoticeRespVO> getHomeNotice() {
        AppNoticeDO appNotice = appNoticeService.getHomeAppNotice();
        return success(appNotice == null ? null : BeanUtils.toBean(appNotice, AppNoticeRespVO.class));
    }

    @GetMapping("/home-list")
    @Operation(summary = "获取首页公告列表")
    @PermitAll
    public CommonResult<List<AppNoticeRespVO>> getHomeNoticeList() {
        List<AppNoticeDO> appNoticeList = appNoticeService.getHomeAppNoticeList();
        return success(BeanUtils.toBean(appNoticeList, AppNoticeRespVO.class));
    }

    @GetMapping("/get")
    @Operation(summary = "获取公告详情")
    @Parameter(name = "id", description = "公告编号", required = true, example = "1024")
    @PermitAll
    public CommonResult<AppNoticeRespVO> getNotice(@RequestParam("id") Long id) {
        AppNoticeDO appNotice = appNoticeService.getVisibleAppNotice(id);
        return success(BeanUtils.toBean(appNotice, AppNoticeRespVO.class));
    }

}
