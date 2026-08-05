package cn.iocoder.yudao.module.erp.dal.dataobject.finance.payable;

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
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDate;

@TableName("erp_payable_expense")
@KeySequence("erp_payable_expense_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ErpPayableExpenseDO extends BaseDO {

    @TableId
    private Long id;

    private String no;

    private Integer status;

    private LocalDate bizTime;

    private String settleMethod;

    private Long accountId;

    private String voucherNo;

    private String expenseType;

    private BigDecimal totalAmount;

    private Long deptId;

    private Long handlerId;

    private String party;

    private String relatedBiz;

    private String sourceType;

    private Long sourceId;

    private String sourceNo;

    private String docType;

    private String remark;

    private String fileUrl;

}
