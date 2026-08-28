package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.out;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 销售单可退明细 Response VO")
@Data
public class ErpSaleReturnableItemRespVO {

    @Schema(description = "来源销售单 ID", example = "17386")
    private Long sourceOutId;
    @Schema(description = "来源销售单项 ID", example = "11756")
    private Long sourceOutItemId;
    @Schema(description = "来源销售单号", example = "XSCK20260510001")
    private String sourceOutNo;
    @Schema(description = "客户编号", example = "1724")
    private Long customerId;

    @Schema(description = "产品编号", example = "10001")
    private Long productId;
    private String productCode;
    private String productName;
    private String productBarCode;
    private String productUnitName;
    @Schema(description = "产品单位编号", example = "1")
    private Long productUnitId;
    @Schema(description = "重量")
    private BigDecimal weight;
    @Schema(description = "包装数")
    private Integer packageQty;
    @Schema(description = "仓库编号", example = "2")
    private Long warehouseId;
    private String warehouseName;
    private Long warehouseDeptId;
    private String warehouseDeptName;
    private Long deptId;
    private String deptName;
    @Schema(description = "批次号", example = "BATCH20260818001")
    private String batchNo;

    @Schema(description = "原销售单价", example = "12.34")
    private BigDecimal productPrice;
    @Schema(description = "原销售数量", example = "100")
    private BigDecimal outCount;
    @Schema(description = "已累计退货数量", example = "20")
    private BigDecimal returnedCount;
    @Schema(description = "可退数量 = outCount - returnedCount", example = "80")
    private BigDecimal returnableCount;

    @Schema(description = "税率", example = "13.00")
    private BigDecimal taxPercent;
    @Schema(description = "备注")
    private String remark;

}
