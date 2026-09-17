package cn.iocoder.yudao.module.erp.controller.admin.report.trade;

import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.module.erp.controller.admin.report.trade.ErpTradeReportModels.*;
import cn.iocoder.yudao.module.erp.service.report.trade.ErpTradeReportService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.Map;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@RestController
@RequestMapping("/erp/business-report/purchase-v2")
@Validated
public class ErpPurchaseReportV2Controller {
    @Resource private ErpTradeReportService service;
    @GetMapping("/status")
    @PreAuthorize("@ss.hasPermission('erp:purchase-report-v2:query')")
    public CommonResult<Map<String,Object>> status(){return success(service.status(false));}
    @GetMapping("/item-page")
    @PreAuthorize("@ss.hasPermission('erp:purchase-report-v2:query')")
    public CommonResult<Bundle> items(@Valid Filter filter){return success(service.page(false,filter,false));}
    @GetMapping("/group-page")
    @PreAuthorize("@ss.hasPermission('erp:purchase-report-v2:query')")
    public CommonResult<Bundle> groups(@Valid Filter filter){return success(service.page(false,filter,true));}
    @GetMapping("/summary")
    @PreAuthorize("@ss.hasPermission('erp:purchase-report-v2:query')")
    public CommonResult<Map<String,Object>> summary(@Valid Filter filter){return success(service.summary(false,filter));}
    @GetMapping("/filter-options")
    @PreAuthorize("@ss.hasPermission('erp:purchase-report-v2:query')")
    public CommonResult<PageResult<Map<String,Object>>> options(@Valid Filter filter,@RequestParam String dimension,@RequestParam(required=false) String keyword){return success(service.options(false,filter,dimension,keyword));}
    @cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog(operateType = cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT)
    @GetMapping("/export-excel")
    @PreAuthorize("@ss.hasPermission('erp:purchase-report-v2:query') && @ss.hasPermission('erp:purchase-report-v2:export')")
    public void export(@Valid Filter filter,@RequestParam String view,HttpServletResponse response)throws IOException{service.export(false,filter,view,response);}
}