package cn.iocoder.yudao.module.erp.controller.admin.report.vo.system;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@ExcelIgnoreUnannotated
public class ErpSystemReportExportRespVO {

    @ExcelProperty("名称")
    private String name;
    @ExcelProperty("编码")
    private String code;
    @ExcelProperty("部门")
    private String deptName;
    @ExcelProperty("经办人")
    private String userName;
    @ExcelProperty("仓库")
    private String warehouseName;
    @ExcelProperty("分类")
    private String categoryName;
    @ExcelProperty("单据类型")
    private String docType;
    @ExcelProperty("单据日期")
    private String docDate;
    @ExcelProperty("单据号")
    private String docNo;
    @ExcelProperty("业务数量")
    private String bizCount;
    @ExcelProperty("入库/销售金额")
    private String inAmount;
    @ExcelProperty("退货/出库金额")
    private String returnAmount;
    @ExcelProperty("净额/库存金额")
    private String netAmount;
    @ExcelProperty("未入/缺口数量")
    private String pendingQty;
    @ExcelProperty("状态")
    private String risk;

}
