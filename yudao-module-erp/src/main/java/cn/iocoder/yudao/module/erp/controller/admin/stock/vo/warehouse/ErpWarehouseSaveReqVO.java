package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import javax.validation.constraints.DecimalMax;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - ERP 仓库新增/修改 Request VO")
@Data
public class ErpWarehouseSaveReqVO {

    @Schema(description = "仓库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "11614")
    private Long id;

    @Schema(description = "仓库名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "李四")
    @NotEmpty(message = "仓库名称不能为空")
    private String name;

    @Schema(description = "所属部门", example = "100")
    private Long deptId;

    @Schema(description = "仓库地址", example = "上海陆家嘴")
    private String address;

    @Schema(description = "地图显示名称", example = "兴宇路通仓库")
    private String mapName;

    @Schema(description = "仓库经度（GCJ-02）", example = "104.066801")
    @DecimalMin(value = "-180", message = "仓库经度必须在 -180 到 180 之间")
    @DecimalMax(value = "180", message = "仓库经度必须在 -180 到 180 之间")
    private BigDecimal longitude;

    @Schema(description = "仓库纬度（GCJ-02）", example = "30.572269")
    @DecimalMin(value = "-90", message = "仓库纬度必须在 -90 到 90 之间")
    @DecimalMax(value = "90", message = "仓库纬度必须在 -90 到 90 之间")
    private BigDecimal latitude;

    @Schema(description = "排序", example = "10")
    private Long sort;

    @Schema(description = "备注", example = "随便")
    private String remark;

    @Schema(description = "负责人", example = "芋头")
    private String principal;

    @Schema(description = "仓储费，单位：元", example = "13973")
    private BigDecimal warehousePrice;

    @Schema(description = "搬运费，单位：元", example = "9903")
    private BigDecimal truckagePrice;

    @Schema(description = "开启状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "2")
    @NotNull(message = "开启状态不能为空")
    @InEnum(CommonStatusEnum.class)
    private Integer status;

    // ========== 扩展字段 ==========

    @Schema(description = "仓库类型(1-正品仓库 2-废品仓库 3-待处理仓库 4-急件仓库 5-旧件仓库 6-寄售仓库 7-托管仓库 8-半成品仓)")
    private Integer warehouseType;

    @Schema(description = "仓储中心ID")
    private Long storageCenterId;

    @Schema(description = "仓储对应仓库ID")
    private Long storageWarehouseId;

    @Schema(description = "销售启用")
    private Boolean saleEnabled;

    @Schema(description = "采购启用")
    private Boolean purchaseEnabled;

    @Schema(description = "入出仓单(0不生成 1生成)")
    private Boolean stockBillEnabled;

    @Schema(description = "允许电商销售")
    private Boolean ecommerceEnabled;

    @Schema(description = "扫码管控")
    private Boolean scanControl;

    @Schema(description = "销售开单管控")
    private Boolean saleBillControl;

    @Schema(description = "销售0库存不显示")
    private Boolean zeroStockHide;

    @Schema(description = "货到分店")
    private String goodsToBranch;

    @Schema(description = "部门")
    private String dept;

    @Schema(description = "仓库地点")
    private String warehouseLocation;

    @Schema(description = "出仓打包装箱")
    private Boolean outPacking;

    @Schema(description = "自动订货")
    private Boolean autoOrder;

    @Schema(description = "允许同时拣货单数")
    private Integer maxPickCount;

    @Schema(description = "仓库编码")
    private String warehouseCode;

    @Schema(description = "是否拆单")
    private Boolean splitOrder;

    @Schema(description = "出入仓分组(1-全部 2-入仓单 3-出仓单 4-全部不分组)")
    private Integer stockGroupType;

    @Schema(description = "额度管控")
    private BigDecimal creditControl;

    @Schema(description = "区域ID")
    private Long regionId;

    @Schema(description = "销售单云打印设备编号")
    private Long cloudPrintDeviceId;

    @Schema(description = "分店租户ID列表")
    private List<Long> branchTenantIds;

}
