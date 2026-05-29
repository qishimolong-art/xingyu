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
 * ERP 采购票据明细 DO
 */
@TableName("erp_purchase_invoice_item")
@KeySequence("erp_purchase_invoice_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpPurchaseInvoiceItemDO extends BaseDO {

    @TableId
    private Long id;

    /**
     * 采购票据编号
     */
    private Long invoiceId;

    /**
     * 来源入库单编号
     */
    private Long sourceInId;

    /**
     * 来源入库单号
     */
    private String sourceInNo;

    /**
     * 来源入库单明细编号
     */
    private Long sourceInItemId;

    /**
     * 产品编号
     */
    private Long productId;

    /**
     * 产品单位名称快照
     */
    private String productUnitName;

    /**
     * 产品条码快照
     */
    private String productBarCode;

    /**
     * 数量
     */
    private BigDecimal count;

    /**
     * 不含税单价
     */
    private BigDecimal productPrice;

    /**
     * 不含税金额
     */
    private BigDecimal taxExclusivePrice;

    /**
     * 税率
     */
    private BigDecimal taxPercent;

    /**
     * 税额
     */
    private BigDecimal taxPrice;

    /**
     * 价税合计
     */
    private BigDecimal totalPrice;

    /**
     * 备注
     */
    private String remark;
}
