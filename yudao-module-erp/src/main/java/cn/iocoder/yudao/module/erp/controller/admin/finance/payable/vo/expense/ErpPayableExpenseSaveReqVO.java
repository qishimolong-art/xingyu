package cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.expense;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "ERP 费用支付新增/修改 Request VO")
@Data
public class ErpPayableExpenseSaveReqVO {

    private Long id;

    @NotNull(message = "单据日期不能为空")
    private LocalDate bizTime;

    @NotNull(message = "结算方式不能为空")
    private String settleMethod;

    @NotNull(message = "结算账户不能为空")
    private Long accountId;

    private String voucherNo;

    @NotNull(message = "费用类型不能为空")
    private String expenseType;

    private Long deptId;

    @NotNull(message = "申请人不能为空")
    private Long handlerId;

    private String party;

    private String relatedBiz;

    private String docType;

    private String remark;

    private String fileUrl;

    @NotEmpty(message = "费用明细不能为空")
    @Valid
    private List<Item> items;

    @Data
    public static class Item {
        private Long id;
        @NotNull(message = "费用项目不能为空")
        private String itemName;
        @NotNull(message = "金额不能为空")
        private BigDecimal amount;
        private String invoiceNo;
        private String party;
        private Long deptId;
        private LocalDate bizDate;
        private Long handlerId;
        private Integer qty;
        private String expenseCategory;
        private String remark;
        private String fileUrl;
    }

}
