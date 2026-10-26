package cn.iocoder.yudao.module.erp.dal.dataobject.sale.pickdelivery;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Data
@Accessors(chain = true)
public class ErpSalePickQuantityAggregateDO {

    private Long pickTaskId;
    private Integer totalItemCount;
    private BigDecimal totalQuantity;
    private BigDecimal pickedQuantity;

}
