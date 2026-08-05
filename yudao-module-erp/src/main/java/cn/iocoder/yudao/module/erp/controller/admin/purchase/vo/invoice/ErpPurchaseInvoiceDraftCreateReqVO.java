package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoice;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 草稿沿用采购票据字段结构，但草稿接口不触发正式提交校验。
 */
@Schema(description = "管理后台 - ERP 采购票据草稿创建 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ErpPurchaseInvoiceDraftCreateReqVO extends ErpPurchaseInvoiceSaveReqVO {
}
