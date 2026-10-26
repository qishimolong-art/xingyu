package cn.iocoder.yudao.module.erp.dal.dataobject.assembly;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/** ERP 组装配方明细。 */
@TableName("erp_assembly_recipe_item")
@KeySequence("erp_assembly_recipe_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpAssemblyRecipeItemDO extends BaseDO {

    @TableId
    private Long id;
    private Long recipeId;
    private Long productId;
    private BigDecimal quantityPerSet;
    private Integer sort;
    private String remark;

}
