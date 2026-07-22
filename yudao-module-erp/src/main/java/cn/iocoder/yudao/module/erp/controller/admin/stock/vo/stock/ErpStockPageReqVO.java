package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.math.BigDecimal;

@Schema(description = "管理后台 - ERP 库存分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpStockPageReqVO extends PageParam {

    @Schema(description = "产品编号", example = "19614")
    private Long productId;
    @Schema(description = "仓库编号", example = "2802")
    private Long warehouseId;
    @Schema(description = "所属部门", example = "100")
    private Long deptId;
    @Schema(description = "Sale business department id, used when bizType=sale")
    private Long saleDeptId;
    @Schema(description = "Business type: purchase or sale")
    private String bizType;

    // ========== 模糊搜索条件 ==========
    @Schema(description = "零件编码（产品 code）")
    private String productCode;
    @Schema(description = "零件名称（产品 name）")
    private String productName;
    @Schema(description = "图号")
    private String drawingNo;
    @Schema(description = "适用车型")
    private String vehicleModel;
    @Schema(description = "产地")
    private String originPlace;
    @Schema(description = "品牌")
    private String brand;
    @Schema(description = "货架号（库存 shelf）")
    private String shelf;
    @Schema(description = "特征码")
    private String featureCode;
    @Schema(description = "规格")
    private String standard;
    @Schema(description = "厂家编码")
    private String factoryCode;
    @Schema(description = "条形码")
    private String barCode;
    @Schema(description = "OE 编号")
    private String oeNumber;
    @Schema(description = "产品分类编号")
    private Long categoryId;
    @Schema(description = "产品状态 0=启用/1=停用")
    private Integer productStatus;

    // ========== 库存数量条件 ==========
    @Schema(description = "库存数量最小值")
    private BigDecimal countMin;
    @Schema(description = "库存数量最大值")
    private BigDecimal countMax;
    @Schema(description = "库存数筛选：0=全部 / 1=大于0 / 2=等于0", example = "0")
    private Integer countFilter;

    // ========== 库存上下限筛选 ==========
    @Schema(description = "库存上限最小值")
    private Integer stockMaxMin;
    @Schema(description = "库存上限最大值")
    private Integer stockMaxMax;
    @Schema(description = "库存下限最小值")
    private Integer stockMinMin;
    @Schema(description = "库存下限最大值")
    private Integer stockMinMax;
    @Schema(description = "标准库存最小值")
    private Integer stockStandardMin;
    @Schema(description = "标准库存最大值")
    private Integer stockStandardMax;

    // ========== 特殊筛选 ==========
    @Schema(description = "只查询同仓库内货架号重复的库存", example = "false")
    private Boolean shelfDuplicateOnly;
    @Schema(description = "只查询货架号为空的库存", example = "false")
    private Boolean shelfEmptyOnly;
    @Schema(description = "只显示正库存（count > 0）", example = "false")
    private Boolean positiveCountOnly;

    // ========== 价格体系动态列 ==========
    @Schema(description = "选定的价格体系 ID（返回结果会包含 currentPrice / currentPriceAmount）")
    private Long priceSystemId;

    // ========== 多仓合并 ==========
    @Schema(description = "多仓合并：true 按产品维度聚合同产品的多仓库存", example = "false")
    private Boolean mergeWarehouse;

    @Schema(description = "是否按批次号展开库存明细", example = "false")
    private Boolean showBatchNo;

    @Schema(description = "排序字段", example = "count")
    private String orderField;

    @Schema(description = "排序方向", example = "asc")
    private String orderDirection;
}
