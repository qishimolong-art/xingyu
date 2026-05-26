package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplierbill;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "管理后台 - ERP 供应商票据新增/修改 Request VO")
@Data
public class ErpSupplierBillSaveReqVO {

    private Long id;
    @NotNull(message = "供应商不能为空")
    private Long supplierId;
    @NotNull(message = "票据日期不能为空")
    private LocalDate billDate;
    @NotEmpty(message = "票据号不能为空")
    private String billNo;
    @NotNull(message = "金额不能为空")
    private BigDecimal amount;
    private String remark;

}
