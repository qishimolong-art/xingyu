package cn.iocoder.yudao.module.erp.controller.admin.cloudprint.vo;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

@Data
public class ErpCloudPrintResumeQueueReqVO {

    @NotNull(message = "云打印设备编号不能为空")
    private Long deviceId;

    @NotBlank(message = "故障任务处理方式不能为空")
    @Pattern(regexp = "RETRY|SKIP", message = "故障任务处理方式必须为 RETRY 或 SKIP")
    private String failedTaskAction;

}
