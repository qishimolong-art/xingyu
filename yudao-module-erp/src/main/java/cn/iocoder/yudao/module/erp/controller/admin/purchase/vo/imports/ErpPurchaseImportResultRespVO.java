package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.imports;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - ERP 采购整单导入 Response VO")
@Data
public class ErpPurchaseImportResultRespVO {

    @Schema(description = "成功单据数")
    private Integer successCount = 0;

    @Schema(description = "失败行数")
    private Integer failureCount = 0;

    @Schema(description = "导入记录编号")
    private Long recordId;

    @Schema(description = "创建成功的单据编号")
    private List<Long> documentIds = new ArrayList<>();

    @Schema(description = "失败明细")
    private List<FailureItem> failureDetails = new ArrayList<>();

    @Data
    public static class FailureItem {

        @Schema(description = "行号")
        private Integer rowNo;

        @Schema(description = "单据标识")
        private String orderNo;

        @Schema(description = "产品编码")
        private String productCode;

        @Schema(description = "失败原因")
        private String reason;

        public FailureItem(Integer rowNo, String orderNo, String productCode, String reason) {
            this.rowNo = rowNo;
            this.orderNo = orderNo;
            this.productCode = productCode;
            this.reason = reason;
        }

    }

}
