package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.outbill;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - ERP 出仓单报表分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpStockOutBillPageReqVO extends PageParam {

    @Schema(description = "出仓单编号列表")
    private List<Long> ids;

    @Schema(description = "出仓单号", example = "CC202606240001")
    private String no;

    @Schema(description = "日期")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] billDate;

    @Schema(description = "仓库编号", example = "1")
    private Long warehouseId;

    @Schema(description = "产品关键词，匹配编码、名称、拼音码、五笔码等")
    private String productKeyword;

    @Schema(description = "发货区", example = "A区")
    private String shippingArea;

    @Schema(description = "来源单位名称", example = "客户A")
    private String sourceUnitName;

    @Schema(description = "来源单号", example = "SO202606240001")
    private String sourceNo;

    @Schema(description = "状态", example = "10")
    private Integer status;

    @Schema(description = "拣货人")
    private String pickUserName;

    @Schema(description = "创建者")
    private String creator;

    @Schema(description = "审核人")
    private String auditor;

    @Schema(description = "排序字段")
    private String orderField;

    @Schema(description = "排序方向（asc/desc）")
    private String orderDirection;

}
