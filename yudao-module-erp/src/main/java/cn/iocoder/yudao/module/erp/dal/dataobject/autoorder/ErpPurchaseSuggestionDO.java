package cn.iocoder.yudao.module.erp.dal.dataobject.autoorder;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ERP 采购建议单 DO
 *
 * @author 汽配ERP
 */
@TableName("erp_purchase_suggestion")
@KeySequence("erp_purchase_suggestion_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpPurchaseSuggestionDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 建议单号(CGJY前缀)
     */
    private String no;
    /**
     * 状态：10待确认 20已确认 30已生成采购单
     */
    private Integer status;
    /**
     * 仓库ID
     */
    private Long warehouseId;
    /**
     * Department id.
     */
    private Long deptId;
    /**
     * 建议时间
     */
    private LocalDateTime suggestTime;
    /**
     * 合计数量
     */
    private BigDecimal totalCount;
    /**
     * 合计金额
     */
    private BigDecimal totalPrice;
    /**
     * 生成的采购订单ID
     */
    private Long purchaseOrderId;

}
