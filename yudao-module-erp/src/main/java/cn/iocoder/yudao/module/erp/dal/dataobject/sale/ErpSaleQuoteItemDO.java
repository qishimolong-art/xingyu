package cn.iocoder.yudao.module.erp.dal.dataobject.sale;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * ERP 报价订单项 DO
 */
@TableName("erp_sale_quote_items")
@KeySequence("erp_sale_quote_items_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpSaleQuoteItemDO extends BaseDO {

    @TableId
    private Long id;
    private Long quoteId;
    private Long productId;
    private Long productUnitId;
    private Long warehouseId;
    private BigDecimal productPrice;
    private BigDecimal count;
    /**
     * 是否赠品
     */
    private Boolean giftFlag;
    private BigDecimal convertedCount;
    private BigDecimal totalPrice;
    private BigDecimal taxPercent;
    private BigDecimal taxPrice;
    private String warehousePosition;
    private String drawingNo;
    private String batchNo;
    private String barCode;
    private String brand;
    private String vehicleModel;
    private String originPlace;
    private String standard;
    private String remark;

}
