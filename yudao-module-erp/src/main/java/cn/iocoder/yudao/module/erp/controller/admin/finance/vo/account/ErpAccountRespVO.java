package cn.iocoder.yudao.module.erp.controller.admin.finance.vo.account;

import cn.iocoder.yudao.framework.excel.core.annotations.DictFormat;
import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "管理后台 - ERP 结算账户 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpAccountRespVO {

    @Schema(description = "结算账户编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "28684")
    @ExcelProperty("结算账户编号")
    private Long id;

    @Schema(description = "账户名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "基本户")
    @ExcelProperty("账户名称")
    private String name;

    @Schema(description = "账户类型", example = "1")
    @ExcelProperty("账户类型")
    @DictFormat(cn.iocoder.yudao.module.erp.enums.DictTypeConstants.ACCOUNT_TYPE)
    private Integer accountType;

    @Schema(description = "开户行", example = "中国银行")
    @ExcelProperty("开户行")
    private String bankName;

    @Schema(description = "银行账号", example = "622202************")
    @ExcelProperty("银行账号")
    private String bankAccount;

    @Schema(description = "账户编码", example = "A88")
    @ExcelProperty("账户编码")
    private String no;

    @Schema(description = "所属部门", example = "100")
    private Long deptId;

    @Schema(description = "所属部门名称", example = "财务部")
    @ExcelProperty("所属部门")
    private String deptName;

    @Schema(description = "备注", example = "备注")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "启用状态", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("启用状态")
    @DictFormat(cn.iocoder.yudao.module.system.enums.DictTypeConstants.COMMON_STATUS)
    private Integer status;

    @Schema(description = "排序", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("排序")
    private Integer sort;

    @Schema(description = "是否默认", example = "true")
    @ExcelProperty("是否默认")
    private Boolean defaultStatus;

    @Schema(description = "当前余额", example = "1000.00")
    @ExcelProperty("当前余额")
    private BigDecimal currentBalance;

    @Schema(description = "创建人", example = "1")
    private String creator;

    @Schema(description = "创建人名称", example = "管理员")
    @ExcelProperty("创建人")
    private String creatorName;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    @Schema(description = "修改人", example = "1")
    private String updater;

    @Schema(description = "修改人名称", example = "管理员")
    @ExcelProperty("修改人")
    private String updaterName;

    @Schema(description = "修改时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("修改时间")
    private LocalDateTime updateTime;

}
