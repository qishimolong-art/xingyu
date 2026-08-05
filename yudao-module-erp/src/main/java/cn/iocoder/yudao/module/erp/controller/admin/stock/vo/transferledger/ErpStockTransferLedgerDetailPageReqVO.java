package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.transferledger;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import javax.validation.constraints.NotNull;
import java.time.LocalDate;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY;

@Schema(description = "管理后台 - ERP 调拨出入库台账明细分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpStockTransferLedgerDetailPageReqVO extends ErpStockTransferLedgerPageReqVO {

    @Schema(description = "钻取业务日期", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "钻取业务日期不能为空")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY)
    private LocalDate businessDate;

}
