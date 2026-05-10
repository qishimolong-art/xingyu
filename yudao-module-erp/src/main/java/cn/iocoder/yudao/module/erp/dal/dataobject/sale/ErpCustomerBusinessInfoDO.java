package cn.iocoder.yudao.module.erp.dal.dataobject.sale;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@TableName("erp_customer_business_info")
@KeySequence("erp_customer_business_info_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpCustomerBusinessInfoDO extends BaseDO {

    @TableId
    private Long id;
    private Long customerId;
    private String creditCode;
    private String legalPerson;
    private String registeredCapital;
    private String establishDate;
    private String businessStatus;
    private String businessScope;
    private String rawData;
    private String remark;

}
