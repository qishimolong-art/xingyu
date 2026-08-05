package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 销售手推车草稿创建 Request VO")
@Data
public class ErpSaleCartDraftCreateReqVO {

    private Long customerId;
    private Long accountId;
    private Long saleUserId;
    private Long deptId;
    private LocalDateTime cartTime;
    private BigDecimal discountPercent;
    private BigDecimal feeAmount;
    private BigDecimal otherPrice;
    private Integer sourceType;
    private Long sourceId;
    private String sourceNo;
    private String remark;

    private String businessType;
    private String orderType;
    private String billingMethod;
    private String settleMethod;
    private String invoiceType;
    private String deliveryMethod;
    private String freightType;
    private String vin;
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

    /**
     * 草稿允许暂时没有明细；已有明细沿用正式单据的字段结构。
     * 此处不使用 {@code @Valid}，避免关闭新增页时触发正式提交校验。
     */
    private List<ErpSaleCartSaveReqVO.Item> items;

}
