package cn.iocoder.yudao.module.erp.controller.admin.base.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - ERP 基础数据分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpBaseDataPageReqVO extends PageParam {

    @Schema(description = "数据类型", example = "region")
    private String type;

    @Schema(description = "名称", example = "华东")
    private String name;

    @Schema(description = "状态", example = "0")
    private Integer status;

}
