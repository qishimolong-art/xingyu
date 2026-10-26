package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 最新网上订单 Response VO")
@Data
public class ErpSaleCartLatestOnlineOrderRespVO {

    @Schema(description = "销售手推车编号", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long id;

    @Schema(description = "销售手推车单号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String no;

    @Schema(description = "商城订单号")
    private String sourceNo;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
