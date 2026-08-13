package cn.iocoder.yudao.module.erp.controller.admin.common.vo.print;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "管理后台 - ERP 打印字段 Response VO")
@Data
public class ErpPrintFieldRespVO {

    private String moduleKey;
    private List<Group> groups;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Group {
        private String key;
        private String name;
        private String target;
        private List<Field> fields;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Field {
        private String name;
        private String code;
        private String source;
    }

}
