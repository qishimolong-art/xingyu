package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.history;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - 采购历史分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ErpPurchaseHistoryPageReqVO extends PageParam {

    @Schema(description = "产品编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @NotNull(message = "产品编号不能为空")
    @Positive(message = "产品编号必须大于 0")
    private Long productId;

    @Schema(description = "供应商编号", example = "2048")
    private Long supplierId;

    @Schema(description = "单据类型，可选 PURCHASE_IN、PURCHASE_RETURN", example = "PURCHASE_IN")
    private String documentTypeCode;

    @Schema(description = "业务日期范围")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] businessTime;

    @Schema(description = "排序字段", example = "businessTime")
    private String orderField;

    @Schema(description = "排序方向", example = "desc")
    private String orderDirection;

    public LocalDateTime getBusinessTimeFrom() {
        return businessTime == null || businessTime.length == 0 ? null : businessTime[0];
    }

    public LocalDateTime getBusinessTimeTo() {
        return businessTime == null || businessTime.length < 2 ? null : businessTime[1];
    }
}
