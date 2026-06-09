package cn.iocoder.yudao.module.system.dal.dataobject.permission;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Set;

@TableName(value = "system_role_form_data_scope", autoResultMap = true)
@KeySequence("system_role_form_data_scope_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class RoleFormDataScopeDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long roleId;

    /** 表单标识，对应 system_field_definition.module */
    private String formKey;

    /** 数据范围，参见 DataScopeEnum */
    private Integer dataScope;

    /** 自定义部门编号列表，仅 DEPT_CUSTOM 时使用 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Set<Long> dataScopeDeptIds;

}
