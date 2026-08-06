package cn.iocoder.yudao.module.erp.enums.purchase;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * ERP 采购发票 OCR 批次状态。
 */
@RequiredArgsConstructor
@Getter
public enum ErpPurchaseInvoiceOcrBatchStatusEnum {

    DRAFT("DRAFT", "已上传"),
    RECOGNIZING("RECOGNIZING", "识别中"),
    RECOGNIZED("RECOGNIZED", "已识别"),
    MATCHED("MATCHED", "已匹配完成"),
    CONFIRMED("CONFIRMED", "已确认生成采购票据"),
    FAILED("FAILED", "处理失败"),
    ;

    private final String status;
    private final String name;

}
