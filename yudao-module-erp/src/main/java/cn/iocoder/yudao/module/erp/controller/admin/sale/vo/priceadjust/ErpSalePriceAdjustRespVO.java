package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 销售调价单 Response VO")
@Data
public class ErpSalePriceAdjustRespVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "调价单号")
    private String no;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "日期")
    private LocalDateTime adjustDate;

    @Schema(description = "客户编号")
    private Long customerId;

    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "部门")
    private String dept;

    @Schema(description = "调价人")
    private String adjustUser;

    @Schema(description = "调价类型")
    private String adjustType;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "调价总金额")
    private BigDecimal totalAdjustPrice;

    @Schema(description = "创建人")
    private String creator;

    @Schema(description = "创建人名称")
    private String creatorName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "调价明细列表")
    private List<Item> items;

    @Data
    public static class Item {

        @Schema(description = "编号")
        private Long id;

        @Schema(description = "销售单号")
        private String saleOutNo;

        @Schema(description = "配件编码")
        private String partCode;

        @Schema(description = "配件名称")
        private String partName;

        @Schema(description = "车型")
        private String vehicleModel;

        @Schema(description = "产地")
        private String originPlace;

        @Schema(description = "品牌")
        private String brand;

        @Schema(description = "单位")
        private String unit;

        @Schema(description = "出库数")
        private BigDecimal outCount;

        @Schema(description = "原售价")
        private BigDecimal oldPrice;

        @Schema(description = "调后价")
        private BigDecimal newPrice;

        @Schema(description = "调价金额")
        private BigDecimal adjustPrice;

        @Schema(description = "产品ID")
        private Long productId;

        @Schema(description = "关联销售出库项ID")
        private Long saleOutItemId;

    }

}
