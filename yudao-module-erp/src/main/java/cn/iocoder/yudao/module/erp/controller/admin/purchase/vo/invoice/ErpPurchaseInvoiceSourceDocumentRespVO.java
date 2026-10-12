package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 采购票据可选来源单据 Response VO")
@Data
public class ErpPurchaseInvoiceSourceDocumentRespVO {

    private Integer sourceType;
    private String sourceTypeName;
    private Long id;
    private String no;
    private Long supplierId;
    private Long deptId;
    private LocalDateTime bizTime;
    private BigDecimal totalCount;
    private BigDecimal totalAmount;
    private BigDecimal allocatedCount;
    private BigDecimal remainingCount;
    private String invoiceStatus;
}
