package cn.iocoder.yudao.module.erp.controller.admin.report.trade;

import cn.iocoder.yudao.framework.common.pojo.PageResult;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;
import javax.validation.constraints.*;
import java.time.LocalDateTime;
import java.util.*;

public final class ErpTradeReportModels {
    private ErpTradeReportModels() { }
    @Data public static class Filter {
        @NotNull @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME,fallbackPatterns={"yyyy-MM-dd HH:mm:ss","yyyy-MM-dd HH:mm:ss.SSSSSS"}) private LocalDateTime postedFrom;
        @NotNull @DateTimeFormat(iso=DateTimeFormat.ISO.DATE_TIME,fallbackPatterns={"yyyy-MM-dd HH:mm:ss","yyyy-MM-dd HH:mm:ss.SSSSSS"}) private LocalDateTime postedTo;
        private List<String> businessTypes;
        @Size(max=128) private String bizNo;
        private Long productId, customerId, supplierId, saleUserId, categoryId;
        @Size(max=255) private String oeNumber, brand, purchaser;
        private List<Long> accountingDeptIds, stockDeptIds, warehouseIds;
        private Boolean noOriginalSale;
        @Min(1) private int pageNo=1;
        @Min(1) @Max(200) private int pageSize=20;
        private String groupBy="PRODUCT";
        private String orderField="postedAt", orderDirection="desc";
    }
    @Data public static class Bundle {
        private PageResult<Map<String,Object>> page;
        private Map<String,Object> summary;
        private Map<String,Object> queryContext;
    }
}
