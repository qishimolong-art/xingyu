package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.returns;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "管理后台 - ERP 销售退货导入结果 Response VO")
public class ErpSaleReturnImportRespVO {

    @Schema(description = "退货模式")
    private Integer returnMode = 20;

    @Schema(description = "成功条数")
    private Integer successCount = 0;

    @Schema(description = "失败条数")
    private Integer failureCount = 0;

    @Schema(description = "导入明细")
    private List<ErpSaleReturnRespVO.Item> items = new ArrayList<>();

    @Schema(description = "失败明细")
    private List<FailureItem> failureDetails = new ArrayList<>();

    @Data
    public static class FailureItem {
        @Schema(description = "行号")
        private Integer rowNo;

        @Schema(description = "产品编码")
        private String productCode;

        @Schema(description = "失败原因")
        private String reason;

        public FailureItem(Integer rowNo, String productCode, String reason) {
            this.rowNo = rowNo;
            this.productCode = productCode;
            this.reason = reason;
        }
    }
}
