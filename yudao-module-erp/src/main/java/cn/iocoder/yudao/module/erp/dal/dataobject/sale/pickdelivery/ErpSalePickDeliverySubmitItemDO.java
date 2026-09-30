package cn.iocoder.yudao.module.erp.dal.dataobject.sale.pickdelivery;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.*;
import lombok.*;
import lombok.experimental.Accessors;
import java.math.BigDecimal;

@TableName("erp_sale_pick_delivery_submit_item")
@KeySequence("erp_sale_pick_delivery_submit_item_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class ErpSalePickDeliverySubmitItemDO extends TenantBaseDO {
    @TableId
    private Long id;
    private Long submitId;
    private Long itemId;
    private String productCode;
    private String productName;
    private BigDecimal quantity;
}
