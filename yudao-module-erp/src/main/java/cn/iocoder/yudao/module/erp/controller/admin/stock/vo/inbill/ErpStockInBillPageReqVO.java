package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.inbill;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - ERP 入仓单报表分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpStockInBillPageReqVO extends PageParam {

    @Schema(description = "入仓单号", example = "RC202606240001")
    private String no;

    @Schema(description = "日期")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] billDate;

    @Schema(description = "仓库编号", example = "1")
    private Long warehouseId;

    @Schema(description = "来源单位", example = "供应商 A")
    private String sourceUnitName;

    @Schema(description = "来源单号", example = "PO202606240001")
    private String sourceNo;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "提货人", example = "张三")
    private String pickupUserName;

    @Schema(description = "创建者")
    private String creator;

}
