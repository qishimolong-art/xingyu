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
 * ERP 采购票据 DO
 */
@TableName("erp_purchase_invoice")
@KeySequence("erp_purchase_invoice_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpPurchaseInvoiceDO extends BaseDO {

    @TableId
    private Long id;

    /**
     * 票据单号
     */
    private String no;

    /**
     * 状态
     *
     * 枚举 {@link cn.iocoder.yudao.module.erp.enums.ErpAuditStatus}
     */
    private Integer status;

    /**
     * 供应商编号
     */
    private Long supplierId;

    /**
     * 开票日期
     */
    private LocalDate invoiceDate;

    /**
     * 票据类型
     */
    private String invoiceType;

    /**
     * 发票号
     */
    private String invoiceNo;

    /**
     * 发票张数
     */
    private Integer invoiceCount;

    /**
     * 不含税金额
     */
    private BigDecimal taxExclusiveAmount;

    /**
     * 税额
     */
    private BigDecimal taxAmount;

    /**
     * 价税合计
     */
    private BigDecimal totalAmount;

    /**
     * 部门编号
     */
    private Long deptId;

    /**
     * 经手人用户编号
     */
    private Long handlerId;

    /**
     * 备注
     */
    private String remark;

    /**
     * 附件
     */
    private String fileUrl;
}
