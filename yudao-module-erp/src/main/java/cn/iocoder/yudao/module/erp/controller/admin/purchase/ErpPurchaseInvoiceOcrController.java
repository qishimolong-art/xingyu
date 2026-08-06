package cn.iocoder.yudao.module.erp.controller.admin.purchase;

import cn.iocoder.yudao.framework.apilog.core.annotation.ApiAccessLog;
import cn.iocoder.yudao.framework.common.pojo.CommonResult;
import cn.iocoder.yudao.framework.common.pojo.PageResult;
import cn.iocoder.yudao.framework.excel.core.util.ExcelUtils;
import cn.iocoder.yudao.framework.common.util.object.BeanUtils;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoiceocr.ErpPurchaseInvoiceOcrErrorExportRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoiceocr.ErpPurchaseInvoiceOcrBatchPageReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoiceocr.ErpPurchaseInvoiceOcrBatchRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoiceocr.ErpPurchaseInvoiceOcrItemRespVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoiceocr.ErpPurchaseInvoiceOcrUploadReqVO;
import cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoiceocr.ErpPurchaseInvoiceOcrUpdateFactoryOrderNoReqVO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceOcrBatchDO;
import cn.iocoder.yudao.module.erp.dal.dataobject.purchase.ErpPurchaseInvoiceOcrItemDO;
import cn.iocoder.yudao.module.erp.enums.purchase.ErpPurchaseInvoiceOcrItemStatusEnum;
import cn.iocoder.yudao.module.erp.service.purchase.ErpPurchaseInvoiceOcrService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.io.IOException;
import java.util.List;

import static cn.iocoder.yudao.framework.common.pojo.CommonResult.success;
import static cn.iocoder.yudao.framework.apilog.core.enums.OperateTypeEnum.EXPORT;

@Tag(name = "管理后台 - ERP 采购发票 OCR")
@RestController
@RequestMapping("/erp/purchase-invoice-ocr")
@Validated
public class ErpPurchaseInvoiceOcrController {

    @Resource
    private ErpPurchaseInvoiceOcrService purchaseInvoiceOcrService;

    @PostMapping("/upload")
    @Operation(summary = "上传采购发票 OCR 批次")
    @PreAuthorize("@ss.hasPermission('erp:purchase-invoice-ocr:upload')")
    public CommonResult<Long> uploadPurchaseInvoiceOcrBatch(
            @Valid @RequestBody ErpPurchaseInvoiceOcrUploadReqVO uploadReqVO) {
        return success(purchaseInvoiceOcrService.uploadPurchaseInvoiceOcrBatch(uploadReqVO));
    }

    @GetMapping("/page")
    @Operation(summary = "获得采购发票 OCR 批次分页")
    @PreAuthorize("@ss.hasPermission('erp:purchase-invoice-ocr:query')")
    public CommonResult<PageResult<ErpPurchaseInvoiceOcrBatchRespVO>> getPurchaseInvoiceOcrBatchPage(
            @Valid ErpPurchaseInvoiceOcrBatchPageReqVO pageReqVO) {
        PageResult<ErpPurchaseInvoiceOcrBatchDO> pageResult =
                purchaseInvoiceOcrService.getPurchaseInvoiceOcrBatchPage(pageReqVO);
        return success(BeanUtils.toBean(pageResult, ErpPurchaseInvoiceOcrBatchRespVO.class));
    }

    @GetMapping("/{batchId}")
    @Operation(summary = "获得采购发票 OCR 批次详情")
    @Parameter(name = "batchId", description = "批次编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:purchase-invoice-ocr:query')")
    public CommonResult<ErpPurchaseInvoiceOcrBatchRespVO> getPurchaseInvoiceOcrBatch(
            @PathVariable("batchId") Long batchId) {
        ErpPurchaseInvoiceOcrBatchDO batch = purchaseInvoiceOcrService.validatePurchaseInvoiceOcrBatch(batchId);
        List<ErpPurchaseInvoiceOcrItemDO> items =
                purchaseInvoiceOcrService.getPurchaseInvoiceOcrItemListByBatchId(batchId);
        ErpPurchaseInvoiceOcrBatchRespVO respVO = BeanUtils.toBean(batch, ErpPurchaseInvoiceOcrBatchRespVO.class);
        respVO.setItems(BeanUtils.toBean(items, ErpPurchaseInvoiceOcrItemRespVO.class));
        return success(respVO);
    }

