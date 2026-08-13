package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - ERP 采购入库分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpPurchaseInPageReqVO extends PageParam {

    public static final Integer PAYMENT_STATUS_NONE = 0;
    public static final Integer PAYMENT_STATUS_PART = 1;
    public static final Integer PAYMENT_STATUS_ALL = 2;

    @Schema(description = "采购单编号", example = "XS001")
    private String no;

    @Schema(description = "厂家单号，模糊匹配", example = "F20260508")
    private String factoryOrderNo;

    @Schema(description = "供应商编号", example = "1724")
    private Long supplierId;

    @Schema(description = "deptId", example = "100")
    private Long deptId;

    @Schema(description = "入库时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] inTime;

    @Schema(description = "备注", example = "你猜")
    private String remark;

    @Schema(description = "采购员")
    private String purchaser;

    @Schema(description = "经手人")
    private String handler;

    @Schema(description = "入库状态", example = "2")
    private Integer status;

    @Schema(description = "创建者")
    private String creator;

    @Schema(description = "产品编号", example = "1")
    private Long productId;

    @Schema(description = "Product keyword for purchase inbound item filtering", example = "P0001")
    private String productKeyword;

    @Schema(description = "仓库编号", example = "1")
    private Long warehouseId;

    @Schema(description = "结算账号编号", example = "1")
    private Long accountId;

    @Schema(description = "付款状态", example = "1")
    private Integer paymentStatus;

    @Schema(description = "是否可付款", example = "true")
    private Boolean paymentEnable; // 对应 paymentStatus = [0, 1]

    @Schema(description = "是否为采购发票选择入库单场景，仅该场景排除已开票入库单", example = "true")
    private Boolean invoiceEnable;

    @Schema(description = "是否排除已开票入库单，兼容采购发票选择弹窗参数", example = "true")
    private Boolean excludeInvoiced;

    @Schema(description = "采购单号", example = "1")
    private String orderNo;

    @Schema(description = "勾选导出的采购入库编号数组", example = "[1,2,3]")
    private List<Long> ids;

    @Schema(description = "Sort field, supports: no, paymentStatus, factoryOrderNo, createTime, status, supplierId, "
            + "supplierName, totalProductPrice, discountPrice, totalPrice, itemCount, totalCount, creator, "
            + "creatorName, purchaser, purchaserName, deptId, deptName, orderMethod, settleMethod, invoiceType, "
            + "orderNo, remark, transportMethod, freightType1, totalFreight1, freightType2, totalFreight2")
    private String orderField;

    @Schema(description = "Sort direction: asc or desc")
    private String orderDirection;

}
