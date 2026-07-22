package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "Admin - ERP create stock transfer-out from purchase in Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErpPurchaseInCreateTransferOutRespVO {

    @Schema(description = "Stock transfer-out id", example = "1024")
    private Long id;

    @Schema(description = "Stock transfer-out ids")
    private List<Long> ids;

}
