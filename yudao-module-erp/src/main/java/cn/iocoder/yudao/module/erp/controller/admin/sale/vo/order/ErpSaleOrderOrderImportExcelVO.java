package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.order;

import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelChoiceRequired;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelRequired;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ErpSaleOrderOrderImportExcelVO {

    @ExcelProperty("导入单号")
    private String importNo;

    @ExcelRequired
    @ExcelProperty("客户名称")
    private String customerName;

    @ExcelProperty("下单时间")
    private String orderTime;

    @ExcelProperty("销售员")
    private String saleUserName;

    @ExcelProperty("备注")
    private String remark;

    @ExcelChoiceRequired
    @ExcelProperty("配件编码（三选一）")
    private String productCode;

    @ExcelChoiceRequired
    @ExcelProperty("配件名称（三选一）")
    private String productName;

    @ExcelChoiceRequired
    @ExcelProperty("厂家编码（三选一）")
    private String factoryCode;

    @ExcelRequired
    @ExcelProperty("所属仓库")
    private String warehouseName;

    @ExcelRequired
    @ExcelProperty("数量")
    @javax.validation.constraints.Digits(integer = 18, fraction = 6, message = "itemCount最多支持 6 位小数")
    private BigDecimal itemCount;

    @ExcelProperty("单价")
    @javax.validation.constraints.Digits(integer = 18, fraction = 6, message = "productPrice最多支持 6 位小数")
    private BigDecimal productPrice;

    @ExcelProperty("赠品")
    private Boolean giftFlag;

    @ExcelProperty("税率")
    @javax.validation.constraints.Digits(integer = 18, fraction = 6, message = "taxPercent最多支持 6 位小数")
    private BigDecimal taxPercent;

    @ExcelProperty("明细备注")
    private String itemRemark;

}
