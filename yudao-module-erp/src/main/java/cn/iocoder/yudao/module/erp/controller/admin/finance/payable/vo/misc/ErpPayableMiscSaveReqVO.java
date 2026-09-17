package cn.iocoder.yudao.module.erp.controller.admin.finance.payable.vo.misc;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;
import java.math.BigDecimal;

@Schema(description = "ERP 其他应付新增/修改 Request VO")
@Data
@Accessors(chain = true)
public class ErpPayableMiscSaveReqVO {

    @Schema(description = "编号")
    private Long id;

    @Schema(description = "供应商编号")
    @NotNull(message = "供应商不能为空")
    private Long supplierId;

    @Schema(description = "账户编号")
    @NotNull(message = "账户不能为空")
    private Long accountId;

    @Schema(description = "金额")
    @NotNull(message = "金额不能为空")
    private BigDecimal amount;

    @Schema(description = "备注")
    private String remark;

    @Schema(description = "附件 URL")
    private String fileUrl;

    @Schema(description = "部门编号")
    @NotNull(message = "部门不能为空")
    private Long deptId;

    @Schema(description = "经手人编号")
    private Long handlerId;

}
