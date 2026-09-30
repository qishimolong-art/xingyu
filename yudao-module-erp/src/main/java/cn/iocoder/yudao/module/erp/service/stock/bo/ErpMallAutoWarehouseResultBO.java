package cn.iocoder.yudao.module.erp.service.stock.bo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * 商城自动分仓结果。
 */
@Data
@Accessors(chain = true)
public class ErpMallAutoWarehouseResultBO {

    public static final String STATUS_READY = "READY";
    public static final String STATUS_LOCATION_REQUIRED = "LOCATION_REQUIRED";
    public static final String STATUS_PRODUCT_NOT_MAPPED = "PRODUCT_NOT_MAPPED";
    public static final String STATUS_WAREHOUSE_LOCATION_MISSING = "WAREHOUSE_LOCATION_MISSING";
    public static final String STATUS_STOCK_SHORTAGE = "STOCK_SHORTAGE";

    private String status;
    private List<Assignment> assignments = new ArrayList<>();
    private List<Issue> issues = new ArrayList<>();

    public boolean isReady() {
        return STATUS_READY.equals(status);
    }

    @Data
    @Accessors(chain = true)
    public static class Item {
        private Integer index;
        private Long spuId;
        private Long skuId;
        private Integer count;
    }

    @Data
    @Accessors(chain = true)
    public static class Assignment {
        private Integer index;
        private Long spuId;
        private Long skuId;
        private Integer count;
        private Long stockId;
        private Long erpProductId;
        private Long warehouseId;
        private String warehouseName;
        private Long distanceMeters;
    }

    @Data
    @Accessors(chain = true)
    public static class Issue {
        private Integer index;
        private Long spuId;
        private Long skuId;
        private Integer count;
        private String reason;
        private String message;
    }

}
