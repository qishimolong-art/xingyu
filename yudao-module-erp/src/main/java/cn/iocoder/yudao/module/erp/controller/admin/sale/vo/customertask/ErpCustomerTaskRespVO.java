package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customertask;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ExcelIgnoreUnannotated
public class ErpCustomerTaskRespVO {

    @ExcelProperty("编号")
    private Long id;
    private Long customerId;
    private Integer year;
    private Integer month;
    private String taskLevel;
    private BigDecimal taskAmount;
    private String remark;
    private LocalDateTime createTime;

}
