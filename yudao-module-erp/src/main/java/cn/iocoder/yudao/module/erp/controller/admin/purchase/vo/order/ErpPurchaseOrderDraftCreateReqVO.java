package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 采购订单草稿沿用正式保存字段，但草稿接口不触发正式提交校验。
 */
@Schema(description = "管理后台 - ERP 采购订单草稿创建 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ErpPurchaseOrderDraftCreateReqVO extends ErpPurchaseOrderSaveReqVO {
}
