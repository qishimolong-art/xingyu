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
     * 调价单编号（FK 到 {@link ErpPurchasePriceAdjustDO#getId()}）
     */
    private Long adjustId;
    /**
     * 采购入库单编号（FK 到 {@link ErpPurchaseInDO#getId()}）
     */
    private Long inId;
    /**
     * 采购入库单号（冗余，便于展示）
     */
    private String inNo;
    /**
     * 采购入库项编号（FK 到 {@link ErpPurchaseInItemDO#getId()}）
     *
     * "按入库单"方式时：该入库单下每条明细都展开成一行子表项
     */
    private Long inItemId;
    /**
     * 产品编号
     */
    private Long productId;
    /**
     * 仓库编号
     */
    private Long warehouseId;
    /**
     * 调价前单价（= 当前 inItem.productPrice）
     */
    private BigDecimal oldPrice;
    /**
     * 调价后单价
     */
    private BigDecimal newPrice;
    /**
     * 入库数量快照
     */
    private BigDecimal count;
    /**
     * 调价比率（"按入库单"方式填；"添加明细"方式为 null）
     */
    private BigDecimal adjustRatio;
    /**
     * 调价金额 = (newPrice - oldPrice) × count
     */
    private BigDecimal adjustPrice;

    // ========== 产品冗余字段，便于列表展示 ==========
    /**
     * 配件编码
     */
    private String productCode;
    /**
     * 配件名称
     */
    private String productName;
    /**
     * 单位名称
     */
    private String productUnitName;
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
     * 图号
     */
    private String drawingNo;
    /**
     * 货架位（仓库内货架位，如 A-01-02）
     */
    private String warehousePosition;

}
