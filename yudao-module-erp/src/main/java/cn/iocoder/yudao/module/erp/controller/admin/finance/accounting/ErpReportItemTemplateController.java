package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.reporttemplate.ErpReportItemTemplatePageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.reporttemplate.ErpReportItemTemplateRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.reporttemplate.ErpReportItemTemplateSaveReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting.ErpReportItemTemplateDO;
import cn.iocoder.yudao.module.erp.service.finance.accounting.ErpReportItemTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - ERP 报表项目模板")
@RestController
@RequestMapping("/erp/report-item-template")
@Validated
public class ErpReportItemTemplateController {

    @Resource
    private ErpReportItemTemplateService reportItemTemplateService;

    @PostMapping("/create")
    @Operation(summary = "创建报表项目")
    @PreAuthorize("@ss.hasPermission('erp:balance-sheet:query')")
    public CommonResult<Long> createReportItemTemplate(@Valid @RequestBody ErpReportItemTemplateSaveReqVO createReqVO) {
        return success(reportItemTemplateService.createReportItemTemplate(createReqVO));
    }

    @PutMapping("/update")
    @Operation(summary = "更新报表项目（用于配公式）")
    @PreAuthorize("@ss.hasPermission('erp:balance-sheet:query')")
    public CommonResult<Boolean> updateReportItemTemplate(@Valid @RequestBody ErpReportItemTemplateSaveReqVO updateReqVO) {
        reportItemTemplateService.updateReportItemTemplate(updateReqVO);
        return success(true);
    }

    @DeleteMapping("/delete")
    @Operation(summary = "删除报表项目")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:balance-sheet:query')")
    public CommonResult<Boolean> deleteReportItemTemplate(@RequestParam("id") Long id) {
        reportItemTemplateService.deleteReportItemTemplate(id);
        return success(true);
    }

    @GetMapping("/get")
    @Operation(summary = "获得报表项目")
    @Parameter(name = "id", description = "编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:balance-sheet:query')")
    public CommonResult<ErpReportItemTemplateRespVO> getReportItemTemplate(@RequestParam("id") Long id) {
        ErpReportItemTemplateDO template = reportItemTemplateService.getReportItemTemplate(id);
        return success(BeanUtils.toBean(template, ErpReportItemTemplateRespVO.class));
    }

    @GetMapping("/list")
    @Operation(summary = "获得报表项目列表（树形渲染用）")
    @Parameter(name = "reportType", description = "报表类型", required = true, example = "1")
    @Parameter(name = "side", description = "区域：1=左 2=右", example = "1")
    @PreAuthorize("@ss.hasPermission('erp:balance-sheet:query')")
    public CommonResult<List<ErpReportItemTemplateRespVO>> getReportItemTemplateList(
            @RequestParam("reportType") Integer reportType,
            @RequestParam(value = "side", required = false) Integer side) {
        List<ErpReportItemTemplateDO> list = reportItemTemplateService.getReportItemTemplateList(reportType, side);
        return success(BeanUtils.toBean(list, ErpReportItemTemplateRespVO.class));
    }

    @GetMapping("/page")
    @Operation(summary = "获得报表项目分页")
    @PreAuthorize("@ss.hasPermission('erp:balance-sheet:query')")
    public CommonResult<PageResult<ErpReportItemTemplateRespVO>> getReportItemTemplatePage(@Valid ErpReportItemTemplatePageReqVO pageReqVO) {
        PageResult<ErpReportItemTemplateDO> pageResult = reportItemTemplateService.getReportItemTemplatePage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ErpReportItemTemplateRespVO.class));
    }

}
