package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoiceocr;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 采购发票 OCR 批次 Response VO")
@Data
public class ErpPurchaseInvoiceOcrBatchRespVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "批次号", example = "PIOCR2026080500001024")
    private String batchNo;

    @Schema(description = "状态", example = "DRAFT")
    private String status;

    @Schema(description = "文件数量")
    private Integer fileCount;

    @Schema(description = "已生成采购票据数量")
    private Integer generatedInvoiceCount;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "异常信息")
    private String errorMsg;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

    @Schema(description = "发票明细")
    private List<ErpPurchaseInvoiceOcrItemRespVO> items;

}
