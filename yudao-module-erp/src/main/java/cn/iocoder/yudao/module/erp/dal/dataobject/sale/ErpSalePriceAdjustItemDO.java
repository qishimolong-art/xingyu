package cn.iocoder.yudao.module.erp.dal.dataobject.sale;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * ERP 销售调价单明细 DO
 *
 * @author 汽配ERP
 */
@TableName("erp_sale_price_adjust_item")
@KeySequence("erp_sale_price_adjust_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpSalePriceAdjustItemDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 调价单ID
     */
    private Long adjustId;
    /**
     * 销售单号(关联)
     */
    private String saleOutNo;
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
     * 出库数
     */
    private BigDecimal outCount;
    /**
     * 原售价
     */
    private BigDecimal oldPrice;
    /**
     * 调后价
     */
    private BigDecimal newPrice;
    /**
     * 调价金额
     */
    private BigDecimal adjustPrice;
    /**
     * 产品ID
     */
    private Long productId;
    /**
     * 关联销售出库项ID
     */
    private Long saleOutItemId;
    /**
     * 关联销售单ID
     */
    private Long saleOutId;
    /**
     * 调价原因
     */
    private String adjustReason;
    /**
     * 备注
     */
    private String itemRemark;

}
