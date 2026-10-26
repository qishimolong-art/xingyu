package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class ErpPurchaseOrderExportTaskStartReqVO extends ErpPurchaseOrderPageReqVO {
    @Schema(description = "导出字段")
    private List<String> fields;
}
