package cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.otherincome;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "ERP 其他收入新增/修改 Request VO")
@Data
public class ErpReceivableOtherIncomeSaveReqVO {
    private Long id;
    @NotNull private LocalDateTime bizTime;
    @NotNull private String settleMethod;
    @NotNull private Long accountId;
    private String voucherNo;
    @NotNull private String incomeType;
    private BigDecimal totalAmount;
    @NotNull private Long deptId;
    @NotNull private Long handlerId;
    private String party;
    private String relatedBiz;
    private String docType;
    private String remark;
    private String fileUrl;
    @NotEmpty @Valid private List<Item> items;
    @Data
    public static class Item {
        private Long id;
        @NotNull private String itemName;
        @NotNull private BigDecimal amount;
        private String invoiceNo;
        private String party;
        private Long customerId;
        private Long deptId;
        private LocalDateTime bizDate;
        private Long handlerId;
        private Integer qty;
        private String freightType;
        private String remark;
        private String fileUrl;
    }
}
