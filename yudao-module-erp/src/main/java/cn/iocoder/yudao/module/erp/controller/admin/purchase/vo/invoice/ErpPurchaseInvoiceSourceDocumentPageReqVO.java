package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - ERP 采购票据来源单据分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpPurchaseInvoiceSourceDocumentPageReqVO extends PageParam {

    @Schema(description = "来源类型：10 入库、20 调价、30 退货；不传时查询全部来源")
    private Integer sourceType;

    @Schema(description = "供应商编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "供应商不能为空")
    private Long supplierId;

    @Schema(description = "单据号关键字")
    private String keyword;
}
