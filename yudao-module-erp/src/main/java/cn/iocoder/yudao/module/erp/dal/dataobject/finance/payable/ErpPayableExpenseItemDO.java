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

@TableName("erp_payable_expense_item")
@KeySequence("erp_payable_expense_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ErpPayableExpenseItemDO extends BaseDO {

    @TableId
    private Long id;

    private Long expenseId;

    private String itemName;

    private BigDecimal amount;

    private String invoiceNo;

    private String party;

    private Long deptId;

    private LocalDate bizDate;

    private Long handlerId;

    private Integer qty;

    private String expenseCategory;

    private String remark;

    private String fileUrl;

}
