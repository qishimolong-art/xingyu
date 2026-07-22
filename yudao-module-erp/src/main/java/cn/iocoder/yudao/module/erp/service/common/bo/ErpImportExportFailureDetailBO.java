package cn.iocoder.yudao.module.erp.service.common.bo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Import failure detail.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ErpImportExportFailureDetailBO {

    private Integer rowNo;
    private String bizKey;
    private String bizName;
    private String failureReason;
    private Object rawData;

}
