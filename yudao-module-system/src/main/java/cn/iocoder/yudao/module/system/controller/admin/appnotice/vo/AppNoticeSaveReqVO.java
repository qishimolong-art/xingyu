package cn.iocoder.yudao.module.system.controller.admin.appnotice.vo;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - 小程序公告创建/修改 Request VO")
@Data
public class AppNoticeSaveReqVO {

    @Schema(description = "公告编号", example = "1024")
    private Long id;

    @Schema(description = "公告标题", requiredMode = Schema.RequiredMode.REQUIRED, example = "放假通知")
    @NotBlank(message = "公告标题不能为空")
    @Size(max = 100, message = "公告标题不能超过100个字符")
    private String title;

    @Schema(description = "公告内容", requiredMode = Schema.RequiredMode.REQUIRED, example = "公告内容")
    @NotBlank(message = "公告内容不能为空")
    private String content;

    @Schema(description = "状态，参见 CommonStatusEnum 枚举类", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    @NotNull(message = "公告状态不能为空")
    @InEnum(value = CommonStatusEnum.class, message = "公告状态必须是 {value}")
    private Integer status;

    @Schema(description = "排序，数字越大越靠前", example = "10")
    private Integer sort;

    @Schema(description = "展示开始时间")
    private LocalDateTime startTime;

    @Schema(description = "展示结束时间")
    private LocalDateTime endTime;

    @AssertTrue(message = "展示结束时间不能早于展示开始时间")
    public boolean isEndTimeValid() {
        return startTime == null || endTime == null || !endTime.isBefore(startTime);
    }

}
