package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.move;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.erp.enums.ErpAuditStatus;
import cn.iocoder.yudao.module.erp.enums.stock.ErpStockTransferDirectionEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.List;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - ERP 库存调拨单分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpStockMovePageReqVO extends PageParam {

    @Schema(description = "调拨单号", example = "S123")
    private String no;

    @Schema(description = "Stock move ids", example = "[1, 2]")
    private List<Long> ids;

    @Schema(description = "调拨方向，10 调拨出库，20 调拨入库", example = "10")
    @InEnum(ErpStockTransferDirectionEnum.class)
    private Integer transferDirection;

    @Schema(description = "关联调拨单编号", example = "1024")
    private Long relatedMoveId;

    @Schema(description = "关联调拨单号", example = "QCDB20260714000001")
    private String relatedMoveNo;

    @Schema(description = "调出部门", example = "100")
    private Long fromDeptId;

    @Schema(description = "调入部门", example = "101")
    private Long toDeptId;

    @Schema(description = "调拨时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] moveTime;

    @Schema(description = "状态", example = "10")
    @InEnum(ErpAuditStatus.class)
    private Integer status;

    @Schema(description = "来源类型", example = "30")
    private Integer sourceType;

    @Schema(description = "来源单据编号", example = "1024")
    private Long sourceId;

    @Schema(description = "来源单据号", example = "SC202606100001")
    private String sourceNo;

    @Schema(description = "备注", example = "随便")
    private String remark;

    @Schema(description = "创建者")
    private String creator;

    @Schema(description = "产品编号", example = "1")
    private Long productId;

    @Schema(description = "调出仓库编号", example = "1")
    private Long fromWarehouseId;

    @Schema(description = "调入仓库编号", example = "1")
    private Long toWarehouseId;

    @Schema(description = "所属部门", example = "100")
    private Long deptId;

    @Schema(description = "关键词")
    private String keyword;

    @Schema(description = "排序字段")
    private String orderField;

    @Schema(description = "排序方向（asc/desc）")
    private String orderDirection;

}
