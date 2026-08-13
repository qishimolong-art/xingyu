package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.in;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

@Schema(description = "管理后台 - ERP 其它入库明细批量修改仓库 Request VO")
@Data
public class ErpStockInItemBatchUpdateReqVO {

    @Schema(description = "其它入库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "17386")
    @NotNull(message = "其它入库编号不能为空")
    private Long inId;

    @Schema(description = "其它入库明细编号列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "其它入库明细不能为空")
    private List<Long> itemIds;

    @Schema(description = "目标仓库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "目标仓库不能为空")
    private Long warehouseId;

}
