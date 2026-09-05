package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.imports;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
public class ErpStockImportResultRespVO {

    private Long recordId;

    private Integer successCount = 0;

    private Integer createCount = 0;

    private Integer updateCount = 0;

    private Integer failureCount = 0;

    private List<FailureItem> failureDetails = new ArrayList<>();

    public void addSuccess() {
        successCount++;
        createCount++;
    }

    public void addFailure(Integer rowNo, String orderNo, String productCode, String reason) {
        failureCount++;
        failureDetails.add(new FailureItem(rowNo, orderNo, productCode, reason));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FailureItem {

        private Integer rowNo;

        private String orderNo;

        private String code;

        private String reason;

    }

}
