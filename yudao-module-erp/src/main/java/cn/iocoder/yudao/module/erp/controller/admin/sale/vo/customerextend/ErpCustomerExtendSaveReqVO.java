package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerextend;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
public class ErpCustomerExtendSaveReqVO {

    private Long id;

    @NotNull(message = "客户编号不能为空")
    private Long customerId;

    @NotBlank(message = "字段标识不能为空")
    private String extendKey;

    private String extendName;
    private String extendValue;
    private String extendType;
    private Integer sort;
    private String remark;

}
