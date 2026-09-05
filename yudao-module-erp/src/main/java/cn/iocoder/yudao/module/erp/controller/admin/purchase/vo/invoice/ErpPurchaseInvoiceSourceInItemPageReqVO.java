package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "管理后台 - ERP 采购票据来源入库明细分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpPurchaseInvoiceSourceInItemPageReqVO extends PageParam {

    @Schema(description = "来源采购入库编号列表", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotEmpty(message = "来源采购入库编号不能为空")
    private List<Long> sourceInIds;

    @Schema(description = "是否按字段权限脱敏", example = "true")
    private Boolean mask = true;

    @Schema(description = "排序字段，支持：id, sourceInId, sourceInNo, productId, productCode, productName, count, "
            + "productPrice, totalPrice, remark")
    private String orderField;

    @Schema(description = "排序方向：asc 或 desc")
    private String orderDirection;

}
