package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.invoiceocr;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@ExcelIgnoreUnannotated
public class ErpPurchaseInvoiceOcrErrorExportRespVO {

    @ExcelProperty("批次号")
    private String batchNo;

    @ExcelProperty("文件名")
    private String fileName;

    private String status;

    @ExcelProperty("状态")
    private String statusName;

    @ExcelProperty("发票号")
    private String invoiceNo;

    @ExcelProperty("开票日期")
    private LocalDate invoiceDate;

    @ExcelProperty("发票类型")
    private String invoiceType;

    @ExcelProperty("发票金额")
    private BigDecimal totalAmount;

    @ExcelProperty("OCR备注")
    private String invoiceRemark;

    @ExcelProperty("OCR解析厂家单号")
    private String parsedFactoryOrderNo;

    @ExcelProperty("当前厂家单号")
    private String factoryOrderNo;

    @ExcelProperty("匹配入库单")
    private String matchedPurchaseInNos;

    @ExcelProperty("入库金额")
    private BigDecimal purchaseInTotalAmount;

    @ExcelProperty("差额")
    private BigDecimal diffAmount;

    @ExcelProperty("异常信息")
    private String errorMsg;

    @ExcelProperty("附件地址")
    private String fileUrl;

}
