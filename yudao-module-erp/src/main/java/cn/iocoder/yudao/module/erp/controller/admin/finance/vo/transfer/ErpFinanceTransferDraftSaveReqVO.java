package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.transfer;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 银行转账草稿新增/修改 Request VO")
@Data
@Accessors(chain = true)
public class ErpFinanceTransferDraftSaveReqVO {

    private Long id;
    private LocalDateTime transferTime;
    private Long outAccountId;
    private Long inAccountId;
    private BigDecimal transferPrice;

    @DecimalMin(value = "0", inclusive = false, message = "汇率必须大于 0")
    private BigDecimal exchangeRate;

    @DecimalMin(value = "0", message = "手续费不能小于 0")
    private BigDecimal feePrice;

    @Size(max = 64, message = "费用项目长度不能超过 64 个字符")
    private String feeExpenseCategory;

    private Long financeUserId;
    private Long deptId;

    @Size(max = 512, message = "备注长度不能超过 512 个字符")
    private String remark;

    @Size(max = 512, message = "附件长度不能超过 512 个字符")
    private String fileUrl;

}
