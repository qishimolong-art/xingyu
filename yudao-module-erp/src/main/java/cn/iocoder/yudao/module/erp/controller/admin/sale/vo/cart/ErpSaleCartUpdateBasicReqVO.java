package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "管理后台 - ERP 销售手推车基础信息更新 Request VO")
@Data
public class ErpSaleCartUpdateBasicReqVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "编号不能为空")
    private Long id;

    @Schema(description = "客户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "2048")
    @NotNull(message = "客户不能为空")
    private Long customerId;

    @Schema(description = "费用", example = "10.00")
    @DecimalMin(value = "0", message = "费用不能小于 0")
    private BigDecimal feeAmount;

    @Schema(description = "备注", example = "修改收货信息")
    private String remark;

}
