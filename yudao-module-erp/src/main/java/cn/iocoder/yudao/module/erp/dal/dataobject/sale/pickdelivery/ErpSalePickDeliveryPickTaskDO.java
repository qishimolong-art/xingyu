package cn.iocoder.yudao.module.erp.dal.dataobject.sale.pickdelivery;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@TableName("erp_sale_pick_delivery_pick_task")
@KeySequence("erp_sale_pick_delivery_pick_task_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ErpSalePickDeliveryPickTaskDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long orderId;
    private Long saleOutId;
    private String saleOutNo;
    private Long customerId;
    private String customerName;
    private Long warehouseId;
    private String warehouseName;

    private Integer status;
    private Integer totalItemCount;
    private Integer pickedItemCount;
    private LocalDateTime latestPickTime;
    private LocalDateTime completeTime;

}
