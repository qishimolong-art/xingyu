package cn.iocoder.yudao.module.erp.controller.admin.product.vo.unit;

import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelRequired;
import lombok.Data;

@Data
public class ErpProductUnitImportExcelVO {

    @ExcelRequired
    @ExcelProperty("单位名称")
    private String name;

    @ExcelProperty("状态")
    private Integer status;

}
