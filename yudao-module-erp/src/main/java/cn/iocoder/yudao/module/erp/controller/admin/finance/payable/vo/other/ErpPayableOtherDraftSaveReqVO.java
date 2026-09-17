package cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.other;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "ERP 应付调账草稿保存 Request VO")
@Data
public class ErpPayableOtherDraftSaveReqVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "业务日期")
    private LocalDate bizTime;

    @Schema(description = "供应商编号")
    private Long supplierId;

    @Schema(description = "凭证号")
    private String voucherNo;

    @Schema(description = "已结金额")
    private BigDecimal settledAmount;

    @Schema(description = "部门编号")
    private Long deptId;

    @Schema(description = "应付金额")
    private BigDecimal payableAmount;

    @Schema(description = "调账项目")
    private String project;

    @Schema(description = "来源类型")
    private String sourceType;

    @Schema(description = "经手人编号")
    private Long handlerId;

    @Schema(description = "调账原因备注")
    private String remark;

    @Schema(description = "附件 URL")
    private String fileUrl;

}
