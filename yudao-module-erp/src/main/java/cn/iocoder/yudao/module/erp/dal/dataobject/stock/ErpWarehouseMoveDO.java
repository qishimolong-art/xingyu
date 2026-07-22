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
 * ERP warehouse move document.
 */
@TableName("erp_warehouse_move")
@KeySequence("erp_warehouse_move_seq")
@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErpWarehouseMoveDO extends BaseDO {

    @TableId
    private Long id;

    private String no;

    private Long deptId;

    private LocalDateTime moveTime;

    private Long fromWarehouseId;

    private Long toWarehouseId;

    private Long handlerId;

    private Integer sourceType;

    private Long sourceId;

    private String sourceNo;

    private BigDecimal totalCount;

    private BigDecimal totalPrice;

    private BigDecimal totalCostAmount;

    /**
     * See {@link cn.iocoder.yudao.module.erp.enums.ErpAuditStatus}.
     */
    private Integer status;

    private Long approveUserId;

    private LocalDateTime approveTime;

    private String remark;

    private String fileUrl;

}
