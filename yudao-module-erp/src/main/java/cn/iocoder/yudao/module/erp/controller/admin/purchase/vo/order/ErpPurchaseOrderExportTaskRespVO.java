package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ErpPurchaseOrderExportTaskRespVO {
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
