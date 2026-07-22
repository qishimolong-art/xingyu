package cn.iocoder.yudao.module.erp.controller.admin.common.vo;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

import static cn.iocoder.yudao.framework.common.util.date.DateUtils.FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND;

@Schema(description = "管理后台 - ERP 导入导出记录分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpImportExportRecordPageReqVO extends PageParam {

    @Schema(description = "操作类型", example = "IMPORT")
    private String operationType;

    @Schema(description = "业务模块", example = "erp_product")
    private String moduleKey;

    @Schema(description = "状态", example = "SUCCESS")
    private String status;

    @Schema(description = "文件名", example = "parts.xlsx")
    private String fileName;

    @Schema(description = "操作人编号", example = "1")
    private Long operatorId;

    @Schema(description = "创建时间")
    @DateTimeFormat(pattern = FORMAT_YEAR_MONTH_DAY_HOUR_MINUTE_SECOND)
    private LocalDateTime[] createTime;

}
