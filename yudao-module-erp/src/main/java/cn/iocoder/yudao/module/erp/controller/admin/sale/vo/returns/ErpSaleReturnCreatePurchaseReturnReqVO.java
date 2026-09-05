package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 销售退货转采购退货草稿 Request VO")
@Data
public class ErpSaleReturnCreatePurchaseReturnReqVO {

    @NotNull(message = "来源销售退货单不能为空")
    private Long returnId;

    @NotNull(message = "供应商不能为空")
    private Long supplierId;

    @NotNull(message = "采购退货部门不能为空")
    private Long deptId;

    private LocalDateTime returnTime;

    private String remark;

    @Valid
    @NotEmpty(message = "采购退货明细不能为空")
    private List<Item> items;

    @Data
    public static class Item {

        @NotNull(message = "来源销售退货明细不能为空")
        private Long sourceSaleReturnItemId;

        @NotNull(message = "退货数量不能为空")
        private BigDecimal count;

        private BigDecimal productPrice;

        private String remark;

    }

}
