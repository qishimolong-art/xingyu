package cn.iocoder.yudao.module.erp.controller.admin.cloudprint.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@Data
@Accessors(chain = true)
public class ErpCloudPrintDeviceRespVO {

    private Long id;
    private String devid;
    private String nickname;
    private Integer devType;
    private Integer contentType;
    private Integer printWidth;
    private Integer printHeight;
    private Integer paperType;
    private Integer rotate;
    private Integer copies;
    private Long deptId;
    private String deptName;
    private Boolean defaulted;
    private Integer status;
    private Integer onlineState;
    private Integer lastStatusCode;
    private LocalDateTime lastStatusTime;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

}
