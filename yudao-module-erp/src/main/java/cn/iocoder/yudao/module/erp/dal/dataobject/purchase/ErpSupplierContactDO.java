package cn.iocoder.yudao.module.erp.dal.dataobject.purchase;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("erp_supplier_contact")
@KeySequence("erp_supplier_contact_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpSupplierContactDO extends BaseDO {

    @TableId
    private Long id;
    private Long supplierId;
    private String name;
    private Boolean salesperson;
    private String telephone;
    private String mobile;
    private String address;
    private String email;
    private Integer gender;
    private String position;
    private Long companyId;
    private String fax;
    private Long deptId;
    private Integer importance;
    private BigDecimal commissionRate;
    private Boolean fixedCommission;
    private String postCode;
    private String qq;
    private LocalDate birthday;
    private Boolean primaryContact;
    private Boolean receiverContact;
    private Boolean settleContact;
    private Boolean messageContact;
    private String wechat;
    private Integer wechatOfficialStatus;
    private Boolean orderAccess;
    private Boolean ecommerceAccess;
    private String businessCardFrontUrl;
    private String businessCardBackUrl;
    private LocalDateTime lastContactTime;
    private Integer status;
    private String remark;

}
