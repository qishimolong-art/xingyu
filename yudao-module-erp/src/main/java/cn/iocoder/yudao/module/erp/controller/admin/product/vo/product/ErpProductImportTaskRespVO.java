package cn.iocoder.yudao.module.erp.controller.admin.product.vo.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Schema(description = "管理后台 - ERP 配件异步导入任务 Response VO")
public class ErpProductImportTaskRespVO {

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
    private List<ErpProductImportRespVO.FailureItem> failureDetails = new ArrayList<>();

}
