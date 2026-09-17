package cn.iocoder.yudao.module.erp.dal.dataobject.sale.pickdelivery;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

@TableName("erp_sale_pick_delivery_submit")
@KeySequence("erp_sale_pick_delivery_submit_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ErpSalePickDeliverySubmitDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long orderId;
    private Long pickTaskId;
    private Long saleOutId;
    private Integer type;
    private Long submitUserId;
    private LocalDateTime submitTime;
    private Integer itemCount;
    private String remark;

}
