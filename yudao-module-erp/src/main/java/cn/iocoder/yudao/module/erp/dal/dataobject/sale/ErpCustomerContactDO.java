package cn.iocoder.yudao.module.erp.dal.dataobject.sale;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@TableName("erp_customer_contact")
@KeySequence("erp_customer_contact_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpCustomerContactDO extends BaseDO {

    @TableId
    private Long id;
    private Long customerId;
    private String name;
    private String mobile;
    private String telephone;
    private String email;
    private String position;
    private String wechat;
    private String qq;
    private String address;
    private Boolean primaryContact;
    private Boolean receiverContact;
    private Boolean settleContact;
    private Boolean messageContact;
    private String businessCardFrontUrl;
    private String businessCardBackUrl;
    private Integer status;
    private String remark;

}
