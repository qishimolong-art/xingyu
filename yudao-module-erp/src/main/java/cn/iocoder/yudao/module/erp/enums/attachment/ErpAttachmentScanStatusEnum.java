package cn.iocoder.yudao.module.erp.enums.attachment;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * ERP 扫码上传附件会话状态。
 */
@Getter
@AllArgsConstructor
public enum ErpAttachmentScanStatusEnum {

    WAITING(10, "待上传"),
    UPLOADED(20, "已上传"),
    EXPIRED(30, "已过期"),
    CANCELED(40, "已取消");

    private final Integer status;
    private final String name;

}
