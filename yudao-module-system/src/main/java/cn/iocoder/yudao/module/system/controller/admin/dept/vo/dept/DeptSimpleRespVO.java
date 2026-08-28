package cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Schema(description = "管理后台 - 部门精简信息 Response VO")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DeptSimpleRespVO {

    @Schema(description = "部门编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "部门名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "芋道")
    private String name;

    @Schema(description = "父部门 ID", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long parentId;

    @Schema(description = "联系电话", example = "15601691000")
    private String phone;

    @Schema(description = "详细地址", example = "四川省成都市高新区天府大道")
    private String address;

    @Schema(description = "经度", example = "104.065735")
    private BigDecimal longitude;

    @Schema(description = "纬度", example = "30.659462")
    private BigDecimal latitude;

    @Schema(description = "地图显示名称", example = "兴宇总部")
    private String mapName;

    public DeptSimpleRespVO(Long id, String name, Long parentId) {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
    }

}
