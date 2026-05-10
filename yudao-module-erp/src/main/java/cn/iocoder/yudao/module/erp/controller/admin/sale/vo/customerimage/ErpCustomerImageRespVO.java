package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.customerimage;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@ExcelIgnoreUnannotated
public class ErpCustomerImageRespVO {

    @ExcelProperty("编号")
    private Long id;
    private Long customerId;
    private String imageType;
    private String imageName;
    private String imageUrl;
    private Boolean defaulted;
    private Integer sort;
    private String remark;
    private LocalDateTime createTime;

}
