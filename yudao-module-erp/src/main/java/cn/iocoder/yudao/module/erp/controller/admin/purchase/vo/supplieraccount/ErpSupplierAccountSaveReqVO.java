package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplieraccount;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - ERP 供应商账户新增/修改 Request VO")
@Data
public class ErpSupplierAccountSaveReqVO {

    private Long id;
    @NotNull(message = "供应商不能为空")
    private Long supplierId;
    @NotEmpty(message = "账户名不能为空")
    private String accountName;
    @NotEmpty(message = "卡号不能为空")
    private String cardNo;
    private String bankName;
    private Boolean defaulted;
    private Integer sort;
    private String remark;

}
