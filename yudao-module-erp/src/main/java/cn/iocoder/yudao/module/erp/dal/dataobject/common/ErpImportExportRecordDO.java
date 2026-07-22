package cn.iocoder.yudao.module.erp.dal.dataobject.common;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

/**
 * ERP import/export main record.
 */
@TableName("erp_import_export_record")
@KeySequence("erp_import_export_record_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpImportExportRecordDO extends BaseDO {

    @TableId
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

}
