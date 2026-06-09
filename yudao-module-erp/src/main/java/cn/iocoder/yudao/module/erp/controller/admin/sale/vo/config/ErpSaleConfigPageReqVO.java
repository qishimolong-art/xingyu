package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.config;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - ERP 销售配置分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpSaleConfigPageReqVO extends PageParam {

    @Schema(description = "配置类型", example = "CONTRACT_TYPE")
    private String configType;

    @Schema(description = "配置编码", example = "annual")
    private String code;

    @Schema(description = "配置名称", example = "年度合同")
    private String name;

    @Schema(description = "状态", example = "0")
    private Integer status;

    @Schema(description = "部门编号", example = "1")
    private Long deptId;

}
