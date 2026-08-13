package cn.iocoder.yudao.module.system.framework.datapermission;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.framework.security.core.util.SecurityFrameworkUtils;
import cn.iocoder.yudao.module.system.enums.permission.FormPermissionRelationEnum;
import cn.iocoder.yudao.module.system.service.permission.formdata.FormPermissionConfigRegistry;
import cn.iocoder.yudao.module.system.service.permission.formdata.FormPermissionService;
import cn.iocoder.yudao.module.system.service.permission.formdata.FormPermissionTableMeta;
import cn.iocoder.yudao.module.system.service.permission.formdata.FormPermissionValueParser;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import lombok.RequiredArgsConstructor;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.SqlCommandType;
import org.apache.ibatis.plugin.Interceptor;
import org.apache.ibatis.plugin.Intercepts;
import org.apache.ibatis.plugin.Invocation;
import org.apache.ibatis.plugin.Signature;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Intercepts(@Signature(type = Executor.class, method = "update", args = {MappedStatement.class, Object.class}))
public class FormPermissionSyncInterceptor implements Interceptor {

    private final ObjectProvider<FormPermissionConfigRegistry> registryProvider;
    private final ObjectProvider<FormPermissionService> formPermissionServiceProvider;
    private final FormPermissionValueParser valueParser;

    @Override
    public Object intercept(Invocation invocation) throws Throwable {
        MappedStatement ms = (MappedStatement) invocation.getArgs()[0];
        Object result = invocation.proceed();
        if (!isAffected(result)) {
            return result;
        }
        Object entity = resolveEntity(invocation.getArgs()[1]);
        TableContext context = buildContext(ms.getSqlCommandType(), entity);
        if (context != null) {
            sync(context);
        }
        return result;
    }

    private TableContext buildContext(SqlCommandType commandType, Object entity) {
        if (entity == null || (commandType != SqlCommandType.INSERT && commandType != SqlCommandType.UPDATE)) {
            return null;
        }
        FormPermissionConfigRegistry registry = registryProvider.getIfAvailable();
        if (registry == null) {
            return null;
        }
        TableInfo tableInfo = TableInfoHelper.getTableInfo(entity.getClass());
        if (tableInfo == null || tableInfo.getKeyProperty() == null) {
            return null;
        }
        String tableName = tableInfo.getTableName();
        FormPermissionTableMeta tableMeta = registry.getTableMeta(tableName);
        if (tableMeta == null) {
            return null;
        }
        Long formId = parseLong(BeanUtil.getProperty(entity, tableInfo.getKeyProperty()));
        if (formId == null) {
            return null;
        }
        Map<String, String> columnPropertyMap = buildColumnPropertyMap(tableInfo);
        Set<Long> mentionedUserIds = new LinkedHashSet<>();
        boolean hasExplicitMentionFieldValue = false;
        for (FormPermissionTableMeta.FieldMeta field : tableMeta.getFields()) {
            String property = columnPropertyMap.get(field.getColumnName());
            if (property == null) {
                continue;
            }
            Object fieldValue = BeanUtil.getProperty(entity, property);
            if (fieldValue != null) {
                hasExplicitMentionFieldValue = true;
            }
            mentionedUserIds.addAll(valueParser.parse(fieldValue, field.getValueType()));
        }
        return new TableContext(commandType, tableName, formId, mentionedUserIds, hasExplicitMentionFieldValue);
    }

    private void sync(TableContext context) {
        FormPermissionService formPermissionService = formPermissionServiceProvider.getIfAvailable();
        if (formPermissionService == null) {
            return;
        }
        if (context.getCommandType() == SqlCommandType.INSERT) {
            Long loginUserId = SecurityFrameworkUtils.getLoginUserId();
            if (loginUserId != null) {
                formPermissionService.syncRelation(context.getTableName(), context.getFormId(),
                        FormPermissionRelationEnum.CREATOR, CollUtil.newArrayList(loginUserId));
            }
        }
        if (context.getCommandType() == SqlCommandType.INSERT || context.isHasExplicitMentionFieldValue()) {
            formPermissionService.syncRelation(context.getTableName(), context.getFormId(),
                    FormPermissionRelationEnum.MENTIONED, context.getMentionedUserIds());
        }
    }

    private Object resolveEntity(Object parameter) {
        if (!(parameter instanceof MapperMethod.ParamMap)) {
            return parameter;
        }
        MapperMethod.ParamMap<?> paramMap = (MapperMethod.ParamMap<?>) parameter;
        Object entity = paramMap.containsKey("et") ? paramMap.get("et") : null;
        if (entity != null) {
            return entity;
        }
        entity = paramMap.containsKey("entity") ? paramMap.get("entity") : null;
        if (entity != null) {
            return entity;
        }
        entity = paramMap.containsKey("param1") ? paramMap.get("param1") : null;
        if (entity != null && !(entity instanceof com.baomidou.mybatisplus.core.conditions.Wrapper)) {
            return entity;
        }
        return null;
    }

    private Map<String, String> buildColumnPropertyMap(TableInfo tableInfo) {
        Map<String, String> map = new HashMap<>();
        map.put(cleanColumn(tableInfo.getKeyColumn()), tableInfo.getKeyProperty());
        for (TableFieldInfo fieldInfo : tableInfo.getFieldList()) {
            map.put(cleanColumn(fieldInfo.getColumn()), fieldInfo.getProperty());
        }
        return map;
    }

    private String cleanColumn(String column) {
        return column == null ? null : column.replace("`", "");
    }

    private Long parseLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private boolean isAffected(Object result) {
        return !(result instanceof Number) || ((Number) result).intValue() > 0;
    }

    @lombok.Data
    @lombok.AllArgsConstructor
    private static class TableContext {

        private SqlCommandType commandType;

        private String tableName;

        private Long formId;

        private Set<Long> mentionedUserIds;

        private boolean hasExplicitMentionFieldValue;

    }

}
