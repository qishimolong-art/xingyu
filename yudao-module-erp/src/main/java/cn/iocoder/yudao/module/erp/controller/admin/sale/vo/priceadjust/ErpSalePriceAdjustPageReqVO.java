package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - ERP 销售调价单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpSalePriceAdjustPageReqVO extends PageParam {

    @Schema(description = "调价单号", example = "XSTJ20240101000001")
    private String no;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "客户编号", example = "1")
    private Long customerId;

    @Schema(description = "部门编号", example = "1")
    private Long deptId;

    @Schema(description = "调价人编号", example = "1")
    private Long adjustUserId;

    @Schema(description = "调价类型", example = "10")
    private Integer adjustType;

    @Schema(description = "产品编号", example = "1")
    private Long productId;

    @Schema(description = "调价时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] adjustDate;

    @Schema(description = "备注", example = "客户议价")
    private String remark;

    @Schema(description = "Only query approved price adjustments with an outstanding receipt amount")
    private Boolean receiptEnable;

    @Schema(description = "调价单编号数组")
    private List<Long> ids;

    @Schema(description = "排序字段", example = "adjustDate")
    private String orderField;

    @Schema(description = "排序方向", example = "desc")
    private String orderDirection;

}
