package cn.iocoder.yudao.module.erp.controller.admin.base.vo.recyclebin;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 基础档案回收站 Response VO")
@Data
public class ErpRecycleBinRespVO {

    @Schema(description = "来源类型", example = "supplier")
    private String sourceType;

    @Schema(description = "来源编号", example = "1001")
    private Long sourceId;

    @Schema(description = "编码", example = "GYS000001")
    private String code;

    @Schema(description = "名称", example = "广州供应商")
    private String name;

    @Schema(description = "停用人用户编号", example = "1")
    private Long disabledBy;

    @Schema(description = "停用人名称", example = "管理员")
    private String disabledByName;

    @Schema(description = "停用时间")
    private LocalDateTime disabledTime;

}
