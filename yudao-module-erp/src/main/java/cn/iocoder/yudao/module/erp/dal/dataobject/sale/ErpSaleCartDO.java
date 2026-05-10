package cn.iocoder.yudao.module.erp.dal.dataobject.sale;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ERP 销售手推车 DO
 */
@TableName("erp_sale_cart")
@KeySequence("erp_sale_cart_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpSaleCartDO extends BaseDO {

    @TableId
    private Long id;
    private String no;
    private Integer status;
    private Long customerId;
    private Long accountId;
    private Long saleUserId;
    private Long deptId;
    private LocalDateTime cartTime;
    private Long firstAuditUserId;
    private LocalDateTime firstAuditTime;
    private Long finalAuditUserId;
    private LocalDateTime finalAuditTime;
    private BigDecimal totalCount;
    private BigDecimal totalPrice;
    private BigDecimal totalProductPrice;
    private BigDecimal totalTaxPrice;
    private BigDecimal discountPercent;
    private BigDecimal discountPrice;
    private BigDecimal otherPrice;
    /**
     * 业务来源类型
     *
     * 枚举 {@link cn.iocoder.yudao.module.erp.enums.sale.ErpSaleBizSourceTypeEnum}
     */
    private Integer sourceType;
    /**
     * 来源单据编号
     */
    private Long sourceId;
    /**
     * 来源单据号
     */
    private String sourceNo;
    private String fileUrl;
    private String remark;

}
