package cn.iocoder.yudao.module.system.controller.admin.user.vo.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.Set;

@Schema(description = "管理后台 - 用户可访问租户范围 Response VO")
@Data
public class TenantVisitScopeRespVO {

    @Schema(description = "是否已经配置访问范围")
    private Boolean configured;

    @Schema(description = "范围类型：ALL 或 CUSTOM")
    private String scopeType;

    @Schema(description = "CUSTOM 模式下允许访问的租户编号")
    private Set<Long> tenantIds = Collections.emptySet();

    @Schema(description = "可配置租户选项")
    private List<TenantOption> tenantOptions = Collections.emptyList();

    @Data
    public static class TenantOption {
        private Long id;
        private String name;
        private Boolean available;
        private String unavailableReason;
    }

}
