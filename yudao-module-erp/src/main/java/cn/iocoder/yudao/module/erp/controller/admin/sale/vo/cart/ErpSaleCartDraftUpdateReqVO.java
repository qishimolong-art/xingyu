package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Schema(description = "管理后台 - ERP 销售手推车草稿修改 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
public class ErpSaleCartDraftUpdateReqVO extends ErpSaleCartDraftCreateReqVO {

    private Long id;

}
