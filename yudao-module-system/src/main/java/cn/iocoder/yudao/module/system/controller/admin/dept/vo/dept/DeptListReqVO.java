package cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Schema(description = "管理后台 - 部门列表 Request VO")
@Data
public class DeptListReqVO {

    @Schema(description = "部门名称，模糊匹配", example = "芋道")
    private String name;

    @Schema(description = "展示状态，参见 CommonStatusEnum 枚举类", example = "1")
    private Integer status;

    @Schema(description = "负责人用户编号", example = "1")
    private Long leaderUserId;

    @Schema(description = "负责人用户昵称，模糊匹配", example = "张三")
    private String leaderUserName;

    @Schema(description = "排序字段", example = "sort")
    private String orderField;

    @Schema(description = "排序方向", example = "asc")
    private String orderDirection;

}
