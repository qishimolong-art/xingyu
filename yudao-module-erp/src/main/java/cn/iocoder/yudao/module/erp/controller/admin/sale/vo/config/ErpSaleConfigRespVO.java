package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.config;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 销售配置 Response VO")
@Data
public class ErpSaleConfigRespVO {

    private Long id;
    private String configType;
    private String code;
    private String name;
    private String configValue;
    private Integer status;
    private Long deptId;
    private String deptName;
    private Integer sort;
    private String remark;
    private LocalDateTime createTime;

}
