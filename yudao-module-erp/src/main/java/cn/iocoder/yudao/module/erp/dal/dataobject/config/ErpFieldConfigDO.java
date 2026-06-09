package cn.iocoder.yudao.module.erp.dal.dataobject.config;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * ERP 字段必填配置 DO
 *
 * @author Claude
 */
@TableName("erp_field_config")
@KeySequence("erp_field_config_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpFieldConfigDO extends BaseDO {

    /** 配置编号 */
    @TableId
    private Long id;
    /** 模块标识（purchase_order / purchase_in / purchase_return / supplier） */
    private String moduleKey;
    /** 字段名（对应前端 schema 的 fieldName） */
    private String fieldName;
    /** 字段中文名（仅展示用） */
    private String fieldLabel;
    /** 是否必填 */
    private Boolean required;
    /** 是否显示 */
    @TableField(exist = false)
    private Boolean visible;
    /** 排序 */
    private Integer sort;
}
