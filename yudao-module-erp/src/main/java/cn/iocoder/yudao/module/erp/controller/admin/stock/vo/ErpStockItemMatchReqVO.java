package cn.iocoder.yudao.module.erp.controller.admin.stock.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Schema(description = "管理后台 - ERP 库存单据整单明细匹配 Request VO")
@Data
public class ErpStockItemMatchReqVO {

    @NotNull(message = "主单编号不能为空")
    private Long documentId;

    @Min(value = 1, message = "页大小不能小于 1")
    @Max(value = 200, message = "页大小不能大于 200")
    private Integer pageSize = 10;

    @Valid
    @NotEmpty(message = "待匹配键不能为空")
    @Size(max = 200, message = "单次最多匹配 200 个键")
    private List<Key> keys;

    private List<Long> excludedItemIds;

    @Data
    public static class Key {
        private Long productId;
        private Long warehouseId;
        private String batchNo;
        private Long fromWarehouseId;
        private Long toWarehouseId;
        private String fromShelf;
    }
}
