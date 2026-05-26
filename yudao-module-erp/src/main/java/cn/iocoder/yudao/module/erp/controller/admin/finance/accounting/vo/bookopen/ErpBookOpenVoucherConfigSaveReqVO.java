package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.bookopen;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - ERP 开账凭证类型勾选批量更新 Request VO")
@Data
public class ErpBookOpenVoucherConfigSaveReqVO {

    @Schema(description = "关联开账主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "关联开账主键不能为空")
    private Long bookOpenId;

    @Schema(description = "凭证类型勾选项列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "凭证类型勾选项不能为空")
    @Valid
    private List<Item> items;

    @Schema(description = "管理后台 - ERP 开账凭证类型勾选项")
    @Data
    public static class Item {

        @Schema(description = "凭证业务类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
        @NotNull(message = "凭证业务类型不能为空")
        private Integer voucherType;

        @Schema(description = "是否启用生成", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
        @NotNull(message = "是否启用不能为空")
        private Boolean enabled;

        @Schema(description = "排序", example = "1")
        private Integer sort;

    }

}
