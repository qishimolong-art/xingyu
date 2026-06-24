package cn.iocoder.yudao.module.system.controller.admin.user.vo.user;

import cn.hutool.core.util.ObjectUtil;
import cn.iocoder.yudao.framework.common.validation.Mobile;
import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.system.framework.operatelog.core.DeptParseFunction;
import cn.iocoder.yudao.module.system.framework.operatelog.core.SexParseFunction;
import cn.iocoder.yudao.module.system.enums.permission.DataScopeEnum;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.mzt.logapi.starter.annotation.DiffLogField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.*;
import java.util.Set;

@Schema(description = "管理后台 - 用户创建/修改 Request VO")
@Data
public class UserSaveReqVO {

    @Schema(description = "用户编号", example = "1024")
    private Long id;

    @Schema(description = "用户账号，固定等于手机号码", example = "15601691300")
    @Pattern(regexp = "^[a-zA-Z0-9]{4,30}$", message = "用户账号由 数字、字母 组成")
    @Size(min = 4, max = 30, message = "用户账号长度为 4-30 个字符")
    @DiffLogField(name = "用户账号")
    private String username;

    @Schema(description = "用户昵称", requiredMode = Schema.RequiredMode.REQUIRED, example = "芋艿")
    @Size(max = 30, message = "用户昵称长度不能超过30个字符")
    @DiffLogField(name = "用户昵称")
    private String nickname;

    @Schema(description = "备注", example = "我是一个用户")
    @DiffLogField(name = "备注")
    private String remark;

    @Schema(description = "部门编号", example = "我是一个用户")
    @DiffLogField(name = "部门", function = DeptParseFunction.NAME)
    private Long deptId;

    @Schema(description = "部门编号数组", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Set<Long> deptIds;

    @Schema(description = "角色编号数组", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Set<Long> roleIds;

    @Schema(description = "数据范围", example = "1")
    @InEnum(value = DataScopeEnum.class, message = "数据范围必须是 {value}")
    @DiffLogField(name = "数据范围")
    private Integer dataScope;

    @Schema(description = "数据范围(指定部门数组)", example = "1")
    private Set<Long> dataScopeDeptIds;

    @Schema(description = "用户邮箱", example = "yudao@iocoder.cn")
    @Email(message = "邮箱格式不正确")
    @Size(max = 50, message = "邮箱长度不能超过 50 个字符")
    @DiffLogField(name = "用户邮箱")
    private String email;

    @Schema(description = "手机号码", requiredMode = Schema.RequiredMode.REQUIRED, example = "15601691300")
    @Mobile
    @DiffLogField(name = "手机号码")
    private String mobile;

    @Schema(description = "用户性别，参见 SexEnum 枚举类", example = "1")
    @DiffLogField(name = "用户性别", function = SexParseFunction.NAME)
    private Integer sex;

    @Schema(description = "用户头像", example = "https://www.iocoder.cn/xxx.png")
    @DiffLogField(name = "用户头像")
    private String avatar;

    // ========== 仅【创建】时，需要传递的字段 ==========

    @Schema(description = "密码", requiredMode = Schema.RequiredMode.REQUIRED, example = "123456")
    @Length(min = 4, max = 16, message = "密码长度为 4-16 位")
    private String password;

    @AssertTrue(message = "密码不能为空")
    @JsonIgnore
    public boolean isPasswordValid() {
        return id != null // 修改时，不需要传递
                || (ObjectUtil.isAllNotEmpty(password)); // 新增时，必须都传递 password
    }

    @AssertTrue(message = "指定部门数据范围时，部门不能为空")
    @JsonIgnore
    public boolean isDataScopeDeptIdsValid() {
        return !ObjectUtil.equals(dataScope, DataScopeEnum.DEPT_CUSTOM.getScope())
                || ObjectUtil.isNotEmpty(dataScopeDeptIds);
    }

}
