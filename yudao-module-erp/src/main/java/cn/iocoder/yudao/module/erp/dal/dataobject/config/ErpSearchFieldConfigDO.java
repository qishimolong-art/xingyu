package cn.iocoder.yudao.module.erp.dal.dataobject.config;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * ERP 搜索字段配置 DO
 */
@TableName("erp_search_field_config")
@KeySequence("erp_search_field_config_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpSearchFieldConfigDO extends BaseDO {

    /** 配置编号 */
    @TableId
    private Long id;
    /** 模块标识 */
    private String moduleKey;
    /** 字段名（对应前端搜索 schema 的 fieldName） */
    private String fieldName;
    /** 字段中文名（仅展示用） */
    private String fieldLabel;
    /** 前端组件类型 */
    private String component;
    /** 是否启用 */
    private Boolean enabled;
    /** 排序 */
    private Integer sort;

}
