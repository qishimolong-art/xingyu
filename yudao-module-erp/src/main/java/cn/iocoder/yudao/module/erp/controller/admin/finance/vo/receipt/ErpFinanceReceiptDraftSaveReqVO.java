package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.receipt;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 收款单草稿保存 Request VO")
@Data
public class ErpFinanceReceiptDraftSaveReqVO {

    @Schema(description = "编号", example = "1")
    private Long id;

    @Schema(description = "收款时间")
    private LocalDateTime receiptTime;

    @Schema(description = "财务人员编号", example = "1")
    private Long financeUserId;

    @Schema(description = "所属部门编号", example = "1")
    private Long deptId;

    @Schema(description = "客户编号", example = "1")
    private Long customerId;

    @Schema(description = "收款账户编号", example = "1")
    private Long accountId;

    @Schema(description = "来源其他应收单编号", example = "1024")
    private Long sourceReceivableMiscId;

    @Schema(description = "来源其他应收单号", example = "QTYSM20260922000001")
    private String sourceReceivableMiscNo;

    @Schema(description = "优惠金额")
    private BigDecimal discountPrice;

    @Schema(description = "合计收款")
    private BigDecimal totalPrice;

    @Schema(description = "实际收款")
    private BigDecimal receiptPrice;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "收款明细")
    private List<ErpFinanceReceiptSaveReqVO.Item> items;

}
