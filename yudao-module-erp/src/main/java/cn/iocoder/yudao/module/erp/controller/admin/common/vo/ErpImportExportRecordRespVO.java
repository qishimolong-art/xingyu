package cn.iocoder.yudao.module.erp.controller.admin.common.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 导入导出记录 Response VO")
@Data
public class ErpImportExportRecordRespVO {

    private Long id;
    private String operationType;
    private String moduleKey;
    private String moduleName;
    private String fileName;
    private String fileType;
    private String status;
    private Integer totalCount;
    private Integer successCount;
    private Integer failureCount;
    private Integer createCount;
    private Integer updateCount;
    private String queryParams;
    private String exportFields;
    private String errorMessage;
    private Long durationMs;
    private Long operatorId;
    private String operatorName;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private LocalDateTime createTime;

}
