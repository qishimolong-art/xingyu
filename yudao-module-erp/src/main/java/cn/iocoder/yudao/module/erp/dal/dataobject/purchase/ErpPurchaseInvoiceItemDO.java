package cn.iocoder.yudao.module.erp.dal.dataobject.purchase;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
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
     * 通用来源类型：10 入库、20 调价、30 退货。
     */
    private Integer sourceType;

    /**
     * 通用来源单据编号、单号和明细编号。
     */
    private Long sourceDocId;
    private String sourceDocNo;
    private Long sourceItemId;

    /**
     * 金额方向：1 正向、-1 负向。
     */
    private Integer amountDirection;

    /**
     * 来源原始数量和当前剩余可开数量，仅用于接口展示。
     */
    @TableField(exist = false)
    private BigDecimal sourceTotalCount;
    @TableField(exist = false)
    private BigDecimal sourceRemainingCount;

    @TableField(exist = false)
    private String sourceTypeName;

    @TableField(exist = false)
    private Long warehouseId;

    @TableField(exist = false)
    private Integer packageQty;

    /**
     * 来源明细扩展展示字段，仅用于详情及导出，不落库。
     */
    @TableField(exist = false)
    private Long sourceDeptId;
    @TableField(exist = false)
    private Integer wholeQty;
    @TableField(exist = false)
    private String batchNo;
    @TableField(exist = false)
    private String businessEntity;

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
