package cn.iocoder.yudao.module.erp.service.stock;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ErpStockOutPickCompletedEvent {

    private final Long stockOutId;
    private final Long pickUserId;

}
