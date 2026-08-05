package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.priceadjust;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 销售调价单草稿保存 Request VO")
@Data
public class ErpSalePriceAdjustDraftSaveReqVO {

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "日期")
    private LocalDateTime adjustDate;

    @Schema(description = "客户编号", example = "1")
    private Long customerId;

    @Schema(description = "部门 ID")
    private Long deptId;

    @Schema(description = "调价人 ID")
    private Long adjustUserId;

    @Schema(description = "调价类型")
    private Integer adjustType;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "结算方式")
    private String settleMethod;

    @Schema(description = "送货方式")
    private String deliveryMethod;

    @Schema(description = "物流公司")
    private String logisticsCompany;

    @Schema(description = "调价明细列表")
    private List<ErpSalePriceAdjustSaveReqVO.Item> items;

}
