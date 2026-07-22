package cn.iocoder.yudao.module.erp.enums.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * ERP import/export record status.
 */
@Getter
@AllArgsConstructor
public enum ErpImportExportRecordStatusEnum {

    PROCESSING("PROCESSING", "处理中"),
    SUCCESS("SUCCESS", "成功"),
    PARTIAL_SUCCESS("PARTIAL_SUCCESS", "部分成功"),
    FAILURE("FAILURE", "失败");

    private final String status;
    private final String name;

    public static String ofCount(Integer successCount, Integer failureCount) {
        int success = successCount == null ? 0 : successCount;
        int failure = failureCount == null ? 0 : failureCount;
        if (failure <= 0) {
            return SUCCESS.getStatus();
        }
        if (success > 0) {
            return PARTIAL_SUCCESS.getStatus();
        }
        return FAILURE.getStatus();
    }

}
