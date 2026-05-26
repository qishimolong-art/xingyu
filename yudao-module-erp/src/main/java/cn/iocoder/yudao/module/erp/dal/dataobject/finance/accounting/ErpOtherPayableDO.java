package cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("erp_other_payable")
@KeySequence("erp_other_payable_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpOtherPayableDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 单号
     */
    private String no;
    /**
     * 状态 10=未审核 20=已审核 30=已驳回
     */
    private Integer status;
    /**
     * 业务时间
     */
    private LocalDateTime bizTime;
    /**
     * 往来单位类型 1=客户 2=供应商 3=员工
     */
    private Integer partyType;
    /**
     * 往来单位 ID
     */
    private Long partyId;
    /**
     * 往来单位名称
     */
    private String partyName;
    /**
     * 结算账户 ID
     */
    private Long accountId;
    /**
     * 合计金额
     */
    private BigDecimal totalAmount;
    /**
     * 优惠金额
     */
    private BigDecimal discountAmount;
    /**
     * 实付金额 = totalAmount - discountAmount
     */
    private BigDecimal actualAmount;
    /**
     * 备注
     */
    private String remark;
    /**
     * 附件 URL
     */
    private String fileUrl;

}
