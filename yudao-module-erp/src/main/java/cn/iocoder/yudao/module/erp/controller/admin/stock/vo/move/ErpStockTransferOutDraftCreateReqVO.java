package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 调拨出库草稿沿用正式单据字段，但草稿接口不触发正式提交校验。
 */
@Schema(description = "管理后台 - ERP 调拨出库单草稿创建 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ErpStockTransferOutDraftCreateReqVO extends ErpStockMoveSaveReqVO {
}
