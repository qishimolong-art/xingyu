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
import java.time.LocalDateTime;

/**
 * ERP 入仓单提货记录 DO.
 */
@TableName("erp_stock_in_bill_pickup_record")
@KeySequence("erp_stock_in_bill_pickup_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ErpStockInBillPickupRecordDO extends BaseDO {

    @TableId
    private Long id;

    private Long billId;

    private Long billItemId;

    private Long sourceId;

    private Long sourceItemId;

    private Long productId;

    private Long warehouseId;

    private BigDecimal pickupCount;

    private Long pickupUserId;

    private String pickupUserName;

    private LocalDateTime pickupTime;

    private String remark;

}
