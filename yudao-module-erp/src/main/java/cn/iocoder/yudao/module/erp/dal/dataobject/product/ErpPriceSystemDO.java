package cn.iocoder.yudao.module.erp.dal.dataobject.product;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * ERP 价格体系 DO
 *
 * @author Claude
 */
@TableName("erp_price_system")
@KeySequence("erp_price_system_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpPriceSystemDO extends BaseDO {

    /** 价格体系编号 */
    @TableId
    private Long id;
    /**
     * Department id.
     */
    private Long deptId;
    /** 编码（唯一，如 "PS001"） */
    private String code;
    /** 名称（如 "批发价"、"零售价"） */
    private String name;
    /** 状态：0=启用，1=停用（枚举 {@link cn.iocoder.yudao.framework.common.enums.CommonStatusEnum}） */
    private Integer status;
    /** 排序 */
    private Integer sort;
    /** 备注 */
    private String remark;
}
