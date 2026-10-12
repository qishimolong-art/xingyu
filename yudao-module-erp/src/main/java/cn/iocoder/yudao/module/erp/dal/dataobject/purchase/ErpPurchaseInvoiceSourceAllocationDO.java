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

/**
 * ERP 采购票据来源分摊 DO。
 *
 * 仅在采购票据审核通过后写入。草稿、待审核单不占用来源可开票额度。
 */
@TableName("erp_purchase_invoice_source_allocation")
@KeySequence("erp_purchase_invoice_source_allocation_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpPurchaseInvoiceSourceAllocationDO extends BaseDO {

    @TableId
    private Long id;

    private Long invoiceId;
    private Long invoiceItemId;
    private Integer sourceType;
    private Long sourceDocId;
    private String sourceDocNo;
    private Long sourceItemId;
    private BigDecimal sourceTotalCount;
    private BigDecimal sourceTotalAmount;
    private BigDecimal allocatedCount;
    private BigDecimal allocatedAmount;
    private Integer amountDirection;
    private Boolean legacyFullAllocation;
}
