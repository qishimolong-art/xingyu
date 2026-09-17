package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.record;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - ERP 产品库存明细分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpStockRecordPageReqVO extends PageParam {

    @Schema(description = "产品编号", example = "10625")
    private Long productId;

    @Schema(description = "仓库编号", example = "32407")
    private Long warehouseId;

    @Schema(description = "批次号（精确匹配）")
    private String batchNo;

    @Schema(description = "是否仅查询未指定批次的流水", example = "false")
    private Boolean unassignedBatch;

    @Schema(description = "是否从产品库存页面按库存查看权限查询")
    private Boolean stockView;

    @Schema(description = "业务类型（单选，兼容旧查询）", example = "10")
    private Integer bizType;

    @Schema(description = "业务类型多选（五期新增）")
    private List<Integer> bizTypes;

    @Schema(description = "业务编号", example = "1024")
    private Long bizId;

    @Schema(description = "业务项编号", example = "2048")
    private Long bizItemId;

    @Schema(description = "业务单号", example = "Z110")
    private String bizNo;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

    @Schema(description = "业务发生日期区间（开单日期）")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] bizDate;

    @Schema(description = "产品关键词，匹配编码、名称、拼音码、五笔码等")
    private String productKeyword;

    // ========== 产品维度过滤（Service 层预查 productIds） ==========
    @Schema(description = "零件编码（模糊）")
    private String productCode;
    @Schema(description = "零件名称（模糊）")
    private String productName;
    @Schema(description = "适用车型（模糊）")
    private String vehicleModel;
    @Schema(description = "产地（模糊）")
    private String originPlace;

    @Schema(description = "来源供应商编号", example = "1724")
    private Long supplierId;

    @Schema(description = "排序字段", example = "bizDate")
    private String orderField;

    @Schema(description = "排序方向", example = "desc")
    private String orderDirection;

}
