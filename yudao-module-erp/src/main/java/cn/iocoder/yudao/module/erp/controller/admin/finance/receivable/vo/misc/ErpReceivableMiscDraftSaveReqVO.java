package cn.iocoder.yudao.module.erp.controller.admin.finance.receivable.vo.misc;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import java.math.BigDecimal;

@Schema(description = "ERP 其他应收草稿保存 Request VO")
@Data
@Accessors(chain = true)
public class ErpReceivableMiscDraftSaveReqVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "客户编号")
    private Long customerId;

    @Schema(description = "账户编号")
    private Long accountId;

    @Schema(description = "金额")
    private BigDecimal amount;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "附件 URL")
    private String fileUrl;

    @Schema(description = "部门编号")
    private Long deptId;

    @Schema(description = "经手人编号")
    private Long handlerId;

}
