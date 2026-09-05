package cn.iocoder.yudao.module.erp.controller.admin.report.vo.system;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpSystemReportReqVO extends PageParam {

    private String periodType;
    private String grain;
    private String dimension;
    private String metric;
    private String keyword;
    private String orderField;
    private String orderDirection;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime startTime;
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime endTime;

    private Long supplierId;
    private String purchaser;
    private Long customerId;
    private Long saleUserId;
    private Long deptId;
    private Long warehouseId;
    private Long categoryId;
    private String stockStatus;

}
