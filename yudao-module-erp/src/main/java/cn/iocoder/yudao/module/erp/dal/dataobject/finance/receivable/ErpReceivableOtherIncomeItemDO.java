package cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("erp_receivable_other_income_item")
@KeySequence("erp_receivable_other_income_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpReceivableOtherIncomeItemDO extends BaseDO {

    @TableId
    private Long id;
    private Long incomeId;
    private String itemName;
    private BigDecimal amount;
    private String invoiceNo;
    private String party;
    private Long deptId;
    private LocalDateTime bizDate;
    private Long handlerId;
    private Integer qty;
    private String freightType;
    private String remark;
    private String fileUrl;
}
