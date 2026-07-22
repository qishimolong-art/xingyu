package cn.iocoder.yudao.module.system.controller.admin.permission.vo.deptpricefield;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 配件价格查看权限配置 Response VO")
@Data
public class DeptPriceFieldConfigRespVO {

    @Schema(description = "配置版本，用于防止并发覆盖")
    private String configVersion;

    @Schema(description = "动态价格字段")
    private List<Field> fields;

    @Schema(description = "租户内全部未删除部门，含停用部门")
    private List<Department> departments;

    @Data
    public static class Field {

        private String fieldKey;

        private String fieldLabel;

        private Integer sort;

        private List<Long> deptIds;

        private Boolean configured;
    }

    @Data
    public static class Department {

        private Long id;

        private String name;

        private Long parentId;

        private Integer sort;

        private Integer status;
    }

}
