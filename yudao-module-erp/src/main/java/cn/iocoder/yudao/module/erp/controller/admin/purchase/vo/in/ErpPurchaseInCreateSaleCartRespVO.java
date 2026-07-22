package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.in;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Schema(description = "Admin - ERP create sale cart from purchase in Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErpPurchaseInCreateSaleCartRespVO {

    @Schema(description = "Sale cart id", example = "100")
    private Long id;

    @Schema(description = "Sale cart no", example = "XSST202607140001")
    private String no;

    @Schema(description = "Sale cart status", example = "20")
    private Integer status;

}
