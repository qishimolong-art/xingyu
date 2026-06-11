package cn.iocoder.yudao.module.erp.controller.admin.common;

import cn.iocoder.yudao.framework.common.exception.ErrorCode;

import static cn.iocoder.yudao.framework.common.exception.util.ServiceExceptionUtil.exception;

public final class ErpAuditStatusRequestValidator {

    private static final Integer APPROVE_STATUS = 20;

    private static final ErrorCode APPROVE_STATUS_REQUIRED =
            new ErrorCode(1_030_000_002, "Only approve status is allowed");

    private ErpAuditStatusRequestValidator() {
    }

    public static void validateApproveStatus(Integer status) {
        if (!APPROVE_STATUS.equals(status)) {
            throw exception(APPROVE_STATUS_REQUIRED);
        }
    }

}
