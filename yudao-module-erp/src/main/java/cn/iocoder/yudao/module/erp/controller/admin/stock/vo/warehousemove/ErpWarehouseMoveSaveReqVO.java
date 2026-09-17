package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove;

import cn.hutool.core.util.ObjectUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Admin - ERP warehouse move create/update Request VO")
@Data
public class ErpWarehouseMoveSaveReqVO {

    @Schema(description = "移货单 ID")
    private Long id;

    @Schema(description = "所属部门")
    private Long deptId;

    @Schema(description = "移货日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "移货日期不能为空")
    private LocalDateTime moveTime;

    @Schema(description = "移出仓库 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "移出仓库不能为空")
    private Long fromWarehouseId;

    @Schema(description = "移入仓库 ID", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "移入仓库不能为空")
    private Long toWarehouseId;

    @Schema(description = "经办人")
    private Long handlerId;

    @Schema(description = "来源类型")
    private Integer sourceType;

    @Schema(description = "来源单据 ID")
    private Long sourceId;

    @Schema(description = "来源单号")
    private String sourceNo;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "附件 URL")
    private String fileUrl;

    @Schema(description = "移货明细", requiredMode = Schema.RequiredMode.REQUIRED)
    @Valid
    private List<Item> items;

    @AssertTrue(message = "移出仓库和移入仓库不能相同")
    @JsonIgnore
    public boolean isWarehouseValid() {
        return ObjectUtil.notEqual(fromWarehouseId, toWarehouseId);
    }

    @Data
    public static class Item {

        @Schema(description = "移货明细 ID")
        private Long id;

        @Schema(description = "明细操作类型：insert 新增，update 修改，delete 删除")
        private String operation;

        @Schema(description = "产品 ID", requiredMode = Schema.RequiredMode.REQUIRED)
        private Long productId;

        @Schema(description = "移货单价")
        private BigDecimal productPrice;

        @Schema(description = "移货数量", requiredMode = Schema.RequiredMode.REQUIRED)
        private BigDecimal count;

        @Schema(description = "移出货架")
        private String fromShelf;

        @Schema(description = "移入货架")
        private String toShelf;

        @Schema(description = "成本单价")
        private BigDecimal costPrice;

        @Schema(description = "包装数")
        private Integer packageQty;

        @Schema(description = "单重")
        private BigDecimal weight;

        @Schema(description = "批次号")
        private String batchNo;

        @Schema(description = "备注")
        private String remark;

    }

}
