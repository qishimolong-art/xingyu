package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class ErpSalePickDeliverySubmitItemRespVO {
    private Long id;
    private Long itemId;
    private String productCode;
    private String productName;
    @com.fasterxml.jackson.databind.annotation.JsonSerialize(using = com.fasterxml.jackson.databind.ser.std.ToStringSerializer.class)
    private BigDecimal quantity;
}
