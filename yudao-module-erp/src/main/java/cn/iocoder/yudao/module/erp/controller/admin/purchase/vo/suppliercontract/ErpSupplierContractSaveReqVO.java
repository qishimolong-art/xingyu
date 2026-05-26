package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.suppliercontract;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 供应商合同新增/修改 Request VO")
@Data
public class ErpSupplierContractSaveReqVO {

    private Long id;
    @NotNull(message = "供应商不能为空")
    private Long supplierId;
    @NotEmpty(message = "合同编号不能为空")
    private String contractNo;
    @NotNull(message = "合同日期不能为空")
    private LocalDateTime contractDate;
    private String contractType;
    private String settleMethod;
    private LocalDateTime startTime;
    private String transportMethod;
    private LocalDateTime endTime;
    private Boolean mainContract;
    private Boolean rebateEnabled;
    private Integer freightSettleMethod;
    private BigDecimal baseAmount;
    private BigDecimal taskAmount;
    private String summary;
    private String attachmentUrl;
    private Integer status;
    private String remark;

}
