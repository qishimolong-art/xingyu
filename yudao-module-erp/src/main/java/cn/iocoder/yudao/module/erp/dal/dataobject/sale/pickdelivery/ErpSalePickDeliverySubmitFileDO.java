package cn.iocoder.yudao.module.erp.dal.dataobject.sale.pickdelivery;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;
import lombok.experimental.Accessors;

@TableName("erp_sale_pick_delivery_submit_file")
@KeySequence("erp_sale_pick_delivery_submit_file_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
public class ErpSalePickDeliverySubmitFileDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long submitId;
    private String fileUrl;
    private String fileName;
    private String fileType;
    private Integer sort;

}
