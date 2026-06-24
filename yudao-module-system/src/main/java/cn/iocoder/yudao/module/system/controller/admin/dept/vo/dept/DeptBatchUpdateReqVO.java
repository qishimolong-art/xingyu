package cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Schema(description = "管理后台 - 部门批量修改 Request VO")
@Data
public class DeptBatchUpdateReqVO {

    @Schema(description = "部门编号列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "请选择要批量修改的部门")
    private List<Long> ids;

    @Schema(description = "是否修改上级部门", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean updateParentId;

    @Schema(description = "上级部门编号，0 表示顶级部门")
    private Long parentId;

    @Schema(description = "是否修改负责人", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean updateLeaderUserId;

    @Schema(description = "负责人用户编号，null 表示清空负责人")
    private Long leaderUserId;

    @Schema(description = "是否修改状态", requiredMode = Schema.RequiredMode.REQUIRED)
    private Boolean updateStatus;

    @Schema(description = "状态，参见 CommonStatusEnum 枚举")
    @InEnum(value = CommonStatusEnum.class, message = "修改状态必须是 {value}")
    private Integer status;

    @AssertTrue(message = "请至少选择一个需要修改的字段")
    public boolean isAnyFieldUpdated() {
        return Boolean.TRUE.equals(updateParentId)
                || Boolean.TRUE.equals(updateLeaderUserId)
                || Boolean.TRUE.equals(updateStatus);
    }

    @AssertTrue(message = "请选择要修改的状态")
    public boolean isStatusValid() {
        return !Boolean.TRUE.equals(updateStatus) || status != null;
    }

}
