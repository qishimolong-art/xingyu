package cn.iocoder.yudao.module.erp.dal.dataobject.purchase;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * ERP 采购调价单明细 DO
 *
 * @author 汽配ERP
 */
@TableName("erp_purchase_price_adjust_item")
@KeySequence("erp_purchase_price_adjust_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpPurchasePriceAdjustItemDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 调价单ID
     */
    private Long adjustId;
    /**
     * 采购单号(关联)
     */
    private String purchaseInNo;
    /**
     * 配件编码
     */
    private String partCode;
    /**
     * 配件名称
     */
    private String partName;
    /**
     * 车型
     */
    private String vehicleModel;
    /**
     * 规格
     */
    private String standard;
    /**
     * 特征码
     */
    private String featureCode;
    /**
     * 产地
     */
    private String originPlace;
    /**
     * 品牌
     */
    private String brand;
    /**
     * 单位
     */
    private String unit;
    /**
     * 图号
     */
    private String drawingNo;
    /**
     * 入库数
     */
    private BigDecimal inCount;
    /**
     * 进价(原价)
     */
    private BigDecimal oldPrice;
    /**
     * 调后价
     */
    private BigDecimal newPrice;
    /**
     * 调价金额=(new-old)*inCount
     */
    private BigDecimal adjustPrice;
    /**
     * 货架
     */
    private String shelf;
    /**
     * 产品ID
     */
    private Long productId;
    /**
     * 关联采购入库项ID
     */
    private Long purchaseInItemId;

}
