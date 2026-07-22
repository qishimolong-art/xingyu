package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - ERP 采购调价单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpPurchasePriceAdjustPageReqVO extends PageParam {

    public static final Integer PAYMENT_STATUS_NONE = 0;
    public static final Integer PAYMENT_STATUS_PART = 1;
    public static final Integer PAYMENT_STATUS_ALL = 2;

    @Schema(description = "调价单号", example = "CGTJ2026050801")
    private String no;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "调价类型", example = "10")
    private Integer adjustType;

    @Schema(description = "供应商编号", example = "1")
    private Long supplierId;

    @Schema(description = "产品编号", example = "1")
    private Long productId;

    @Schema(description = "调价日期区间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] adjustTime;

    @Schema(description = "备注", example = "单价填错纠正")
    private String remark;

    @Schema(description = "付款状态", example = "1")
    private Integer paymentStatus;

    @Schema(description = "是否可付款", example = "true")
    private Boolean paymentEnable;

    @Schema(description = "创建人")
    private String creator;

    @Schema(description = "部门编号")
    private Long deptId;

    @Schema(description = "调价人（系统用户 ID）")
    private Long adjuster;

    @Schema(description = "勾选导出的采购调价单编号数组", example = "[1,2,3]")
    private List<Long> ids;

    @Schema(description = "Sort field, supports: no, status, adjustTime, supplierName, originalTotalPrice, "
            + "adjustedTotalPrice, totalAdjustPrice, adjusterName, approverName, approveTime, remark")
    private String orderField;

    @Schema(description = "Sort direction: asc or desc")
    private String orderDirection;

}
