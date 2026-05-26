package cn.iocoder.yudao.module.erp.dal.dataobject.purchase;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

@TableName("erp_supplier_extend")
@KeySequence("erp_supplier_extend_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpSupplierExtendDO extends BaseDO {

    @TableId
    private Long id;
    private Long supplierId;
    private String extendKey;
    private String extendName;
    private String extendValue;
    private String extendType;
    private Boolean required;
    private String optionValues;
    private Integer sort;
    private String remark;

}
