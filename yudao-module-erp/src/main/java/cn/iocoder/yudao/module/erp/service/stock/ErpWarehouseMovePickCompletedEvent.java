package cn.iocoder.yudao.module.erp.service.stock;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ErpWarehouseMovePickCompletedEvent {

    private final Long warehouseMoveId;
    private final Long pickUserId;

}
