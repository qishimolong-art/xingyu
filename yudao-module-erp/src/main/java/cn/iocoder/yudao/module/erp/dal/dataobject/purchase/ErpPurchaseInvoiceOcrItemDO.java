package cn.iocoder.yudao.module.erp.dal.dataobject.purchase;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * ERP 采购发票 OCR 明细 DO。
 */
@TableName("erp_purchase_invoice_ocr_item")
@KeySequence("erp_purchase_invoice_ocr_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpPurchaseInvoiceOcrItemDO extends BaseDO {

    @TableId
    private Long id;

    /**
     * OCR 批次编号。
     */
    private Long batchId;

    /**
     * 明细状态。
     *
     * 枚举 {@link cn.iocoder.yudao.module.erp.enums.purchase.ErpPurchaseInvoiceOcrItemStatusEnum}
     */
    private String status;

    /**
     * 原始文件名。
     */
    private String fileName;

    /**
     * 文件地址。
     */
    private String fileUrl;

    /**
     * 文件类型。
     */
    private String fileType;

    /**
     * 文件大小。
     */
    private Long fileSize;

    /**
     * 发票号码。
     */
    private String invoiceNo;

    /**
     * 开票日期。
     */
    private LocalDate invoiceDate;

    /**
     * 发票类型。
     */
    private String invoiceType;

    /**
     * 发票金额。
     */
    private BigDecimal totalAmount;

    /**
     * 不含税金额。
     */
    private BigDecimal taxExclusiveAmount;

    /**
     * 税额。
     */
    private BigDecimal taxAmount;

    /**
     * OCR 识别备注。
     */
    private String invoiceRemark;

    /**
     * OCR 自动解析的厂家单号。
     */
    private String parsedFactoryOrderNo;

    /**
     * 最终用于匹配的厂家单号。
     */
    private String factoryOrderNo;

    /**
     * OCR 原始响应。
     */
    private String rawJson;

    /**
     * 匹配到的采购入库单 ID 快照，逗号分隔。
     */
    private String matchedPurchaseInIds;

    /**
     * 匹配到的采购入库单号快照，逗号分隔。
     */
    private String matchedPurchaseInNos;

    /**
     * 匹配时采购入库金额合计快照。
     */
    private BigDecimal purchaseInTotalAmount;

    /**
     * 差额，发票金额 - 入库金额。
     */
    private BigDecimal diffAmount;

    /**
     * 生成的采购票据编号。
     */
    private Long generatedInvoiceId;

    /**
     * 异常信息。
     */
    private String errorMsg;

}
