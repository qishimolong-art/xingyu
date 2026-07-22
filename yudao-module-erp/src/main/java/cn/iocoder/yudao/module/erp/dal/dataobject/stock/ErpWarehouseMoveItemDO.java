package cn.iocoder.yudao.module.erp.dal.dataobject.stock;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * ERP warehouse move item.
 */
@TableName("erp_warehouse_move_item")
@KeySequence("erp_warehouse_move_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpWarehouseMoveItemDO extends BaseDO {

    @TableId
    private Long id;

    private Long moveId;

    private Long fromWarehouseId;

    private Long toWarehouseId;

    private Long productId;

    private Long productUnitId;

    private BigDecimal productPrice;

    private BigDecimal count;

    private BigDecimal totalPrice;

    private String fromShelf;

    private String toShelf;

    private BigDecimal costPrice;

    private BigDecimal costAmount;

    private BigDecimal weight;

    private BigDecimal totalWeight;

    private String batchNo;

    private String remark;

}
