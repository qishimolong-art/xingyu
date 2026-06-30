package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.outbill;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Schema(description = "管理后台 - ERP 出仓单明细 Response VO")
@Data
public class ErpStockOutBillItemRespVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "出仓单编号")
    private Long billId;

    @Schema(description = "来源单据编号")
    private Long sourceId;

    @Schema(description = "来源单据明细编号")
    private Long sourceItemId;

    @Schema(description = "来源单号")
    private String sourceNo;

    @Schema(description = "仓库编号")
    private Long warehouseId;

    @Schema(description = "仓库名称")
    private String warehouseName;

    @Schema(description = "产品编号")
    private Long productId;

    @Schema(description = "产品编码")
    private String productCode;

    @Schema(description = "产品名称")
    private String productName;

    @Schema(description = "产品单位编号")
    private Long productUnitId;

    @Schema(description = "产品单位名称")
    private String productUnitName;

    @Schema(description = "产品单价")
    private BigDecimal productPrice;

    @Schema(description = "应拣数量")
    private BigDecimal count;

    @Schema(description = "已拣数量")
    private BigDecimal pickedCount;

    @Schema(description = "剩余待拣数量")
    private BigDecimal remainCount;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "包装数")
    private Integer packageQty;

    @Schema(description = "整件数")
    private Integer wholeQty;

    @Schema(description = "货架位")
    private String warehousePosition;

    @Schema(description = "图号")
    private String drawingNo;

    @Schema(description = "批次")
    private String batchNo;

    @Schema(description = "条形码")
    private String barCode;

    @Schema(description = "品牌")
    private String brand;

    @Schema(description = "适用车型")
    private String vehicleModel;

    @Schema(description = "产地")
    private String originPlace;

    @Schema(description = "备注")
    private String remark;

}
