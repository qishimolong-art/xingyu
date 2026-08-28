package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.bookopen;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;

@Schema(description = "管理后台 - ERP 系统开账新增/修改 Request VO")
@Data
public class ErpBookOpenSaveReqVO {

    @Schema(description = "开账主键", example = "1024")
    private Long id;

    @Schema(description = "连锁名称（仅文本）", example = "总部")
    private String chainName;

    @Schema(description = "会计年度", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026")
    @NotNull(message = "会计年度不能为空")
    @Min(value = 1900, message = "会计年度不合法")
    private Integer fiscalYear;

    @Schema(description = "开账期间（兼容旧字段；年度开账固定保存为 1）", example = "1")
    private Integer period;

    @Schema(description = "期间开始时间（兼容旧字段；年度开账固定保存为当年 1 月 1 日）", example = "2026-01-01")
    private LocalDate startDate;

}
