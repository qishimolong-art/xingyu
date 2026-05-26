package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - ERP 采购调价单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpPurchasePriceAdjustPageReqVO extends PageParam {

    @Schema(description = "调价单号", example = "CGTJ2026050801")
    private String no;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "调价类型", example = "10")
    private Integer adjustType;

    @Schema(description = "供应商编号", example = "1")
    private Long supplierId;

    @Schema(description = "调价日期区间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] adjustTime;

    @Schema(description = "备注", example = "单价填错纠正")
    private String remark;

    @Schema(description = "创建人")
    private String creator;

    @Schema(description = "部门编号")
    private Long deptId;

    @Schema(description = "调价人（系统用户 ID）")
    private Long adjuster;

}
