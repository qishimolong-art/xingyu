package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.imports;

import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelRequired;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ErpStockImportExcelVO {

    @ExcelProperty("单据编号")
    private String orderNo;

    @ExcelProperty("供应商名称")
    private String supplierName;

    @ExcelProperty("客户名称")
    private String customerName;

    @ExcelRequired
    @ExcelProperty("业务时间")
    private String bizTime;

    @ExcelProperty("盘点类型")
    private String checkTypeName;

    @ExcelProperty("仓库名称")
    private String warehouseName;

    @ExcelProperty("调出仓库名称")
    private String fromWarehouseName;

    @ExcelProperty("调入部门")
    private String toDeptName;

    @ExcelProperty("调入仓库名称")
    private String toWarehouseName;

    @ExcelRequired
    @ExcelProperty("产品编码")
    private String productCode;

    @ExcelRequired
    @ExcelProperty("数量")
    private BigDecimal count;

    @ExcelProperty("单价")
    private BigDecimal productPrice;

    @ExcelProperty("账面库存")
    private BigDecimal stockCount;

    @ExcelProperty("实际库存")
    private BigDecimal actualCount;

    @ExcelProperty("单据备注")
    private String remark;

    @ExcelProperty("明细备注")
    private String itemRemark;

}
