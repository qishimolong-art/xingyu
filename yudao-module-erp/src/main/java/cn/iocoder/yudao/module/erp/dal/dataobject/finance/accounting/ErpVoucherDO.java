package cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * ERP 凭证主表 DO
 *
 * @author Claude
 */
@TableName("erp_voucher")
@KeySequence("erp_voucher_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpVoucherDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 凭证字（默认 "记"）
     */
    private String voucherWord;
    /**
     * 凭证编号（如 "记-202605-000001"）
     */
    private String voucherNo;
    /**
     * 凭证日期
     */
    private LocalDate voucherDate;
    /**
     * 归属年（跨期归属用）
     */
    private Integer periodYear;
    /**
     * 归属月（1-12）
     */
    private Integer periodMonth;
    /**
     * 附件张数
     */
    private Integer attachmentCount;
    /**
     * 摘要（取第一行分录摘要）
     */
    private String summary;
    /**
     * 借方合计
     */
    private BigDecimal totalDebit;
    /**
     * 贷方合计
     */
    private BigDecimal totalCredit;
    /**
     * 审核状态
     *
     * 枚举 {@link cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherAuditStatusEnum}
     */
    private Integer auditStatus;
    /**
     * 凭证来源类型
     *
     * 枚举 {@link cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherSourceTypeEnum}
     */
    private Integer sourceType;
    /**
     * 业务来源类型
     *
     * 枚举 {@link cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherTypeEnum}
     */
    private Integer sourceBizType;
    /**
     * 业务单据 ID
     */
    private Long sourceBizId;
    /**
     * 业务单号（冗余）
     */
    private String sourceBizNo;
    /**
     * 业务单据金额
     */
    private BigDecimal sourceBizAmount;
    /**
     * 是否产生业务单据
     */
    private Boolean generateBusinessDoc;
    /**
     * 辅助：项目 ID
     */
    private Long projectId;
    /**
     * 辅助：部门 ID
     */
    private Long deptId;
    /**
     * 辅助：客户 ID
     */
    private Long customerId;
    /**
     * 辅助：个人（用户 ID）
     */
    private Long personUserId;
    /**
     * 制单人 ID
     */
    private Long makerUserId;
    /**
     * 制单人姓名
     */
    private String makerUserName;
    /**
     * 记账人
     */
    private String bookkeeper;
    /**
     * 出纳
     */
    private String cashier;
    /**
     * 主管
     */
    private String supervisor;
    /**
     * 审核人 ID
     */
    private Long auditorUserId;
    /**
     * 审核人姓名
     */
    private String auditorUserName;
    /**
     * 审核日期
     */
    private LocalDateTime auditTime;
    /**
     * 备注
     */
    private String remark;

}
