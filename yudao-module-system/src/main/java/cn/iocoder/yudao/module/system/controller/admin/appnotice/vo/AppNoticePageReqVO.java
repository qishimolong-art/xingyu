package cn.iocoder.yudao.module.system.controller.admin.appnotice.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 小程序公告分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class AppNoticePageReqVO extends PageParam {

    @Schema(description = "公告标题，模糊匹配", example = "放假通知")
    private String title;

    @Schema(description = "状态，参见 CommonStatusEnum 枚举类", example = "0")
    private Integer status;

    @Schema(description = "创建时间", example = "[2026-08-01 00:00:00, 2026-08-31 23:59:59]")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
