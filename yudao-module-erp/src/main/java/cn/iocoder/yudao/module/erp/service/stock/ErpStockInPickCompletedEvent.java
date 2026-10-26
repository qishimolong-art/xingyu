package cn.iocoder.yudao.module.erp.service.stock;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ErpStockInPickCompletedEvent {

    private final Long stockInId;
    private final Long pickUserId;

}
