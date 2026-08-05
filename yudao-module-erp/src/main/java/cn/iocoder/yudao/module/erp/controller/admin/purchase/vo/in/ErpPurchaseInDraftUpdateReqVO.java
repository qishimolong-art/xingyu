package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - ERP 采购入库草稿修改 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ErpPurchaseInDraftUpdateReqVO extends ErpPurchaseInDraftCreateReqVO {
}
