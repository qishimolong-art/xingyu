package cn.iocoder.yudao.module.system.controller.admin.appnotice;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.system.controller.admin.appnotice.vo.AppNoticePageReqVO;
import cn.iocoder.yudao.module.system.controller.admin.appnotice.vo.AppNoticeRespVO;
import cn.iocoder.yudao.module.system.controller.admin.appnotice.vo.AppNoticeSaveReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.appnotice.AppNoticeDO;
import cn.iocoder.yudao.module.system.service.appnotice.AppNoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - 小程序公告")
@RestController("adminAppNoticeController")
@RequestMapping("/system/app-notice")
@Validated
public class AppNoticeController {

    @Resource
    private AppNoticeService appNoticeService;

    @PostMapping("/create")
    @Operation(summary = "创建小程序公告")
    @PreAuthorize("@ss.hasPermission('system:app-notice:create')")
    public CommonResult<Long> createAppNotice(@Valid @RequestBody AppNoticeSaveReqVO createReqVO) {
        return success(appNoticeService.createAppNotice(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "修改小程序公告")
    @PreAuthorize("@ss.hasPermission('system:app-notice:update')")
    public CommonResult<Boolean> updateAppNotice(@Valid @RequestBody AppNoticeSaveReqVO updateReqVO) {
        appNoticeService.updateAppNotice(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除小程序公告")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:app-notice:delete')")
    public CommonResult<Boolean> deleteAppNotice(@RequestParam("id") Long id) {
        appNoticeService.deleteAppNotice(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得小程序公告")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('system:app-notice:query')")
    public CommonResult<AppNoticeRespVO> getAppNotice(@RequestParam("id") Long id) {
        AppNoticeDO appNotice = appNoticeService.getAppNotice(id);
        return success(BeanUtils.toBean(appNotice, AppNoticeRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得小程序公告分页")
    @PreAuthorize("@ss.hasPermission('system:app-notice:query')")
    public CommonResult<PageResult<AppNoticeRespVO>> getAppNoticePage(@Validated AppNoticePageReqVO pageReqVO) {
        PageResult<AppNoticeDO> pageResult = appNoticeService.getAppNoticePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, AppNoticeRespVO.class));
    }

}
