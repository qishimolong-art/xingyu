package cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ERP 系统开账主表 DO
 *
 * @author Claude
 */
@TableName("erp_book_open")
@KeySequence("erp_book_open_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpBookOpenDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 开账编号（前缀 KZ）
     */
    private String no;
    /**
     * 连锁名称（仅文本）
     */
    private String chainName;
    /**
     * 会计年度
     */
    private Integer fiscalYear;
    /**
     * 开账期间（1-12 月）
     */
    private Integer period;
    /**
     * 期间开始时间
     */
    private LocalDate startDate;
    /**
     * 是否开账：false=未开账 true=已开账
     */
    private Boolean opened;
    /**
     * 操作时间
     */
    private LocalDateTime operateTime;
    /**
     * 操作人姓名（冗余）
     */
    private String operator;
    /**
     * 操作人 ID
     */
    private Long operatorUserId;

}
