package cn.iocoder.yudao.module.erp.dal.dataobject.stock;

import cn.iocoder.yudao.framework.mybatis.core.dataobject.BaseDO;
import com.baomidou.mybatisplus.annotation.KeySequence;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * ERP stock move document.
 */
@TableName("erp_stock_move")
@KeySequence("erp_stock_move_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpStockMoveDO extends BaseDO {

    @TableId
    private Long id;

    private String no;

    private Long deptId;

    /**
     * Transfer direction: 10 = transfer out, 20 = transfer in.
     */
    private Integer transferDirection;

    private Long relatedMoveId;

    private String relatedMoveNo;

    private Long fromDeptId;

    private Long toDeptId;

    private LocalDateTime moveTime;

    private Integer sourceType;

    private Long sourceId;

    private String sourceNo;

    private BigDecimal totalCount;

    private BigDecimal totalPrice;

    /**
     * Transfer-out records use
     * {@link cn.iocoder.yudao.module.erp.enums.stock.ErpStockTransferOutStatusEnum}.
     */
    private Integer status;

    private Long approveUserId;

    private LocalDateTime approveTime;

    private String remark;

    private String fileUrl;

}
