package cn.iocoder.yudao.module.erp.controller.admin.mall.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 分类同步商城状态 Response VO")
@Data
public class ErpMallCategorySyncStatusRespVO {

    @Schema(description = "ERP 分类编号", example = "1001")
    private Long erpCategoryId;

    @Schema(description = "商城分类编号", example = "2001")
    private Long mallCategoryId;

    @Schema(description = "同步状态：0 成功，1 失败", example = "0")
    private Integer syncStatus;

    @Schema(description = "最近同步时间")
    private LocalDateTime lastSyncTime;

    @Schema(description = "失败原因")
    private String failReason;

}
