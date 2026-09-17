package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ErpSalePickExportRespVO {

    @ExcelProperty("销售单号")
    private String saleOutNo;
    @ExcelProperty("客户")
    private String customerName;
    @ExcelProperty("仓库")
    private String warehouseName;
    @ExcelProperty("拣货状态")
    private Integer status;
    @ExcelProperty("拣货进度")
    private String pickProgress;
    @ExcelProperty("最近拣货时间")
    private LocalDateTime latestPickTime;
    @ExcelProperty("完成时间")
    private LocalDateTime completeTime;

}
