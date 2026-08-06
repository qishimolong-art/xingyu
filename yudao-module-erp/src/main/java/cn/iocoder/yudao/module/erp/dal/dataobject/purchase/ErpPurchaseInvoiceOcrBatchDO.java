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

/**
 * ERP 采购发票 OCR 批次 DO。
 */
@TableName("erp_purchase_invoice_ocr_batch")
@KeySequence("erp_purchase_invoice_ocr_batch_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpPurchaseInvoiceOcrBatchDO extends BaseDO {

    @TableId
    private Long id;

    /**
     * 批次号。
     */
    private String batchNo;

    /**
     * 批次状态。
     *
     * 枚举 {@link cn.iocoder.yudao.module.erp.enums.purchase.ErpPurchaseInvoiceOcrBatchStatusEnum}
     */
    private String status;

    /**
     * 文件数量。
     */
    private Integer fileCount;

    /**
     * 已生成采购票据数量。
     */
    private Integer generatedInvoiceCount;

    /**
     * 备注。
     */
    private String remark;

    /**
     * 异常信息。
     */
    private String errorMsg;

}
