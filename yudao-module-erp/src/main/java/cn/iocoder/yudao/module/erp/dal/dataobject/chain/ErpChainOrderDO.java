package cn.iocoder.yudao.module.erp.dal.dataobject.chain;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ERP 连锁开单 DO（跨租户，使用 @TenantIgnore）
 *
 * @author 汽配ERP
 */
@TableName("erp_chain_order")
@KeySequence("erp_chain_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpChainOrderDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 连锁开单号(LSKD前缀)
     */
    private String no;
    /**
     * 状态
     *
     * 10-待审核 20-已审核 30-已完成 90-已取消
     */
    private Integer status;
    /**
     * 总公司租户ID
     */
    private Long hqTenantId;
    /**
     * 分公司租户ID
     */
    private Long branchTenantId;
    /**
     * Department id.
     */
    private Long deptId;
    /**
     * 终端客户ID(无仓分公司)
     */
    private Long customerId;
    /**
     * 总公司出库仓库ID
     */
    private Long hqWarehouseId;
    /**
     * 分公司入库仓库ID(有仓)
     */
    private Long branchWarehouseId;
    /**
     * 分公司类型
     *
     * 1-有仓分公司 2-无仓分公司
     */
    private Integer branchType;
    /**
     * 下单时间
     */
    private LocalDateTime orderTime;
    /**
     * 审核时间
     */
    private LocalDateTime approveTime;
    /**
     * 合计数量
     */
    private BigDecimal totalCount;
    /**
     * 合计金额
     */
    private BigDecimal totalPrice;
    /**
     * 总公司销售出库单ID
     */
    private Long hqSaleOutId;
    /**
     * 分公司采购入库单ID(有仓)
     */
    private Long branchPurchaseInId;
    /**
     * 备注
     */
    private String remark;

}
