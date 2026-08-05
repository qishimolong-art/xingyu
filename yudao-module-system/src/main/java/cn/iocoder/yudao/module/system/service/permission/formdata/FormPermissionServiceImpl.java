package cn.iocoder.yudao.module.system.service.permission.formdata;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.FormDataPermissionDO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.FormDataPermissionMapper;
import cn.iocoder.yudao.module.system.enums.permission.FormPermissionRelationEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FormPermissionServiceImpl implements FormPermissionService {

    @Resource
    private FormDataPermissionMapper formDataPermissionMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void syncRelation(String formType, Long formId, FormPermissionRelationEnum relation, Collection<Long> userIds) {
        validateKeys(formType, formId, relation);
        Set<Long> targetUserIds = userIds == null ? new HashSet<>() : userIds.stream()
                .filter(userId -> userId != null && userId > 0)
                .collect(Collectors.toSet());
        if (CollUtil.isEmpty(targetUserIds)) {
            formDataPermissionMapper.deleteByFormAndRelation(formType, formId, relation.getCode());
            return;
        }

        List<FormDataPermissionDO> exists = formDataPermissionMapper.selectListByFormAndRelation(
                formType, formId, relation.getCode());
        Set<Long> existsUserIds = exists.stream().map(FormDataPermissionDO::getUserId).collect(Collectors.toSet());
        for (FormDataPermissionDO permission : exists) {
            if (!targetUserIds.contains(permission.getUserId())) {
                formDataPermissionMapper.deleteByFormUserRelation(formType, formId, relation.getCode(), permission.getUserId());
            }
        }
        for (Long userId : targetUserIds) {
            if (!existsUserIds.contains(userId)) {
                insertIgnoreDuplicate(formType, formId, relation, userId);
            }
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void grant(String formType, Long formId, FormPermissionRelationEnum relation, Long userId) {
        validateKeys(formType, formId, relation);
        if (userId == null || userId <= 0) {
            return;
        }
        if (formDataPermissionMapper.selectOneByFormUserRelation(formType, formId, userId, relation.getCode()) != null) {
            return;
        }
        insertIgnoreDuplicate(formType, formId, relation, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void revoke(String formType, Long formId, FormPermissionRelationEnum relation, Long userId) {
        validateKeys(formType, formId, relation);
        if (userId == null) {
            return;
        }
        formDataPermissionMapper.deleteByFormUserRelation(formType, formId, relation.getCode(), userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeByForm(String formType, Long formId) {
        FormPermissionIdentifierUtils.checkIdentifier(formType, "表名");
        if (formId == null) {
            return;
        }
        formDataPermissionMapper.deleteByForm(formType, formId);
    }

    @Override
    public boolean hasPermission(String formType, Long formId, Long userId) {
        FormPermissionIdentifierUtils.checkIdentifier(formType, "表名");
        if (formId == null || userId == null) {
            return false;
        }
        return formDataPermissionMapper.selectCountByFormAndUser(formType, formId, userId) > 0;
    }

    private void insertIgnoreDuplicate(String formType, Long formId, FormPermissionRelationEnum relation, Long userId) {
        FormDataPermissionDO permission = new FormDataPermissionDO();
        permission.setFormType(formType);
        permission.setFormId(formId);
        permission.setRelation(relation.getCode());
        permission.setUserId(userId);
        try {
            formDataPermissionMapper.insert(permission);
        } catch (DuplicateKeyException ex) {
            log.debug("[insertIgnoreDuplicate][formType({}) formId({}) relation({}) userId({}) 已存在]",
                    formType, formId, relation.getCode(), userId);
        }
    }

    private void validateKeys(String formType, Long formId, FormPermissionRelationEnum relation) {
        FormPermissionIdentifierUtils.checkIdentifier(formType, "表名");
        if (formId == null) {
            throw new IllegalArgumentException("表单ID不能为空");
        }
        if (relation == null) {
            throw new IllegalArgumentException("关系类型不能为空");
        }
    }

}