    @PostMapping("/{batchId}/recognize")
    @Operation(summary = "识别采购发票 OCR 批次")
    @Parameter(name = "batchId", description = "批次编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:purchase-invoice-ocr:recognize')")
    public CommonResult<Boolean> recognizePurchaseInvoiceOcrBatch(@PathVariable("batchId") Long batchId) {
        return success(purchaseInvoiceOcrService.recognizePurchaseInvoiceOcrBatch(batchId));
    }

    @PostMapping("/{batchId}/match")
    @Operation(summary = "匹配采购发票 OCR 批次")
    @Parameter(name = "batchId", description = "批次编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:purchase-invoice-ocr:match')")
    public CommonResult<Boolean> matchPurchaseInvoiceOcrBatch(@PathVariable("batchId") Long batchId) {
        return success(purchaseInvoiceOcrService.matchPurchaseInvoiceOcrBatch(batchId));
    }

    @PostMapping("/{batchId}/confirm")
    @Operation(summary = "确认生成采购票据")
    @Parameter(name = "batchId", description = "批次编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:purchase-invoice-ocr:confirm')")
    public CommonResult<Boolean> confirmPurchaseInvoiceOcrBatch(@PathVariable("batchId") Long batchId) {
        return success(purchaseInvoiceOcrService.confirmPurchaseInvoiceOcrBatch(batchId));
    }

    @PostMapping("/item/{id}/update-factory-order-no")
    @Operation(summary = "修改采购发票 OCR 明细厂家单号")
    @Parameter(name = "id", description = "明细编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:purchase-invoice-ocr:update-factory-order-no')")
    public CommonResult<Boolean> updatePurchaseInvoiceOcrItemFactoryOrderNo(
            @PathVariable("id") Long id,
            @Valid @RequestBody ErpPurchaseInvoiceOcrUpdateFactoryOrderNoReqVO updateReqVO) {
        return success(purchaseInvoiceOcrService.updatePurchaseInvoiceOcrItemFactoryOrderNo(id, updateReqVO));
    }

    @GetMapping("/{batchId}/export-errors")
    @Operation(summary = "导出采购发票 OCR 异常明细")
    @Parameter(name = "batchId", description = "批次编号", required = true, example = "1024")
    @PreAuthorize("@ss.hasPermission('erp:purchase-invoice-ocr:export')")
    @ApiAccessLog(operateType = EXPORT)
    public void exportPurchaseInvoiceOcrErrors(@PathVariable("batchId") Long batchId,
                                               HttpServletResponse response) throws IOException {
        ErpPurchaseInvoiceOcrBatchDO batch = purchaseInvoiceOcrService.validatePurchaseInvoiceOcrBatch(batchId);
        List<ErpPurchaseInvoiceOcrItemDO> items = purchaseInvoiceOcrService.getPurchaseInvoiceOcrErrorItemList(batchId);
        List<ErpPurchaseInvoiceOcrErrorExportRespVO> rows = BeanUtils.toBean(items,
                ErpPurchaseInvoiceOcrErrorExportRespVO.class, row -> row
                        .setBatchNo(batch.getBatchNo())
                        .setStatusName(getItemStatusName(row.getStatus())));
        ExcelUtils.write(response, "采购发票识别异常.xls", "异常明细",
                ErpPurchaseInvoiceOcrErrorExportRespVO.class, rows);
    }

    private String getItemStatusName(String status) {
        for (ErpPurchaseInvoiceOcrItemStatusEnum statusEnum : ErpPurchaseInvoiceOcrItemStatusEnum.values()) {
            if (statusEnum.getStatus().equals(status)) {
                return statusEnum.getName();
            }
        }
        return status;
    }

}
