package cn.iocoder.yudao.module.erp.service.common.bo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ErpImportExportRecordCreateReqBO {

    private String operationType;
    private String moduleKey;
    private String moduleName;
    private String templateKey;
    private String fileName;
    private String fileType;
    private String queryParams;
    private String exportFields;
    private LocalDateTime startTime;

}
