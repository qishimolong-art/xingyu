package cn.iocoder.yudao.module.system.controller.admin.permission.vo.deptpricefield;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Schema(description = "管理后台 - 更新配件价格查看权限 Request VO")
@Data
public class DeptPriceFieldUpdateReqVO {

    @Schema(description = "查询配置时返回的版本", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "配置版本不能为空")
    private String configVersion;

    @Schema(description = "发生变化的价格字段授权")
    @Valid
    @NotNull(message = "价格字段授权不能为空")
    @Size(max = 500, message = "单次最多保存 500 个价格字段")
    private List<Item> items;

    @Data
    public static class Item {

        @NotBlank(message = "价格字段编码不能为空")
        @Size(max = 100, message = "价格字段编码长度不能超过 100 个字符")
        private String fieldKey;

        @NotNull(message = "授权部门不能为空")
        @Size(max = 5000, message = "单个价格字段最多授权 5000 个部门")
        private List<Long> deptIds;
    }

}
