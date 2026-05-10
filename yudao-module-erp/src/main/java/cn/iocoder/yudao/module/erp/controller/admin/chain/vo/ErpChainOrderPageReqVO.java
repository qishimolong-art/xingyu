package cn.iocoder.yudao.module.erp.controller.admin.chain.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - ERP 连锁开单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpChainOrderPageReqVO extends PageParam {

    @Schema(description = "连锁开单号", example = "LSKD20240101000001")
    private String no;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "总公司租户ID", example = "1")
    private Long hqTenantId;

    @Schema(description = "分公司租户ID", example = "2")
    private Long branchTenantId;

    @Schema(description = "分公司类型", example = "1")
    private Integer branchType;

    @Schema(description = "下单时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] orderTime;

}
