package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerbusinessinfo;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ExcelIgnoreUnannotated
public class ErpCustomerBusinessInfoRespVO {

    private Long id;
    private Long customerId;
    private String creditCode;
    private String legalPerson;
    private String registeredCapital;
    private String establishDate;
    private String businessStatus;
    private String businessScope;
    private String rawData;
    private String remark;
    private LocalDateTime createTime;

}
