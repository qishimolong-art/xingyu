package cn.iocoder.yudao.module.erp.dal.dataobject.autoorder;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * ERP 采购建议单明细 DO
 *
 * @author 汽配ERP
 */
@TableName("erp_purchase_suggestion_item")
@KeySequence("erp_purchase_suggestion_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpPurchaseSuggestionItemDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 建议单ID
     */
    private Long suggestionId;
    /**
     * 产品ID
     */
    private Long productId;
    /**
     * 当前库存
     */
    private BigDecimal currentStock;
    /**
     * 锁定库存
     */
    private BigDecimal lockStock;
    /**
     * 日均销量
     */
    private BigDecimal avgDailySale;
    /**
     * 建议采购量
     */
    private BigDecimal suggestCount;
    /**
     * 供应商ID
     */
    private Long supplierId;
    /**
     * 最近采购价
     */
    private BigDecimal lastPurchasePrice;

}
