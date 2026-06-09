package cn.iocoder.yudao.module.erp.dal.dataobject.product;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * ERP 配件通用件（可替代关系） DO
 *
 * 每条记录表示"当前配件 product_id 可被 universal_code 对应的通用件替代，针对 universal_vehicle 车型"。
 * 如一条通用件支持多个车型，则多条记录。
 *
 * @author 芋道源码
 */
@TableName("erp_product_universal")
@KeySequence("erp_product_universal_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpProductUniversalDO extends BaseDO {

    /**
     * 主键
     */
    @TableId
    private Long id;
    /**
     * Department id.
     */
    private Long deptId;
    /**
     * 主配件编号
     *
     * 关联 {@link ErpProductDO#getId()}
     */
    private Long productId;
    /**
     * 通用件编码（对应 {@link ErpProductDO#getCode()}）
     */
    private String universalCode;
    /**
     * 通用件名称（冗余，便于展示）
     */
    private String universalName;
    /**
     * 适用车型（单车型）
     */
    private String universalVehicle;

}
