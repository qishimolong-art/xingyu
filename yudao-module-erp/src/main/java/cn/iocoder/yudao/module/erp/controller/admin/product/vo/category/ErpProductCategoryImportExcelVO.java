package cn.iocoder.yudao.module.erp.controller.admin.product.vo.category;

import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelRequired;
import lombok.Data;

@Data
public class ErpProductCategoryImportExcelVO {

    @ExcelProperty("上级分类编码")
    private String parentCode;

    @ExcelRequired
    @ExcelProperty("分类名称")
    private String name;

    @ExcelRequired
    @ExcelProperty("分类编码")
    private String code;

    @ExcelProperty("状态")
    private Integer status;

}
