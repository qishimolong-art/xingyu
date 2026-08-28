package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 销售单可调价明细 Response VO")
@Data
public class ErpSaleOutItemForAdjustRespVO {

    @Schema(description = "销售单ID")
    private Long saleOutId;

    @Schema(description = "销售单号")
    private String saleOutNo;

    @Schema(description = "客户编号")
    private Long customerId;

    @Schema(description = "出库时间")
    private LocalDateTime outTime;

    @Schema(description = "产品ID")
    private Long productId;

    @Schema(description = "配件编码")
    private String productCode;

    @Schema(description = "配件名称")
    private String productName;

    @Schema(description = "车型")
    private String vehicleModel;

    @Schema(description = "产地")
    private String originPlace;

    @Schema(description = "品牌")
    private String brand;

    @Schema(description = "单位")
    private String unitName;

    @Schema(description = "重量")
    private BigDecimal weight;

    @Schema(description = "包装数")
    private Integer packageQty;

    @Schema(description = "出库数")
    private BigDecimal count;

    @Schema(description = "仓库编号")
    private Long warehouseId;

    @Schema(description = "仓库名称")
    private String warehouseName;

    @Schema(description = "仓库所属部门 ID")
    private Long warehouseDeptId;

    @Schema(description = "仓库所属部门名称")
    private String warehouseDeptName;

    @Schema(description = "批次号", example = "BATCH20260818001")
    private String batchNo;

    @Schema(description = "当前售价")
    private BigDecimal productPrice;

    @Schema(description = "销售出库项ID")
    private Long saleOutItemId;

    @Schema(description = "是否已调价")
    private Boolean adjusted;

    @Schema(description = "所属部门ID")
    private Long deptId;

}
