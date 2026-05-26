package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customercontract;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 客户合同保存 Request VO")
@Data
public class ErpCustomerContractSaveReqVO {

    private Long id;

    @NotNull(message = "客户编号不能为空")
    private Long customerId;

    private String contractNo;
    private LocalDateTime contractDate;
    private String contractType;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String settleMethod;
    private String transportMethod;
    private BigDecimal baseAmount;
    private BigDecimal taskAmount;
    private String attachmentUrl;
    @Schema(description = "是否主要合同")
    private Boolean mainContract;
    @Schema(description = "返点证集（是否启用返点）")
    private Boolean rebateEnabled;
    @Schema(description = "运费结算方式 1=我方承担 2=客户承担 3=双方平摊 4=月结")
    private Integer freightSettleMethod;
    @Schema(description = "合同摘要")
    private String summary;
    private Integer status;
    private String remark;

}
