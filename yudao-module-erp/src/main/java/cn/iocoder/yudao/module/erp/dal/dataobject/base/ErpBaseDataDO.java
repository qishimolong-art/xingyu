package cn.iocoder.yudao.module.erp.dal.dataobject.base;

import lombok.*;
import com.baomidou.mybatisplus.annotation.*;
import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;

/**
 * ERP 基础数据 DO
 *
 * @author 芋道源码
 */
@TableName("erp_base_data")
@KeySequence("erp_base_data_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpBaseDataDO extends BaseDO {

    /**
     * 编号
     */
    @TableId
    private Long id;
    /**
     * 数据类型（region/category/supplier_type/logistics_company）
     */
    private String type;
    /**
     * 名称
     */
    private String name;
    /**
     * 稳定业务编码
     */
    private String code;
    /**
     * 排序
     */
    private Integer sort;
    /**
     * 状态（0禁用 1启用）
     */
    private Integer status;
    /**
     * 备注
     */
    private String remark;

}
