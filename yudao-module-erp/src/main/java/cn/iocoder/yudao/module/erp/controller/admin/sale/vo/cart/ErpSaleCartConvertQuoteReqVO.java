package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - ERP 销售手推车部分转报价请求 VO")
@Data
public class ErpSaleCartConvertQuoteReqVO {

    @Schema(description = "手推车编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "手推车编号不能为空")
    private Long cartId;

    @Schema(description = "转出的手推车商品行编号列表（已废弃，后端整单转换忽略此字段）")
    private List<Long> cartItemIds;
}
