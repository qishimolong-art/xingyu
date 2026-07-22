package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.quote;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - ERP 报价订单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpSaleQuotePageReqVO extends PageParam {

    @Schema(description = "报价单号", example = "BJ20260509000001")
    private String no;

    @Schema(description = "客户编号", example = "1724")
    private Long customerId;

    @Schema(description = "销售员编号", example = "1888")
    private Long saleUserId;

    private Long deptId;

    @Schema(description = "报价时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] quoteTime;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "备注", example = "客户缺货报价")
    private String remark;

    @Schema(description = "VIN车架号", example = "LGBH52E03HY123456")
    private String vin;

    @Schema(description = "产品编号", example = "1")
    private Long productId;

    @Schema(description = "报价订单编号数组")
    private List<Long> ids;

    @Schema(description = "排序字段，支持：no, quoteTime, status, customerId/customerName, totalProductPrice, discountPrice, totalPrice, saleUserId/saleUserName, deptId/deptName, deliveryAddress, expectedDeliveryTime, remark, internalRemark, creator/creatorName, createTime, updater/updaterName, updateTime", example = "quoteTime")
    private String orderField;

    @Schema(description = "排序方向，仅支持 asc、desc；非法或为空时默认按 id desc", example = "desc")
    private String orderDirection;

}
