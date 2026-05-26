package cn.iocoder.yudao.module.erp.dal.dataobject.sale;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

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

    /** 是否导购员 */
    private Boolean salesperson;
    /** 性别 1=男 2=女 */
    private Integer gender;
    /** 所属公司（客户ID，自关联） */
    private Long companyId;
    /** 传真 */
    private String fax;
    /** 所属部门 */
    private Long deptId;
    /** 重要性 1=普通 2=重要 3=决策人 */
    private Integer importance;
    /** 提成率（百分比） */
    private BigDecimal commissionRate;
    /** 固定提成 */
    private Boolean fixedCommission;
    /** 邮编 */
    private String postCode;
    /** 生日 */
    private LocalDate birthday;
    /** 最后联系时间 */
    private LocalDateTime lastContactTime;

    private String remark;

}
