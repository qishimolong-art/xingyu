package cn.iocoder.yudao.module.system.controller.admin.user.vo.user;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.framework.common.validation.Mobile;
import cn.iocoder.yudao.framework.dict.validation.InDict;
import cn.iocoder.yudao.module.system.enums.DictTypeConstants;
import cn.iocoder.yudao.module.system.enums.permission.DataScopeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.Set;

@Schema(description = "管理后台 - 用户批量修改 Request VO")
@Data
public class UserBatchUpdateReqVO {

    @Schema(description = "用户编号列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "请选择需要批量修改的用户")
    private List<Long> ids;

    @Schema(description = "是否修改用户昵称")
    private Boolean updateNickname;

    @Schema(description = "用户昵称", example = "芋艿")
    @Size(max = 30, message = "用户昵称长度不能超过30个字符")
    private String nickname;

    @Schema(description = "是否修改部门编号数组")
    private Boolean updateDeptIds;

    @Schema(description = "部门编号数组", example = "1")
    private Set<Long> deptIds;

    @Schema(description = "是否修改角色编号数组")
    private Boolean updateRoleIds;

    @Schema(description = "角色编号数组", example = "1")
    private Set<Long> roleIds;

    @Schema(description = "是否修改用户邮箱")
    private Boolean updateEmail;

    @Schema(description = "用户邮箱", example = "yudao@iocoder.cn")
    @Email(message = "邮箱格式不正确")
    @Size(max = 50, message = "邮箱长度不能超过 50 个字符")
    private String email;

    @Schema(description = "是否修改手机号码")
    private Boolean updateMobile;

    @Schema(description = "手机号码", example = "15601691300")
    @Mobile
    private String mobile;

    @Schema(description = "是否修改用户性别")
    private Boolean updateSex;

    @Schema(description = "用户性别，参见 SexEnum 枚举类", example = "1")
    private Integer sex;

    @Schema(description = "是否修改状态")
    private Boolean updateStatus;

    @Schema(description = "状态，见 CommonStatusEnum 枚举", example = "1")
    @InEnum(value = CommonStatusEnum.class, message = "修改状态必须是 {value}")
    @InDict(type = DictTypeConstants.COMMON_STATUS)
    private Integer status;

    @Schema(description = "是否修改数据范围")
    private Boolean updateDataScope;

    @Schema(description = "数据范围，见 DataScopeEnum 枚举", example = "1")
    private Integer dataScope;

    @Schema(description = "数据范围(指定部门数组)", example = "1")
    private Set<Long> dataScopeDeptIds;

    @Schema(description = "是否修改备注")
    private Boolean updateRemark;

    @Schema(description = "备注", example = "我是一个用户")
    private String remark;

    @AssertTrue(message = "请至少选择一个需要修改的字段")
    public boolean isAnyFieldUpdated() {
        return Boolean.TRUE.equals(updateNickname)
                || Boolean.TRUE.equals(updateDeptIds)
                || Boolean.TRUE.equals(updateRoleIds)
                || Boolean.TRUE.equals(updateEmail)
                || Boolean.TRUE.equals(updateMobile)
                || Boolean.TRUE.equals(updateSex)
                || Boolean.TRUE.equals(updateStatus)
                || Boolean.TRUE.equals(updateDataScope)
                || Boolean.TRUE.equals(updateRemark);
    }

    @AssertTrue(message = "请输入名称")
    public boolean isNicknameValid() {
        return !Boolean.TRUE.equals(updateNickname)
                || (nickname != null && !nickname.trim().isEmpty());
    }

    @AssertTrue(message = "请选择归属部门")
    public boolean isDeptIdsValid() {
        return !Boolean.TRUE.equals(updateDeptIds)
                || (deptIds != null && !deptIds.isEmpty());
    }

    @AssertTrue(message = "请选择角色")
    public boolean isRoleIdsValid() {
        return !Boolean.TRUE.equals(updateRoleIds)
                || (roleIds != null && !roleIds.isEmpty());
    }

    @AssertTrue(message = "请选择用户性别")
    public boolean isSexValid() {
        return !Boolean.TRUE.equals(updateSex) || sex != null;
    }

    @AssertTrue(message = "请选择用户状态")
    public boolean isStatusValid() {
        return !Boolean.TRUE.equals(updateStatus) || status != null;
    }

    @AssertTrue(message = "请选择数据范围")
    public boolean isDataScopeValid() {
        return !Boolean.TRUE.equals(updateDataScope) || dataScope != null;
    }

    @AssertTrue(message = "指定部门数据范围时，部门不能为空")
    public boolean isDataScopeDeptIdsValid() {
        return !Boolean.TRUE.equals(updateDataScope)
                || !DataScopeEnum.DEPT_CUSTOM.getScope().equals(dataScope)
                || (dataScopeDeptIds != null && !dataScopeDeptIds.isEmpty());
    }

    @AssertTrue(message = "请输入手机号码")
    public boolean isMobileValid() {
        return !Boolean.TRUE.equals(updateMobile)
                || (mobile != null && !mobile.trim().isEmpty());
    }

}
