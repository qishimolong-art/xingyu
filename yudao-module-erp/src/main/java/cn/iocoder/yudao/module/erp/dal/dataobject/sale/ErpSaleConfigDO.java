package cn.iocoder.yudao.module.erp.dal.dataobject.sale;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * ERP 销售配置 DO
 */
@TableName("erp_sale_config")
@KeySequence("erp_sale_config_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpSaleConfigDO extends BaseDO {

    @TableId
    private Long id;

    /**
     * 配置类型，例如 CONTRACT_TYPE、ROUTE、FREIGHT_EXPLAIN。
     */
    private String configType;

    /**
     * 配置编码，同一配置类型下唯一。
     */
    private String code;

    /**
     * 配置名称
     */
    private String name;

    /**
     * 配置值。用于内部账户、集团信息等需要额外结构化内容的场景。
     */
    private String configValue;

    /**
     * 状态
     */
    private Integer status;

    /**
     * 排序
     */
    private Integer sort;

    /**
     * 备注
     */
    private String remark;

}
