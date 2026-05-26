package cn.iocoder.yudao.module.erp.controller.admin.product.vo.product;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ErpProductImportExcelVO {

    @ExcelProperty("配件编码")
    private String code;

    @ExcelProperty("产品名称")
    private String name;

    @ExcelProperty("产品条码")
    private String barCode;

    @ExcelProperty("产品分类")
    private String categoryName;

    @ExcelProperty("单位")
    private String unitName;

    @ExcelProperty("状态")
    private Integer status;

    @ExcelProperty("默认仓库")
    private String defaultWarehouseName;

    @ExcelProperty("适用车型")
    private String vehicleModel;

    @ExcelProperty("厂家编码")
    private String factoryCode;

    @ExcelProperty("采购价格")
    private BigDecimal purchasePrice;

    @ExcelProperty("销售价格")
    private BigDecimal salePrice;

    @ExcelProperty("最低价格")
    private BigDecimal minPrice;

    @ExcelProperty("规格")
    private String standard;

    @ExcelProperty("备注")
    private String remark;

    @ExcelProperty("保质期天数")
    private Integer expiryDay;

    @ExcelProperty("重量")
    private BigDecimal weight;

    @ExcelProperty("参考价")
    private BigDecimal referencePrice;

    @ExcelProperty("零售价")
    private BigDecimal retailPrice;

    @ExcelProperty("最后一次采购入库价")
    private BigDecimal lastPurchasePrice;

    @ExcelProperty("毛利率")
    private Integer grossProfitRate;

    @ExcelProperty("备用价1")
    private BigDecimal backupPrice1;

    @ExcelProperty("批发价")
    private BigDecimal wholesalePrice;

    @ExcelProperty("库存上限")
    private Integer stockMax;

    @ExcelProperty("库存下限")
    private Integer stockMin;

    @ExcelProperty("标准库存")
    private Integer stockStandard;

    @ExcelProperty("包装数")
    private Integer packageQty;

    @ExcelProperty("主图URL")
    private String mainImage;

    @ExcelProperty("详情内容")
    private String detailContent;

}
