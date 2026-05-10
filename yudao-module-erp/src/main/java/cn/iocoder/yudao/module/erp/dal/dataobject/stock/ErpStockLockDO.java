package cn.iocoder.yudao.module.erp.dal.dataobject.stock;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * ERP 库存占用 DO
 *
 * @author 汽配ERP
 */
@TableName("erp_stock_lock")
@KeySequence("erp_stock_lock_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpStockLockDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 产品编号
     */
    private Long productId;
    /**
     * 仓库编号
     */
    private Long warehouseId;
    /**
     * 锁定数量
     */
    private BigDecimal lockCount;
    /**
     * 业务类型
     *
     * 1-销售订单 2-连锁开单
     */
    private Integer bizType;
    /**
     * 业务单据ID
     */
    private Long bizId;
    /**
     * 业务单据项ID
     */
    private Long bizItemId;
    /**
     * 业务单号
     */
    private String bizNo;
    /**
     * 状态
     *
     * 1-锁定中 2-已释放 3-已扣减
     */
    private Integer status;

}
