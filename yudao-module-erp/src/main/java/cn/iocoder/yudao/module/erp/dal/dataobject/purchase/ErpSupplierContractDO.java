package cn.iocoder.yudao.module.erp.dal.dataobject.purchase;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("erp_supplier_contract")
@KeySequence("erp_supplier_contract_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpSupplierContractDO extends BaseDO {

    @TableId
    private Long id;
    private Long supplierId;
    private String contractNo;
    private LocalDateTime contractDate;
    private String contractType;
    private String settleMethod;
    private LocalDateTime startTime;
    private String transportMethod;
    private LocalDateTime endTime;
    private Boolean mainContract;
    private Boolean rebateEnabled;
    private Integer freightSettleMethod;
    private BigDecimal baseAmount;
    private BigDecimal taskAmount;
    private String summary;
    private String attachmentUrl;
    private Integer status;
    private String remark;

}
