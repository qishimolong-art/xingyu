package cn.iocoder.yudao.module.erp.service.cloudprint;

import lombok.Data;

@Data
public class ErpCloudPrintCallbackRequest {

    private String method;
    private String devid;
    private String reqid;
    private Integer code;
    private String message;

}
