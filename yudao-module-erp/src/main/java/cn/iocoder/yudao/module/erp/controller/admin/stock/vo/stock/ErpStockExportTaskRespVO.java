package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.stock;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - 产品库存异步导出任务 Response VO")
@Data
public class ErpStockExportTaskRespVO {

    private Long taskId;
    private String status;
    private String stage;
    private String stageName;
    private Integer progressPercent;
    private Integer totalCount;
    private Integer processedCount;
    private Integer exportedCount;
    private String fileName;
    private String operatorName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;
    private String errorMessage;
    private Boolean downloadAvailable;
    private LocalDateTime expiresAt;

}
