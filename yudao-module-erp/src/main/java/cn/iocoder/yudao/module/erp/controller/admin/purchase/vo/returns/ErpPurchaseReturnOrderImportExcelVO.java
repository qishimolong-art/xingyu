package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.returns;

import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelChoiceRequired;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelRequired;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ErpPurchaseReturnOrderImportExcelVO {

    @ExcelProperty("退货单号")
    private String no;

    @ExcelRequired
    @ExcelProperty("供应商")
    private String supplierName;

    @ExcelProperty("退货时间")
    private String returnTime;

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
    @ExcelProperty("退货数量")
    private BigDecimal itemCount;

    @ExcelProperty("退货单价")
    private BigDecimal productPrice;

    @ExcelProperty("批次号")
    private String batchNo;

    @ExcelProperty("明细备注")
    private String itemRemark;

}
