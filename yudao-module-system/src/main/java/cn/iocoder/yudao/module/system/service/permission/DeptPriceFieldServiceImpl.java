package cn.iocoder.yudao.module.system.service.permission;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.crypto.digest.DigestUtil;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.framework.tenant.core.context.TenantContextHolder;
import cn.iocoder.yudao.module.system.controller.admin.dept.vo.dept.DeptListReqVO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.deptpricefield.DeptPriceFieldConfigRespVO;
import cn.iocoder.yudao.module.system.controller.admin.permission.vo.deptpricefield.DeptPriceFieldUpdateReqVO;
import cn.iocoder.yudao.module.system.dal.dataobject.dept.DeptDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.DeptPriceFieldDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.FieldDefinitionDO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.DeptPriceFieldMapper;
import cn.iocoder.yudao.module.system.service.dept.DeptService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.DEPT_PRICE_FIELD_CONFIG_CHANGED;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.DEPT_PRICE_FIELD_DUPLICATE_FIELD;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.DEPT_PRICE_FIELD_INVALID_DEPT;
import static cn.iocoder.yudao.module.system.enums.ErrorCodeConstants.DEPT_PRICE_FIELD_INVALID_FIELD;

@Service
public class DeptPriceFieldServiceImpl implements DeptPriceFieldService {

    @Resource
    private DeptPriceFieldMapper deptPriceFieldMapper;
    @Resource
    private ProductPriceFieldCatalogService productPriceFieldCatalogService;
    @Resource
    private DeptService deptService;

