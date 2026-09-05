package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - ERP 采购票据明细分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpPurchaseInvoiceItemPageReqVO extends PageParam {

    @Schema(description = "采购票据编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "采购票据编号不能为空")
    private Long invoiceId;

    @Schema(description = "是否按字段权限脱敏", example = "true")
    private Boolean mask = true;

    @Schema(description = "排序字段，支持：id, sourceInId, sourceInNo, productId, productCode, productName, count, "
            + "productPrice, taxPercent, taxPrice, totalPrice, remark")
    private String orderField;

    @Schema(description = "排序方向：asc 或 desc")
    private String orderDirection;

}
