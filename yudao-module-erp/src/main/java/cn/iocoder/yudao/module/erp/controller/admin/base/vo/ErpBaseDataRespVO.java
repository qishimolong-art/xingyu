package cn.iocoder.yudao.module.erp.controller.admin.base.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 基础数据 Response VO")
@Data
public class ErpBaseDataRespVO {

    @Schema(description = "编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "数据类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "region")
    private String type;

    @Schema(description = "名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "华东")
    private String name;

    @Schema(description = "稳定业务编码", example = "cash")
    private String code;

    @Schema(description = "排序", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer sort;

    @Schema(description = "状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "0")
    private Integer status;

    @Schema(description = "备注", example = "备注信息")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
