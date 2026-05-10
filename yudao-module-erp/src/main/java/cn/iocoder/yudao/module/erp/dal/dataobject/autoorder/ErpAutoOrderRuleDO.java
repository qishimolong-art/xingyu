package cn.iocoder.yudao.module.erp.dal.dataobject.autoorder;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * ERP 自动订货规则 DO
 *
 * @author 汽配ERP
 */
@TableName("erp_auto_order_rule")
@KeySequence("erp_auto_order_rule_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpAutoOrderRuleDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 产品ID（空=全局规则）
     */
    private Long productId;
    /**
     * 产品分类ID
     */
    private Long categoryId;
    /**
     * 仓库ID
     */
    private Long warehouseId;
    /**
     * 最低库存
     */
    private BigDecimal minStock;
    /**
     * 最高库存（补到此值）
     */
    private BigDecimal maxStock;
    /**
     * 销量计算天数
     */
    private Integer calcDays;
    /**
     * 安全天数
     */
    private Integer safetyDays;
    /**
     * 采购提前期(天)
     */
    private Integer leadDays;
    /**
     * 状态（0正常 1停用）
     */
    private Integer status;

}
