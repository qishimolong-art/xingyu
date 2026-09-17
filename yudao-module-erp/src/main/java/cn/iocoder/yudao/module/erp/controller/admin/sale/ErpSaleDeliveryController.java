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

@Tag(name = "管理后台 - ERP 销售送货单")
@RestController
@RequestMapping("/erp/sale-delivery")
@Validated
public class ErpSaleDeliveryController {

    @Resource
    private ErpSalePickDeliveryService salePickDeliveryService;

    @GetMapping("/page")
    @Operation(summary = "获得销售送货单分页")
    @PreAuthorize("@ss.hasPermission('erp:sale-delivery:query')")
    public CommonResult<PageResult<ErpSaleDeliveryOrderRespVO>> getDeliveryPage(@Valid ErpSaleDeliveryPageReqVO pageReqVO) {
        return success(salePickDeliveryService.getDeliveryPage(pageReqVO, false));
    }

    @GetMapping("/get")
    @Operation(summary = "获得销售送货单详情")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:sale-delivery:query')")
    public CommonResult<ErpSaleDeliveryOrderRespVO> getDelivery(@RequestParam("id") Long id,
                                                                @RequestParam(value = "includeDetail", defaultValue = "true") Boolean includeDetail) {
        return success(salePickDeliveryService.getDelivery(id, false, includeDetail));
    }

    @GetMapping("/item-page")
    @Operation(summary = "获得销售送货单明细分页")
    @PreAuthorize("@ss.hasPermission('erp:sale-delivery:query')")
    public CommonResult<PageResult<ErpSalePickDeliveryItemRespVO>> getDeliveryItemPage(
            @Valid ErpSaleDeliveryItemPageReqVO pageReqVO) {
        return success(salePickDeliveryService.getDeliveryItemPage(pageReqVO.getOrderId(), pageReqVO, false));
    }

    @GetMapping("/submit-page")
    @Operation(summary = "获得销售送货单提交记录分页")
    @PreAuthorize("@ss.hasPermission('erp:sale-delivery:query')")
    public CommonResult<PageResult<ErpSalePickDeliverySubmitRespVO>> getDeliverySubmitPage(
            @Valid ErpSaleDeliverySubmitPageReqVO pageReqVO) {
        return success(salePickDeliveryService.getDeliverySubmitPage(pageReqVO.getOrderId(), pageReqVO, false));
    }

    @GetMapping("/export-excel")
    @Operation(summary = "导出销售送货单")
    @PreAuthorize("@ss.hasPermission('erp:sale-delivery:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportDeliveryExcel(@Valid ErpSaleDeliveryPageReqVO pageReqVO,
                                    HttpServletResponse response) throws IOException {
        pageReqVO.setPageSize(PageParam.PAGE_SIZE_NONE);
        PageResult<ErpSaleDeliveryOrderRespVO> pageResult = salePickDeliveryService.getDeliveryPage(pageReqVO, false);
        ExcelUtils.write(response, "销售送货单.xls", "数据", ErpSaleDeliveryExportRespVO.class,
                BeanUtils.toBean(pageResult.getList(), ErpSaleDeliveryExportRespVO.class));
    }

    @GetMapping("/mobile/page")
    @Operation(summary = "移动端获得销售送货单分页")
    @PreAuthorize("@ss.hasPermission('erp:sale-delivery:delivery')")
    public CommonResult<PageResult<ErpSaleDeliveryOrderRespVO>> getMobileDeliveryPage(
            @Valid ErpSaleDeliveryPageReqVO pageReqVO) {
        return success(salePickDeliveryService.getDeliveryPage(pageReqVO, true));
    }

    @GetMapping("/mobile/get")
    @Operation(summary = "移动端获得销售送货单详情")
    @Parameter(name = "id", description = "编号", required = true)
    @PreAuthorize("@ss.hasPermission('erp:sale-delivery:delivery')")
    public CommonResult<ErpSaleDeliveryOrderRespVO> getMobileDelivery(@RequestParam("id") Long id,
                                                                      @RequestParam(value = "includeDetail", defaultValue = "true") Boolean includeDetail) {
        return success(salePickDeliveryService.getDelivery(id, true, includeDetail));
    }

    @GetMapping("/mobile/item-page")
    @Operation(summary = "移动端获得销售送货单明细分页")
    @PreAuthorize("@ss.hasPermission('erp:sale-delivery:delivery')")
    public CommonResult<PageResult<ErpSalePickDeliveryItemRespVO>> getMobileDeliveryItemPage(
            @Valid ErpSaleDeliveryItemPageReqVO pageReqVO) {
        return success(salePickDeliveryService.getDeliveryItemPage(pageReqVO.getOrderId(), pageReqVO, true));
    }

    @GetMapping("/mobile/submit-page")
    @Operation(summary = "移动端获得销售送货单提交记录分页")
    @PreAuthorize("@ss.hasPermission('erp:sale-delivery:delivery')")
    public CommonResult<PageResult<ErpSalePickDeliverySubmitRespVO>> getMobileDeliverySubmitPage(
            @Valid ErpSaleDeliverySubmitPageReqVO pageReqVO) {
        return success(salePickDeliveryService.getDeliverySubmitPage(pageReqVO.getOrderId(), pageReqVO, true));
    }

    @PostMapping("/mobile/submit")
    @Operation(summary = "移动端提交销售送货")
    @PreAuthorize("@ss.hasPermission('erp:sale-delivery:delivery')")
    public CommonResult<Boolean> submitDelivery(@Valid @RequestBody ErpSaleDeliverySubmitReqVO reqVO) {
        salePickDeliveryService.submitDelivery(reqVO);
        return success(true);
    }

    @PostMapping("/mobile/upload-voucher")
    @Operation(summary = "移动端上传销售送货凭证")
    @PreAuthorize("@ss.hasPermission('erp:sale-delivery:delivery')")
    public CommonResult<String> uploadDeliveryVoucher(@RequestParam("file") MultipartFile file) throws IOException {
        return success(salePickDeliveryService.uploadVoucher(file.getBytes(), file.getOriginalFilename()));
    }

}
