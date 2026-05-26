package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.subject;

import cn.iocoder.yudao.framework.common.validation.InEnum;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpSubjectCategoryEnum;
import cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpSubjectVoucherTypeEnum;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.List;

@Schema(description = "管理后台 - ERP 会计科目新增/修改 Request VO")
@Data
public class ErpAccountingSubjectSaveReqVO {

    @Schema(description = "主键编号", example = "1024")
    private Long id;

    @Schema(description = "科目编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @NotEmpty(message = "科目编码不能为空")
    private String subjectCode;

    @Schema(description = "科目名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "库存现金")
    @NotEmpty(message = "科目名称不能为空")
    private String subjectName;

    @Schema(description = "简称", example = "现金")
    private String shortName;

    @Schema(description = "科目大类：1-资产 2-负债 3-共同 4-权益 5-成本 6-损益", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @NotNull(message = "科目大类不能为空")
    @InEnum(value = ErpSubjectCategoryEnum.class)
    private Integer subjectCategory;

    @Schema(description = "父科目编码", example = "1001")
    private String parentCode;

    @Schema(description = "层级", example = "1")
    private Integer subjectLevel;

    @Schema(description = "余额方向：1-借 2-贷", example = "1")
    private Integer balanceDirection;

    @Schema(description = "凭证类型：1-客户 2-连锁", example = "1")
    @InEnum(value = ErpSubjectVoucherTypeEnum.class)
    private Integer voucherType;

    @Schema(description = "期初余额", example = "1000.00")
    private BigDecimal openingBalance;

    @Schema(description = "是否启用", example = "true")
    private Boolean enable;

    @Schema(description = "排序", example = "1")
    private Integer sort;

    @Schema(description = "备注", example = "随便")
    private String remark;

    /**
     * 辅助核算类型列表（与科目挂的核算维度，可选）
     */
    @Schema(description = "辅助核算类型列表", example = "[\"supplier\",\"dept\"]")
    private List<String> auxiliaryTypes;

}
