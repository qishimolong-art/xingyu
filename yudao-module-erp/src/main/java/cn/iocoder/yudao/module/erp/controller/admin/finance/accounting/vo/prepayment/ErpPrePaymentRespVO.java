package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.prepayment;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 预付款单 Response VO")
@Data
public class ErpPrePaymentRespVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "预付款单号")
    private String no;

    @Schema(description = "状态")
    private Integer status;

    @Schema(description = "业务时间")
    private LocalDateTime bizTime;

    @Schema(description = "往来单位类型")
    private Integer partyType;

    @Schema(description = "往来单位 ID")
    private Long partyId;

    @Schema(description = "往来单位名称")
    private String partyName;

    @Schema(description = "结算账户 ID")
    private Long accountId;

    @Schema(description = "合计金额")
    private BigDecimal totalAmount;

    @Schema(description = "优惠金额")
    private BigDecimal discountAmount;

    @Schema(description = "实付金额")
    private BigDecimal actualAmount;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "附件")
    private String fileUrl;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "子表明细")
    private List<Item> items;

    @Data
    public static class Item {

        @Schema(description = "编号")
        private Long id;

        @Schema(description = "摘要")
        private String summary;

        @Schema(description = "金额")
        private BigDecimal amount;

        @Schema(description = "备注")
        private String remark;

    }

}
