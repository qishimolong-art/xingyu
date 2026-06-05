package cn.iocoder.yudao.module.system.dal.dataobject.permission;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("system_role_field_permission")
@KeySequence("system_role_field_permission_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class RoleFieldPermissionDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long roleId;

    private Long fieldId;

    private Boolean hidden;

}
