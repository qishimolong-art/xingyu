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
    @Digits(integer = 18, fraction = 0, message = "本次数量必须为整数，最多18位")
    private BigDecimal quantity;
}
