package cn.iocoder.yudao.module.erp.controller.admin.finance.accounting.vo.subject;

import cn.idev.excel.annotation.ExcelProperty;
import cn.iocoder.yudao.framework.excel.core.annotations.ExcelRequired;
import lombok.Data;

import java.math.BigDecimal;

/**
 * ERP 会计科目导入 Excel VO
 *
 * 导入策略：按 subjectCode 匹配现有科目；
 *   - 已存在：根据 mode (OVERWRITE/SKIP) 决定是否覆盖
 *   - 不存在：根据其他字段补齐后新增
 */
@Data
public class ErpAccountingSubjectImportExcelVO {

    @ExcelRequired
    @ExcelProperty("科目编码")
    private String subjectCode;

    @ExcelProperty("科目名称")
    private String subjectName;

    @ExcelProperty("科目别名")
    private String shortName;

    /** 科目类型（1-资产 2-负债 3-共同 4-权益 5-成本 6-损益），允许填中文或数字 */
    @ExcelProperty("科目类型")
    private String subjectCategory;

    @ExcelProperty("父科目编码")
    private String parentCode;

    /** 余额方向（1-借 2-贷），允许填"借/贷"或数字 */
    @ExcelProperty("余额方向")
    private String balanceDirection;

    @ExcelProperty("期初余额")
    private BigDecimal openingBalance;

    /** 凭证类型（1-客户 2-连锁），允许填中文或数字 */
    @ExcelProperty("凭证类型")
    private String voucherType;

}