    @Override
    public DeptPriceFieldConfigRespVO getConfig() {
        return buildConfig(loadSnapshot(null));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateConfig(DeptPriceFieldUpdateReqVO reqVO) {
        Long tenantId = TenantContextHolder.getRequiredTenantId();
        // 锁住本租户价格字段目录，使并发保存串行校验配置摘要，避免后提交者静默覆盖。
        List<FieldDefinitionDO> lockedDefinitions = productPriceFieldCatalogService.getPriceFieldsForUpdate(tenantId);
        ConfigSnapshot snapshot = loadSnapshot(lockedDefinitions);
        if (!Objects.equals(reqVO.getConfigVersion(), calculateVersion(snapshot))) {
            throw exception(DEPT_PRICE_FIELD_CONFIG_CHANGED);
        }
        if (CollUtil.isEmpty(reqVO.getItems())) {
            return;
        }

        Map<String, FieldDefinitionDO> definitionMap = snapshot.definitions.stream()
                .collect(Collectors.toMap(item -> ProductPriceFieldKeys.normalize(item.getFieldKey()),
                        item -> item, (a, b) -> a, LinkedHashMap::new));
        Set<String> submittedFieldKeys = new HashSet<>();
        Map<String, Set<Long>> desiredDeptIdsByField = new LinkedHashMap<>();
        Set<Long> allSubmittedDeptIds = new LinkedHashSet<>();
        for (DeptPriceFieldUpdateReqVO.Item item : reqVO.getItems()) {
            String fieldKey = ProductPriceFieldKeys.normalize(item.getFieldKey());
            if (!submittedFieldKeys.add(fieldKey)) {
                throw exception(DEPT_PRICE_FIELD_DUPLICATE_FIELD, fieldKey);
            }
            if (!definitionMap.containsKey(fieldKey)) {
                throw exception(DEPT_PRICE_FIELD_INVALID_FIELD, fieldKey);
            }
            Set<Long> deptIds = item.getDeptIds().stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            desiredDeptIdsByField.put(fieldKey, deptIds);
            allSubmittedDeptIds.addAll(deptIds);
        }
        validateDeptIds(allSubmittedDeptIds);

        Map<String, Set<Long>> existingDeptIdsByField = snapshot.relations.stream()
                .filter(item -> submittedFieldKeys.contains(ProductPriceFieldKeys.normalize(item.getFieldKey())))
                .collect(Collectors.groupingBy(item -> ProductPriceFieldKeys.normalize(item.getFieldKey()),
                        LinkedHashMap::new,
                        Collectors.mapping(DeptPriceFieldDO::getDeptId, Collectors.toSet())));
        Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
        String operator = String.valueOf(loginUserId != null ? loginUserId : 0L);
        desiredDeptIdsByField.forEach((fieldKey, desiredDeptIds) -> {
            Set<Long> existingDeptIds = existingDeptIdsByField.getOrDefault(fieldKey, Collections.emptySet());
            Set<Long> createDeptIds = new LinkedHashSet<>(desiredDeptIds);
            createDeptIds.removeAll(existingDeptIds);
            createDeptIds.forEach(deptId -> deptPriceFieldMapper.restoreOrInsert(
                    deptId, fieldKey, operator, tenantId));

            Set<Long> deleteDeptIds = new LinkedHashSet<>(existingDeptIds);
            deleteDeptIds.removeAll(desiredDeptIds);
            if (CollUtil.isNotEmpty(deleteDeptIds)) {
                deptPriceFieldMapper.softDeleteByFieldKeyAndDeptIds(
                        fieldKey, deleteDeptIds, operator, tenantId);
            }
        });
    }

    @Override
    public List<String> getHiddenPriceFields(Collection<Long> enabledDeptIds) {
        List<FieldDefinitionDO> definitions = productPriceFieldCatalogService.getPriceFields();
        if (CollUtil.isEmpty(definitions)) {
            return Collections.emptyList();
        }
        Set<String> allowedFieldKeys = CollUtil.isEmpty(enabledDeptIds)
                ? Collections.emptySet()
                : deptPriceFieldMapper.selectListByDeptIds(enabledDeptIds).stream()
                        .map(DeptPriceFieldDO::getFieldKey)
                        .map(ProductPriceFieldKeys::normalize)
                        .collect(Collectors.toSet());
        List<String> hiddenFields = new ArrayList<>();
        for (FieldDefinitionDO definition : definitions) {
            String fieldKey = ProductPriceFieldKeys.normalize(definition.getFieldKey());
            if (!allowedFieldKeys.contains(fieldKey)) {
                ProductPriceFieldKeys.addHiddenField(hiddenFields, fieldKey);
            }
        }
        return hiddenFields;
    }

    @Override
    public void deleteByFieldKeys(Collection<String> fieldKeys) {
        if (CollUtil.isEmpty(fieldKeys)) {
            return;
        }
        Set<String> normalizedKeys = fieldKeys.stream()
                .filter(StrUtil::isNotBlank)
                .map(StrUtil::trim)
                .collect(Collectors.toSet());
        if (CollUtil.isNotEmpty(normalizedKeys)) {
            deptPriceFieldMapper.physicalDeleteByFieldKeys(
                    normalizedKeys, TenantContextHolder.getRequiredTenantId());
        }
    }

    private void validateDeptIds(Set<Long> deptIds) {
        if (CollUtil.isEmpty(deptIds)) {
            return;
        }
        Set<Long> existingDeptIds = deptService.getDeptList(deptIds).stream()
                .map(DeptDO::getId)
                .collect(Collectors.toSet());
        Set<Long> invalidDeptIds = new LinkedHashSet<>(deptIds);
        invalidDeptIds.removeAll(existingDeptIds);
        if (CollUtil.isNotEmpty(invalidDeptIds)) {
            throw exception(DEPT_PRICE_FIELD_INVALID_DEPT,
                    invalidDeptIds.stream().map(String::valueOf).collect(Collectors.joining(",")));
        }
    }

    private ConfigSnapshot loadSnapshot(List<FieldDefinitionDO> definitions) {
        List<FieldDefinitionDO> priceDefinitions = new ArrayList<>(definitions != null ? definitions
                : productPriceFieldCatalogService.getPriceFields());
        priceDefinitions.sort(Comparator
                .comparing(FieldDefinitionDO::getSort, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(FieldDefinitionDO::getId, Comparator.nullsLast(Long::compareTo)));
        List<DeptDO> departments = deptService.getDeptList(new DeptListReqVO()).stream()
                .sorted(Comparator.comparing(DeptDO::getSort, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(DeptDO::getId))
                .collect(Collectors.toList());
        Set<String> fieldKeys = priceDefinitions.stream()
                .map(FieldDefinitionDO::getFieldKey)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        List<DeptPriceFieldDO> relations = fieldKeys.isEmpty() ? new ArrayList<>()
                : new ArrayList<>(deptPriceFieldMapper.selectListByFieldKeys(fieldKeys));
        relations.sort(Comparator.comparing(DeptPriceFieldDO::getFieldKey)
                .thenComparing(DeptPriceFieldDO::getDeptId));
        return new ConfigSnapshot(priceDefinitions, departments, relations);
    }

    private DeptPriceFieldConfigRespVO buildConfig(ConfigSnapshot snapshot) {
        Map<String, List<Long>> deptIdsByField = snapshot.relations.stream()
                .collect(Collectors.groupingBy(item -> ProductPriceFieldKeys.normalize(item.getFieldKey()),
                        LinkedHashMap::new,
                        Collectors.mapping(DeptPriceFieldDO::getDeptId, Collectors.toList())));
        List<DeptPriceFieldConfigRespVO.Field> fields = snapshot.definitions.stream().map(definition -> {
            String fieldKey = ProductPriceFieldKeys.normalize(definition.getFieldKey());
            List<Long> deptIds = new ArrayList<>(deptIdsByField.getOrDefault(
                    fieldKey, Collections.emptyList()));
            Collections.sort(deptIds);
            DeptPriceFieldConfigRespVO.Field field = new DeptPriceFieldConfigRespVO.Field();
            field.setFieldKey(fieldKey);
            field.setFieldLabel(definition.getFieldLabel());
            field.setSort(definition.getSort());
            field.setDeptIds(deptIds);
            field.setConfigured(CollUtil.isNotEmpty(deptIds));
            return field;
        }).collect(Collectors.toList());
        List<DeptPriceFieldConfigRespVO.Department> departments = snapshot.departments.stream().map(dept -> {
            DeptPriceFieldConfigRespVO.Department department = new DeptPriceFieldConfigRespVO.Department();
            department.setId(dept.getId());
            department.setName(dept.getName());
            department.setParentId(dept.getParentId());
            department.setSort(dept.getSort());
            department.setStatus(dept.getStatus());
            return department;
        }).collect(Collectors.toList());
        DeptPriceFieldConfigRespVO result = new DeptPriceFieldConfigRespVO();
        result.setConfigVersion(calculateVersion(snapshot));
        result.setFields(fields);
        result.setDepartments(departments);
        return result;
    }

    private String calculateVersion(ConfigSnapshot snapshot) {
        StringBuilder canonical = new StringBuilder();
        snapshot.definitions.forEach(field -> canonical.append("F|")
                .append(field.getFieldKey()).append('|')
                .append(StrUtil.nullToEmpty(field.getFieldLabel())).append('|')
                .append(field.getSort()).append('\n'));
        snapshot.departments.forEach(dept -> canonical.append("D|")
                .append(dept.getId()).append('|')
                .append(dept.getParentId()).append('|')
                .append(dept.getSort()).append('|')
                .append(dept.getStatus()).append('\n'));
        snapshot.relations.forEach(relation -> canonical.append("R|")
                .append(relation.getFieldKey()).append('|')
                .append(relation.getDeptId()).append('\n'));
        return DigestUtil.sha256Hex(canonical.toString());
    }

    private static final class ConfigSnapshot {
        private final List<FieldDefinitionDO> definitions;
        private final List<DeptDO> departments;
        private final List<DeptPriceFieldDO> relations;

        private ConfigSnapshot(List<FieldDefinitionDO> definitions, List<DeptDO> departments,
                               List<DeptPriceFieldDO> relations) {
            this.definitions = definitions;
            this.departments = departments;
            this.relations = relations;
        }
    }

}
