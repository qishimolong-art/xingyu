package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

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

    @Schema(description = "调价时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] adjustDate;

}
