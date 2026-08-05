package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 销售退货草稿沿用正式保存字段，但草稿接口不触发正式提交校验。
 */
@Schema(description = "管理后台 - ERP 销售退货草稿创建 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ErpSaleReturnDraftCreateReqVO extends ErpSaleReturnSaveReqVO {
}
