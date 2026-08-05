package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.account;

import cn.iocoder.yudao.framework.common.enums.CommonStatusEnum;
import cn.iocoder.yudao.framework.common.validation.InEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Schema(description = "管理后台 - ERP 结算账户新增/修改 Request VO")
@Data
public class ErpAccountSaveReqVO {

    @Schema(description = "结算账户编号", example = "28684")
    private Long id;

    @Schema(description = "账户名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "基本户")
    @NotBlank(message = "账户名称不能为空")
    private String name;

    @Schema(description = "账户类型", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "账户类型不能为空")
    private Integer accountType;

    @Schema(description = "开户行", example = "中国银行")
    @Size(max = 128, message = "开户行长度不能超过 128 个字符")
    private String bankName;

    @Schema(description = "银行账号", example = "622202************")
    @Size(max = 64, message = "银行账号长度不能超过 64 个字符")
    private String bankAccount;

    @Schema(description = "账户编码", example = "A88")
    private String no;

    @Schema(description = "所属部门", example = "100")
    private Long deptId;

    @Schema(description = "备注", example = "备注")
    private String remark;

    @Schema(description = "启用状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "启用状态不能为空")
    @InEnum(value = CommonStatusEnum.class)
    private Integer status;

    @Schema(description = "排序", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "排序不能为空")
    private Integer sort;

    @Schema(description = "是否默认", example = "false")
    private Boolean defaultStatus;

}
