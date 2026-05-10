package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerbusinessinfo;

import lombok.Data;

import javax.validation.constraints.NotNull;

@Data
public class ErpCustomerBusinessInfoSaveReqVO {

    private Long id;

    @NotNull(message = "客户编号不能为空")
    private Long customerId;

    private String creditCode;
    private String legalPerson;
    private String registeredCapital;
    private String establishDate;
    private String businessStatus;
    private String businessScope;
    private String rawData;
    private String remark;

}
