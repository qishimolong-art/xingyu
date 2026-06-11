package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move;

import cn.hutool.core.util.ObjectUtil;
import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "Admin - ERP stock move create/update Request VO")
@Data
public class ErpStockMoveSaveReqVO {

    @Schema(description = "Stock move id", example = "11756")
    private Long id;

    @Schema(description = "Customer id", example = "3113")
    private Long customerId;

    @Schema(description = "Department id", example = "100")
    private Long deptId;

    @Schema(description = "Move time", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "Move time cannot be empty")
    private LocalDateTime moveTime;

    @Schema(description = "Source type", example = "30")
    private Integer sourceType;

    @Schema(description = "Source document id", example = "1024")
    private Long sourceId;

    @Schema(description = "Source document no", example = "SC202606100001")
    private String sourceNo;

    @Schema(description = "Remark", example = "remark")
    private String remark;

    @Schema(description = "Attachment URL", example = "https://www.iocoder.cn/1.doc")
    private String fileUrl;

    @Schema(description = "Move items", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "Move items cannot be empty")
    @Valid
    private List<Item> items;

    @Data
    public static class Item {

        @Schema(description = "Move item id", example = "11756")
        private Long id;

        @Schema(description = "From warehouse id", requiredMode = Schema.RequiredMode.REQUIRED, example = "3113")
        @NotNull(message = "From warehouse id cannot be empty")
        private Long fromWarehouseId;

        @Schema(description = "To warehouse id", requiredMode = Schema.RequiredMode.REQUIRED, example = "888")
        @NotNull(message = "To warehouse id cannot be empty")
        private Long toWarehouseId;

        @Schema(description = "Product id", requiredMode = Schema.RequiredMode.REQUIRED, example = "3113")
        @NotNull(message = "Product id cannot be empty")
        private Long productId;

        @Schema(description = "Product price", example = "100.00")
        private BigDecimal productPrice;

        @Schema(description = "Product count", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
        @NotNull(message = "Product count cannot be empty")
        private BigDecimal count;

        @Schema(description = "Remark", example = "remark")
        private String remark;

        @AssertTrue(message = "调出仓库和调入仓库不能相同")
        @JsonIgnore
        public boolean isWarehouseValid() {
            return ObjectUtil.notEqual(fromWarehouseId, toWarehouseId);
        }

    }

}
