package cn.iocoder.yudao.module.system.service.permission.formdata;

import cn.iocoder.yudao.framework.datapermission.core.rule.dept.DeptDataPermissionRule;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.FormPermissionFieldConfigDO;
import cn.iocoder.yudao.module.system.dal.dataobject.permission.FormPermissionTableConfigDO;
import cn.iocoder.yudao.module.system.dal.mysql.permission.FormPermissionFieldConfigMapper;
import cn.iocoder.yudao.module.system.dal.mysql.permission.FormPermissionTableConfigMapper;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class FormPermissionConfigRegistry {

    private final FormPermissionTableConfigMapper tableConfigMapper;
    private final FormPermissionFieldConfigMapper fieldConfigMapper;
    private final DeptDataPermissionRule deptDataPermissionRule;

    private volatile Map<String, FormPermissionTableMeta> tableMetaMap = Collections.emptyMap();

    public FormPermissionConfigRegistry(FormPermissionTableConfigMapper tableConfigMapper,
                                        FormPermissionFieldConfigMapper fieldConfigMapper,
                                        ObjectProvider<DeptDataPermissionRule> deptRuleProvider) {
        this.tableConfigMapper = tableConfigMapper;
        this.fieldConfigMapper = fieldConfigMapper;
        this.deptDataPermissionRule = deptRuleProvider.getIfAvailable();
    }

    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        refresh();
    }

    public synchronized void refresh() {
        List<FormPermissionTableConfigDO> tables = tableConfigMapper.selectListByEnabled(Boolean.TRUE);
        if (tables == null || tables.isEmpty()) {
            tableMetaMap = Collections.emptyMap();
            syncDeptRuleExcludes(Collections.emptySet());
            return;
        }
        Set<String> formTypes = tables.stream().map(FormPermissionTableConfigDO::getFormType).collect(Collectors.toSet());
        List<FormPermissionFieldConfigDO> fields = fieldConfigMapper.selectListByFormTypes(formTypes);
        Map<String, List<FormPermissionFieldConfigDO>> fieldMap = fields.stream()
                .filter(field -> Boolean.TRUE.equals(field.getEnabled()))
                .collect(Collectors.groupingBy(FormPermissionFieldConfigDO::getFormType));

        Map<String, FormPermissionTableMeta> newMap = new HashMap<>();
        for (FormPermissionTableConfigDO table : tables) {
            List<FormPermissionTableMeta.FieldMeta> metas = new ArrayList<>();
            for (FormPermissionFieldConfigDO field : fieldMap.getOrDefault(table.getFormType(), Collections.emptyList())) {
                metas.add(new FormPermissionTableMeta.FieldMeta(field.getColumnName(), field.getValueType()));
            }
            newMap.put(table.getFormType(), new FormPermissionTableMeta(table.getFormType(), metas));
        }
        tableMetaMap = Collections.unmodifiableMap(newMap);
        syncDeptRuleExcludes(newMap.keySet());
    }

    public Set<String> getEnabledFormTypes() {
        return new HashSet<>(tableMetaMap.keySet());
    }

    public FormPermissionTableMeta getTableMeta(String formType) {
        return tableMetaMap.get(formType);
    }

    public boolean contains(String formType) {
        return tableMetaMap.containsKey(formType);
    }

    private void syncDeptRuleExcludes(Collection<String> formTypes) {
        if (deptDataPermissionRule != null) {
            deptDataPermissionRule.setExcludeTableNames(formTypes);
        }
    }

}
