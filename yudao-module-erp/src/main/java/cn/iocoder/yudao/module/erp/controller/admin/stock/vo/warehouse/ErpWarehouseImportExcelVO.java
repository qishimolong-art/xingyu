package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse;

import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelRequired;
import lombok.Data;

@Data
public class ErpWarehouseImportExcelVO {

    @ExcelRequired
    @ExcelProperty("仓库名称")
    private String name;

    @ExcelProperty("仓库编码")
    private String warehouseCode;

    @ExcelProperty("所属部门")
    private String deptName;

    @ExcelProperty("所属部门ID")
    private Long deptId;

    @ExcelProperty("仓库类型")
    private Integer warehouseType;

    @ExcelProperty("开启状态")
    private Integer status;

    @ExcelProperty("销售启用")
    private Boolean saleEnabled;

    @ExcelProperty("采购启用")
    private Boolean purchaseEnabled;

    @ExcelProperty("入出仓单")
    private Boolean stockBillEnabled;

    @ExcelProperty("扫码管控")
    private Boolean scanControl;

    @ExcelProperty("是否拆单")
    private Boolean splitOrder;

    @ExcelProperty("排序")
    private Long sort;

    @ExcelProperty("备注")
    private String remark;

}
