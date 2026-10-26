package cn.iocoder.yudao.module.erp.controller.admin.assembly.vo.order;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 套件组装单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ErpAssemblyOrderPageReqVO extends PageParam {

    @Schema(description = "组装单号、成品编号、名称或工厂编号关键词")
    private String keyword;
    private Long warehouseId;
    private Integer status;

    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] assemblyTime;

}
