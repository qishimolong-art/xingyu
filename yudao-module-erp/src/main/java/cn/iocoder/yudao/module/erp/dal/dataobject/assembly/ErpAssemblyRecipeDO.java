package cn.iocoder.yudao.module.erp.dal.dataobject.assembly;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/** ERP 组装配方主表。 */
@TableName("erp_assembly_recipe")
@KeySequence("erp_assembly_recipe_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpAssemblyRecipeDO extends BaseDO {

    @TableId
    private Long id;
    private String no;
    private Long productId;
    private Integer purpose;
    private String moduleCode;
    private String moduleName;
    private BigDecimal laborCostPerSet;
    private Long deptId;
    private Integer version;
    private Integer status;
    private String remark;

}
