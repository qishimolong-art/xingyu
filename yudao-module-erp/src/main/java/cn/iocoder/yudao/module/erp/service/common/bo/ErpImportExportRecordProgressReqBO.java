package cn.iocoder.yudao.module.erp.service.common.bo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ErpImportExportRecordProgressReqBO {

    private String stage;
    private Integer totalCount;
    private Integer processedCount;
    private Integer successCount;
    private Integer failureCount;
    private Integer createCount;
    private Integer updateCount;
    private String queryParams;
    private Long durationMs;
    private LocalDateTime heartbeatTime;

}
