package cn.iocoder.yudao.module.erp.controller.admin.product.vo.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.Map;

@Schema(description = "管理后台 - 配件列表直接编辑保存单条 Request VO")
@Data
public class ErpPartsBatchUpdatePriceFieldsReqVO {

    @Schema(description = "配件编号", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "配件编号不能为空")
    private Long id;

    @Schema(description = "备用价1")
    private BigDecimal backupPrice1;

    @Schema(description = "特价（specialPrice 字段，DO 中暂无，预留）")
    private BigDecimal specialPrice;

    @Schema(description = "参考价")
    private BigDecimal referencePrice;

    @Schema(description = "零售价")
    private BigDecimal retailPrice;

    @Schema(description = "批发价")
    private BigDecimal wholesalePrice;

    @Schema(description = "股份价")
    private BigDecimal sharePrice;

    @Schema(description = "批量价（batchPrice 字段，DO 中暂无，预留）")
    private BigDecimal batchPrice;

    @Schema(description = "库存上限")
    private Integer stockMax;

    @Schema(description = "库存下限")
    private Integer stockMin;

    @Schema(description = "标准库存")
    private Integer stockStandard;

    @Schema(description = "自定义价格字段值，key 为字段编码")
    private Map<String, Object> customFields;

}
