package cn.iocoder.yudao.module.erp.dal.dataobject.sale.pickdelivery;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@TableName("erp_sale_pick_delivery_item")
@KeySequence("erp_sale_pick_delivery_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ErpSalePickDeliveryItemDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long orderId;
    private Long pickTaskId;
    private Long saleOutId;
    private Long saleOutItemId;
    private Long warehouseId;
    private String warehouseName;
    private Long productId;
    private String productCode;
    private String productName;
    private String standard;
    private BigDecimal count;
    private String warehousePosition;
    private Integer packageQty;

    private Integer pickStatus;
    private Long pickUserId;
    private LocalDateTime pickTime;
    private Integer deliveryStatus;
    private Long deliveryUserId;
    private LocalDateTime deliveryTime;

}
