package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.subject;

import cn.idev.excel.annotation.ExcelIgnoreUnannotated;
import cn.idev.excel.annotation.ExcelProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Schema(description = "管理后台 - ERP 会计科目 Response VO")
@Data
@ExcelIgnoreUnannotated
public class ErpAccountingSubjectRespVO {

    @Schema(description = "主键编号", requiredMode = Schema.RequiredMode.REQUIRED, example = "1024")
    @ExcelProperty("主键编号")
    private Long id;

    @Schema(description = "科目编码", requiredMode = Schema.RequiredMode.REQUIRED, example = "1001")
    @ExcelProperty("科目编码")
    private String subjectCode;

    @Schema(description = "科目名称", requiredMode = Schema.RequiredMode.REQUIRED, example = "库存现金")
    @ExcelProperty("科目名称")
    private String subjectName;

    @Schema(description = "简称", example = "现金")
    @ExcelProperty("简称")
    private String shortName;

    @Schema(description = "科目大类：1-资产 2-负债 3-共同 4-权益 5-成本 6-损益", requiredMode = Schema.RequiredMode.REQUIRED, example = "1")
    @ExcelProperty("科目大类")
    private Integer subjectCategory;

    @Schema(description = "父科目编码", example = "1001")
    @ExcelProperty("父科目编码")
    private String parentCode;

    @Schema(description = "层级", example = "1")
    @ExcelProperty("层级")
    private Integer subjectLevel;

    @Schema(description = "是否末级", example = "true")
    @ExcelProperty("是否末级")
    private Boolean isLeaf;

    @Schema(description = "余额方向：1-借 2-贷", example = "1")
    @ExcelProperty("余额方向")
    private Integer balanceDirection;

    @Schema(description = "凭证类型：1-客户 2-连锁", example = "1")
    @ExcelProperty("凭证类型")
    private Integer voucherType;

    @Schema(description = "期初余额", example = "1000.00")
    @ExcelProperty("期初余额")
    private BigDecimal openingBalance;

    @Schema(description = "是否启用", example = "true")
    @ExcelProperty("是否启用")
    private Boolean enable;

    @Schema(description = "排序", example = "1")
    @ExcelProperty("排序")
    private Integer sort;

    @Schema(description = "备注", example = "随便")
    @ExcelProperty("备注")
    private String remark;

    @Schema(description = "创建时间", requiredMode = Schema.RequiredMode.REQUIRED)
    @ExcelProperty("创建时间")
    private LocalDateTime createTime;

    /**
     * 该科目挂的辅助核算类型列表（凭证录入时按此带出辅助核算项）
     */
    @Schema(description = "辅助核算类型列表", example = "[\"supplier\",\"dept\"]")
    private List<String> auxiliaryTypes = new ArrayList<>();

    /**
     * 子科目列表（树形展示时使用）
     */
    @Schema(description = "子科目列表")
    private List<ErpAccountingSubjectRespVO> children = new ArrayList<>();

}
