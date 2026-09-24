package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse;

import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.iocoder.yudao.framework.excel.core.convert.DictConvert;
import cn.iocoder.yudao.module.system.enums.DictTypeConstants;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 仓库 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpWarehouseRespVO {

    @Schema(description = "仓库编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "11614")
    @ExcelProperty("仓库编号")
    private Long id;

    @Schema(description = "仓库名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "李四")
    @ExcelProperty("仓库名称")
    private String name;

    @Schema(description = "所属部门", example = "100")
    @ExcelProperty("所属部门ID")
    private Long deptId;

    @Schema(description = "所属部门名称")
    @ExcelProperty("所属部门")
    private String deptName;

    @Schema(description = "是否直发仓")
    private Boolean directWarehouse;

    @Schema(description = "仓库地址", example = "上海陆家嘴")
    @ExcelProperty("仓库地址")
    private String address;

    @Schema(description = "地图显示名称", example = "兴宇路通仓库")
    @ExcelProperty("地图名称")
    private String mapName;

    @Schema(description = "仓库经度（GCJ-02）", example = "104.066801")
    @ExcelProperty("经度")
    private BigDecimal longitude;

    @Schema(description = "仓库纬度（GCJ-02）", example = "30.572269")
    @ExcelProperty("纬度")
    private BigDecimal latitude;

    @Schema(description = "排序", requiredMode = Schema.RequiredMode.REQUIRED, example = "10")
    @ExcelProperty("排序")
    private Long sort;

    @Schema(description = "备注", example = "随便")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "负责人", example = "芋头")
    @ExcelProperty("负责人")
    private String principal;

    @Schema(description = "仓储费，单位：元", example = "13973")
    @ExcelProperty("仓储费，单位：元")
    private BigDecimal warehousePrice;

    @Schema(description = "搬运费，单位：元", example = "9903")
    @ExcelProperty("搬运费，单位：元")
    private BigDecimal truckagePrice;

    @Schema(description = "开启状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty(value = "开启状态", converter = DictConvert.class)
    @DictFormat(DictTypeConstants.COMMON_STATUS)
    private Integer status;

    @Schema(description = "是否默认", example = "1")
    @ExcelProperty("是否默认")
    private Boolean defaultStatus;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "创建人名称")
    private String creator;

    private String updater;

    private String creatorName;

    @Schema(description = "修改人名称")
    private String updaterName;

    @Schema(description = "修改时间")
    private LocalDateTime updateTime;

    // ========== 扩展字段 ==========

    @Schema(description = "仓库类型")
    private Integer warehouseType;

    @Schema(description = "仓储中心ID")
    private Long storageCenterId;

    @Schema(description = "仓储对应仓库ID")
    private Long storageWarehouseId;

    @Schema(description = "销售启用")
    private Boolean saleEnabled;

    @Schema(description = "采购启用")
    private Boolean purchaseEnabled;

    @Schema(description = "历史领货设置")
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

    @Schema(description = "出入仓分组")
    private Integer stockGroupType;

    @Schema(description = "额度管控")
    private BigDecimal creditControl;

    @Schema(description = "区域ID")
    private Long regionId;

    @Schema(description = "销售单云打印设备编号")
    private Long cloudPrintDeviceId;

    // ========== 关联数据 ==========

    @Schema(description = "分店租户ID列表")
    private List<Long> branchTenantIds;

    @Schema(description = "仓储中心名称")
    private String storageCenterName;

    @Schema(description = "仓储对应仓库名称")
    private String storageWarehouseName;

    @Schema(description = "区域名称")
    private String regionName;

    @Schema(description = "Whether the warehouse is readonly because it is visible through sale department distribution")
    private Boolean readonlyBySaleDistribution;

}
