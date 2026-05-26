package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.supplierbusinessinfo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotNull;

@Schema(description = "管理后台 - ERP 供应商工商信息新增/修改 Request VO")
@Data
public class ErpSupplierBusinessInfoSaveReqVO {

    private Long id;
    @NotNull(message = "供应商不能为空")
    private Long supplierId;
    private String companyName;
    private String creditCode;
    private String legalPerson;
    private String registeredAddress;
    private String businessScope;
    private String registeredCapital;
    private String establishDate;
    private String businessStatus;
    private String rawData;
    private String remark;

}
