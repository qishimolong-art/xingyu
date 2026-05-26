package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.cart;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ErpSaleCartImportExcelVO {

    @ExcelProperty("产品编码")
    private String productCode;

    @ExcelProperty("仓库名称")
    private String warehouseName;

    @ExcelProperty("数量")
    private BigDecimal count;

    @ExcelProperty("单价")
    private BigDecimal productPrice;

    @ExcelProperty("品牌")
    private String brand;

    @ExcelProperty("车型")
    private String vehicleModel;

    @ExcelProperty("规格")
    private String standard;

    @ExcelProperty("备注")
    private String remark;
}
