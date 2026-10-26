package cn.iocoder.yudao.module.system.dal.dataobject.tenant;

import cn.iocoder.yudao.framework.tenant.core.db.TenantBaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Set;

/** 平台用户可访问租户范围。 */
@TableName(value = "system_user_tenant_visit_scope", autoResultMap = true)
@KeySequence("system_user_tenant_visit_scope_seq")
@Data
@EqualsAndHashCode(callSuper = true)
public class UserTenantVisitScopeDO extends TenantBaseDO {

    @TableId
    private Long id;

    private Long userId;

    /** ALL 或 CUSTOM。 */
    private String scopeType;

    /** CUSTOM 模式下允许访问的租户编号。 */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private Set<Long> tenantIds;

}
