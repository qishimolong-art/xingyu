package cn.iocoder.yudao.module.erp.controller.admin.common;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.print.ErpPrintFieldRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.print.ErpPrintRecordCreateReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.print.ErpPrintTemplateRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.common.vo.print.ErpPrintTemplateSaveReqVO;
import cn.iocoder.yudao.module.erp.service.common.ErpPrintService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.validation.Valid;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - ERP 打印模板")
@RestController
@RequestMapping("/erp/print")
@Validated
public class ErpPrintController {

    @Resource
    private ErpPrintService printService;

    @GetMapping("/fields")
    @Operation(summary = "获取打印字段")
    @PreAuthorize("@ss.hasPermission('erp:purchase-order:print-template')")
    public CommonResult<ErpPrintFieldRespVO> getFields(@RequestParam("moduleKey") String moduleKey) {
        return success(printService.getFields(moduleKey));
    }

    @GetMapping("/template/default")
    @Operation(summary = "获取默认打印模板")
    @PreAuthorize("@ss.hasAnyPermissions('erp:purchase-order:print', 'erp:purchase-order:print-template')")
    public CommonResult<ErpPrintTemplateRespVO> getDefaultTemplate(@RequestParam("moduleKey") String moduleKey) {
        return success(printService.getDefaultTemplate(moduleKey));
    }

    @GetMapping("/template/list")
    @Operation(summary = "获取打印模板列表")
    @PreAuthorize("@ss.hasPermission('erp:purchase-order:print-template')")
    public CommonResult<List<ErpPrintTemplateRespVO>> getTemplateList(@RequestParam("moduleKey") String moduleKey) {
        return success(printService.getTemplateList(moduleKey));
    }

    @GetMapping("/template/get")
    @Operation(summary = "获取打印模板详情")
    @PreAuthorize("@ss.hasAnyPermissions('erp:purchase-order:print', 'erp:purchase-order:print-template')")
    public CommonResult<ErpPrintTemplateRespVO> getTemplate(@RequestParam("id") Long id) {
        return success(printService.getTemplate(id));
    }

    @PutMapping("/template/save")
    @Operation(summary = "保存打印模板")
    @PreAuthorize("@ss.hasPermission('erp:purchase-order:print-template')")
    public CommonResult<Long> saveTemplate(@Valid @RequestBody ErpPrintTemplateSaveReqVO reqVO) {
        return success(printService.saveTemplate(reqVO));
    }

    @PostMapping("/template/save-as")
    @Operation(summary = "另存为打印模板")
    @PreAuthorize("@ss.hasPermission('erp:purchase-order:print-template')")
    public CommonResult<Long> saveAsTemplate(@Valid @RequestBody ErpPrintTemplateSaveReqVO reqVO) {
        return success(printService.saveAsTemplate(reqVO));
    }

    @PostMapping("/record")
    @Operation(summary = "记录打印")
    @PreAuthorize("@ss.hasPermission('erp:purchase-order:print')")
    public CommonResult<Boolean> recordPrint(@Valid @RequestBody ErpPrintRecordCreateReqVO reqVO) {
        printService.recordPrint(reqVO);
        return success(true);
    }

}
