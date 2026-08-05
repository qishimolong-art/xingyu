package cn.iocoder.yudao.module.system.service.permission.formdata;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
@AllArgsConstructor
public class FormPermissionTableMeta {

    private String formType;

    private List<FieldMeta> fields;

    public List<FieldMeta> getFields() {
        return fields == null ? Collections.emptyList() : fields;
    }

    @Data
    @AllArgsConstructor
    public static class FieldMeta {

        private String columnName;

        private String valueType;

    }

}
