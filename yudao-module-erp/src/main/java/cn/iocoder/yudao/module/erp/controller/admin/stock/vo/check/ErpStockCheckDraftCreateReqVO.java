package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.check;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 库存盘点草稿沿用正式单据字段，但草稿接口不触发正式提交校验。
 */
@Schema(description = "管理后台 - ERP 库存盘点草稿创建 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ErpStockCheckDraftCreateReqVO extends ErpStockCheckSaveReqVO {
}
