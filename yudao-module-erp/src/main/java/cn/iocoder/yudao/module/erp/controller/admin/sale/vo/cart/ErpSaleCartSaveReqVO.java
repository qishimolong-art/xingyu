package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 销售手推车新增/修改 Request VO")
@Data
public class ErpSaleCartSaveReqVO {

    private Long id;

    @NotNull(message = "客户编号不能为空")
    private Long customerId;

    private Long accountId;

    private Long saleUserId;

    private Long deptId;

    @Schema(description = "开单时间，由系统自动生成")
    private LocalDateTime cartTime;

    private BigDecimal discountPercent;

    private BigDecimal otherPrice;

    private String fileUrl;

    private String remark;

    // ========== 扩展字段 ==========
    private String businessType;
    private String orderType;
    private String billingMethod;
    private String settleMethod;
    private String invoiceType;
    private String deliveryMethod;
    private String freightType;
    private String priority;
    private String priceType;
    private String logisticsCompany;
    private Long developerUserId;
    private String contactPerson;
    private String contactPhone;
    private String deliveryAddress;
    private LocalDateTime deliveryDate;
    private BigDecimal taxRate;
    private BigDecimal totalFreight;
    private LocalDateTime paymentDate;
    private String businessEntity;
    private String orderMethod;
    private String sourceType2;
    private String remark2;

    @Valid
    @NotEmpty(message = "手推车明细不能为空")
    private List<Item> items;

    @Data
    public static class Item {

        private Long id;

        @NotNull(message = "仓库编号不能为空")
        private Long warehouseId;

        @NotNull(message = "产品编号不能为空")
        private Long productId;

        private BigDecimal productPrice;

        @NotNull(message = "产品数量不能为空")
        private BigDecimal count;

        private BigDecimal taxPercent;

        private String warehousePosition;
        private String drawingNo;
        private String batchNo;
        private String barCode;
        private String brand;
        private String vehicleModel;
        private String originPlace;
        private String standard;
        private String remark;
    }

}
