package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.account;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.Size;

@Schema(description = "管理后台 - ERP 结算账户草稿保存 Request VO")
@Data
public class ErpAccountDraftSaveReqVO {

    @Schema(description = "结算账户编号", example = "28684")
    private Long id;

    @Schema(description = "账户名称", example = "基本户")
    private String name;

    @Schema(description = "账户类型", example = "1")
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

    @Schema(description = "启用状态", example = "0")
    private Integer status;

    @Schema(description = "排序", example = "1")
    private Integer sort;

    @Schema(description = "是否默认", example = "false")
    private Boolean defaultStatus;

}
