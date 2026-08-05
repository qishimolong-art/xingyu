package cn.iocoder.yudao.module.system.service.permission.formdata;

import cn.iocoder.yudao.module.system.enums.permission.FormPermissionRelationEnum;

import java.util.Collection;

public interface FormPermissionService {

    void syncRelation(String formType, Long formId, FormPermissionRelationEnum relation, Collection<Long> userIds);

    void grant(String formType, Long formId, FormPermissionRelationEnum relation, Long userId);

    void revoke(String formType, Long formId, FormPermissionRelationEnum relation, Long userId);

    void removeByForm(String formType, Long formId);

    boolean hasPermission(String formType, Long formId, Long userId);

}
