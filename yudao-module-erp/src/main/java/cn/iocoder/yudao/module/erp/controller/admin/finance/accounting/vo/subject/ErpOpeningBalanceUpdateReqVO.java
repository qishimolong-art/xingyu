package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.subject;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - ERP 会计科目期初余额批量保存 Request VO")
@Data
public class ErpOpeningBalanceUpdateReqVO {

    @Schema(description = "期初余额列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "期初余额列表不能为空")
    @Valid
    private List<Item> items;

    @Schema(description = "期初余额单项")
    @Data
    public static class Item {

        @Schema(description = "会计科目编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
        @NotNull(message = "会计科目编号不能为空")
        private Long id;

        @Schema(description = "期初余额", example = "1000.00")
        private BigDecimal openingBalance;

    }

}
