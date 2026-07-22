package cn.iocoder.yudao.module.erp.controller.admin.common.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 导入导出记录明细 Response VO")
@Data
public class ErpImportExportRecordDetailRespVO {

    private Long id;
    private Long recordId;
    private Integer rowNo;
    private String bizKey;
    private String bizName;
    private String failureReason;
    private String rawData;
    private LocalDateTime createTime;

}
