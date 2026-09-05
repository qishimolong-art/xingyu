package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - ERP 销售退货可转调拨明细 Response VO")
@Data
public class ErpSaleReturnTransferOutableItemRespVO {

    @Schema(description = "来源销售退货单 ID")
    private Long sourceSaleReturnId;

    @Schema(description = "来源销售退货明细 ID")
    private Long sourceSaleReturnItemId;

    @Schema(description = "来源销售退货单号")
    private String sourceSaleReturnNo;

    private Long productId;
    private String productCode;
    private String productName;
    private Long productUnitId;
    private String productUnitName;
    private BigDecimal weight;
    private Integer packageQty;
    private BigDecimal productPrice;
    private String batchNo;
    private String warehousePosition;
    private String remark;

    private Long fromWarehouseId;
    private String fromWarehouseName;
    private Long fromDeptId;
    private String fromDeptName;

    private BigDecimal returnCount;
    private BigDecimal transferredCount;
    private BigDecimal transferOutableCount;

}
