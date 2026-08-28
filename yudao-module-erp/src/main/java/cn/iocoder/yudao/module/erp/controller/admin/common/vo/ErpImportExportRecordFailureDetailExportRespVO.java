package cn.iocoder.yudao.module.erp.controller.admin.common.vo;

import cn.idev.excel.annotation.ExcelProperty;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ErpImportExportRecordFailureDetailExportRespVO {

    @ExcelProperty("行号")
    private Integer rowNo;

    @ExcelProperty("业务标识")
    private String bizKey;

    @ExcelProperty("业务名称")
    private String bizName;

    @ExcelProperty("失败原因")
    private String failureReason;

    @ExcelProperty("原始数据")
    private String rawData;

    @ExcelProperty("记录时间")
    private LocalDateTime createTime;

}
