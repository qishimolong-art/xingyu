package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - ERP 仓库移货单草稿修改 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ErpWarehouseMoveDraftUpdateReqVO extends ErpWarehouseMoveDraftCreateReqVO {
}
