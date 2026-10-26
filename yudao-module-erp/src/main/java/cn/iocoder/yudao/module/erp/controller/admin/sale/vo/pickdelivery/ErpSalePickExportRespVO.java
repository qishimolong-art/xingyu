package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ErpSalePickExportRespVO {

    @ExcelProperty("业务单号")
    private String displayNo;
    @ExcelProperty("拣货来源")
    private String sourceTypeName;
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
