package cn.iocoder.yudao.module.erp.controller.admin.base.vo.recyclebin;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - ERP 基础档案回收站分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpRecycleBinPageReqVO extends PageParam {

    @Schema(description = "来源类型", example = "supplier")
    private String sourceType;

    @Schema(description = "名称", example = "广州供应商")
    private String name;

    @Schema(description = "编码", example = "GYS000001")
    private String code;

}
