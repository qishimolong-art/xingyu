package cn.iocoder.yudao.module.erp.controller.admin.sale.vo.pickdelivery;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ErpSaleDeliveryExportRespVO {

    @ExcelProperty("销售单号")
    private String saleOutNo;
    @ExcelProperty("客户")
    private String customerName;
    @ExcelProperty("拣货状态")
    private Integer pickStatus;
    @ExcelProperty("送货状态")
    private Integer deliveryStatus;
    @ExcelProperty("拣货进度")
    private String pickProgress;
    @ExcelProperty("送货进度")
    private String deliveryProgress;
    @ExcelProperty("最近送货时间")
    private LocalDateTime latestDeliveryTime;
    @ExcelProperty("送货完成时间")
    private LocalDateTime deliveryCompleteTime;

}
