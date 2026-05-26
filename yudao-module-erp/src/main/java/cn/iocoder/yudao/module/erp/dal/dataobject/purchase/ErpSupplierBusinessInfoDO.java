package cn.iocoder.yudao.module.erp.dal.dataobject.purchase;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@TableName("erp_supplier_business_info")
@KeySequence("erp_supplier_business_info_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpSupplierBusinessInfoDO extends BaseDO {

    @TableId
    private Long id;
    private Long supplierId;
    private String companyName;
    private String creditCode;
    private String legalPerson;
    private String registeredAddress;
    private String businessScope;
    private String registeredCapital;
    private String establishDate;
    private String businessStatus;
    private String rawData;
    private String remark;

}
