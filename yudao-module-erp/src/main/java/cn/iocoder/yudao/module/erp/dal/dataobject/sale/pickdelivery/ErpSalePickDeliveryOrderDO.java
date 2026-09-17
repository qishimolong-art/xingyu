package cn.iocoder.yudao.module.erp.dal.dataobject.sale.pickdelivery;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@TableName("erp_sale_pick_delivery_order")
@KeySequence("erp_sale_pick_delivery_order_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ErpSalePickDeliveryOrderDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long saleOutId;
    private String saleOutNo;
    private Long customerId;
    private String customerName;
    private Long deptId;

    private Integer pickStatus;
    private Integer deliveryStatus;
    private Integer totalItemCount;
    private Integer pickedItemCount;
    private Integer deliveredItemCount;

    private LocalDateTime latestPickTime;
    private LocalDateTime latestDeliveryTime;
    private LocalDateTime pickCompleteTime;
    private LocalDateTime deliveryCompleteTime;

}
