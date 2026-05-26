package cn.iocoder.yudao.module.erp.service.sale.bo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ErpCustomerSaleStatsBO {
    private Long customerId;
    private LocalDateTime lastSaleTime;
    private BigDecimal totalSaleAmount;
    private BigDecimal receivableBalance;
}
