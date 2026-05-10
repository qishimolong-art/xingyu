package cn.iocoder.yudao.module.erp.dal.dataobject.chain;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * ERP 连锁开单明细 DO（跨租户）
 *
 * @author 汽配ERP
 */
@TableName("erp_chain_order_item")
@KeySequence("erp_chain_order_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpChainOrderItemDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 连锁开单ID
     */
    private Long chainOrderId;
    /**
     * 产品编号
     */
    private Long productId;
    /**
     * 产品单位编号
     */
    private Long productUnitId;
    /**
     * 产品单价
     */
    private BigDecimal productPrice;
    /**
     * 数量
     */
    private BigDecimal count;
    /**
     * 总价
     */
    private BigDecimal totalPrice;
    /**
     * 备注
     */
    private String remark;

}
