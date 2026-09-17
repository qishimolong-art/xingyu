package cn.iocoder.yudao.module.erp.controller.admin.sale;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery.*;
import cn.iocoder.yudao.module.erp.service.sale.ErpSalePickDeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;

import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;
import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;

@Tag(name = "管理后台 - ERP 销售拣货单")
@RestController
@RequestMapping("/erp/sale-pick")
@Validated
public class ErpSalePickController {

    @Resource
    private ErpSalePickDeliveryService salePickDeliveryService;

    @GetMapping("/page")
    @Operation(summary = "获得销售拣货单分页")
    @PreAuthorize("@ss.hasPermission('erp:sale-pick:query')")
    public CommonResult<PageResult<ErpSalePickTaskRespVO>> getPickPage(@Valid ErpSalePickPageReqVO pageReqVO) {
        return success(salePickDeliveryService.getPickPage(pageReqVO, false));
    }

    @GetMapping("/get")
    @Operation(summary = "获得销售拣货单详情")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:sale-pick:query')")
    public CommonResult<ErpSalePickTaskRespVO> getPick(@RequestParam("id") Long id,
                                                       @RequestParam(value = "includeDetail", defaultValue = "true") Boolean includeDetail) {
        return success(salePickDeliveryService.getPick(id, false, includeDetail));
    }

    @GetMapping("/item-page")
    @Operation(summary = "获得销售拣货单明细分页")
    @PreAuthorize("@ss.hasPermission('erp:sale-pick:query')")
    public CommonResult<PageResult<ErpSalePickDeliveryItemRespVO>> getPickItemPage(@Valid ErpSalePickItemPageReqVO pageReqVO) {
        return success(salePickDeliveryService.getPickItemPage(pageReqVO.getTaskId(), pageReqVO, false));
    }

    @GetMapping("/submit-page")
    @Operation(summary = "获得销售拣货单提交记录分页")
    @PreAuthorize("@ss.hasPermission('erp:sale-pick:query')")
    public CommonResult<PageResult<ErpSalePickDeliverySubmitRespVO>> getPickSubmitPage(@Valid ErpSalePickSubmitPageReqVO pageReqVO) {
        return success(salePickDeliveryService.getPickSubmitPage(pageReqVO.getTaskId(), pageReqVO, false));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出销售拣货单")
    @PreAuthorize("@ss.hasPermission('erp:sale-pick:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportPickExcel(@Valid ErpSalePickPageReqVO pageReqVO, HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        PageResult<ErpSalePickTaskRespVO> pageResult = salePickDeliveryService.getPickPage(pageReqVO, false);
        ExcelUtils.write(response, "销售拣货单.xls", "数据", ErpSalePickExportRespVO.class,
                BeanUtils.toBean(pageResult.getList(), ErpSalePickExportRespVO.class));
    }

    @GetMapping("/mobile/page")
    @Operation(summary = "移动端获得当前拣货员销售拣货单分页")
    @PreAuthorize("@ss.hasPermission('erp:sale-pick:pick')")
    public CommonResult<PageResult<ErpSalePickTaskRespVO>> getMobilePickPage(@Valid ErpSalePickPageReqVO pageReqVO) {
        return success(salePickDeliveryService.getPickPage(pageReqVO, true));
    }

    @GetMapping("/mobile/get")
    @Operation(summary = "移动端获得当前拣货员销售拣货单详情")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:sale-pick:pick')")
    public CommonResult<ErpSalePickTaskRespVO> getMobilePick(@RequestParam("id") Long id,
                                                             @RequestParam(value = "includeDetail", defaultValue = "true") Boolean includeDetail) {
        return success(salePickDeliveryService.getPick(id, true, includeDetail));
    }

    @GetMapping("/mobile/item-page")
    @Operation(summary = "移动端获得当前拣货员销售拣货单明细分页")
    @PreAuthorize("@ss.hasPermission('erp:sale-pick:pick')")
    public CommonResult<PageResult<ErpSalePickDeliveryItemRespVO>> getMobilePickItemPage(
            @Valid ErpSalePickItemPageReqVO pageReqVO) {
        return success(salePickDeliveryService.getPickItemPage(pageReqVO.getTaskId(), pageReqVO, true));
    }

    @GetMapping("/mobile/submit-page")
    @Operation(summary = "移动端获得当前拣货员销售拣货单提交记录分页")
    @PreAuthorize("@ss.hasPermission('erp:sale-pick:pick')")
    public CommonResult<PageResult<ErpSalePickDeliverySubmitRespVO>> getMobilePickSubmitPage(
            @Valid ErpSalePickSubmitPageReqVO pageReqVO) {
        return success(salePickDeliveryService.getPickSubmitPage(pageReqVO.getTaskId(), pageReqVO, true));
    }

    @PostMapping("/mobile/submit")
    @Operation(summary = "移动端提交销售拣货")
    @PreAuthorize("@ss.hasPermission('erp:sale-pick:pick')")
    public CommonResult<Boolean> submitPick(@Valid @RequestBody ErpSalePickSubmitReqVO reqVO) {
        salePickDeliveryService.submitPick(reqVO);
        return success(true);
    }

    @PostMapping("/mobile/upload-voucher")
    @Operation(summary = "移动端上传销售拣货凭证")
    @PreAuthorize("@ss.hasPermission('erp:sale-pick:pick')")
    public CommonResult<String> uploadPickVoucher(@RequestParam("file") MultipartFile file) throws IOException {
        return success(salePickDeliveryService.uploadVoucher(file.getBytes(), file.getOriginalFilename()));
    }

}
