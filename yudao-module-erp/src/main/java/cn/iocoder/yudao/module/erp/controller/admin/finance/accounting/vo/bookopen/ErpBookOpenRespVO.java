package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.bookopen;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Schema(description = "管理后台 - ERP 系统开账 Response VO")
@Data
public class ErpBookOpenRespVO {

    @Schema(description = "开账主键", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    private Long id;

    @Schema(description = "开账编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "KZ20260514000001")
    private String no;

    @Schema(description = "连锁名称", example = "总部")
    private String chainName;

    @Schema(description = "会计年度", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026")
    private Integer fiscalYear;

    @Schema(description = "开账期间（兼容旧字段；年度开账固定为 1）", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    private Integer period;

    @Schema(description = "期间开始时间", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-01-01")
    private LocalDate startDate;

    @Schema(description = "是否开账", requiredMode = Schema.RequiredMode.REQUIRED, example = "true")
    private Boolean opened;

    @Schema(description = "操作时间", example = "2026-05-14 10:00:00")
    private LocalDateTime operateTime;

    @Schema(description = "操作人姓名", example = "张三")
    private String operator;

    @Schema(description = "操作人 ID", example = "1024")
    private Long operatorUserId;

    @Schema(description = "凭证类型勾选配置")
    private List<ErpBookOpenVoucherConfigRespVO> voucherConfigs;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    private LocalDateTime createTime;

}
