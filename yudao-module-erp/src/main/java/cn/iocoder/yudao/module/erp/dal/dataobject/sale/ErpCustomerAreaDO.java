package cn.iocoder.yudao.module.erp.dal.dataobject.sale;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

@TableName("erp_customer_area")
@KeySequence("erp_customer_area_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpCustomerAreaDO extends BaseDO {

    @TableId
    private Long id;
    private Long customerId;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String mapAddress;
    private String detailAddress;
    private Boolean defaulted;
    private String remark;

}
