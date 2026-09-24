package cn.iocoder.yudao.module.erp.controller.admin.cloudprint.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class ErpCloudPrintTaskRespVO {

    private Long id;
    private String reqid;
    private String bizType;
    private Long bizId;
    private String bizNo;
    private Long warehouseId;
    private String warehouseName;
    private String devid;
    private Long deviceId;
    private Long templateId;
    private Integer status;
    private String errorMsg;
    private Integer callbackCode;
    private String callbackMsg;
    private LocalDateTime submitTime;
    private LocalDateTime callbackTime;

}
