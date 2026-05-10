package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerarea;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ExcelIgnoreUnannotated
public class ErpCustomerAreaRespVO {

    private Long id;
    private Long customerId;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String mapAddress;
    private String detailAddress;
    private Boolean defaulted;
    private String remark;
    private LocalDateTime createTime;

}
