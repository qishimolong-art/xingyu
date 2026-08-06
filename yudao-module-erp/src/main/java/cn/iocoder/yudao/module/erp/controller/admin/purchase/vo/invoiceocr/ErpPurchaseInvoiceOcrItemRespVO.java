package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoiceocr;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 采购发票 OCR 明细 Response VO")
@Data
public class ErpPurchaseInvoiceOcrItemRespVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "批次编号", example = "2048")
    private Long batchId;

    @Schema(description = "状态", example = "PENDING")
    private String status;

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "文件 URL")
    private String fileUrl;

    @Schema(description = "文件类型")
    private String fileType;

    @Schema(description = "文件大小")
    private Long fileSize;

    @Schema(description = "发票号码")
    private String invoiceNo;

    @Schema(description = "开票日期")
    private LocalDate invoiceDate;

    @Schema(description = "发票类型")
    private String invoiceType;

    @Schema(description = "发票金额")
    private BigDecimal totalAmount;

    @Schema(description = "不含税金额")
    private BigDecimal taxExclusiveAmount;

    @Schema(description = "税额")
    private BigDecimal taxAmount;

    @Schema(description = "OCR 识别备注")
    private String invoiceRemark;

    @Schema(description = "OCR 原始响应")
    private String rawJson;

    @Schema(description = "OCR 自动解析的厂家单号")
    private String parsedFactoryOrderNo;

    @Schema(description = "最终用于匹配的厂家单号")
    private String factoryOrderNo;

    @Schema(description = "匹配到的采购入库单 ID 快照")
    private String matchedPurchaseInIds;

    @Schema(description = "匹配到的采购入库单号快照")
    private String matchedPurchaseInNos;

    @Schema(description = "匹配时采购入库金额合计快照")
    private BigDecimal purchaseInTotalAmount;

    @Schema(description = "差额")
    private BigDecimal diffAmount;

    @Schema(description = "生成的采购票据编号")
    private Long generatedInvoiceId;

    @Schema(description = "异常信息")
    private String errorMsg;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "更新时间")
    private LocalDateTime updateTime;

}
