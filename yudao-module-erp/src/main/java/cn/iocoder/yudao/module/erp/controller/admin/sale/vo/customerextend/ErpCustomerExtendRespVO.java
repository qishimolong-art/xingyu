package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerextend;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ExcelIgnoreUnannotated
public class ErpCustomerExtendRespVO {

    private Long id;
    private Long customerId;
    private String extendKey;
    private String extendName;
    private String extendValue;
    private String extendType;
    private Integer sort;
    private String remark;
    private LocalDateTime createTime;

}
