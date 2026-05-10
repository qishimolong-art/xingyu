package cn.iocoder.yudao.module.erp.dal.dataobject.purchase;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ERP 采购调价单 DO
 *
 * @author 汽配ERP
 */
@TableName("erp_purchase_price_adjust")
@KeySequence("erp_purchase_price_adjust_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpPurchasePriceAdjustDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 调价单号(CGTJ前缀)
     */
    private String no;
    /**
     * 状态
     *
     * 枚举 {@link cn.iocoder.yudao.module.erp.enums.ErpAuditStatus}
     */
    private Integer status;
    /**
     * 日期
     */
    private LocalDateTime adjustDate;
    /**
     * 供应商编号
     */
    private Long supplierId;
    /**
     * 部门
     */
    private String dept;
    /**
     * 调价人
     */
    private String adjustUser;
    /**
     * 调价类型(按入库单调价等)
     */
    private String adjustType;
    /**
     * 备注
     */
    private String remark;
    /**
     * 调价总金额(正加负减)
     */
    private BigDecimal totalAdjustPrice;

}
