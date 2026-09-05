package cn.iocoder.yudao.module.erp.service.common.bo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ErpImportExportRecordFinishReqBO {

    private String fileName;
    private String fileType;
    private String templateKey;
    private String status;
    private Integer totalCount;
    private Integer successCount;
    private Integer failureCount;
    private Integer createCount;
    private Integer updateCount;
    private String exportFields;
    private String errorMessage;
    private Long durationMs;
    private LocalDateTime endTime;
    private List<ErpImportExportFailureDetailBO> failureDetails;

}
