package cn.iocoder.yudao.module.erp.controller.admin.config.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Schema(description = "管理后台 - 库存选择价格名称配置 Response VO")
@Data
public class ErpStockSelectPriceConfigRespVO {

    private String configVersion;

    private List<Field> fields;

    @Data
    public static class Field {

        private String fieldKey;

        private String fieldLabel;

        private String fieldSource;

        private Integer sort;

        private Boolean saleVisible;

        private Boolean purchaseVisible;
    }

}
