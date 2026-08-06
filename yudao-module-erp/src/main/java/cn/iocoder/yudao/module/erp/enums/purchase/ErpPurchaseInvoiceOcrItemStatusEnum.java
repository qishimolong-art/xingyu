package cn.iocoder.yudao.module.erp.enums.purchase;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * ERP 采购发票 OCR 明细状态。
 */
@RequiredArgsConstructor
@Getter
public enum ErpPurchaseInvoiceOcrItemStatusEnum {

    PENDING("PENDING", "待处理"),
    OCR_FAILED("OCR_FAILED", "OCR 失败"),
    NO_FACTORY_NO("NO_FACTORY_NO", "无法解析厂家单号"),
    NO_PURCHASE_IN("NO_PURCHASE_IN", "找不到采购入库"),
    AMOUNT_DIFF("AMOUNT_DIFF", "金额不一致"),
    MATCHED("MATCHED", "已匹配"),
    CONFIRMED("CONFIRMED", "已确认"),
    IGNORED("IGNORED", "已忽略"),
    ;

    private final String status;
    private final String name;

}
