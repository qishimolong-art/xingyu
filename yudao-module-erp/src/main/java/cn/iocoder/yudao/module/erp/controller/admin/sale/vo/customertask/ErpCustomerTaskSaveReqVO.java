package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customertask;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Data
public class ErpCustomerTaskSaveReqVO {

    private Long id;

    @NotNull(message = "客户编号不能为空")
    private Long customerId;

    @NotNull(message = "年份不能为空")
    private Integer year;

    private Integer month;
    private String taskLevel;
    private BigDecimal taskAmount;
    private String remark;

}
