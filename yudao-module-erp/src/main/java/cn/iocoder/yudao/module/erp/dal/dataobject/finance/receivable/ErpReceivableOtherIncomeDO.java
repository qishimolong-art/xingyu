package cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("erp_receivable_other_income")
@KeySequence("erp_receivable_other_income_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpReceivableOtherIncomeDO extends BaseDO {

    @TableId
    private Long id;
    private String no;
    private Integer status;
    private LocalDateTime bizTime;
    private String settleMethod;
    private Long accountId;
    private String voucherNo;
    private String incomeType;
    private BigDecimal totalAmount;
    private Long deptId;
    private Long handlerId;
    private String party;
    private String relatedBiz;
    private String docType;
    private String remark;
    private String fileUrl;
}
