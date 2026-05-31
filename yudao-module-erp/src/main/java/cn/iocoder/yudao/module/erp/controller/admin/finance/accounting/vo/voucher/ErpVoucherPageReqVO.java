package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.voucher;

import cn.iocoder.yudao.framework.common.pojo.PageParam;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - ERP 凭证分页 Request VO")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ErpVoucherPageReqVO extends PageParam {

    @Schema(description = "勾选导出的凭证编号数组", example = "[1,2,3]")
    private List<Long> ids;

    @Schema(description = "凭证编号", example = "记-202605-000001")
    private String voucherNo;

    @Schema(description = "凭证字", example = "记")
    private String voucherWord;

    @Schema(description = "凭证日期范围", example = "['2026-05-01','2026-05-31']")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate[] voucherDate;

    @Schema(description = "归属年", example = "2026")
    private Integer periodYear;

    @Schema(description = "归属月", example = "5")
    private Integer periodMonth;

    @Schema(description = "审核状态", example = "10")
    private Integer auditStatus;

    @Schema(description = "凭证来源类型", example = "1")
    private Integer sourceType;

    @Schema(description = "业务来源类型", example = "1")
    private Integer sourceBizType;

    @Schema(description = "制单人 ID", example = "100")
    private Long makerUserId;

    @Schema(description = "审核人 ID", example = "100")
    private Long auditorUserId;

    @Schema(description = "摘要（模糊匹配）", example = "差旅")
    private String summary;

    @Schema(description = "备注", example = "你猜")
    private String remark;

}
