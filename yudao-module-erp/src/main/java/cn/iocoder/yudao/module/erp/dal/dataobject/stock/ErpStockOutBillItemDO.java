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
import lombok.experimental.Accessors;

import java.math.BigDecimal;

/**
 * ERP stock out bill item DO.
 */
@TableName("erp_stock_out_bill_item")
@KeySequence("erp_stock_out_bill_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ErpStockOutBillItemDO extends BaseDO {

    @TableId
    private Long id;

    private Long billId;

    private Long sourceId;

    private Long sourceItemId;

    private String sourceNo;

    private Long warehouseId;

    private Long productId;

    private Long productUnitId;

    private BigDecimal productPrice;

    private BigDecimal count;

    private BigDecimal pickedCount;

    private Integer status;

    private Integer packageQty;

    private Integer wholeQty;

    private String warehousePosition;

    private String drawingNo;

    private String batchNo;

    private String barCode;

    private String brand;

    private String vehicleModel;

    private String originPlace;

    private String remark;

}
