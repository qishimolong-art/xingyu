package cn.iocoder.yudao.module.erp.controller.admin.base;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.base.vo.recyclebin.ErpRecycleBinBatchReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.base.vo.recyclebin.ErpRecycleBinPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.base.vo.recyclebin.ErpRecycleBinRespVO;
import cn.iocoder.yudao.module.erp.service.base.ErpRecycleBinService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - ERP 基础档案回收站")
@RestController
@RequestMapping("/erp/recycle-bin")
@Validated
public class ErpRecycleBinController {

    @Resource
    private ErpRecycleBinService recycleBinService;

    @GetMapping("/page")
    @Operation(summary = "获得回收站分页")
    @PreAuthorize("@ss.hasPermission('erp:recycle-bin:query')")
    public CommonResult<PageResult<ErpRecycleBinRespVO>> getRecycleBinPage(@Valid ErpRecycleBinPageReqVO pageReqVO) {
        return success(recycleBinService.getRecycleBinPage(pageReqVO));
    }

    @PutMapping("/restore")
    @Operation(summary = "还原回收站数据")
    @PreAuthorize("@ss.hasPermission('erp:recycle-bin:restore')")
    public CommonResult<Boolean> restore(@Valid @RequestBody ErpRecycleBinBatchReqVO reqVO) {
        recycleBinService.restore(reqVO);
        return success(true);
    }

    @DeleteMapping("/clear")
    @Operation(summary = "清除回收站数据")
    @PreAuthorize("@ss.hasPermission('erp:recycle-bin:clear')")
    public CommonResult<Boolean> clear(@Valid @RequestBody ErpRecycleBinBatchReqVO reqVO) {
        recycleBinService.clear(reqVO);
        return success(true);
    }

}
