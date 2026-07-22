package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehousemove;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "Admin - ERP warehouse move page Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpWarehouseMovePageReqVO extends PageParam {

    @Schema(description = "ID 集合")
    private List<Long> ids;

    @Schema(description = "移货单号")
    private String no;

    @Schema(description = "移货日期")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] moveTime;

    @Schema(description = "状态")
    @InEnum(ErpAuditStatus.class)
    private Integer status;

    @Schema(description = "来源类型")
    private Integer sourceType;

    @Schema(description = "来源单据 ID")
    private Long sourceId;

    @Schema(description = "来源单号")
    private String sourceNo;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "创建人")
    private String creator;

    @Schema(description = "产品 ID")
    private Long productId;

    @Schema(description = "移出仓库 ID")
    private Long fromWarehouseId;

    @Schema(description = "移入仓库 ID")
    private Long toWarehouseId;

    @Schema(description = "所属部门")
    private Long deptId;

    @Schema(description = "排序字段")
    private String orderField;

    @Schema(description = "排序方向（asc/desc）")
    private String orderDirection;

}
