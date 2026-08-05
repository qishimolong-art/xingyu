package cn.iocoder.yudao.module.erp.controller.admin.config.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

@Schema(description = "管理后台 - 更新库存选择价格名称配置 Request VO")
@Data
public class ErpStockSelectPriceConfigUpdateReqVO {

    @NotBlank
    private String configVersion;

    @Valid
    @NotNull
    @Size(max = 200)
    private List<Item> items;

    @Data
    public static class Item {

        @NotBlank
        @Size(max = 100)
        private String fieldKey;

        @NotNull
        private Boolean saleVisible;

        @NotNull
        private Boolean purchaseVisible;
    }

}
