package cn.iocoder.yudao.module.system.service.permission.formdata;

import cn.hutool.core.collection.CollUtil;
import cn.iocoder.yudao.module.system.enums.permission.FormPermissionRelationEnum;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class FormPermissionRebuildServiceImpl implements FormPermissionRebuildService {

    private static final int BATCH_SIZE = 500;

    private final FormPermissionConfigRegistry registry;
    private final FormPermissionService formPermissionService;
    private final FormPermissionValueParser valueParser;
    private final JdbcTemplate jdbcTemplate;

    @Override
    @Async
    public void rebuild(String formType) {
        FormPermissionIdentifierUtils.checkIdentifier(formType, "表名");
        FormPermissionTableMeta tableMeta = registry.getTableMeta(formType);
        if (tableMeta == null) {
            throw new IllegalArgumentException("表单数据权限未启用: " + formType);
        }
        long lastId = 0L;
        int total = 0;
        while (true) {
            List<Map<String, Object>> rows = selectBatch(formType, lastId);
            if (CollUtil.isEmpty(rows)) {
                break;
            }
            for (Map<String, Object> row : rows) {
                Long formId = parseLong(row.get("id"));
                if (formId == null) {
                    continue;
                }
                syncCreator(formType, formId, row);
                syncMentioned(formType, formId, tableMeta, row);
                lastId = Math.max(lastId, formId);
                total++;
            }
        }
        log.info("[rebuild][表单数据权限重建完成，formType({}) total({})]", formType, total);
    }

    private List<Map<String, Object>> selectBatch(String formType, Long lastId) {
        String table = FormPermissionIdentifierUtils.quote(formType);
        String deletedCondition = hasColumn(formType, "deleted") ? " AND deleted = 0" : "";
        return jdbcTemplate.queryForList("SELECT * FROM " + table + " WHERE id > ?" + deletedCondition
                + " ORDER BY id ASC LIMIT " + BATCH_SIZE, lastId);
    }

    private void syncCreator(String formType, Long formId, Map<String, Object> row) {
        Object creator = findValue(row, "creator");
        Long creatorUserId = parseLong(creator);
        if (creatorUserId == null) {
            return;
        }
        formPermissionService.syncRelation(formType, formId, FormPermissionRelationEnum.CREATOR,
                CollUtil.newArrayList(creatorUserId));
    }

    private void syncMentioned(String formType, Long formId, FormPermissionTableMeta tableMeta, Map<String, Object> row) {
        Set<Long> userIds = new LinkedHashSet<>();
        for (FormPermissionTableMeta.FieldMeta field : tableMeta.getFields()) {
            Object value = findValue(row, field.getColumnName());
            userIds.addAll(valueParser.parse(value, field.getValueType()));
        }
        formPermissionService.syncRelation(formType, formId, FormPermissionRelationEnum.MENTIONED, userIds);
    }

    private boolean hasColumn(String tableName, String columnName) {
        Integer count = jdbcTemplate.queryForObject("SELECT COUNT(1) FROM information_schema.columns "
                + "WHERE table_schema = DATABASE() AND table_name = ? AND column_name = ?",
                Integer.class, tableName, columnName);
        return count != null && count > 0;
    }

    private Object findValue(Map<String, Object> row, String columnName) {
        Object value = row.get(columnName);
        if (value != null) {
            return value;
        }
        return row.get(columnName.toUpperCase(Locale.ROOT));
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

}
