package cn.iocoder.yudao.module.erp.controller.admin.assembly.vo.order;

import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Accessors(chain = true)
public class ErpAssemblyOrderRespVO {

    private Long id;
    private String no;
    private Long recipeId;
    private Integer recipeVersion;
    private String recipeModuleCode;
    private String recipeModuleName;
    private Long productId;
    private Long customerId;
    private String customerName;
    private Long handlerId;
    private String handlerName;
    private Integer setCount;
    private Long warehouseId;
    private Long outWarehouseId;
    private Long warehouseMoveId;
    private String warehouseMoveNo;
    private Integer warehouseMoveStatus;
    private Long deptId;
    private String shelf;
    private LocalDateTime assemblyTime;
    private String finishedBatchNo;
    private BigDecimal laborCostPerSet;
    private BigDecimal otherCost;
    private BigDecimal materialCost;
    private BigDecimal laborCost;
    private BigDecimal totalCost;
    private BigDecimal unitCost;
    private Integer status;
    private Integer executionRevision;
    private Long confirmUserId;
    private String confirmUserName;
    private LocalDateTime confirmTime;
    private Long reverseConfirmUserId;
    private String reverseConfirmUserName;
    private LocalDateTime reverseConfirmTime;
    private Long startUserId;
    private String startUserName;
    private LocalDateTime startTime;
    private Long cancelStartUserId;
    private String cancelStartUserName;
    private LocalDateTime cancelStartTime;
    private Long completeUserId;
    private String completeUserName;
    private LocalDateTime completeTime;
    private Long reverseAssemblyUserId;
    private String reverseAssemblyUserName;
    private LocalDateTime reverseAssemblyTime;
    private Long voidUserId;
    private String voidUserName;
    private LocalDateTime voidTime;
    private String voidReason;
    private String remark;
    private String creator;
    private String creatorName;
    private LocalDateTime createTime;
    private String updater;
    private String updaterName;
    private LocalDateTime updateTime;
    private Integer itemCount;
    private String productCode;
    private String productName;
    private String productFactoryCode;
    private String productStandard;
    private String productUnitName;
    private String productDrawingNo;
    private String productOriginPlace;
    private String productFeatureCode;
    private String productBrand;
    private String productVehicleModel;
    private BigDecimal productRetailPrice;
    private String productShelf;
    private Boolean batchNoEnabled;
    private String warehouseName;
    private String outWarehouseName;
    private Integer printCount;
    private LocalDateTime printTime;
    private List<Item> items;

    @Data
    @Accessors(chain = true)
    public static class Item {
        private Long id;
        private Long orderId;
        private Long recipeItemId;
        private Long productId;
        private BigDecimal quantityPerSet;
        private BigDecimal requiredQuantity;
        private String batchNo;
        private BigDecimal consumeQuantity;
        private BigDecimal costPrice;
        private BigDecimal costAmount;
        private String productCode;
        private String productName;
        private String productStandard;
        private String productUnitName;
        private String productFactoryCode;
        private String productDrawingNo;
        private String productOriginPlace;
        private String productBrand;
        private String productVehicleModel;
        private String productFeatureCode;
        private String productSupplierName;
        private BigDecimal productRetailPrice;
        private BigDecimal stockCount;
        private BigDecimal occupiedCount;
        private BigDecimal shortageCount;
        private Boolean batchNoEnabled;
        private Integer sort;
        private String remark;
    }

}
