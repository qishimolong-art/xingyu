package cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.expense;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "ERP 费用支付草稿保存 Request VO")
@Data
public class ErpPayableExpenseDraftSaveReqVO {

    private Long id;

    private LocalDate bizTime;

    private String settleMethod;

    private Long accountId;

    private String voucherNo;

    private String expenseBizType;

    private String expenseType;

    private Long deptId;

    private Long handlerId;

    private String party;

    private String relatedBiz;

    private String docType;

    private String remark;

    private String fileUrl;

    private List<ErpPayableExpenseSaveReqVO.Item> items;

}
