package cn.iocoder.yudao.module.system.controller.admin.user.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.AssertTrue;
import java.util.Collections;
import java.util.Set;

@Schema(description = "管理后台 - 用户可访问租户范围保存 Request VO")
@Data
public class TenantVisitScopeSaveReqVO {

    @Schema(description = "范围类型：ALL 或 CUSTOM；clear=true 时可不传")
    private String scopeType;

    @Schema(description = "CUSTOM 模式下允许访问的租户编号")
    private Set<Long> tenantIds = Collections.emptySet();

    @Schema(description = "是否清空租户访问范围")
    private Boolean clear;

    @AssertTrue(message = "清空租户访问范围时不能同时设置范围类型或租户")
    public boolean isClearRequestValid() {
        if (!Boolean.TRUE.equals(clear)) {
            return true;
        }
        return (scopeType == null || scopeType.trim().isEmpty())
                && (tenantIds == null || tenantIds.isEmpty());
    }

    @AssertTrue(message = "租户访问范围类型不能为空")
    public boolean isScopeTypePresent() {
        return Boolean.TRUE.equals(clear) || scopeType != null && !scopeType.trim().isEmpty();
    }

}
