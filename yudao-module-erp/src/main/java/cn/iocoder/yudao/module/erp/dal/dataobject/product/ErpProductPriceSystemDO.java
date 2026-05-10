package cn.iocoder.yudao.module.erp.dal.dataobject.product;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;

/**
 * ERP 产品-价格体系关联 DO
 *
 * @author Claude
 */
@TableName("erp_product_price_system")
@KeySequence("erp_product_price_system_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpProductPriceSystemDO extends BaseDO {

    /** 编号 */
    @TableId
    private Long id;
    /** 产品编号（关联 {@link ErpProductDO#getId()}） */
    private Long productId;
    /** 价格体系编号（关联 {@link ErpPriceSystemDO#getId()}） */
    private Long priceSystemId;
    /** 单价 */
    private BigDecimal price;
}
