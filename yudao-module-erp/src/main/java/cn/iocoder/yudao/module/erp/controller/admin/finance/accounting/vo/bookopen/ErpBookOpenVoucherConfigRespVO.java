package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.bookopen;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - ERP 开账凭证类型勾选 Response VO")
@Data
public class ErpBookOpenVoucherConfigRespVO {

    @Schema(description = "主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "关联开账主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    private Long bookOpenId;

    @Schema(description = "凭证业务类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer voucherType;

    @Schema(description = "凭证类型名称（前端展示用）", example = "销售凭证")
    private String voucherTypeName;

    @Schema(description = "是否启用生成", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean enabled;

    @Schema(description = "排序", example = "1")
    private Integer sort;

}
