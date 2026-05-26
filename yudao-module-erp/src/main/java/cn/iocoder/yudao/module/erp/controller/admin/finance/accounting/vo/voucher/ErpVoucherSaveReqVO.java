package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.voucher;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

@Schema(description = "管理后台 - ERP 凭证 新增/修改 Request VO")
@Data
public class ErpVoucherSaveReqVO {

    @Schema(description = "凭证编号", example = "10")
    private Long id;

    @Schema(description = "凭证字（默认 '记'）", example = "记")
    private String voucherWord;

    @Schema(description = "凭证日期", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-05-14")
    @NotNull(message = "凭证日期不能为空")
    private LocalDate voucherDate;

    @Schema(description = "归属年（不传则按 voucherDate 推断）", example = "2026")
    private Integer periodYear;

    @Schema(description = "归属月（不传则按 voucherDate 推断）", example = "5")
    private Integer periodMonth;

    @Schema(description = "附件张数", example = "0")
    private Integer attachmentCount;

    @Schema(description = "摘要（不传则取首行分录摘要）", example = "差旅费报销")
    private String summary;

    @Schema(description = "辅助：项目 ID", example = "1024")
    private Long projectId;

    @Schema(description = "辅助：部门 ID", example = "1024")
    private Long deptId;

    @Schema(description = "辅助：客户 ID", example = "1024")
    private Long customerId;

    @Schema(description = "辅助：个人（用户 ID）", example = "1024")
    private Long personUserId;

    @Schema(description = "记账人", example = "张三")
    private String bookkeeper;

    @Schema(description = "记账人用户 ID", example = "100")
    private Long bookkeeperUserId;

    @Schema(description = "出纳", example = "李四")
    private String cashier;

    @Schema(description = "出纳用户 ID", example = "100")
    private Long cashierUserId;

    @Schema(description = "主管", example = "王五")
    private String supervisor;

    @Schema(description = "主管用户 ID", example = "100")
    private Long supervisorUserId;

    @Schema(description = "审核人用户 ID", example = "100")
    private Long auditorUserId;

    @Schema(description = "业务单据类型（银行转账/代垫运费）", example = "银行转账")
    private String bizDocType;

    @Schema(description = "备注", example = "你猜")
    private String remark;

    @Schema(description = "凭证分录列表", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty(message = "凭证分录列表不能为空")
    @Valid
    private List<ErpVoucherItemSaveReqVO> items;

}
