package cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * ERP 三大报表项目模板 DO
 *
 * 用于资产负债表 / 利润表 / 现金流量表的项目结构与取数公式（公式本期留空，后期与财务确认填）。
 *
 * @author Claude
 */
@TableName("erp_report_item_template")
@KeySequence("erp_report_item_template_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpReportItemTemplateDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 报表类型：1=资产负债表 2=利润表 3=现金流量表
     */
    private Integer reportType;
    /**
     * 区域：1=左侧 2=右侧（资产负债表/现金流量表）
     */
    private Integer side;
    /**
     * 项目名称
     */
    private String itemName;
    /**
     * 行次
     */
    private Integer rowNo;
    /**
     * 取数公式（先空，后期填）
     */
    private String formula;
    /**
     * 父项 ID（树形）
     */
    private Long parentId;
    /**
     * 排序
     */
    private Integer sort;
    /**
     * 是否启用
     */
    private Boolean enable;
    /**
     * 备注
     */
    private String remark;

}
