package cn.iocoder.yudao.module.erp.controller.admin.product.vo.product;

import lombok.*;
import io.swagger.v3.oas.annotations.media.Schema;
import cn.iocoder.yudao.framework.common.pojo.PageParam;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDateTime;
import java.util.Collection;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - ERP 产品分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpProductPageReqVO extends PageParam {

    @Schema(description = "产品名称", example = "刹车片")
    private String name;

    @Schema(description = "商品分类编号", example = "11161")
    private Long categoryId;

    @Schema(description = "所属部门编号", example = "100")
    private Long deptId;

    @Schema(description = "配件编码", example = "P000001")
    private String code;

    @Schema(description = "适用车型", example = "宝马 X5")
    private String vehicleModel;

    @Schema(description = "厂家编码", example = "FCT-001")
    private String factoryCode;

    @Schema(description = "Keyword for product fuzzy search", example = "P000001")
    private String keyword;

    @Schema(description = "状态 0=启用/1=停用", example = "0")
    private Integer status;

    @Schema(description = "仓库编号", example = "1")
    private Long warehouseId;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

    @Schema(description = "排序字段", example = "createTime")
    private String orderField;

    @Schema(description = "排序方向", example = "desc")
    private String orderDirection;

    @Schema(description = "是否包含销售分配仓库带来的档案只读数据", hidden = true)
    private Boolean includeSaleDistributedArchive;

    @Schema(description = "内部字段：可见部门编号", hidden = true)
    private Collection<Long> visibleDeptIds;

    @Schema(description = "内部字段：可见仓库编号", hidden = true)
    private Collection<Long> visibleWarehouseIds;

    @Schema(description = "内部字段：是否有全部数据权限", hidden = true)
    private Boolean visibleAll;

    @Schema(description = "内部字段：可看本人数据的用户编号", hidden = true)
    private Long visibleSelfUserId;

}
