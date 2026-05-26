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
     * 调价单号（CGTJ 前缀）
     */
    private String no;
    /**
     * 状态：10=待审批（未审核）20=已通过（已审核）30=已拒绝
     *
     * 枚举 {@link cn.iocoder.yudao.module.erp.enums.ErpAuditStatus}
     */
    private Integer status;
    /**
     * 调价日期
     */
    private LocalDateTime adjustTime;
    /**
     * 供应商编号
     *
     * 关联 {@link ErpSupplierDO#getId()}
     */
    private Long supplierId;
    /**
     * 部门 ID
     */
    private Long deptId;
    /**
     * 调价人（系统用户 ID）
     */
    private Long adjuster;
    /**
     * 调价类型
     *
     * 枚举 {@link cn.iocoder.yudao.module.erp.enums.purchase.ErpPurchasePriceAdjustTypeEnum}
     */
    private Integer adjustType;
    /**
     * 备注
     */
    private String remark;
    /**
     * 调价总金额（可正可负）
     */
    private BigDecimal totalAdjustPrice;
    /**
     * 审批通过时间
     */
    private LocalDateTime approveTime;

}
