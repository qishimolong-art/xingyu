package cn.iocoder.yudao.module.system.controller.admin.permission.vo.formdata;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@Schema(description = "管理后台 - 表单数据权限配置分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class FormPermissionPageReqVO extends PageParam {

    @Schema(description = "表名", example = "erp_purchase_order")
    private String formType;

    @Schema(description = "启用状态")
    private Boolean enabled;

}
