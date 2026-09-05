package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.imports;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Schema(description = "ERP sale whole-document import response")
@Data
public class ErpSaleImportResultRespVO {

    @Schema(description = "Import record id")
    private Long recordId;

    @Schema(description = "Success document count")
    private Integer successCount = 0;

    @Schema(description = "Failure row count")
    private Integer failureCount = 0;

    @Schema(description = "Created document ids")
    private List<Long> documentIds = new ArrayList<>();

    @Schema(description = "Failure details")
    private List<FailureItem> failureDetails = new ArrayList<>();

    @Data
    public static class FailureItem {

        @Schema(description = "Row number")
        private Integer rowNo;

        @Schema(description = "Document key")
        private String orderNo;

        @Schema(description = "Product code")
        private String productCode;

        @Schema(description = "Failure reason")
        private String reason;

        public FailureItem(Integer rowNo, String orderNo, String productCode, String reason) {
            this.rowNo = rowNo;
            this.orderNo = orderNo;
            this.productCode = productCode;
            this.reason = reason;
        }

    }

}
