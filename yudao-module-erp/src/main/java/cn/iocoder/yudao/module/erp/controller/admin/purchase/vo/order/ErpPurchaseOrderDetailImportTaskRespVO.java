package cn.iocoder.yudao.module.erp.controller.admin.purchase.vo.order;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ErpPurchaseOrderDetailImportTaskRespVO {
    private Long taskId;
    private Long recordId;
    private String formSessionId;
    private String status;
    private String stage;
    private String stageName;
    private Integer progressPercent;
    private Integer totalCount;
    private Integer processedCount;
    private Integer successCount;
    private Integer failureCount;
    private String fileName;
    private String operatorName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;
    private String errorMessage;
    private Boolean resultAvailable;
    private LocalDateTime expiresAt;
    private List<ErpPurchaseOrderImportRespVO.FailureItem> failureDetails = new ArrayList<>();
}
