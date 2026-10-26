package cn.iocoder.yudao.module.erp.controller.admin.stock.vo.warehouse;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Schema(description = "管理后台 - ERP 仓库异步导出任务 Response VO")
public class ErpWarehouseExportTaskRespVO {

    private Long taskId;
    private String status;
    private String stage;
    private String stageName;
    private Integer progressPercent;
    private Integer totalCount;
    private Integer processedCount;
    private Integer exportedCount;
    private String fileName;
    private List<String> selectedFields;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;
    private String errorMessage;
    private Boolean downloadAvailable;
    private LocalDateTime expiresAt;

}
