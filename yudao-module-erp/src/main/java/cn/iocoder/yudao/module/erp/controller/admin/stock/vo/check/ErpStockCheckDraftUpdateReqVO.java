package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.check;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - ERP 库存盘点草稿修改 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ErpStockCheckDraftUpdateReqVO extends ErpStockCheckDraftCreateReqVO {
}
