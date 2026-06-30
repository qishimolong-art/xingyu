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
 * ERP stock out bill picking record DO.
 */
@TableName("erp_stock_out_bill_pick_record")
@KeySequence("erp_stock_out_bill_pick_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ErpStockOutBillPickRecordDO extends BaseDO {

    @TableId
    private Long id;

    private Long billId;

    private Long billItemId;

    private Long sourceId;

    private Long sourceItemId;

    private Long productId;

    private Long warehouseId;

    private BigDecimal pickCount;

    private Long pickUserId;

    private String pickUserName;

    private LocalDateTime pickTime;

    private String remark;

}
