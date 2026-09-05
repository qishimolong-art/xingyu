package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 销售退货转调拨出库草稿 Request VO")
@Data
public class ErpSaleReturnCreateTransferOutReqVO {

    @NotNull(message = "来源销售退货单不能为空")
    private Long returnId;

    private LocalDateTime moveTime;

    private String remark;

    @Valid
    @NotEmpty(message = "调拨明细不能为空")
    private List<Item> items;

    @Data
    public static class Item {

        @NotNull(message = "来源销售退货明细不能为空")
        private Long sourceSaleReturnItemId;

        @NotNull(message = "调出部门不能为空")
        private Long fromDeptId;

        @NotNull(message = "调出仓库不能为空")
        private Long fromWarehouseId;

        @NotNull(message = "调入部门不能为空")
        private Long toDeptId;

        @NotNull(message = "调入仓库不能为空")
        private Long toWarehouseId;

        @NotNull(message = "调拨数量不能为空")
        private BigDecimal count;

        private String remark;

    }

}
