package cn.iocoder.yudao.module.erp.dal.dataobject.sale;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("erp_customer_contract")
@KeySequence("erp_customer_contract_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpCustomerContractDO extends BaseDO {

    @TableId
    private Long id;
    private Long customerId;
    private String contractNo;
    private LocalDateTime contractDate;
    private String contractType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String settleMethod;
    private String transportMethod;
    private BigDecimal baseAmount;
    private BigDecimal taskAmount;
    private String attachmentUrl;
    private Boolean mainContract;
    private Boolean rebateEnabled;
    private Integer freightSettleMethod;
    private String summary;
    private Integer status;
    private String remark;

}
