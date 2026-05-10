package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerarea;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class ErpCustomerAreaSaveReqVO {

    private Long id;

    @NotNull(message = "客户编号不能为空")
    private Long customerId;

    private BigDecimal longitude;
    private BigDecimal latitude;
    private String mapAddress;
    private String detailAddress;
    private Boolean defaulted;
    private String remark;

}
