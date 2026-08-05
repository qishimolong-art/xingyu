package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.priceadjust;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 采购调价单草稿保存 Request VO")
@Data
public class ErpPurchasePriceAdjustDraftSaveReqVO {

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "调价日期")
    private LocalDateTime adjustTime;

    @Schema(description = "供应商编号", example = "1")
    private Long supplierId;

    @Schema(description = "部门编号", example = "1")
    private Long deptId;

    @Schema(description = "调价人", example = "1")
    private Long adjuster;

    @Schema(description = "调价类型：10=按入库单调价 20=添加明细", example = "10")
    private Integer adjustType;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "调价明细")
    private List<ErpPurchasePriceAdjustSaveReqVO.Item> items;

}
