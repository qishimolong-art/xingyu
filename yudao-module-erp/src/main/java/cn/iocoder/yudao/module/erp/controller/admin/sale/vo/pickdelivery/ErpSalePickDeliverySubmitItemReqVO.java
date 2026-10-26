package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery;

import lombok.Data;
import javax.validation.constraints.*;
import java.math.BigDecimal;

@Data
public class ErpSalePickDeliverySubmitItemReqVO {
    @NotNull
    private Long itemId;
    @NotNull
    @DecimalMin(value = "0", inclusive = false)
    @Digits(integer = 18, fraction = 6, message = "本次数量最多18位整数、6位小数")
    private BigDecimal quantity;
}
