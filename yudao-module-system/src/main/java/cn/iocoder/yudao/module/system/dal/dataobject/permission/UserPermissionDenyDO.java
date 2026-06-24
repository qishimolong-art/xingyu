package cn.iocoder.yudao.module.system.dal.dataobject.permission;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@TableName("system_user_permission_deny")
@KeySequence("system_user_permission_deny_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class UserPermissionDenyDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long userId;

    private String permission;

}
