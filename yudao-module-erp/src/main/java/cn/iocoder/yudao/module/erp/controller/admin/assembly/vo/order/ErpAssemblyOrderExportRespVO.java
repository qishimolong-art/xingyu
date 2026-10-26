package cn.iocoder.yudao.module.erp.controller.admin.assembly.vo.order;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import cn.idev.excel.annotation.format.NumberFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@ExcelIgnoreUnannotated
public class ErpAssemblyOrderExportRespVO {

    @ExcelProperty("单号")
    private String no;
    @ExcelProperty("单据日期")
    private LocalDateTime assemblyTime;
    @ExcelProperty("单据状态")
    private String statusName;
    @ExcelProperty("对象")
    private String objectName;
    @ExcelProperty("图号")
    private String drawingNo;
    @ExcelProperty("产品编码")
    private String productCode;
    @ExcelProperty("产品名称")
    private String productName;
    @ExcelProperty("产地")
    private String originPlace;
    @ExcelProperty("规格")
    private String standard;
    @ExcelProperty("特征码")
    private String featureCode;
    @ExcelProperty("品牌")
    private String brand;
    @ExcelProperty("车型")
    private String vehicleModel;
    @ExcelProperty("仓库")
    private String warehouseName;
    @ExcelProperty("仓位")
    private String shelf;
    @ExcelProperty("数量")
    private Integer setCount;
    @ExcelProperty("组装成本金额")
    @NumberFormat("0.00")
    private BigDecimal totalCost;
    @ExcelProperty("成本价")
    @NumberFormat("0.00")
    private BigDecimal unitCost;
    @ExcelProperty("零售价")
    @NumberFormat("0.00")
    private BigDecimal retailPrice;
    @ExcelProperty("创建人")
    private String creatorName;
    @ExcelProperty("编辑人")
    private String updaterName;
    @ExcelProperty("打印次数")
    private Integer printCount;
    @ExcelProperty("打印时间")
    private LocalDateTime printTime;

}
