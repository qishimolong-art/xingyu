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
import java.util.Map;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - ERP 打印模板")
@RestController
@RequestMapping("/erp/print")
@Validated
public class ErpPrintController {

    private static final String ANY_PRINT_PERMISSION =
            "@ss.hasAnyPermissions('erp:purchase-order:print', 'erp:purchase-in:print', " +
                    "'erp:purchase-return:print', 'erp:purchase-invoice:print', " +
                    "'erp:purchase-price-adjust:print', 'erp:sale-order:print', " +
                    "'erp:sale-out:print', 'erp:sale-return:print', 'erp:sale-quote:print', " +
                    "'erp:sale-price-adjust:print', 'erp:sale-cart:print', " +
                    "'erp:finance-payment:print', 'erp:finance-receipt:print', " +
                    "'erp:payable-other:print', 'erp:payable-expense:print', " +
                    "'erp:receivable-other:print', " +
                    "'erp:receivable-other-income:print', " +
                    "'erp:finance-transfer:print', 'erp:voucher:print', " +
                    "'erp:stock-in:print', 'erp:stock-out:print', 'erp:stock-transfer-out:print', " +
                    "'erp:warehouse-move:print', 'erp:stock-check:print')";
    private static final String ANY_TEMPLATE_PERMISSION =
            "@ss.hasAnyPermissions('erp:purchase-order:print-template', 'erp:purchase-in:print-template', " +
                    "'erp:purchase-return:print-template', 'erp:purchase-invoice:print-template', " +
                    "'erp:purchase-price-adjust:print-template', 'erp:sale-order:print-template', " +
                    "'erp:sale-out:print-template', 'erp:sale-return:print-template', " +
                    "'erp:sale-quote:print-template', 'erp:sale-price-adjust:print-template', " +
                    "'erp:sale-cart:print-template', 'erp:finance-payment:print-template', " +
                    "'erp:finance-receipt:print-template', 'erp:payable-other:print-template', " +
                    "'erp:payable-expense:print-template', " +
                    "'erp:receivable-other:print-template', " +
                    "'erp:receivable-other-income:print-template', " +
                    "'erp:finance-transfer:print-template', " +
                    "'erp:voucher:print-template', " +
                    "'erp:stock-in:print-template', 'erp:stock-out:print-template', " +
                    "'erp:stock-transfer-out:print-template', " +
                    "'erp:warehouse-move:print-template', 'erp:stock-check:print-template')";
    private static final String ANY_READ_PERMISSION =
            "@ss.hasAnyPermissions('erp:purchase-order:print', 'erp:purchase-order:print-template', " +
                    "'erp:purchase-in:print', 'erp:purchase-in:print-template', " +
                    "'erp:purchase-return:print', 'erp:purchase-return:print-template', " +
                    "'erp:purchase-invoice:print', 'erp:purchase-invoice:print-template', " +
                    "'erp:purchase-price-adjust:print', 'erp:purchase-price-adjust:print-template', " +
                    "'erp:sale-order:print', 'erp:sale-order:print-template', " +
                    "'erp:sale-out:print', 'erp:sale-out:print-template', " +
                    "'erp:sale-return:print', 'erp:sale-return:print-template', " +
                    "'erp:sale-quote:print', 'erp:sale-quote:print-template', " +
                    "'erp:sale-price-adjust:print', 'erp:sale-price-adjust:print-template', " +
                    "'erp:sale-cart:print', 'erp:sale-cart:print-template', " +
                    "'erp:finance-payment:print', 'erp:finance-payment:print-template', " +
                    "'erp:finance-receipt:print', 'erp:finance-receipt:print-template', " +
                    "'erp:payable-other:print', 'erp:payable-other:print-template', " +
                    "'erp:payable-expense:print', 'erp:payable-expense:print-template', " +
                    "'erp:receivable-other:print', 'erp:receivable-other:print-template', " +
                    "'erp:receivable-other-income:print', 'erp:receivable-other-income:print-template', " +
                    "'erp:finance-transfer:print', 'erp:finance-transfer:print-template', " +
                    "'erp:voucher:print', 'erp:voucher:print-template', " +
                    "'erp:stock-in:print', 'erp:stock-in:print-template', " +
                    "'erp:stock-out:print', 'erp:stock-out:print-template', " +
                    "'erp:stock-transfer-out:print', 'erp:stock-transfer-out:print-template', " +
                    "'erp:warehouse-move:print', 'erp:warehouse-move:print-template', " +
                    "'erp:stock-check:print', 'erp:stock-check:print-template')";

    @Resource
    private ErpPrintService printService;

    @GetMapping("/fields")
    @Operation(summary = "获取打印字段")
    @PreAuthorize(ANY_TEMPLATE_PERMISSION)
    public CommonResult<ErpPrintFieldRespVO> getFields(@RequestParam("moduleKey") String moduleKey) {
        return success(printService.getFields(moduleKey));
    }

    @GetMapping("/data")
    @Operation(summary = "获取打印数据")
    @PreAuthorize(ANY_PRINT_PERMISSION)
    public CommonResult<Map<String, Object>> getPrintData(@RequestParam("moduleKey") String moduleKey,
                                                          @RequestParam("businessId") Long businessId) {
        return success(printService.getPrintData(moduleKey, businessId));
    }

    @GetMapping("/template/default")
    @Operation(summary = "获取默认打印模板")
    @PreAuthorize(ANY_READ_PERMISSION)
    public CommonResult<ErpPrintTemplateRespVO> getDefaultTemplate(@RequestParam("moduleKey") String moduleKey) {
        return success(printService.getDefaultTemplate(moduleKey));
    }

    @GetMapping("/template/list")
    @Operation(summary = "获取打印模板列表")
    @PreAuthorize(ANY_READ_PERMISSION)
    public CommonResult<List<ErpPrintTemplateRespVO>> getTemplateList(@RequestParam("moduleKey") String moduleKey) {
        return success(printService.getTemplateList(moduleKey));
    }

    @GetMapping("/template/get")
    @Operation(summary = "获取打印模板详情")
    @PreAuthorize(ANY_READ_PERMISSION)
    public CommonResult<ErpPrintTemplateRespVO> getTemplate(@RequestParam("id") Long id) {
        return success(printService.getTemplate(id));
    }

    @PutMapping("/template/save")
    @Operation(summary = "保存打印模板")
    @PreAuthorize(ANY_TEMPLATE_PERMISSION)
    public CommonResult<Long> saveTemplate(@Valid @RequestBody ErpPrintTemplateSaveReqVO reqVO) {
        return success(printService.saveTemplate(reqVO));
    }

    @PostMapping("/template/save-as")
    @Operation(summary = "另存为打印模板")
    @PreAuthorize(ANY_TEMPLATE_PERMISSION)
    public CommonResult<Long> saveAsTemplate(@Valid @RequestBody ErpPrintTemplateSaveReqVO reqVO) {
        return success(printService.saveAsTemplate(reqVO));
    }

    @PostMapping("/template/set-default")
    @Operation(summary = "设置默认打印模板")
    @PreAuthorize(ANY_TEMPLATE_PERMISSION)
    public CommonResult<Boolean> setDefaultTemplate(@RequestParam("id") Long id) {
        printService.setDefaultTemplate(id);
        return success(true);
    }

    @PostMapping("/record")
    @Operation(summary = "记录打印")
    @PreAuthorize(ANY_PRINT_PERMISSION)
    public CommonResult<Boolean> recordPrint(@Valid @RequestBody ErpPrintRecordCreateReqVO reqVO) {
        printService.recordPrint(reqVO);
        return success(true);
    }

}
