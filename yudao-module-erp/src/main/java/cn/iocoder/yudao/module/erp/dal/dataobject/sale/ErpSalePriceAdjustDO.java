package cn.iocoder.yudao.module.erp.dal.dataobject.sale;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ERP 销售调价单 DO
 *
 * @author 汽配ERP
 */
@TableName("erp_sale_price_adjust")
@KeySequence("erp_sale_price_adjust_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpSalePriceAdjustDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 调价单号(XSTJ前缀)
     */
    private String no;
    /**
     * 状态：10未审核 20已审核
     */
    private Integer status;
    /**
     * 日期
     */
    private LocalDateTime adjustDate;
    /**
     * 客户ID
     */
    private Long customerId;
    /**
     * 部门
     */
    private String dept;
    /**
     * 调价人
     */
    private String adjustUser;
    /**
     * 调价类型
     */
    private String adjustType;
    /**
     * 备注
     */
    private String remark;
    /**
     * 调价总金额
     */
    private BigDecimal totalAdjustPrice;

}
