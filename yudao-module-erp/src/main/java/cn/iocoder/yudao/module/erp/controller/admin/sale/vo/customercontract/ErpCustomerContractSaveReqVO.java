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
    private Integer status;
    private String remark;

}
