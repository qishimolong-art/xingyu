package cn.iocoder.yudao.module.system.dal.dataobject.permission;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("system_field_definition")
@KeySequence("system_field_definition_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class FieldDefinitionDO extends TenantBaseDO {

    @TableId
    private Long id;

    private String module;

    private String fieldKey;

    private String fieldLabel;

    private String fieldGroup;

    private Integer sort;

}
