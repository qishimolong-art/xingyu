package cn.iocoder.yudao.module.erp.controller.admin.product.vo.archive;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class ErpArchiveImportTaskRespVO {

    private Long taskId;
    private Long recordId;
    private String status;
    private String stage;
    private String stageName;
    private Integer progressPercent;
    private Integer totalCount;
    private Integer processedCount;
    private Integer successCount;
    private Integer failureCount;
    private Integer createCount;
    private Integer updateCount;
    private String fileName;
    private String operatorName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long durationMs;
    private String errorMessage;
    private List<ErpArchiveImportFailureRespVO> failureDetails = new ArrayList<>();

}
