package cn.iocoder.yudao.module.erp.controller.admin.common.vo.print;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 打印模板 Response VO")
@Data
public class ErpPrintTemplateRespVO {

    private Long id;
    private String moduleKey;
    private String name;
    private Boolean defaulted;
    private Integer status;
    private String templateJson;
    private String paperConfig;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

}
