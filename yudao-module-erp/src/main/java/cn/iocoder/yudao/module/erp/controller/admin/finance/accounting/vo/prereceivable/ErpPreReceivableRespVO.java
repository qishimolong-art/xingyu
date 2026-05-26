package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.prereceivable;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 预收账款单 Response VO")
@Data
public class ErpPreReceivableRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Long id;

    @Schema(description = "预收账款单号", requiredMode = Schema.RequiredMode.REQUIRED)
    private String no;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer status;

    @Schema(description = "业务时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime bizTime;

    @Schema(description = "往来类型：1=客户 2=供应商 3=员工")
    private Integer partyType;

    @Schema(description = "往来方 ID")
    private Long partyId;

    @Schema(description = "往来方名称")
    private String partyName;

    @Schema(description = "结算账户编号")
    private Long accountId;

    @Schema(description = "结算账户名称")
    private String accountName;

    @Schema(description = "合计金额")
    private BigDecimal totalAmount;

    @Schema(description = "优惠金额")
    private BigDecimal discountAmount;

    @Schema(description = "实收金额")
    private BigDecimal actualAmount;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "附件 URL")
    private String fileUrl;

    @Schema(description = "创建人")
    private String creator;

    @Schema(description = "创建人名称")
    private String creatorName;

    @Schema(description = "创建时间")
    private LocalDateTime createTime;

    @Schema(description = "明细列表")
    private List<Item> items;

    @Data
    public static class Item {

        @Schema(description = "明细编号")
        private Long id;

        @Schema(description = "摘要")
        private String summary;

        @Schema(description = "金额")
        private BigDecimal amount;

        @Schema(description = "备注")
        private String remark;

    }

}
