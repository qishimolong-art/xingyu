package cn.iocoder.yudao.module.erp.enums.sale;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ErpSalePickDeliverySubmitTypeEnum {

    PICK(10, "拣货"),
    DELIVERY(20, "送货");

    private final Integer type;
    private final String name;

}
