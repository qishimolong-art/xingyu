package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.outbill;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 出仓单报表 Response VO")
@Data
public class ErpStockOutBillRespVO {

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "优先级")
    private String priority;

    @Schema(description = "单号")
    private String no;

    @Schema(description = "拣货标记")
    private Boolean pickFlag;

    @Schema(description = "拣货")
    private String pick;

    @Schema(description = "拣货人")
    private String pickUserName;

    @Schema(description = "日期")
    private LocalDateTime billDate;

    @Schema(description = "仓库编号")
    private Long warehouseId;

    @Schema(description = "仓库")
    private String warehouseName;

    @Schema(description = "发货区")
    private String shippingArea;

    @Schema(description = "来源单位名称")
    private String sourceUnitName;

    @Schema(description = "来源单号")
    private String sourceNo;

    @Schema(description = "来源业务类型")
    private Integer sourceBizType;

    @Schema(description = "来源业务编号")
    private Long sourceId;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "创建者")
    private String creator;

    @Schema(description = "创建人名称")
    private String creatorName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "修改者")
    private String updater;

    @Schema(description = "修改名称")
    private String updaterName;

    @Schema(description = "修改时间")
    private LocalDateTime updateTime;

    @Schema(description = "审核人")
    private String auditor;

    @Schema(description = "审核人名称")
    private String auditorName;

    @Schema(description = "审核时间")
    private LocalDateTime auditTime;

    @Schema(description = "打印时间")
    private LocalDateTime printTime;

    @Schema(description = "打印次数")
    private Integer printCount;

    @Schema(description = "来源单据备注")
    private String sourceRemark;

    @Schema(description = "总重")
    private BigDecimal totalWeight;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "超时")
    private Boolean timeoutFlag;

    @Schema(description = "整件数")
    private BigDecimal wholeQty;

    @Schema(description = "散件数")
    private BigDecimal looseQty;

    @Schema(description = "应拣数量")
    private BigDecimal totalCount;

    @Schema(description = "已拣数量")
    private BigDecimal pickedCount;

}
