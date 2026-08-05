package cn.iocoder.yudao.module.system.enums.permission;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum FormPermissionRelationEnum {

    CREATOR("creator", "创建人"),
    MENTIONED("mentioned", "相关人"),
    APPROVER("approver", "审批人"),
    CC("cc", "抄送人"),
    SHARED("shared", "手动分享");

    private final String code;
    private final String name;

}
