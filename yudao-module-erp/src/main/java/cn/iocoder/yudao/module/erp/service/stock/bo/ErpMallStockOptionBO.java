package cn.iocoder.yudao.module.erp.service.stock.bo;

import lombok.Data;

import java.math.BigDecimal;

/**
 * Mall purchase stock option backed by ERP stock.
 */
@Data
public class ErpMallStockOptionBO {

    private Long stockId;

    private Long erpProductId;

    private Long warehouseId;

    private String warehouseName;

    private Boolean available;

    private BigDecimal availableCount;

    private String availableStatusText;

    private Long distanceMeters;

    private String distanceText;

}
