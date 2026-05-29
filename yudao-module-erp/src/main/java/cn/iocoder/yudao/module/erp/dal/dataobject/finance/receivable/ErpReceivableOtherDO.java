package cn.iocoder.yudao.module.erp.dal.dataobject.finance.receivable;

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

@TableName("erp_receivable_other")
@KeySequence("erp_receivable_other_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ErpReceivableOtherDO extends BaseDO {

    @TableId
    private Long id;

    private String no;

    private Integer status;

    private LocalDate bizTime;

    private Long customerId;

    private String voucherNo;

    private BigDecimal settledAmount;

    private Long deptId;

    private BigDecimal receivableAmount;

    private String project;

    private String sourceType;

    private Long handlerId;

    private String receivableType;

    private BigDecimal costAmount;

    private String remark;

    private Boolean isPaperNote;

    private String paperNoteDesc;

    private String sourceNo;

    private String fileUrl;
}
