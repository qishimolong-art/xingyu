package cn.iocoder.yudao.module.erp.controller.admin.product.vo.brand;

import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelRequired;
import lombok.Data;

@Data
public class ErpProductBrandImportExcelVO {

    @ExcelRequired
    @ExcelProperty("品牌名称")
    private String name;

    @ExcelProperty("状态")
    private Integer status;

    @ExcelProperty("排序")
    private Integer sort;

}
