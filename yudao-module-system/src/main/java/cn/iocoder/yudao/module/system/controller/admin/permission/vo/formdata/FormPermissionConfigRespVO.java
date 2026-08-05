package cn.iocoder.yudao.module.system.controller.admin.permission.vo.formdata;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 表单数据权限配置 Response VO")
@Data
public class FormPermissionConfigRespVO {

    @Schema(description = "编号", example = "1024")
    private Long id;

    @Schema(description = "表名", requiredMode = Schema.RequiredMode.REQUIRED, example = "erp_purchase_order")
    private String formType;

    @Schema(description = "表说明", example = "ERP 采购订单")
    private String tableDesc;

    @Schema(description = "是否启用", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean enabled;

    @Schema(description = "字段数量", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer fieldCount;

    @Schema(description = "字段配置")
    private List<FormPermissionFieldConfigRespVO> fields;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

}
