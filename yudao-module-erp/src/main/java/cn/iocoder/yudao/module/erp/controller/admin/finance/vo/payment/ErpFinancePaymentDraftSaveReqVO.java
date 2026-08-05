package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.payment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 付款单草稿保存 Request VO")
@Data
public class ErpFinancePaymentDraftSaveReqVO {

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "付款时间")
    private LocalDateTime paymentTime;

    @Schema(description = "经手人编号", example = "1")
    private Long financeUserId;

    @Schema(description = "所属部门编号", example = "1")
    private Long deptId;

    @Schema(description = "供应商编号", example = "1")
    private Long supplierId;

    @Schema(description = "付款账户编号", example = "1")
    private Long accountId;

    @Schema(description = "优惠金额")
    private BigDecimal discountPrice;

    @Schema(description = "合计付款")
    private BigDecimal totalPrice;

    @Schema(description = "实际付款")
    private BigDecimal paymentPrice;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "付款明细")
    private List<ErpFinancePaymentSaveReqVO.Item> items;

}
