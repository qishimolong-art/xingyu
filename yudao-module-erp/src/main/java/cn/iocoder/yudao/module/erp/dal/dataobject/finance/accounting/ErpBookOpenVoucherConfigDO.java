package cn.iocoder.yudao.module.erp.dal.dataobject.finance.accounting;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.*;

/**
 * ERP 开账凭证类型勾选 DO
 *
 * 与 {@link ErpBookOpenDO} 1:N，记录某次开账"自动生成凭证配置区"的 11 种凭证类型勾选状态。
 *
 * @author Claude
 */
@TableName("erp_book_open_voucher_config")
@KeySequence("erp_book_open_voucher_config_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpBookOpenVoucherConfigDO extends BaseDO {

    @TableId
    private Long id;
    /**
     * 关联 {@link ErpBookOpenDO#getId()}
     */
    private Long bookOpenId;
    /**
     * 凭证业务类型
     *
     * 枚举 {@link cn.iocoder.yudao.module.erp.enums.finance.accounting.ErpVoucherTypeEnum}
     */
    private Integer voucherType;
    /**
     * 是否启用生成
     */
    private Boolean enabled;
    /**
     * 排序
     */
    private Integer sort;

}
