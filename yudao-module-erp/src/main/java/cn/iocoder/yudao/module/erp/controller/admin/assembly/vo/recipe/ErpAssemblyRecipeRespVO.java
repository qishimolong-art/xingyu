package cn.iocoder.yudao.module.erp.controller.admin.assembly.vo.recipe;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - 组装配方 Response VO")
@Data
@Accessors(chain = true)
public class ErpAssemblyRecipeRespVO {

    private Long id;
    private String no;
    private Long productId;
    private Integer purpose;
    private String moduleCode;
    private String moduleName;
    private BigDecimal laborCostPerSet;
    private Long deptId;
    private Integer version;
    private Integer status;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
    private String creator;
    private String creatorName;
    private String updater;
    private String updaterName;
    private Integer itemCount;
    private String productCode;
    private String productName;
    private String productDrawingNo;
    private String productFeatureCode;
    private String productOriginPlace;
    private String productVehicleModel;
    private BigDecimal productReferencePrice;
    private String productFactoryCode;
    private String productStandard;
    private String productUnitName;
    private Boolean batchNoEnabled;
    private List<Item> items;

    @Data
    @Accessors(chain = true)
    public static class Item {
        private Long id;
        private Long recipeId;
        private Long productId;
        private BigDecimal quantityPerSet;
        private Integer sort;
        private String remark;
        private String productCode;
        private String productName;
        private String productFactoryCode;
        private String productStandard;
        private String productUnitName;
        private String productDrawingNo;
        private String productOriginPlace;
        private String productBrand;
        private String productVehicleModel;
        private String productFeatureCode;
        private BigDecimal productRetailPrice;
        private Boolean batchNoEnabled;
    }

}
